import {AlertTriangle, BellRing, Check, ChefHat, Clock3, Minus, Plus, Search, Send, ShoppingBasket, UtensilsCrossed, X} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {api} from '../api';
import type {DiningOrder, DiningOrderItemStatus, MenuItem, Reservation} from '../types';
import {useOperationalEvents} from '../hooks/useOperationalEvents';

const itemLabel: Record<DiningOrderItemStatus, string> = {
  SUBMITTED: 'Chờ bếp nhận',
  PREPARING: 'Đang nấu',
  DELAYED: 'Món đang chậm',
  READY: 'Bếp đã báo xong',
  SERVED: 'Đã mang lên',
  CANCELLED: 'Đã hủy'
};

export default function ServicePage() {
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [menu, setMenu] = useState<MenuItem[]>([]);
  const [orders, setOrders] = useState<Record<number, DiningOrder[]>>({});
  const [selected, setSelected] = useState<Reservation>();
  const [quantities, setQuantities] = useState<Record<number, number>>({});
  const [note, setNote] = useState('');
  const [search, setSearch] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function load() {
    try {
      const [all, dishes] = await Promise.all([api.staffServiceReservations(), api.menu()]);
      const active = all.filter(row => row.status === 'CHECKED_IN' && row.serviceSessionId);
      const pairs = await Promise.all(active.map(async row =>
        [row.id, await api.sessionOrders(row.serviceSessionId!)] as const));
      setReservations(active); setMenu(dishes); setOrders(Object.fromEntries(pairs)); setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được dữ liệu phục vụ');
    }
  }
  useOperationalEvents(() => void load());

  useEffect(() => {
    void load();
    const timer = setInterval(() => void load(), 5000);
    return () => clearInterval(timer);
  }, []);

  const filtered = useMemo(() => menu.filter(item =>
    item.name.toLowerCase().includes(search.toLowerCase()) ||
    item.category.toLowerCase().includes(search.toLowerCase())), [menu, search]);
  const readyItems = useMemo(() => reservations.flatMap(reservation =>
    (orders[reservation.id] || []).flatMap(order => order.items
      .filter(item => item.status === 'READY')
      .map(item => ({item, order, reservation})))), [orders, reservations]);

  function change(id: number, delta: number) {
    setQuantities(value => ({...value, [id]: Math.max(0, (value[id] || 0) + delta)}));
  }

  async function submit() {
    if (!selected?.serviceSessionId) return;
    const items = Object.entries(quantities).filter(([, quantity]) => quantity > 0)
      .map(([menuItemId, quantity]) => ({menuItemId: Number(menuItemId), quantity}));
    if (!items.length) { setError('Hãy chọn ít nhất một món'); return; }
    setBusy(true);
    try {
      await api.createOrder(selected.serviceSessionId, {items, note});
      setSelected(undefined); setQuantities({}); setNote(''); await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không gửi được phiếu món');
    } finally { setBusy(false); }
  }

  async function action(operation: () => Promise<unknown>) {
    setBusy(true); setError('');
    try { await operation(); await load(); }
    catch (err) { setError(err instanceof Error ? err.message : 'Không cập nhật được món'); }
    finally { setBusy(false); }
  }

  return <section className="page-section container service-page">
    <p className="eyebrow dark">PHỤC VỤ TẠI BÀN</p>
    <h1>Món bếp đã xong & gọi thêm món</h1>
    <p className="page-lead">Nhân viên theo dõi riêng từng món. Khi bếp báo xong, món xuất hiện ở đầu trang để mang lên đúng bàn.</p>
    {error && <p className="error">{error}</p>}

    <section className={`ready-dish-board ${readyItems.length ? 'has-ready' : ''}`}>
      <header><div><BellRing/><span><b>{readyItems.length} món cần mang lên</b><small>Tự cập nhật mỗi 5 giây</small></span></div></header>
      {readyItems.length
        ? <div>{readyItems.map(({item, order, reservation}) =>
          <article key={item.id}>
            <span><strong>{reservation.assignedTables.map(table => table.code).join(', ')}</strong><small>Phiếu #{order.id}</small></span>
            <b>{item.quantity}× {item.itemName}</b>
            <button disabled={busy} onClick={() => action(() => api.serveOrderItem(item.id))}>
              <Check/> Xác nhận đã mang lên
            </button>
          </article>)}</div>
        : <p><Check/> Hiện không có món nào chờ mang lên.</p>}
    </section>

    <div className="service-cards">
      {reservations.map(row => {
        const rowOrders = orders[row.id] || [];
        const unfinished = rowOrders.some(order => order.items.some(item =>
          ['SUBMITTED', 'PREPARING', 'DELAYED', 'READY'].includes(item.status)));
        return <article key={row.id}>
          <header>
            <span><UtensilsCrossed/><b>{row.assignedTables.map(table => table.code).join(', ')}</b></span>
            <small>{row.customerName} · {row.partySize} khách</small>
          </header>
          <div className="ticket-list item-tracking-list">
            {rowOrders.map(order => <div className="service-order" key={order.id}>
              <div className="service-order-head">
                <b>Phiếu #{order.id} {order.source === 'PREORDER' && <em>Đặt trước</em>}</b>
                {order.status === 'SUBMITTED' &&
                  <button className="cancel-ticket" disabled={busy} onClick={() => action(() => api.cancelOrder(order.id))}>Hủy phiếu</button>}
              </div>
              {order.items.map(item => <div className={`service-dish ${item.status.toLowerCase()}`} key={item.id}>
                <span>
                  <b>{item.quantity}× {item.itemName}</b>
                  {item.status === 'DELAYED' && <small><AlertTriangle/> {item.delayReason || 'Bếp báo món chậm'}</small>}
                </span>
                <i>{itemLabel[item.status]}</i>
                {item.status === 'READY' &&
                  <button disabled={busy} onClick={() => action(() => api.serveOrderItem(item.id))}><Check/> Đã mang lên</button>}
              </div>)}
            </div>)}
            {!rowOrders.length && <p>Chưa có phiếu gọi món.</p>}
          </div>
          <div className="service-card-actions">
            <button className="btn btn-green" onClick={() => { setSelected(row); setQuantities({}); setError(''); }}>
              <Plus/> Gọi thêm món
            </button>
            <button className="btn complete-table" disabled={busy || !row.paid || unfinished}
              onClick={() => action(() => api.completeService(row.id))}><Check/> Hoàn tất lượt khách</button>
          </div>
          {unfinished
            ? <p className="service-block-note"><Clock3/> Vẫn còn món chưa mang lên bàn.</p>
            : !row.paid
              ? <p className="service-block-note">Đã phục vụ món xong nhưng hóa đơn chưa thanh toán.</p>
              : <p className="service-paid-note">Đã phục vụ và thanh toán, có thể hoàn tất lượt khách.</p>}
        </article>;
      })}
      {!reservations.length && <div className="service-empty"><ChefHat/><h2>Chưa có bàn đang phục vụ</h2>
        <p>Khách cần được xác nhận, xếp bàn và check-in trước khi gọi món.</p></div>}
    </div>

    {selected && <div className="order-modal">
      <div className="order-modal-head">
        <div><p className="eyebrow dark">TẠO PHIẾU GỌI MÓN</p>
          <h2>{selected.assignedTables.map(table => table.code).join(', ')} — {selected.customerName}</h2></div>
        <button aria-label="Đóng phiếu gọi món" onClick={() => setSelected(undefined)}><X/></button>
      </div>
      <div className="order-search"><Search/><input placeholder="Tìm món..." value={search}
        onChange={event => setSearch(event.target.value)}/></div>
      <div className="order-menu">{filtered.map(item =>
        <article className={quantities[item.id] ? 'selected' : ''} key={item.id}>
          <img src={item.imageUrl} alt=""/><span><small>{item.category}</small><b>{item.name}</b>
            <i>{Number(item.price).toLocaleString('vi-VN')}đ</i></span>
          <div><button aria-label={`Giảm ${item.name}`} onClick={() => change(item.id, -1)}><Minus/></button>
            <strong>{quantities[item.id] || 0}</strong>
            <button aria-label={`Thêm ${item.name}`} onClick={() => change(item.id, 1)}><Plus/></button></div>
        </article>)}</div>
      <textarea placeholder="Ghi chú cho bếp: ít cay, không hành..." value={note}
        onChange={event => setNote(event.target.value)}/>
      <div className="order-total">
        <span><ShoppingBasket/> {Object.values(quantities).reduce((sum, value) => sum + value, 0)} món</span>
        <b>{menu.reduce((sum, item) => sum + (quantities[item.id] || 0) * Number(item.price), 0).toLocaleString('vi-VN')}đ</b>
        <button disabled={busy} onClick={submit}><Send/> Gửi phiếu xuống bếp</button>
      </div>
    </div>}
  </section>;
}

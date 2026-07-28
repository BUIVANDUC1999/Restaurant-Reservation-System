import {AlertTriangle, BellRing, CheckCircle2, ChefHat, Clock3, Flame, RefreshCw} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {api} from '../api';
import type {DiningOrder, DiningOrderItem, DiningOrderItemStatus} from '../types';

const label: Record<DiningOrderItemStatus, string> = {
  SUBMITTED: 'Chờ nấu',
  PREPARING: 'Đang nấu',
  DELAYED: 'Nấu chậm',
  READY: 'Bếp đã báo xong',
  SERVED: 'Nhân viên đã mang lên',
  CANCELLED: 'Đã hủy'
};

const time = (value: string) =>
  new Intl.DateTimeFormat('vi-VN', {hour: '2-digit', minute: '2-digit'}).format(new Date(value));

export default function KitchenPage() {
  const [orders, setOrders] = useState<DiningOrder[]>([]);
  const [busy, setBusy] = useState<number>();
  const [error, setError] = useState('');

  async function load() {
    try { setOrders(await api.kitchenOrders()); setError(''); }
    catch (e) { setError(e instanceof Error ? e.message : 'Không tải được bảng bếp'); }
  }

  useEffect(() => {
    void load();
    const timer = setInterval(() => void load(), 5000);
    return () => clearInterval(timer);
  }, []);

  async function update(item: DiningOrderItem, status: string, delayMinutes?: number, reason?: string) {
    setBusy(item.id);
    try { await api.updateKitchenItem(item.id, {status, delayMinutes, reason}); await load(); }
    catch (e) { setError(e instanceof Error ? e.message : 'Không cập nhật được món'); }
    finally { setBusy(undefined); }
  }

  function delay(item: DiningOrderItem) {
    const minutes = Number(prompt('Món chậm thêm bao nhiêu phút?', '15'));
    if (!minutes) return;
    const reason = prompt('Lý do món bị chậm?', 'Bếp đang đông') || '';
    void update(item, 'DELAYED', minutes, reason);
  }

  const all = useMemo(() => orders.flatMap(order =>
    order.items.map(item => ({...item, tableCodes: order.tableCodes}))), [orders]);

  return <section className="kitchen-page page-section container">
    <div className="kitchen-heading">
      <div>
        <p className="eyebrow dark">THEO DÕI TỪNG MÓN</p>
        <h1>Bảng điều phối bếp</h1>
        <p>Bếp cập nhật riêng từng món. Bấm “Báo món xong” để nhân viên nhận thông báo và mang lên đúng bàn.</p>
      </div>
      <button onClick={() => void load()}><RefreshCw/> Làm mới</button>
    </div>
    {error && <p className="error">{error}</p>}

    <div className="kitchen-stats kitchen-status-guide">
      <span><BellRing/><b>{all.filter(x => x.status === 'SUBMITTED').length}</b> chờ nấu</span>
      <span><Flame/><b>{all.filter(x => x.status === 'PREPARING').length}</b> đang nấu</span>
      <span className="delayed"><AlertTriangle/><b>{all.filter(x => x.status === 'DELAYED').length}</b> nấu chậm</span>
      <span className="ready"><CheckCircle2/><b>{all.filter(x => x.status === 'READY').length}</b> chờ nhân viên mang</span>
    </div>

    <div className="kitchen-quick-board">
      <section className="kitchen-queue delayed-queue">
        <h2><AlertTriangle/> Món đang chậm</h2>
        {all.filter(x => x.status === 'DELAYED').map(item =>
          <p key={item.id}><b>{item.quantity}× {item.itemName}</b><span>{item.tableCodes.join(', ')}</span><small>{item.delayReason}</small></p>)}
        {!all.some(x => x.status === 'DELAYED') && <i>Không có món chậm.</i>}
      </section>
      <section className="kitchen-queue ready-queue">
        <h2><CheckCircle2/> Bếp đã xong, chờ mang lên</h2>
        {all.filter(x => x.status === 'READY').map(item =>
          <p key={item.id}><b>{item.quantity}× {item.itemName}</b><span>{item.tableCodes.join(', ')}</span></p>)}
        {!all.some(x => x.status === 'READY') && <i>Chưa có món chờ mang lên.</i>}
      </section>
    </div>

    <div className="kitchen-board">
      {orders.map(order => <article className={order.status.toLowerCase()} key={order.id}>
        <header>
          <div><b>PHIẾU #{order.id}</b><strong>{order.tableCodes.join(', ')}</strong></div>
          <span><Clock3/> {time(order.createdAt)}</span>
        </header>
        <p>{order.customerName} · {order.reservationCode}</p>
        <div className="kitchen-item-list">
          {order.items.map(item => <div className={`kitchen-item ${item.status.toLowerCase()}`} key={item.id}>
            <span>
              <b>{item.quantity}× {item.itemName}</b>
              <small>Dự kiến {time(item.estimatedReadyAt)} · SLA {item.preparationMinutes} phút</small>
              {item.delayReason && <em><AlertTriangle/> {item.delayReason}</em>}
            </span>
            <i>{label[item.status]}</i>
            <div>
              {item.status === 'SUBMITTED' &&
                <button disabled={busy === item.id} onClick={() => update(item, 'PREPARING')}><Flame/> Bắt đầu nấu</button>}
              {(item.status === 'PREPARING' || item.status === 'DELAYED') && <>
                <button className="delay" disabled={busy === item.id} onClick={() => delay(item)}><AlertTriangle/> Báo chậm</button>
                <button className="dish-done" disabled={busy === item.id} onClick={() => update(item, 'READY')}>
                  <BellRing/> Báo món xong
                </button>
              </>}
              {item.status === 'READY' && <small className="waiting-server">Đã báo nhân viên phục vụ</small>}
              {item.status === 'SERVED' && <small className="served-confirmation"><CheckCircle2/> Đã mang lên bàn</small>}
            </div>
          </div>)}
        </div>
        {order.note && <blockquote>“{order.note}”</blockquote>}
      </article>)}
      {!orders.length && <div className="kitchen-empty"><ChefHat/><h2>Bếp đã xử lý hết món</h2></div>}
    </div>
  </section>;
}

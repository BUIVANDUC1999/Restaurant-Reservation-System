import {AlertTriangle, BellRing, CheckCircle2, ChefHat, Clock3, Flame, RefreshCw, TimerReset} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {api} from '../api';
import type {DiningOrder, DiningOrderItem, DiningOrderItemStatus, TimeoutPolicy} from '../types';
import {useOperationalEvents} from '../hooks/useOperationalEvents';

const label: Record<DiningOrderItemStatus, string> = {
  SUBMITTED: 'Chờ nấu',
  PREPARING: 'Đang nấu',
  DELAYED: 'Bếp đã báo chậm',
  READY: 'Bếp đã báo xong',
  SERVED: 'Nhân viên đã mang lên',
  CANCELLED: 'Đã hủy'
};

const defaultPolicy: TimeoutPolicy = {
  reservationHoldMinutes: 10,
  reservationConfirmationMinutes: 5,
  cleaningBufferMinutes: 15,
  upcomingAlertMinutes: 30,
  lateWarningMinutes: 15,
  lateCriticalMinutes: 20,
  tableRequestAckMinutes: 3,
  kitchenPrewarningMinutes: 3,
  kitchenWaiterEscalationMinutes: 5,
  kitchenCriticalOverdueMinutes: 10,
  cleaningTargetMinutes: 15
};

type KitchenFilter = 'ALL' | 'ATTENTION' | 'COOKING' | 'READY';
type SlaLevel = 'NORMAL' | 'DUE_SOON' | 'OVERDUE' | 'WAITER' | 'CRITICAL' | 'DONE';
type KitchenQueueItem = DiningOrderItem & {
  orderId: number;
  orderCreatedAt: string;
  source: DiningOrder['source'];
  tableCodes: string[];
};

const activeStatuses: DiningOrderItemStatus[] = ['SUBMITTED', 'PREPARING', 'DELAYED'];
const time = (value: string) =>
  new Intl.DateTimeFormat('vi-VN', {hour: '2-digit', minute: '2-digit'}).format(new Date(value));

function sla(item: DiningOrderItem, now: number, policy: TimeoutPolicy) {
  if (!activeStatuses.includes(item.status)) return {level: 'DONE' as SlaLevel, minutes: 0};
  const difference = new Date(item.estimatedReadyAt).getTime() - now;
  if (difference > 0) {
    const minutes = Math.max(1, Math.ceil(difference / 60000));
    return {level: minutes <= policy.kitchenPrewarningMinutes ? 'DUE_SOON' as SlaLevel : 'NORMAL' as SlaLevel, minutes};
  }
  const minutes = Math.max(1, Math.floor(Math.abs(difference) / 60000));
  if (minutes >= policy.kitchenCriticalOverdueMinutes) return {level: 'CRITICAL' as SlaLevel, minutes};
  if (minutes >= policy.kitchenWaiterEscalationMinutes) return {level: 'WAITER' as SlaLevel, minutes};
  return {level: 'OVERDUE' as SlaLevel, minutes};
}

function slaMessage(item: DiningOrderItem, now: number, policy: TimeoutPolicy) {
  const state = sla(item, now, policy);
  if (state.level === 'DUE_SOON') return `Còn ${state.minutes}p`;
  if (state.level === 'OVERDUE') return `Chậm ${state.minutes}p`;
  if (state.level === 'WAITER') return `Chậm ${state.minutes}p · Báo khách`;
  if (state.level === 'CRITICAL') return `Chậm ${state.minutes}p · Điều phối`;
  if (item.status === 'DELAYED') return `ETA còn ${state.minutes}p`;
  return '';
}

function itemPriority(item: DiningOrderItem, now: number, policy: TimeoutPolicy) {
  const level = sla(item, now, policy).level;
  if (level === 'CRITICAL') return 0;
  if (level === 'WAITER') return 1;
  if (level === 'OVERDUE') return 2;
  if (item.status === 'DELAYED') return 3;
  if (level === 'DUE_SOON') return 4;
  if (item.status === 'READY') return 5;
  if (activeStatuses.includes(item.status)) return 6;
  return 7;
}

function compareItems(
  left: DiningOrderItem,
  right: DiningOrderItem,
  now: number,
  policy: TimeoutPolicy
) {
  const priority = itemPriority(left, now, policy) - itemPriority(right, now, policy);
  if (priority !== 0) return priority;
  const eta = new Date(left.estimatedReadyAt).getTime() - new Date(right.estimatedReadyAt).getTime();
  return eta !== 0 ? eta : left.id - right.id;
}

function orderPriority(order: DiningOrder, now: number, policy: TimeoutPolicy) {
  return Math.min(...order.items.map(item => itemPriority(item, now, policy)), 7);
}

function waitingMinutes(value: string, now: number) {
  return Math.max(0, Math.floor((now - new Date(value).getTime()) / 60000));
}

export default function KitchenPage() {
  const [orders, setOrders] = useState<DiningOrder[]>([]);
  const [policy, setPolicy] = useState<TimeoutPolicy>(defaultPolicy);
  const [filter, setFilter] = useState<KitchenFilter>('ALL');
  const [now, setNow] = useState(Date.now());
  const [busy, setBusy] = useState<number>();
  const [error, setError] = useState('');

  async function load() {
    try { setOrders(await api.kitchenOrders()); setError(''); }
    catch (e) { setError(e instanceof Error ? e.message : 'Không tải được bảng bếp'); }
  }
  useOperationalEvents(() => void load());

  useEffect(() => {
    void load();
    void api.timeoutPolicy().then(setPolicy).catch(() => undefined);
    const refreshTimer = setInterval(() => void load(), 5000);
    const clockTimer = setInterval(() => setNow(Date.now()), 10000);
    return () => { clearInterval(refreshTimer); clearInterval(clockTimer); };
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

  const all = useMemo<KitchenQueueItem[]>(() => orders.flatMap(order =>
    order.items.map(item => ({
      ...item,
      orderId: order.id,
      orderCreatedAt: order.createdAt,
      source: order.source,
      tableCodes: order.tableCodes
    }))), [orders]);
  const needsAttention = (item: DiningOrderItem) => {
    const level = sla(item, now, policy).level;
    return item.status === 'DELAYED' || ['DUE_SOON', 'OVERDUE', 'WAITER', 'CRITICAL'].includes(level);
  };
  const sortedOrders = [...orders].sort((left, right) => {
    const priority = orderPriority(left, now, policy) - orderPriority(right, now, policy);
    if (priority !== 0) return priority;
    const created = new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime();
    return created !== 0 ? created : left.id - right.id;
  });
  const filteredOrders = sortedOrders.filter(order => {
    if (filter === 'ALL') return true;
    if (filter === 'ATTENTION') return order.items.some(needsAttention);
    if (filter === 'COOKING') return order.items.some(item => ['SUBMITTED', 'PREPARING', 'DELAYED'].includes(item.status));
    return order.items.some(item => item.status === 'READY');
  });
  const attention = all.filter(needsAttention).sort((left, right) => {
    const priority = itemPriority(left, now, policy) - itemPriority(right, now, policy);
    if (priority !== 0) return priority;
    const created = new Date(left.orderCreatedAt).getTime() - new Date(right.orderCreatedAt).getTime();
    if (created !== 0) return created;
    const eta = new Date(left.estimatedReadyAt).getTime() - new Date(right.estimatedReadyAt).getTime();
    return eta !== 0 ? eta : left.id - right.id;
  });
  const readyItems = all.filter(item => item.status === 'READY').sort((left, right) => {
    const created = new Date(left.orderCreatedAt).getTime() - new Date(right.orderCreatedAt).getTime();
    return created !== 0 ? created : left.id - right.id;
  });

  return <section className="kitchen-page page-section container">
    <div className="kitchen-heading">
      <div>
        <p className="eyebrow dark">THEO DÕI SLA TỰ ĐỘNG</p>
        <h1>Bảng điều phối bếp</h1>
        <p>Tự báo sắp trễ, chậm và cần điều phối.</p>
      </div>
      <button onClick={() => void load()}><RefreshCw/> Làm mới</button>
    </div>
    {error && <p className="error">{error}</p>}

    <div className="kitchen-stats kitchen-status-guide">
      <span><BellRing/><b>{all.filter(x => x.status === 'SUBMITTED').length}</b> chờ nấu</span>
      <span><Flame/><b>{all.filter(x => x.status === 'PREPARING').length}</b> đang nấu</span>
      <span className="due-soon"><TimerReset/><b>{all.filter(x => sla(x, now, policy).level === 'DUE_SOON').length}</b> sắp trễ</span>
      <span className="delayed"><AlertTriangle/><b>{all.filter(x => ['OVERDUE', 'WAITER', 'CRITICAL'].includes(sla(x, now, policy).level)).length}</b> món chậm</span>
      <span className="ready"><CheckCircle2/><b>{all.filter(x => x.status === 'READY').length}</b> chờ mang lên</span>
    </div>

    <div className="kitchen-filters" aria-label="Lọc phiếu bếp">
      {([
        ['ALL', 'Tất cả', orders.length],
        ['ATTENTION', 'Cần chú ý', orders.filter(order => order.items.some(needsAttention)).length],
        ['COOKING', 'Đang xử lý', orders.filter(order => order.items.some(item => activeStatuses.includes(item.status))).length],
        ['READY', 'Chờ phục vụ', orders.filter(order => order.items.some(item => item.status === 'READY')).length]
      ] as const).map(([value, text, count]) =>
        <button className={filter === value ? 'active' : ''} onClick={() => setFilter(value)} key={value}>{text}<b>{count}</b></button>)}
    </div>

    <div className="kitchen-priority-rule">
      <span><AlertTriangle/> Chậm nhiều xếp trước</span>
      <span><Clock3/> Cùng mức: phiếu vào trước xếp trước</span>
      <span><b>1</b> Số nhỏ xử lý trước</span>
    </div>

    <div className="kitchen-quick-board">
      <section className="kitchen-queue delayed-queue kitchen-priority-list">
        <h2><AlertTriangle/> Ưu tiên xử lý <small>{attention.length} món</small></h2>
        {attention.map((item, index) => {
          const state = sla(item, now, policy);
          return <p className={`sla-${state.level.toLowerCase()}`} key={item.id}>
            <em>{index + 1}</em>
            <b>{item.quantity}× {item.itemName}</b><span>{item.tableCodes.join(', ')}</span>
            <small>{slaMessage(item, now, policy)} · phiếu {time(item.orderCreatedAt)}{item.delayReason ? ` · ${item.delayReason}` : ''}</small>
          </p>;
        })}
        {!attention.length && <i>Không có món sắp trễ hoặc đang chậm.</i>}
      </section>
      <section className="kitchen-queue ready-queue kitchen-priority-list">
        <h2><CheckCircle2/> Chờ mang lên <small>{readyItems.length} món</small></h2>
        {readyItems.map((item, index) =>
          <p key={item.id}><em>{index + 1}</em><b>{item.quantity}× {item.itemName}</b><span>{item.tableCodes.join(', ')}</span>
            <small>Phiếu {time(item.orderCreatedAt)} · vào trước phục vụ trước</small>
          </p>)}
        {!readyItems.length && <i>Chưa có món chờ mang lên.</i>}
      </section>
    </div>

    <div className="kitchen-board">
      {filteredOrders.map((order, orderIndex) => <article className={`${order.status.toLowerCase()} ${order.items.some(needsAttention) ? 'priority-order' : ''}`} key={order.id}>
        <header>
          <div><b><i>ƯU TIÊN {orderIndex + 1}</i> PHIẾU #{order.id}</b><strong>{order.tableCodes.join(', ')}</strong></div>
          <span><Clock3/> {time(order.createdAt)} · {waitingMinutes(order.createdAt, now)}p</span>
        </header>
        <p className="kitchen-order-summary">
          <span>{order.customerName} · {order.reservationCode}</span>
          <strong>{order.source === 'PREORDER' ? 'MÓN ĐẶT TRƯỚC' : 'GỌI TẠI BÀN'}</strong>
        </p>
        <div className="kitchen-item-list">
          {[...order.items].sort((left, right) => compareItems(left, right, now, policy)).map((item, itemIndex) => {
            const state = sla(item, now, policy);
            const message = slaMessage(item, now, policy);
            return <div className={`kitchen-item ${item.status.toLowerCase()} sla-${state.level.toLowerCase()}`} data-priority={itemIndex + 1} key={item.id}>
              <span>
                <b>{item.quantity}× {item.itemName}</b>
                <small>ETA {time(item.estimatedReadyAt)} · {item.preparationMinutes}p</small>
                {message && <strong className="kitchen-sla"><Clock3/> {message}</strong>}
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
            </div>;
          })}
        </div>
        {order.note && <blockquote>“{order.note}”</blockquote>}
      </article>)}
      {!filteredOrders.length && <div className="kitchen-empty"><ChefHat/><h2>Không có phiếu phù hợp bộ lọc</h2></div>}
    </div>
  </section>;
}

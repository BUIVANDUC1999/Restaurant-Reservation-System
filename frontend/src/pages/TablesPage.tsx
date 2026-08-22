import {AlertTriangle, Armchair, ArrowRightLeft, BellRing, Check, ChefHat, ChevronRight, Clock3, Phone, QrCode, UserCheck, Users, UtensilsCrossed, X} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {api} from '../api';
import type {DiningOrder, DiningOrderItem, Notification, OperationalTimeout, TableOverview, TableRequest, TimeoutPolicy, WaiterAssignmentEvent, WaiterSummary} from '../types';
import {useOperationalEvents} from '../hooks/useOperationalEvents';
import {useAuth} from '../auth';

const serviceLabels: Record<TableOverview['serviceState'], string> = {
  EMPTY: 'Bàn trống', RESERVED: 'Khách sắp đến', DINING: 'Đang dùng bữa',
  WAITING_KITCHEN: 'Đang chờ bếp', NEEDS_SERVING: 'Có món cần mang ra',
  NEEDS_CLEANING: 'Cần dọn bàn', INACTIVE: 'Tạm ngưng'
};
const requestLabels = {
  CALL_WAITER: 'Gọi nhân viên', WATER: 'Xin thêm nước',
  UTENSILS: 'Xin dụng cụ', PAYMENT: 'Yêu cầu thanh toán'
};
const itemLabels:Record<DiningOrderItem['status'],string>={
  SUBMITTED:'Chờ bếp nhận',PREPARING:'Đang chế biến',DELAYED:'Bếp báo chậm',
  READY:'Đã xong · chờ mang',SERVED:'Đã phục vụ',CANCELLED:'Đã hủy'
};
const activeItemStatuses:DiningOrderItem['status'][]=['SUBMITTED','PREPARING','DELAYED'];
const timeoutLabels:Record<OperationalTimeout['type'],string>={
  RESERVATION_HOLD:'Hết hạn cọc',RESERVATION_CONFIRMATION:'Chờ xác nhận',CUSTOMER_LATE:'Khách trễ',
  KITCHEN_SLA:'Món chậm',SERVICE_REQUEST_ACK:'QR chưa nhận',TABLE_CLEANING:'Dọn bàn chậm'
};
const firstSentence=(message:string)=>message.split(/[.!?]/)[0].replace(/\s+phút\b/g,'p').trim();
const timeoutTarget=(item:OperationalTimeout)=>{
  const text=`${item.title} ${item.details}`;
  return text.match(/KV-[A-Z0-9]+/)?.[0]||text.match(/B\d{2}/)?.[0]||'';
};
const overdueMinutes=(deadline:string)=>Math.max(1,Math.floor((Date.now()-new Date(deadline).getTime())/60000));
const guestBaseStorageKey = 'restaurant_guest_base_url';
const initialGuestBaseUrl = () => {
  const configured = import.meta.env.VITE_GUEST_BASE_URL?.trim();
  const currentHostIsLan = !['localhost', '127.0.0.1'].includes(location.hostname);
  return (currentHostIsLan ? location.origin : configured || localStorage.getItem(guestBaseStorageKey) || location.origin)
    .replace(/\/$/, '');
};

export default function TablesPage() {
  const {user} = useAuth();
  const [tables, setTables] = useState<TableOverview[]>([]);
  const [requests, setRequests] = useState<TableRequest[]>([]);
  const [alerts, setAlerts] = useState<Notification[]>([]);
  const [timeouts, setTimeouts] = useState<OperationalTimeout[]>([]);
  const [policy, setPolicy] = useState<TimeoutPolicy>();
  const [waiters, setWaiters] = useState<WaiterSummary[]>([]);
  const [assignmentHistory, setAssignmentHistory] = useState<WaiterAssignmentEvent[]>([]);
  const [tableView, setTableView] = useState<'MINE'|'ALL'>(user?.role === 'STAFF' ? 'MINE' : 'ALL');
  const [qr, setQr] = useState<TableOverview>();
  const [selectedId, setSelectedId] = useState<number>();
  const [detailOrders, setDetailOrders] = useState<DiningOrder[]>([]);
  const [detailError, setDetailError] = useState('');
  const [guestBaseUrl, setGuestBaseUrl] = useState(initialGuestBaseUrl);
  const [rosterOpen, setRosterOpen] = useState(true);
  const [assignmentBusy, setAssignmentBusy] = useState<number>();
  const [error, setError] = useState('');

  const load = async () => {
    try {
      const [t, r, n, o, p, w, h] = await Promise.all([
        api.tableOverview(), api.tableRequests(), api.notifications(), api.timeouts(), api.timeoutPolicy(),
        api.waiters(), api.waiterAssignmentHistory()
      ]);
      setTables(t); setRequests(r); setAlerts(n); setTimeouts(o); setPolicy(p); setWaiters(w);
      setAssignmentHistory(h); setError('');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không tải được dữ liệu vận hành');
    }
  };
  const realtime = useOperationalEvents(() => void load());

  useEffect(() => {
    void load();
    const timer = setInterval(() => void load(), 10000);
    return () => clearInterval(timer);
  }, []);

  const openRequests = useMemo(
    () => requests.filter(r => r.status === 'NEW' || r.status === 'ACKNOWLEDGED'), [requests]
  );
  const openTimeouts = useMemo(() => timeouts.filter(t => t.status === 'OPEN'), [timeouts]);
  const visibleTables = useMemo(() => tableView === 'ALL' || user?.role !== 'STAFF'
    ? tables : tables.filter(table => table.assignedStaffEmail === user.email), [tables, tableView, user]);
  const visibleTableIds = useMemo(() => new Set(visibleTables.map(table => table.id)), [visibleTables]);
  const visibleRequests = useMemo(() => openRequests.filter(request => {
    const table=tables.find(value=>value.id===request.tableId);
    return visibleTableIds.has(request.tableId)||!table?.assignedStaffEmail;
  }), [openRequests, visibleTableIds, tables]);
  const selected = useMemo(()=>tables.find(table=>table.id===selectedId),[tables,selectedId]);
  const selectedSessionId = selected?.serviceSessionId;
  useEffect(()=>{
    let cancelled=false;
    if(!selectedSessionId)return;
    const refresh=()=>api.sessionOrders(selectedSessionId).then(data=>{
      if(!cancelled){setDetailOrders(data);setDetailError('');}
    }).catch(e=>{if(!cancelled)setDetailError(e instanceof Error?e.message:'Không tải được phiếu món của bàn');});
    void refresh();const timer=setInterval(()=>void refresh(),10000);
    return()=>{cancelled=true;clearInterval(timer);};
  },[selectedSessionId]);
  const selectedItems=detailOrders.flatMap(order=>order.items);
  const selectedRequests=selected?openRequests.filter(request=>request.tableId===selected.id):[];
  const selectedDelayedItems=selectedItems.filter(item=>item.status==='DELAYED'||
    activeItemStatuses.includes(item.status)&&new Date(item.estimatedReadyAt).getTime()<=Date.now());
  const guestUrl = qr ? `${guestBaseUrl}/ban/${qr.publicToken}` : '';
  const changeGuestBaseUrl = (value:string) => {
    const normalized = value.trim().replace(/\/$/, '');
    setGuestBaseUrl(normalized);
    if (normalized) localStorage.setItem(guestBaseStorageKey, normalized);
    else localStorage.removeItem(guestBaseStorageKey);
  };
  const serviceGroups = useMemo(() => {
    const groups = new Map<number, {
      sessionId:number;tableCodes:string[];staffId?:number;staffName?:string;staffEmail?:string
    }>();
    tables.filter(table => table.serviceSessionId).forEach(table => {
      const sessionId = table.serviceSessionId!;
      const current = groups.get(sessionId) || {
        sessionId, tableCodes: [], staffId: table.assignedStaffId,
        staffName: table.assignedStaffName, staffEmail: table.assignedStaffEmail
      };
      current.tableCodes.push(table.code); groups.set(sessionId, current);
    });
    return [...groups.values()];
  }, [tables]);
  async function requestAction(id: number, status: string) { await api.updateTableRequest(id, status); await load(); }
  async function openAlert(notification: Notification) {
    let targetTableId:number|undefined;
    if (notification.type === 'TABLE_CALL') {
      const tableCode=notification.title.match(/B\d{2}/)?.[0];
      targetTableId=tables.find(table=>table.code===tableCode)?.id;
    }
    await api.readNotification(notification.id);
    await load();
    if (targetTableId) {
      setDetailOrders([]);setDetailError('');setSelectedId(targetTableId);
    }
  }
  async function resolveTimeout(id: number) { await api.resolveTimeout(id); await load(); }
  async function acknowledgeTimeout(id: number) { await api.acknowledgeTimeout(id); await load(); }
  async function transferTimeout(id: number) {
    const assignee = prompt('Email người nhận việc:');
    if (assignee) { await api.assignTimeout(id, assignee, 'Điều phối lại từ trung tâm timeout'); await load(); }
  }
  async function assignWaiter(group:{sessionId:number;staffId?:number;staffName?:string}, staffId?:number) {
    let reason:string|undefined;
    if (user?.role === 'ADMIN' && group.staffId && group.staffId !== staffId) {
      reason = prompt(staffId ? `Lý do bàn giao từ ${group.staffName}:` : `Lý do bỏ phân công của ${group.staffName}:`)?.trim();
      if (!reason) return;
    }
    setAssignmentBusy(group.sessionId);setError('');
    try {
      if (user?.role === 'ADMIN') {
        if (staffId) await api.assignServiceSession(group.sessionId, staffId, reason);
        else await api.unassignServiceSession(group.sessionId, reason || 'Admin bỏ phân công');
      } else {
        await api.claimServiceSession(group.sessionId);
      }
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không cập nhật được nhân viên phụ trách');
    } finally { setAssignmentBusy(undefined); }
  }

  return <section className="tables-page page-section container">
    <div className="staff-heading">
      <div><p className="eyebrow dark">SƠ ĐỒ VẬN HÀNH THỜI GIAN THỰC</p><h1>Nhà hàng · Một tầng</h1></div>
      <span>{realtime.connected ? '● Realtime đang kết nối' : 'Tự làm mới mỗi 10 giây'}</span>
    </div>
    {error && <p className="error">{error}</p>}
    {user?.role === 'STAFF' && <div className="table-view-tabs">
      <button className={tableView==='MINE'?'active':''} onClick={()=>setTableView('MINE')}>
        <UserCheck/> Bàn của tôi <b>{tables.filter(table=>table.assignedStaffEmail===user.email).length}</b>
      </button>
      <button className={tableView==='ALL'?'active':''} onClick={()=>setTableView('ALL')}>
        <Armchair/> Tất cả bàn <b>{tables.length}</b>
      </button>
    </div>}

    <div className="operations-alert-strip">
      {alerts.filter(a => !a.readAt).slice(0, 5).map(a =>
        <button key={a.id} onClick={() => void openAlert(a)}>
          <BellRing/><span><b>{a.title}</b><small>{firstSentence(a.message)}</small></span>
          {a.type==='TABLE_CALL'?<ChevronRight/>:<Check/>}
        </button>)}
    </div>

    <div className="timeout-board">
      <div className="timeout-board-title">
        <div><h2><Clock3/> Việc quá hạn</h2><p>{openTimeouts.length} việc cần xử lý</p></div>
        {policy && <small>Giữ cọc {policy.reservationHoldMinutes}p · QR {policy.tableRequestAckMinutes}p ·
          trễ {policy.lateWarningMinutes}/{policy.lateCriticalMinutes}p · dọn bàn {policy.cleaningTargetMinutes}p</small>}
      </div>
      {openTimeouts.length === 0
        ? <p className="timeout-empty"><Check/> Không có công việc quá hạn</p>
        : <div className="timeout-grid">{openTimeouts.map(item =>
          <article key={item.id} className={item.severity.toLowerCase()}>
            <span>{item.severity === 'CRITICAL' ? 'Khẩn' : 'Chậm'} · {overdueMinutes(item.deadlineAt)}p</span>
            <b>{timeoutLabels[item.type]}{timeoutTarget(item)?` · ${timeoutTarget(item)}`:''}</b>
            <small>{item.assignedTo||'Chưa phân công'}{item.acknowledgedAt?' · Đã nhận':''}</small>
            {!item.acknowledgedAt && <button onClick={() => acknowledgeTimeout(item.id)}>
              <Users/> {item.assignedTo ? 'Xác nhận' : 'Nhận việc'}
            </button>}
            <button onClick={() => transferTimeout(item.id)}>Chuyển việc</button>
            <button onClick={() => resolveTimeout(item.id)}><Check/> Đã xử lý</button>
          </article>)}</div>}
    </div>

    {!!visibleRequests.length && <div className="table-request-board">
      <h2><BellRing/> Yêu cầu từ QR tại bàn</h2>
      {visibleRequests.map(r => {
        const table = tables.find(t => t.id === r.tableId);
        return <article className={r.status.toLowerCase()} key={r.id}>
          <b>{table?.code || `Bàn #${r.tableId}`}</b><span>{requestLabels[r.type]}</span><small>{r.note}</small>
          {r.status === 'NEW'
            ? <button onClick={() => requestAction(r.id, 'ACKNOWLEDGED')}>Nhận xử lý</button>
            : <button onClick={() => requestAction(r.id, 'DONE')}><Check/> Đã xong</button>}
        </article>;
      })}
    </div>}

    <div className="table-legend">
      {Object.entries(serviceLabels).map(([state, label]) =>
        <span key={state}><i className={`dot ${state.toLowerCase()}`}/>{label}</span>)}
      <span><i className="dot urgent"/>Gọi nhân viên</span>
    </div>
    <div className="restaurant-map">
      <div className="map-zone perimeter-zone">6 BÀN KHU VỰC XUNG QUANH</div>
      <div className="map-zone center-zone">2 BÀN TRUNG TÂM</div>
      {visibleTables.map(table => {
        const call = visibleRequests.some(r => r.tableId === table.id);
        return <article key={table.id} className={`map-table ${table.serviceState.toLowerCase()} ${call ? 'urgent' : ''} ${table.shape.toLowerCase()}`}
          style={{left: `${table.layoutX}%`, top: `${table.layoutY}%`}} role="button" tabIndex={0}
          title={`Xem chi tiết ${table.name}`} onClick={()=>{setDetailOrders([]);setDetailError('');setSelectedId(table.id);}}
          onKeyDown={event=>{if(event.key==='Enter'||event.key===' '){event.preventDefault();setDetailOrders([]);setDetailError('');setSelectedId(table.id);}}}>
          <b>{table.code}</b><small><Users/> {table.seats}</small>
          <span>{call ? 'Đang gọi NV' : serviceLabels[table.serviceState]}</span>
          {table.serviceSessionId && <em className={table.assignedStaffName ? '' : 'unassigned'}>
            <UserCheck/> {table.assignedStaffName || 'Chưa phân công'}
          </em>}
          <button title="Mã QR bàn" onClick={event => {event.stopPropagation();setQr(table);}}><QrCode/></button>
        </article>;
      })}
    </div>
    <div className="floor-summary">
      <span><Armchair/> {visibleTables.length} bàn đang hiển thị</span><span><Users/> {visibleTables.reduce((n, t) => n + t.seats, 0)} chỗ</span>
      <span><BellRing/> {visibleRequests.length} yêu cầu đang mở</span>
      <span><Clock3/> {visibleTables.filter(t => t.serviceState === 'WAITING_KITCHEN').length} bàn chờ bếp</span>
    </div>

    <button className={`waiter-panel-toggle ${rosterOpen ? 'open' : ''}`} onClick={() => setRosterOpen(value => !value)}>
      <UserCheck/><span>Phân công bàn</span><b>{serviceGroups.filter(group => !group.staffId).length}</b><ChevronRight/>
    </button>
    {rosterOpen && <aside className="waiter-corner-panel">
      <header><div><UserCheck/><span><b>Nhân viên đang trong ca</b><small>{serviceGroups.length} lượt khách đang dùng bàn</small></span></div>
        <button onClick={() => setRosterOpen(false)} aria-label="Đóng phân công"><X/></button></header>
      <div className="waiter-workload">
        {waiters.map(waiter => {
          const codes = serviceGroups.filter(group => group.staffId === waiter.id).flatMap(group => group.tableCodes);
          return <span key={waiter.id} className={`${codes.length ? 'working' : ''} ${waiter.loadLevel.toLowerCase()}`}>
            <i>{waiter.fullName.split(' ').slice(-2).map(part => part[0]).join('')}</i>
            <small><b>{waiter.fullName}{waiter.recommended&&<em>Đề xuất</em>}</b>
              {codes.length ? `Bàn ${codes.join(', ')} · ${waiter.guestCount} khách` : 'Chưa nhận bàn'}</small>
            <strong title={`${waiter.sessionCount} lượt khách`}>{waiter.tableCount}</strong>
          </span>;
        })}
      </div>
      <div className="session-assignments">
        {serviceGroups.length === 0 && <p>Chưa có bàn nào đang phục vụ.</p>}
        {serviceGroups.map(group => <label key={group.sessionId} className={!group.staffId ? 'needs-waiter' : ''}>
          <span><b>{group.tableCodes.join(', ')}</b><small>{group.staffName || 'Chưa có người phụ trách'}</small></span>
          {user?.role === 'ADMIN'
            ? <select aria-label={`Phân công bàn ${group.tableCodes.join(', ')}`}
                disabled={assignmentBusy === group.sessionId} value={group.staffId || ''}
                onChange={event => void assignWaiter(group, Number(event.target.value) || undefined)}>
                <option value="">Chưa phân công</option>
                {group.staffId&&!waiters.some(waiter=>waiter.id===group.staffId)&&
                  <option value={group.staffId} disabled>{group.staffName} (ngoài ca)</option>}
                {waiters.map(waiter => <option key={waiter.id} value={waiter.id}>{waiter.fullName}</option>)}
              </select>
            : !group.staffId
              ? <button disabled={assignmentBusy === group.sessionId}
                  onClick={() => void assignWaiter(group)}>Nhận bàn</button>
              : group.staffEmail === user?.email && <strong>Bàn của tôi</strong>}
        </label>)}
      </div>
      <div className="assignment-history-mini"><h3><ArrowRightLeft/> Bàn giao gần đây</h3>
        {!assignmentHistory.length&&<p>Chưa có lịch sử phân công.</p>}
        {assignmentHistory.slice(0,5).map(event=><article key={event.id}>
          <span><b>{event.fromStaffName||'Trống'}</b><ArrowRightLeft/><b>{event.toStaffName||'Trống'}</b></span>
          <small>{event.reason} · {new Date(event.createdAt).toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'})}</small>
        </article>)}
      </div>
    </aside>}

    {selected&&<div className="admin-table-detail-backdrop" onMouseDown={event=>{if(event.target===event.currentTarget)setSelectedId(undefined);}}>
      <aside className="admin-table-detail table-operations-detail">
        <header className={selected.serviceState.toLowerCase()}>
          <div><span>{selected.code}</span><section><small>{serviceLabels[selected.serviceState]}</small><h2>{selected.name}</h2></section></div>
          <button onClick={()=>setSelectedId(undefined)} aria-label="Đóng chi tiết bàn"><X/></button>
        </header>
        <div className="admin-table-detail-summary">
          <span><Users/><small>Số khách</small><b>{selected.partySize?`${selected.partySize} người`:'Chưa có khách'}</b></span>
          <span><UserCheck/><small>Nhân viên</small><b>{selected.assignedStaffName||'Chưa phân công'}</b></span>
          <span><UtensilsCrossed/><small>Phiếu món mở</small><b>{selected.openOrderCount}</b></span>
          <span className={selectedDelayedItems.length?'danger':''}><AlertTriangle/><small>Món chậm</small><b>{selectedDelayedItems.length}</b></span>
        </div>
        {selected.reservationCode&&<div className="admin-table-customer">
          <span><b>{selected.customerName}</b><small>Mã {selected.reservationCode} · {selected.partySize} khách</small></span>
          <span><Phone/> {selected.customerPhone||'Không có số điện thoại'}</span>
        </div>}
        {!!selectedRequests.length&&<section className="table-detail-requests"><h3><BellRing/> Yêu cầu đang xử lý</h3>
          {selectedRequests.map(request=><p key={request.id}><b>{requestLabels[request.type]}</b><span>{request.note||'Không có ghi chú'} · {request.status==='NEW'?'Chưa nhận':'Đã có người nhận'}</span>
            {request.status==='NEW'?<button onClick={()=>void requestAction(request.id,'ACKNOWLEDGED')}>Nhận xử lý</button>:<button onClick={()=>void requestAction(request.id,'DONE')}><Check/> Đã xong</button>}
          </p>)}
        </section>}
        <section className="admin-dish-progress">
          <div className="admin-detail-title"><h3><ChefHat/> Tiến độ từng món</h3><small>{selectedItems.length} món</small></div>
          {detailError&&<p className="error">{detailError}</p>}
          {!selected.serviceSessionId&&<p className="admin-detail-empty">Bàn chưa check-in nên chưa có phiên phục vụ.</p>}
          {selected.serviceSessionId&&!detailError&&!selectedItems.length&&<p className="admin-detail-empty">Bàn chưa gọi món.</p>}
          {selectedItems.map(item=>{
            const delayed=selectedDelayedItems.some(delayedItem=>delayedItem.id===item.id);
            return <article className={`${item.status.toLowerCase()} ${delayed?'delayed':''}`} key={item.id}>
              <span>{item.status==='READY'?<Check/>:item.status==='PREPARING'?<ChefHat/>:<Clock3/>}</span>
              <div><b>{item.quantity}× {item.itemName}</b><small>{itemLabels[item.status]} · {item.preparationMinutes}p</small>
                {item.delayReason&&<em>Lý do: {item.delayReason}</em>}</div>
              <strong>{delayed?'Chậm':`ETA ${new Date(item.estimatedReadyAt).toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'})}`}</strong>
            </article>;
          })}
        </section>
      </aside>
    </div>}

    {qr && <div className="qr-table-modal"><div>
      <button className="close" onClick={() => setQr(undefined)}>×</button><QrCode/><h2>QR phục vụ · {qr.code}</h2>
      <label className="qr-guest-base">Địa chỉ điện thoại truy cập
        <input type="url" value={guestBaseUrl} onChange={event=>changeGuestBaseUrl(event.target.value)} placeholder="http://192.168.x.x:5173"/>
      </label>
      {guestBaseUrl.includes('localhost')&&<p className="qr-warning">Điện thoại không mở được localhost. Hãy nhập địa chỉ Wi-Fi của máy tính.</p>}
      {guestUrl&&<img src={`https://api.qrserver.com/v1/create-qr-code/?size=260x260&data=${encodeURIComponent(guestUrl)}`} alt={`QR ${qr.code}`}/>}
      <p>Điện thoại và máy tính cần dùng cùng Wi-Fi. Khách có thể gọi nhân viên hỗ trợ ngay cả trước khi check-in.</p>
      <b className="qr-url">{guestUrl}</b><button onClick={() => window.print()}>In mã QR</button>
    </div></div>}
  </section>;
}

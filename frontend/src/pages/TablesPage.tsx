import {Armchair, ArrowRightLeft, BellRing, Check, ChevronRight, Clock3, QrCode, UserCheck, Users, X} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {api} from '../api';
import type {Notification, OperationalTimeout, TableOverview, TableRequest, TimeoutPolicy, WaiterAssignmentEvent, WaiterSummary} from '../types';
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
  const visibleRequests = useMemo(() => openRequests.filter(request => visibleTableIds.has(request.tableId)),
    [openRequests, visibleTableIds]);
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
        <button key={a.id} onClick={async () => { await api.readNotification(a.id); await load(); }}>
          <BellRing/><span><b>{a.title}</b><small>{a.message}</small></span><Check/>
        </button>)}
    </div>

    <div className="timeout-board">
      <div className="timeout-board-title">
        <div><h2><Clock3/> Trung tâm quản lý timeout</h2><p>{openTimeouts.length} việc đang quá hạn · kiểm tra mỗi phút</p></div>
        {policy && <small>Giữ cọc {policy.reservationHoldMinutes}p · QR {policy.tableRequestAckMinutes}p ·
          trễ {policy.lateWarningMinutes}/{policy.lateCriticalMinutes}p · dọn bàn {policy.cleaningTargetMinutes}p</small>}
      </div>
      {openTimeouts.length === 0
        ? <p className="timeout-empty"><Check/> Không có công việc quá hạn</p>
        : <div className="timeout-grid">{openTimeouts.map(item =>
          <article key={item.id} className={item.severity.toLowerCase()}>
            <span>{item.severity === 'CRITICAL' ? 'Khẩn cấp' : 'Cảnh báo'} · hạn lúc {
              new Date(item.deadlineAt).toLocaleTimeString('vi-VN', {hour: '2-digit', minute: '2-digit'})
            }</span>
            <b>{item.title}</b><p>{item.details}</p>
            <small>{item.assignedTo ? `Phụ trách: ${item.assignedTo}` : 'Chưa có người phụ trách'}
              {item.acknowledgedAt ? ' · đã xác nhận' : ''}</small>
            {!item.acknowledgedAt && <button onClick={() => acknowledgeTimeout(item.id)}>
              <Users/> {item.assignedTo ? 'Xác nhận nhận việc' : `Nhận việc (${user?.fullName || 'tôi'})`}
            </button>}
            <button onClick={() => transferTimeout(item.id)}>Chuyển người phụ trách</button>
            <button onClick={() => resolveTimeout(item.id)}><Check/> Đánh dấu đã xử lý</button>
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
      <div className="map-zone window-zone">KHU CỬA SỔ</div><div className="map-zone hall-zone">SẢNH CHÍNH</div>
      <div className="map-zone family-zone">KHU GIA ĐÌNH / RIÊNG TƯ</div>
      {visibleTables.map(table => {
        const call = visibleRequests.some(r => r.tableId === table.id);
        return <article key={table.id} className={`map-table ${table.serviceState.toLowerCase()} ${call ? 'urgent' : ''} ${table.shape.toLowerCase()}`}
          style={{left: `${table.layoutX}%`, top: `${table.layoutY}%`}}>
          <b>{table.code}</b><small><Users/> {table.seats}</small>
          <span>{call ? 'Đang gọi NV' : serviceLabels[table.serviceState]}</span>
          {table.serviceSessionId && <em className={table.assignedStaffName ? '' : 'unassigned'}>
            <UserCheck/> {table.assignedStaffName || 'Chưa phân công'}
          </em>}
          <button title="Mã QR bàn" onClick={() => setQr(table)}><QrCode/></button>
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

    {qr && <div className="qr-table-modal"><div>
      <button className="close" onClick={() => setQr(undefined)}>×</button><QrCode/><h2>QR phục vụ · {qr.code}</h2>
      <img src={`https://api.qrserver.com/v1/create-qr-code/?size=260x260&data=${encodeURIComponent(`${location.origin}/ban/${qr.publicToken}`)}`} alt={`QR ${qr.code}`}/>
      <p>In mã này và đặt trên bàn. QR chỉ nhận yêu cầu khi bàn có phiên phục vụ.</p>
      <b>{location.origin}/ban/{qr.publicToken}</b><button onClick={() => window.print()}>In mã QR</button>
    </div></div>}
  </section>;
}

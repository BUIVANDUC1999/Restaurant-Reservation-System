import {
  AlertTriangle, BellRing, CalendarCheck, Clock3, RefreshCw, TableProperties,
  UsersRound, UtensilsCrossed
} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {Link} from 'react-router-dom';
import {api} from '../api';
import {useAuth} from '../auth';
import {useOperationalEvents} from '../hooks/useOperationalEvents';
import type {OperationalTimeout, Reservation, TableOverview, TableRequest, WalkInVisit} from '../types';

const terminalReservationStatuses=['COMPLETED','CANCELLED','REJECTED','NO_SHOW','EXPIRED'];
const terminalWalkInStatuses=['COMPLETED','LEFT','NO_RESPONSE','CANCELLED'];
const requestLabels:Record<TableRequest['type'],string>={
  CALL_WAITER:'Gọi nhân viên',WATER:'Xin thêm nước',UTENSILS:'Cần dụng cụ',PAYMENT:'Yêu cầu thanh toán'
};

function dateKey(value:Date){
  const year=value.getFullYear();
  const month=String(value.getMonth()+1).padStart(2,'0');
  const day=String(value.getDate()).padStart(2,'0');
  return `${year}-${month}-${day}`;
}

export default function StaffDashboardPage(){
  const{user}=useAuth();
  const[reservations,setReservations]=useState<Reservation[]>([]);
  const[tables,setTables]=useState<TableOverview[]>([]);
  const[requests,setRequests]=useState<TableRequest[]>([]);
  const[timeouts,setTimeouts]=useState<OperationalTimeout[]>([]);
  const[walkIns,setWalkIns]=useState<WalkInVisit[]>([]);
  const[loading,setLoading]=useState(true);
  const[error,setError]=useState('');

  async function load(silent=false){
    if(!silent)setLoading(true);
    try{
      const[reservationData,tableData,requestData,timeoutData,walkInData]=await Promise.all([
        api.staffReservations(),api.tableOverview(),api.tableRequests(),api.timeouts(),api.walkIns()
      ]);
      setReservations(reservationData);setTables(tableData);setRequests(requestData);setTimeouts(timeoutData);setWalkIns(walkInData);setError('');
    }catch(err){setError(err instanceof Error?err.message:'Không tải được dashboard nhân viên');}
    finally{if(!silent)setLoading(false);}
  }
  useOperationalEvents(()=>void load(true));
  useEffect(()=>{void load();const timer=setInterval(()=>void load(true),10000);return()=>clearInterval(timer);},[]);

  const today=dateKey(new Date());
  const todayReservations=reservations.filter(item=>item.reservationDate===today&&item.source!=='WALK_IN');
  const pendingReservations=reservations.filter(item=>item.status==='PENDING'&&item.source!=='WALK_IN');
  const activeTables=tables.filter(table=>Boolean(table.serviceSessionId));
  const myTables=user?.role==='ADMIN'?activeTables:activeTables.filter(table=>table.assignedStaffEmail===user?.email);
  const openRequests=requests.filter(item=>item.status==='NEW'||item.status==='ACKNOWLEDGED');
  const myTableIds=new Set(myTables.map(table=>table.id));
  const visibleRequests=user?.role==='ADMIN'?openRequests:openRequests.filter(item=>{
    const table=tables.find(value=>value.id===item.tableId);
    return myTableIds.has(item.tableId)||!table?.assignedStaffEmail;
  });
  const openTimeouts=timeouts.filter(item=>item.status==='OPEN').sort((left,right)=>{
    if(left.severity!==right.severity)return left.severity==='CRITICAL'?-1:1;
    return new Date(left.deadlineAt).getTime()-new Date(right.deadlineAt).getTime();
  });
  const activeWalkIns=walkIns.filter(item=>!terminalWalkInStatuses.includes(item.status));
  const readyDishes=myTables.reduce((sum,table)=>sum+table.readyOrderCount,0);
  const attentionReservations=useMemo(()=>reservations.filter(item=>
    !terminalReservationStatuses.includes(item.status)&&item.source!=='WALK_IN'
  ).sort((left,right)=>new Date(`${left.reservationDate}T${left.reservationTime}`).getTime()-new Date(`${right.reservationDate}T${right.reservationTime}`).getTime()).slice(0,5),[reservations]);

  return <section className="role-dashboard page-section container">
    <div className="role-dashboard-heading"><div><p className="eyebrow dark">DASHBOARD NHÂN VIÊN PHỤC VỤ</p>
      <h1>Chào ca làm việc, {user?.fullName}</h1><p>Ưu tiên yêu cầu của khách, bàn đang phụ trách và lịch đặt sắp tới.</p></div>
      <button type="button" onClick={()=>void load()} disabled={loading}><RefreshCw/> {loading?'Đang tải':'Làm mới'}</button></div>
    {error&&<p className="error">{error}</p>}
    <div className="role-metrics">
      <Link to="/staff/dat-ban"><CalendarCheck/><span><b>{todayReservations.length}</b><small>Đặt bàn hôm nay</small></span></Link>
      <Link to="/staff/ban"><TableProperties/><span><b>{myTables.length}</b><small>{user?.role==='ADMIN'?'Bàn đang phục vụ':'Bàn của tôi'}</small></span></Link>
      <Link className={visibleRequests.length?'warning':''} to="/staff/phuc-vu"><BellRing/><span><b>{visibleRequests.length}</b><small>Yêu cầu từ bàn</small></span></Link>
      <Link className={openTimeouts.length?'danger':''} to="/staff/phuc-vu"><AlertTriangle/><span><b>{openTimeouts.length}</b><small>Cảnh báo cần xử lý</small></span></Link>
    </div>

    <div className="role-dashboard-grid">
      <section className="role-panel priority"><header><div><h2><BellRing/> Việc cần làm ngay</h2><p>Tự sắp theo mức độ cần xử lý.</p></div><Link to="/staff/phuc-vu">Mở phục vụ →</Link></header>
        <div className="role-task-list">
          {openTimeouts.slice(0,4).map(item=><article className={item.severity.toLowerCase()} key={`timeout-${item.id}`}><AlertTriangle/><div><b>{item.title}</b><small>{item.details}</small></div><time>{item.severity==='CRITICAL'?'Khẩn cấp':'Cảnh báo'}</time></article>)}
          {visibleRequests.slice(0,4).map(item=>{const table=tables.find(value=>value.id===item.tableId);return <article key={`request-${item.id}`}><BellRing/><div><b>{table?.code||`Bàn #${item.tableId}`} · {requestLabels[item.type]}</b><small>{item.note||'Khách vừa gửi yêu cầu từ mã QR tại bàn.'}</small></div><time>{item.status==='NEW'?'Mới':'Đã nhận'}</time></article>;})}
          {!openTimeouts.length&&!visibleRequests.length&&<p className="role-empty">Không có cảnh báo hoặc yêu cầu bàn đang chờ.</p>}
        </div>
      </section>

      <section className="role-panel"><header><div><h2><CalendarCheck/> Lịch phục vụ gần nhất</h2><p>Đơn đang hoạt động, xếp theo giờ đến.</p></div><Link to="/staff/dat-ban">Xem tất cả →</Link></header>
        <div className="role-reservation-list">{attentionReservations.map(item=><article key={item.id}><time><b>{item.reservationDate.split('-').reverse().slice(0,2).join('/')}</b><span>{item.reservationTime.slice(0,5)}</span></time><div><b>{item.customerName}</b><small>{item.code} · {item.partySize} khách · {item.assignedTables.map(table=>table.code).join(', ')||'Chưa xếp bàn'}</small></div><i className={item.status.toLowerCase()}>{item.status==='PENDING'?'Chờ xác nhận':item.status==='CONFIRMED'?'Sắp đến':'Đang phục vụ'}</i></article>)}
          {!attentionReservations.length&&<p className="role-empty">Chưa có lịch đặt bàn đang hoạt động.</p>}</div>
      </section>
    </div>

    <div className="role-shortcuts">
      <Link to="/staff/walk-in"><UsersRound/><span><b>Khách tại quán</b><small>{activeWalkIns.length} lượt đang xử lý</small></span></Link>
      <Link to="/staff/phuc-vu"><UtensilsCrossed/><span><b>Điều phối phục vụ</b><small>{readyDishes} món đã xong chờ mang lên</small></span></Link>
      <Link to="/staff/ban"><TableProperties/><span><b>Sơ đồ bàn</b><small>{tables.filter(table=>table.serviceState==='EMPTY').length} bàn đang trống</small></span></Link>
      <Link to="/staff/dat-ban"><Clock3/><span><b>Đơn chờ xác nhận</b><small>{pendingReservations.length} đơn cần kiểm tra cọc</small></span></Link>
    </div>
  </section>;
}

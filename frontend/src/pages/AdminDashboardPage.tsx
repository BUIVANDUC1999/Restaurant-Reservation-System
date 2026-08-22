import {
  Activity, AlertTriangle, Armchair, Banknote, CalendarCheck, CalendarClock, CheckCircle2,
  ChefHat, Clock3, Flame, TableProperties, UserCheck, UserCog, Users, UsersRound, UtensilsCrossed, X
} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {Link} from 'react-router-dom';
import {api} from '../api';
import type {AuthUser, DiningOrder, DiningOrderItem, OperationsDetails, OperationsReport, OperationalTimeout, TableOverview, UserStats, UserSummary} from '../types';
import {useOperationalEvents} from '../hooks/useOperationalEvents';

const money=(value:number)=>value.toLocaleString('vi-VN')+' ₫';
const serviceLabels:Record<TableOverview['serviceState'],string>={
  EMPTY:'Bàn trống',RESERVED:'Đã đặt trước',DINING:'Đang dùng bữa',WAITING_KITCHEN:'Đang chờ bếp',
  NEEDS_SERVING:'Có món chờ mang',NEEDS_CLEANING:'Cần dọn bàn',INACTIVE:'Tạm ngưng'
};
const itemLabels:Record<DiningOrderItem['status'],string>={
  SUBMITTED:'Chờ bếp nhận',PREPARING:'Đang chế biến',DELAYED:'Bếp đã báo chậm',
  READY:'Đã xong · chờ mang',SERVED:'Đã phục vụ',CANCELLED:'Đã hủy'
};
const activeItemStatuses:DiningOrderItem['status'][]=['SUBMITTED','PREPARING','DELAYED'];
type MetricDetail='ALL_USERS'|'EMPLOYEES'|'CUSTOMERS'|'ACTIVE_USERS'|'TODAY_RESERVATIONS'|'ACTIVE_SESSIONS'|'TODAY_REVENUE'|'MONTH_REVENUE';
const metricTitles:Record<MetricDetail,string>={
  ALL_USERS:'Toàn bộ tài khoản',EMPLOYEES:'Tài khoản nhân viên',CUSTOMERS:'Tài khoản khách hàng',
  ACTIVE_USERS:'Tài khoản đang hoạt động',TODAY_RESERVATIONS:'Đặt bàn hôm nay',
  ACTIVE_SESSIONS:'Các bàn đang phục vụ',TODAY_REVENUE:'Doanh thu hôm nay',MONTH_REVENUE:'Doanh thu tháng này'
};
const roleLabels:Record<AuthUser['role'],string>={ADMIN:'Quản trị viên',STAFF:'Nhân viên phục vụ',KITCHEN:'Nhân viên bếp',CUSTOMER:'Khách hàng'};
const reservationLabels:Record<string,string>={PENDING:'Chờ xác nhận',CONFIRMED:'Đã xác nhận',CHECKED_IN:'Đã đến',COMPLETED:'Hoàn tất',CANCELLED:'Đã hủy',REJECTED:'Từ chối',NO_SHOW:'Không đến',EXPIRED:'Hết hạn'};
const paymentLabels:Record<string,string>={CASH:'Tiền mặt',BANK_TRANSFER:'Chuyển khoản',QR:'QR',CARD:'Thẻ',PAYPAL:'PayPal'};

function itemTiming(item:DiningOrderItem,now:number){
  if(!activeItemStatuses.includes(item.status))return{delayed:false,minutes:0,text:''};
  const difference=new Date(item.estimatedReadyAt).getTime()-now;
  if(difference<=0){
    const minutes=Math.max(1,Math.floor(Math.abs(difference)/60000));
    return{delayed:true,minutes,text:`Chậm ${minutes}p`};
  }
  const minutes=Math.max(1,Math.ceil(difference/60000));
  return{delayed:item.status==='DELAYED',minutes,text:item.status==='DELAYED'?`ETA còn ${minutes}p`:`Còn ${minutes}p`};
}

export default function AdminDashboardPage(){
  const[stats,setStats]=useState<UserStats>();
  const[report,setReport]=useState<OperationsReport>();
  const[operationDetails,setOperationDetails]=useState<OperationsDetails>();
  const[users,setUsers]=useState<UserSummary[]>([]);
  const[tables,setTables]=useState<TableOverview[]>([]);
  const[orders,setOrders]=useState<DiningOrder[]>([]);
  const[timeouts,setTimeouts]=useState<OperationalTimeout[]>([]);
  const[selectedId,setSelectedId]=useState<number>();
  const[metricDetail,setMetricDetail]=useState<MetricDetail>();
  const[now,setNow]=useState(Date.now());
  const[error,setError]=useState('');

  async function load(){
    try{
      const[userStats,operations,details,userData,tableData,orderData,timeoutData]=await Promise.all([
        api.adminUserStats(),api.adminOperationsReport(),api.adminOperationsDetails(),api.adminUsers(),api.tableOverview(),api.kitchenOrders(),api.timeouts()
      ]);
      setStats(userStats);setReport(operations);setOperationDetails(details);setUsers(userData);setTables(tableData);setOrders(orderData);setTimeouts(timeoutData);setError('');
    }catch(err){setError(err instanceof Error?err.message:'Không tải được thống kê');}
  }
  const realtime=useOperationalEvents(()=>void load());
  useEffect(()=>{
    void load();
    const refresh=setInterval(()=>void load(),10000);
    const clock=setInterval(()=>setNow(Date.now()),10000);
    return()=>{clearInterval(refresh);clearInterval(clock);};
  },[]);

  const selected=useMemo(()=>tables.find(table=>table.id===selectedId),[tables,selectedId]);
  const openKitchenTimeouts=useMemo(()=>timeouts.filter(timeout=>timeout.status==='OPEN'&&timeout.type==='KITCHEN_SLA'),[timeouts]);
  const tableOrders=(table:TableOverview)=>orders.filter(order=>
    order.serviceSessionId===table.serviceSessionId||order.tableCodes.includes(table.code));
  const delayedItems=(table:TableOverview)=>tableOrders(table).flatMap(order=>order.items)
    .filter(item=>itemTiming(item,now).delayed);
  const selectedOrders=selected?tableOrders(selected):[];
  const selectedItems=selectedOrders.flatMap(order=>order.items).sort((left,right)=>{
    const leftTiming=itemTiming(left,now),rightTiming=itemTiming(right,now);
    if(leftTiming.delayed!==rightTiming.delayed)return leftTiming.delayed?-1:1;
    return new Date(left.estimatedReadyAt).getTime()-new Date(right.estimatedReadyAt).getTime();
  });
  const servingTables=tables.filter(table=>table.serviceSessionId);
  const totalGuests=servingTables.reduce((sum,table)=>sum+(table.partySize||0),0);
  const tablesWithDelay=tables.filter(table=>delayedItems(table).length>0);
  const detailUsers=users.filter(user=>metricDetail==='EMPLOYEES'?['STAFF','KITCHEN'].includes(user.role):
    metricDetail==='CUSTOMERS'?user.role==='CUSTOMER':metricDetail==='ACTIVE_USERS'?user.active:true);
  const detailPayments=metricDetail==='TODAY_REVENUE'?operationDetails?.paymentsToday||[]:
    metricDetail==='MONTH_REVENUE'?operationDetails?.paymentsThisMonth||[]:[];
  const metricLink=metricDetail&&['ALL_USERS','EMPLOYEES','CUSTOMERS','ACTIVE_USERS'].includes(metricDetail)?'/admin/tai-khoan':
    metricDetail==='TODAY_RESERVATIONS'?'/staff/dat-ban':metricDetail==='ACTIVE_SESSIONS'?'/staff/ban':'/staff/thanh-toan';

  return <section className="page-section container admin-page">
    <div className="admin-live-heading">
      <div><p className="eyebrow dark">TRUNG TÂM QUẢN TRỊ</p><h1>Tổng quan hệ thống</h1>
        <p className="page-lead">Theo dõi tài khoản, doanh thu và tình trạng vận hành nhà hàng.</p></div>
      <span className={realtime.connected?'connected':''}>{realtime.connected?'● Realtime đang kết nối':'Tự làm mới mỗi 10 giây'}</span>
    </div>
    {error&&<p className="error">{error}</p>}
    <div className="admin-stats">
      <button type="button" className="admin-stat-card" onClick={()=>setMetricDetail('ALL_USERS')}><Users/><span><b>{stats?.totalCount??'—'}</b><small>Tổng tài khoản</small><em>Xem chi tiết →</em></span></button>
      <button type="button" className="admin-stat-card" onClick={()=>setMetricDetail('EMPLOYEES')}><UserCog/><span><b>{stats?.employeeCount??'—'}</b><small>Tài khoản nhân viên</small><em>Xem chi tiết →</em></span></button>
      <button type="button" className="admin-stat-card" onClick={()=>setMetricDetail('CUSTOMERS')}><UsersRound/><span><b>{stats?.customerCount??'—'}</b><small>Tài khoản khách hàng</small><em>Xem chi tiết →</em></span></button>
      <button type="button" className="admin-stat-card" onClick={()=>setMetricDetail('ACTIVE_USERS')}><Activity/><span><b>{stats?.activeCount??'—'}</b><small>Tài khoản hoạt động</small><em>Xem chi tiết →</em></span></button>
    </div>

    <h2 className="admin-section-title">Vận hành hôm nay</h2>
    <div className="admin-stats operations-stats">
      <button type="button" className="admin-stat-card" onClick={()=>setMetricDetail('TODAY_RESERVATIONS')}><CalendarCheck/><span><b>{report?.reservationsToday??'—'}</b><small>Đặt bàn hôm nay</small><em>Xem danh sách →</em></span></button>
      <button type="button" className="admin-stat-card" onClick={()=>setMetricDetail('ACTIVE_SESSIONS')}><Activity/><span><b>{report?.activeSessions??'—'}</b><small>Bàn đang phục vụ</small><em>Xem từng bàn →</em></span></button>
      <button type="button" className="admin-stat-card" onClick={()=>setMetricDetail('TODAY_REVENUE')}><Banknote/><span><b>{report?money(report.revenueToday):'—'}</b><small>Doanh thu hôm nay</small><em>Xem hóa đơn →</em></span></button>
      <button type="button" className="admin-stat-card" onClick={()=>setMetricDetail('MONTH_REVENUE')}><Banknote/><span><b>{report?money(report.revenueThisMonth):'—'}</b><small>Doanh thu tháng này</small><em>Xem hóa đơn →</em></span></button>
    </div>

    <div className="admin-table-section">
      <header>
        <div><p className="eyebrow dark">GIÁM SÁT BÀN TRỰC TIẾP</p><h2>Tình trạng toàn bộ nhà hàng</h2>
          <p>Bấm vào từng bàn để xem khách, nhân viên phụ trách và tiến độ món ăn.</p></div>
        <Link to="/staff/ban">Mở sơ đồ điều phối <b>→</b></Link>
      </header>
      <div className="admin-table-metrics">
        <span><Armchair/><b>{tables.length}</b><small>Tổng số bàn</small></span>
        <span><TableProperties/><b>{servingTables.length}</b><small>Đang phục vụ</small></span>
        <span><Users/><b>{totalGuests}</b><small>Khách đang dùng bữa</small></span>
        <span className={tablesWithDelay.length?'danger':''}><AlertTriangle/><b>{tablesWithDelay.length}</b><small>Bàn có món chậm</small></span>
      </div>
      <div className="admin-table-grid">
        {tables.map(table=>{
          const delayed=delayedItems(table);
          const critical=openKitchenTimeouts.some(timeout=>timeout.tableId===table.id&&timeout.severity==='CRITICAL');
          return <button key={table.id} className={`${table.serviceState.toLowerCase()} ${delayed.length?'has-delay':''} ${critical?'critical':''}`}
            onClick={()=>setSelectedId(table.id)}>
            <span><b>{table.code}</b><small>{serviceLabels[table.serviceState]}</small></span>
            <strong><Users/> {table.serviceSessionId?`${table.partySize||0} khách`:`${table.seats} chỗ`}</strong>
            {table.serviceSessionId&&<em><UserCheck/> {table.assignedStaffName||'Chưa phân công'}</em>}
            {delayed.length>0&&<i><AlertTriangle/> {delayed.length} món chậm</i>}
          </button>;
        })}
      </div>
    </div>

    <h2 className="admin-section-title">Chức năng quản trị</h2>
    <div className="admin-actions">
      <Link to="/admin/tai-khoan"><UserCog/><div><h2>Quản lý tài khoản</h2><p>Xem danh sách Admin, nhân viên và khách hàng.</p></div><b>→</b></Link>
      <Link to="/admin/ca-lam-viec"><CalendarClock/><div><h2>Ca làm việc</h2><p>Xếp ca, theo dõi tải bàn và kiểm tra lịch sử bàn giao.</p></div><b>→</b></Link>
      <Link to="/staff/dat-ban"><CalendarCheck/><div><h2>Quản lý đặt bàn</h2><p>Xác nhận yêu cầu, món đặt trước và tiếp nhận khách.</p></div><b>→</b></Link>
      <Link to="/staff/ban"><TableProperties/><div><h2>Quản lý bàn</h2><p>Theo dõi sơ đồ và trạng thái bàn theo thời gian thực.</p></div><b>→</b></Link>
      <Link to="/staff/thuc-don"><UtensilsCrossed/><div><h2>Quản lý món ăn</h2><p>Thêm món, sửa giá, hình ảnh và trạng thái phục vụ.</p></div><b>→</b></Link>
      <Link to="/bep/dieu-phoi"><ChefHat/><div><h2>Điều phối bếp</h2><p>Theo dõi phiếu mới, món đang chế biến và món sẵn sàng.</p></div><b>→</b></Link>
      <Link to="/staff/thanh-toan"><Banknote/><div><h2>Thanh toán</h2><p>Lập hóa đơn, giảm giá và xác nhận thanh toán theo bàn.</p></div><b>→</b></Link>
    </div>

    {selected&&<div className="admin-table-detail-backdrop" onMouseDown={event=>{if(event.target===event.currentTarget)setSelectedId(undefined);}}>
      <aside className="admin-table-detail">
        <header className={selected.serviceState.toLowerCase()}>
          <div><span>{selected.code}</span><section><small>{serviceLabels[selected.serviceState]}</small><h2>{selected.name}</h2></section></div>
          <button onClick={()=>setSelectedId(undefined)} aria-label="Đóng chi tiết bàn"><X/></button>
        </header>
        <div className="admin-table-detail-summary">
          <span><Users/><small>Số khách</small><b>{selected.serviceSessionId?`${selected.partySize||0} người`:'Chưa phục vụ'}</b></span>
          <span><UserCheck/><small>Nhân viên phụ trách</small><b>{selected.assignedStaffName||'Chưa phân công'}</b></span>
          <span><UtensilsCrossed/><small>Phiếu món đang mở</small><b>{selected.openOrderCount}</b></span>
          <span className={delayedItems(selected).length?'danger':''}><AlertTriangle/><small>Món chậm</small><b>{delayedItems(selected).length}</b></span>
        </div>
        {selected.reservationCode&&<div className="admin-table-customer">
          <b>{selected.customerName}</b><span>Mã {selected.reservationCode} · {selected.partySize} khách</span>
        </div>}
        <section className="admin-dish-progress">
          <div className="admin-detail-title"><h3><ChefHat/> Tiến độ món ăn</h3><small>{selectedItems.length} món đang theo dõi</small></div>
          {!selected.serviceSessionId&&<p className="admin-detail-empty">Bàn chưa có phiên phục vụ đang mở.</p>}
          {selected.serviceSessionId&&!selectedItems.length&&<p className="admin-detail-empty">Bàn chưa gọi món hoặc các phiếu đã hoàn tất.</p>}
          {selectedItems.map(item=>{
            const timing=itemTiming(item,now);
            return <article className={`${item.status.toLowerCase()} ${timing.delayed?'delayed':''}`} key={item.id}>
              <span>{item.status==='READY'?<CheckCircle2/>:item.status==='PREPARING'?<Flame/>:<Clock3/>}</span>
              <div><b>{item.quantity}× {item.itemName}</b><small>{itemLabels[item.status]} · ETA {new Date(item.estimatedReadyAt).toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'})}</small>
                {item.delayReason&&<em>Lý do: {item.delayReason}</em>}</div>
              <strong>{timing.text||itemLabels[item.status]}</strong>
            </article>;
          })}
        </section>
        <footer><Link to="/staff/ban" onClick={()=>setSelectedId(undefined)}>Điều phối nhân viên</Link><Link to="/bep/dieu-phoi" onClick={()=>setSelectedId(undefined)}>Mở bảng bếp</Link></footer>
      </aside>
    </div>}

    {metricDetail&&<div className="admin-metric-backdrop" onMouseDown={event=>{if(event.target===event.currentTarget)setMetricDetail(undefined);}}>
      <section className="admin-metric-detail">
        <header><div><p className="eyebrow dark">CHI TIẾT CHỈ SỐ</p><h2>{metricTitles[metricDetail]}</h2><span>Dữ liệu mới nhất từ hệ thống vận hành</span></div>
          <button type="button" onClick={()=>setMetricDetail(undefined)} aria-label="Đóng chi tiết"><X/></button></header>
        <div className="admin-metric-content">
          {['ALL_USERS','EMPLOYEES','CUSTOMERS','ACTIVE_USERS'].includes(metricDetail)&&<div className="admin-metric-table-wrap"><table><thead><tr><th>Họ tên</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th>Ngày tạo</th></tr></thead>
            <tbody>{detailUsers.map(user=><tr key={user.id}><td><b>{user.fullName}</b></td><td>{user.email}</td><td>{roleLabels[user.role]}</td><td><i className={user.active?'active':'inactive'}>{user.active?'Hoạt động':'Đã khóa'}</i></td><td>{new Date(user.createdAt).toLocaleDateString('vi-VN')}</td></tr>)}</tbody></table>
            {!detailUsers.length&&<p className="admin-metric-empty">Chưa có tài khoản thuộc nhóm này.</p>}</div>}
          {metricDetail==='TODAY_RESERVATIONS'&&<div className="admin-metric-table-wrap"><table><thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Giờ / số khách</th><th>Bàn</th><th>Trạng thái</th></tr></thead>
            <tbody>{operationDetails?.reservationsToday.map(item=><tr key={item.id}><td><b>{item.code}</b><small>{item.source==='ONLINE'?'Đặt online':'Khách tại quán'}</small></td><td>{item.customerName}<small>{item.phone}</small></td><td>{item.reservationTime.slice(0,5)} · {item.partySize} khách</td><td>{item.tableCodes.join(', ')||'Chưa xếp'}</td><td><i>{reservationLabels[item.status]||item.status}</i></td></tr>)}</tbody></table>
            {!operationDetails?.reservationsToday.length&&<p className="admin-metric-empty">Hôm nay chưa có lượt đặt bàn.</p>}</div>}
          {metricDetail==='ACTIVE_SESSIONS'&&<div className="admin-session-list">{operationDetails?.activeSessions.map(item=><article key={item.serviceSessionId}><span><b>{item.tableCodes.join(', ')||'Chưa xếp bàn'}</b><small>{item.reservationCode}</small></span><div><strong>{item.customerName}</strong><small>{item.partySize} khách · Bắt đầu {new Date(item.openedAt).toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'})}</small></div><em><UserCheck/> {item.assignedStaffName||'Chưa phân công'}</em></article>)}
            {!operationDetails?.activeSessions.length&&<p className="admin-metric-empty">Hiện không có bàn đang phục vụ.</p>}</div>}
          {['TODAY_REVENUE','MONTH_REVENUE'].includes(metricDetail)&&<div className="admin-metric-table-wrap"><table><thead><tr><th>Hóa đơn</th><th>Đơn / khách hàng</th><th>Phương thức</th><th>Thời gian</th><th>Thành tiền</th></tr></thead>
            <tbody>{detailPayments.map(item=><tr key={item.id}><td><b>{item.invoiceCode}</b></td><td>{item.reservationCode}<small>{item.customerName}</small></td><td>{paymentLabels[item.method]||item.method}</td><td>{new Date(item.paidAt).toLocaleString('vi-VN')}</td><td><strong>{money(item.totalAmount)}</strong></td></tr>)}</tbody></table>
            {!detailPayments.length&&<p className="admin-metric-empty">Chưa có hóa đơn đã thanh toán trong khoảng thời gian này.</p>}
            {!!detailPayments.length&&<div className="admin-metric-total"><span>Tổng cộng</span><b>{money(detailPayments.reduce((sum,item)=>sum+item.totalAmount,0))}</b></div>}</div>}
        </div>
        <footer><small>Bấm vào khu vực ngoài cửa sổ để đóng.</small><Link to={metricLink} onClick={()=>setMetricDetail(undefined)}>Mở màn hình quản lý <b>→</b></Link></footer>
      </section>
    </div>}
  </section>;
}

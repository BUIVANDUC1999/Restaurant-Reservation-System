import {AlertTriangle, BellRing, ChefHat, CheckCircle2, Clock3, Flame, RefreshCw} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {Link} from 'react-router-dom';
import {api} from '../api';
import {useAuth} from '../auth';
import {useOperationalEvents} from '../hooks/useOperationalEvents';
import type {DiningOrder, DiningOrderItem} from '../types';

type QueueItem=DiningOrderItem&{orderId:number;createdAt:string;tableCodes:string[];customerName:string};
const activeStatuses=['SUBMITTED','PREPARING','DELAYED'];
const statusLabels:Record<DiningOrderItem['status'],string>={SUBMITTED:'Chờ bếp nhận',PREPARING:'Đang chế biến',DELAYED:'Đã báo chậm',READY:'Chờ mang lên',SERVED:'Đã phục vụ',CANCELLED:'Đã hủy'};
const time=(value:string)=>new Date(value).toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'});

export default function KitchenDashboardPage(){
  const{user}=useAuth();
  const[orders,setOrders]=useState<DiningOrder[]>([]);
  const[loading,setLoading]=useState(true);
  const[error,setError]=useState('');
  const[now,setNow]=useState(Date.now());
  async function load(silent=false){if(!silent)setLoading(true);try{setOrders(await api.kitchenOrders());setError('');}catch(err){setError(err instanceof Error?err.message:'Không tải được dashboard bếp');}finally{if(!silent)setLoading(false);}}
  useOperationalEvents(()=>void load(true));
  useEffect(()=>{void load();const refresh=setInterval(()=>void load(true),5000);const clock=setInterval(()=>setNow(Date.now()),10000);return()=>{clearInterval(refresh);clearInterval(clock);};},[]);

  const items=useMemo<QueueItem[]>(()=>orders.flatMap(order=>order.items.map(item=>({...item,orderId:order.id,createdAt:order.createdAt,tableCodes:order.tableCodes,customerName:order.customerName}))),[orders]);
  const submitted=items.filter(item=>item.status==='SUBMITTED');
  const preparing=items.filter(item=>item.status==='PREPARING'||item.status==='DELAYED');
  const ready=items.filter(item=>item.status==='READY');
  const delayed=items.filter(item=>activeStatuses.includes(item.status)&&new Date(item.estimatedReadyAt).getTime()<=now);
  const priority=[...items].filter(item=>activeStatuses.includes(item.status)||item.status==='READY').sort((left,right)=>{
    const leftLate=new Date(left.estimatedReadyAt).getTime()<=now,rightLate=new Date(right.estimatedReadyAt).getTime()<=now;
    if(leftLate!==rightLate)return leftLate?-1:1;
    if(left.status==='READY'&&right.status!=='READY')return -1;
    if(right.status==='READY'&&left.status!=='READY')return 1;
    return new Date(left.estimatedReadyAt).getTime()-new Date(right.estimatedReadyAt).getTime();
  }).slice(0,8);

  return <section className="role-dashboard kitchen-role-dashboard page-section container">
    <div className="role-dashboard-heading"><div><p className="eyebrow dark">DASHBOARD NHÂN VIÊN BẾP</p><h1>Nhịp bếp hiện tại, {user?.fullName}</h1><p>Nắm nhanh tải bếp, món sắp trễ và món cần bàn phục vụ nhận ngay.</p></div><button type="button" onClick={()=>void load()} disabled={loading}><RefreshCw/> {loading?'Đang tải':'Làm mới'}</button></div>
    {error&&<p className="error">{error}</p>}
    <div className="role-metrics">
      <Link to="/bep/dieu-phoi"><BellRing/><span><b>{submitted.length}</b><small>Chờ bếp nhận</small></span></Link>
      <Link to="/bep/dieu-phoi"><Flame/><span><b>{preparing.length}</b><small>Đang chế biến</small></span></Link>
      <Link className={delayed.length?'danger':''} to="/bep/dieu-phoi"><AlertTriangle/><span><b>{delayed.length}</b><small>Món đã quá ETA</small></span></Link>
      <Link className={ready.length?'success':''} to="/bep/dieu-phoi"><CheckCircle2/><span><b>{ready.length}</b><small>Chờ mang lên bàn</small></span></Link>
    </div>
    <section className="role-panel kitchen-priority-panel"><header><div><h2><ChefHat/> Hàng đợi ưu tiên</h2><p>Món quá ETA và món đã xong được đưa lên đầu.</p></div><Link to="/bep/dieu-phoi">Mở bảng điều phối →</Link></header>
      <div className="kitchen-dashboard-list">{priority.map((item,index)=>{const late=activeStatuses.includes(item.status)&&new Date(item.estimatedReadyAt).getTime()<=now;const lateMinutes=late?Math.max(1,Math.floor((now-new Date(item.estimatedReadyAt).getTime())/60000)):0;return <article className={`${late?'late':''} ${item.status.toLowerCase()}`} key={item.id}><em>{index+1}</em><div><b>{item.quantity}× {item.itemName}</b><small>Phiếu #{item.orderId} · {item.tableCodes.join(', ')||'Chưa có bàn'} · {item.customerName}</small></div><span><strong>{late?`Chậm ${lateMinutes}p`:statusLabels[item.status]}</strong><small>ETA {time(item.estimatedReadyAt)}</small></span></article>;})}
        {!priority.length&&<p className="role-empty">Bếp chưa có món cần xử lý.</p>}</div>
    </section>
    <div className="kitchen-dashboard-footer"><span><Clock3/> Tự cập nhật 5 giây một lần</span><Link className="btn btn-green" to="/bep/dieu-phoi"><ChefHat/> Bắt đầu điều phối món</Link></div>
  </section>;
}

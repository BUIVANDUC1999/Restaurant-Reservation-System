import{BellRing,ChefHat,Clock3,Flame,RefreshCw}from'lucide-react';
import{useEffect,useState}from'react';
import{api}from'../api';
import type{DiningOrder}from'../types';

export default function KitchenPage(){
  const[orders,setOrders]=useState<DiningOrder[]>([]);const[busy,setBusy]=useState<number>();const[error,setError]=useState('');
  async function load(){try{setOrders(await api.kitchenOrders());setError('')}catch(err){setError(err instanceof Error?err.message:'Không tải được phiếu bếp')}}
  useEffect(()=>{void load();const timer=setInterval(()=>void load(),10000);return()=>clearInterval(timer)},[]);
  async function update(order:DiningOrder,status:'PREPARING'|'READY'){setBusy(order.id);try{await api.updateKitchenOrder(order.id,status);await load()}catch(err){setError(err instanceof Error?err.message:'Không cập nhật được phiếu')}finally{setBusy(undefined)}}
  return <section className="kitchen-page page-section container"><div className="kitchen-heading"><div><p className="eyebrow dark">BỘ PHẬN BẾP</p><h1>Bảng điều phối món</h1><p>Tự động làm mới mỗi 10 giây.</p></div><button onClick={()=>void load()}><RefreshCw/> Làm mới</button></div>{error&&<p className="error">{error}</p>}
    <div className="kitchen-stats"><span><BellRing/><b>{orders.filter(x=>x.status==='SUBMITTED').length}</b> phiếu mới</span><span><Flame/><b>{orders.filter(x=>x.status==='PREPARING').length}</b> đang nấu</span><span><ChefHat/><b>{orders.filter(x=>x.status==='READY').length}</b> chờ phục vụ</span></div>
    <div className="kitchen-board">{orders.map(order=><article className={order.status.toLowerCase()} key={order.id}><header><div><b>PHIẾU #{order.id} {order.source==='PREORDER'&&<i>• MÓN ĐẶT TRƯỚC</i>}</b><strong>{order.tableCodes.join(', ')}</strong></div><span><Clock3/> {new Intl.DateTimeFormat('vi-VN',{hour:'2-digit',minute:'2-digit'}).format(new Date(order.createdAt))}</span></header><p>{order.customerName} • {order.reservationCode}</p><ul>{order.items.map(item=><li key={item.id}><b>{item.quantity}×</b><span>{item.itemName}</span></li>)}</ul>{order.note&&<blockquote>“{order.note}”</blockquote>}<footer>{order.status==='SUBMITTED'?<button disabled={busy===order.id} onClick={()=>update(order,'PREPARING')}><Flame/> Bắt đầu chế biến</button>:order.status==='PREPARING'?<button disabled={busy===order.id} onClick={()=>update(order,'READY')}><BellRing/> Báo món sẵn sàng</button>:<span><ChefHat/> Đang chờ nhân viên mang món</span>}</footer></article>)}{!orders.length&&<div className="kitchen-empty"><ChefHat/><h2>Bếp đã xử lý hết phiếu</h2><p>Phiếu mới từ nhân viên phục vụ sẽ xuất hiện tại đây.</p></div>}</div>
  </section>
}

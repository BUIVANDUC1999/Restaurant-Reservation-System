import{AlertTriangle,BellRing,ChefHat,Clock3,Flame,RefreshCw}from'lucide-react';
import{useEffect,useState}from'react';
import{api}from'../api';
import type{DiningOrder,DiningOrderItem}from'../types';

const label={SUBMITTED:'Mới nhận',PREPARING:'Đang nấu',DELAYED:'Chậm món',READY:'Sẵn sàng',SERVED:'Đã mang ra',CANCELLED:'Đã hủy'};
export default function KitchenPage(){
 const[orders,setOrders]=useState<DiningOrder[]>([]);const[busy,setBusy]=useState<number>();const[error,setError]=useState('');
 async function load(){try{setOrders(await api.kitchenOrders());setError('')}catch(e){setError(e instanceof Error?e.message:'Không tải được phiếu bếp')}}
 useEffect(()=>{void load();const timer=setInterval(()=>void load(),10000);return()=>clearInterval(timer)},[]);
 async function update(item:DiningOrderItem,status:string,delayMinutes?:number,reason?:string){setBusy(item.id);try{await api.updateKitchenItem(item.id,{status,delayMinutes,reason});await load()}catch(e){setError(e instanceof Error?e.message:'Không cập nhật được món')}finally{setBusy(undefined)}}
 function delay(item:DiningOrderItem){const minutes=Number(prompt('Món chậm thêm bao nhiêu phút?','15'));if(!minutes)return;const reason=prompt('Lý do chậm món?','Bếp đang đông')||'';void update(item,'DELAYED',minutes,reason)}
 const all=orders.flatMap(o=>o.items);
 return <section className="kitchen-page page-section container"><div className="kitchen-heading"><div><p className="eyebrow dark">SLA TỪNG MÓN</p><h1>Bảng điều phối bếp</h1><p>Mỗi món có thời gian dự kiến và lịch sử riêng.</p></div><button onClick={()=>void load()}><RefreshCw/> Làm mới</button></div>{error&&<p className="error">{error}</p>}
  <div className="kitchen-stats"><span><BellRing/><b>{all.filter(x=>x.status==='SUBMITTED').length}</b> món mới</span><span><Flame/><b>{all.filter(x=>x.status==='PREPARING').length}</b> đang nấu</span><span className="delayed"><AlertTriangle/><b>{all.filter(x=>x.status==='DELAYED').length}</b> chậm món</span><span><ChefHat/><b>{all.filter(x=>x.status==='READY').length}</b> chờ mang ra</span></div>
  <div className="kitchen-board">{orders.map(order=><article className={order.status.toLowerCase()} key={order.id}><header><div><b>PHIẾU #{order.id}</b><strong>{order.tableCodes.join(', ')}</strong></div><span><Clock3/> {new Intl.DateTimeFormat('vi-VN',{hour:'2-digit',minute:'2-digit'}).format(new Date(order.createdAt))}</span></header><p>{order.customerName} · {order.reservationCode}</p><div className="kitchen-item-list">{order.items.map(item=><div className={`kitchen-item ${item.status.toLowerCase()}`} key={item.id}><span><b>{item.quantity}× {item.itemName}</b><small>Dự kiến {new Intl.DateTimeFormat('vi-VN',{hour:'2-digit',minute:'2-digit'}).format(new Date(item.estimatedReadyAt))} · SLA {item.preparationMinutes} phút</small>{item.delayReason&&<em><AlertTriangle/> {item.delayReason}</em>}</span><i>{label[item.status]}</i><div>{item.status==='SUBMITTED'&&<button disabled={busy===item.id} onClick={()=>update(item,'PREPARING')}><Flame/> Bắt đầu</button>}{(item.status==='PREPARING'||item.status==='DELAYED')&&<><button className="delay" disabled={busy===item.id} onClick={()=>delay(item)}><AlertTriangle/> Báo chậm</button><button disabled={busy===item.id} onClick={()=>update(item,'READY')}><BellRing/> Sẵn sàng</button></>}</div></div>)}</div>{order.note&&<blockquote>“{order.note}”</blockquote>}</article>)}{!orders.length&&<div className="kitchen-empty"><ChefHat/><h2>Bếp đã xử lý hết món</h2></div>}</div>
 </section>
}

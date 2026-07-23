import{Armchair,BellRing,Check,Clock3,QrCode,Users}from'lucide-react';
import{useEffect,useMemo,useState}from'react';
import{api}from'../api';
import type{Notification,TableOverview,TableRequest}from'../types';

const serviceLabels:Record<TableOverview['serviceState'],string>={EMPTY:'Bàn trống',RESERVED:'Khách sắp đến',DINING:'Đang dùng bữa',WAITING_KITCHEN:'Đang chờ bếp',NEEDS_SERVING:'Có món cần mang ra',NEEDS_CLEANING:'Cần dọn bàn',INACTIVE:'Tạm ngưng'};
const requestLabels={CALL_WAITER:'Gọi nhân viên',WATER:'Xin thêm nước',UTENSILS:'Xin dụng cụ',PAYMENT:'Yêu cầu thanh toán'};

export default function TablesPage(){
 const[tables,setTables]=useState<TableOverview[]>([]);const[requests,setRequests]=useState<TableRequest[]>([]);const[alerts,setAlerts]=useState<Notification[]>([]);const[qr,setQr]=useState<TableOverview>();const[error,setError]=useState('');
 const load=async()=>{try{const[t,r,n]=await Promise.all([api.tableOverview(),api.tableRequests(),api.notifications()]);setTables(t);setRequests(r);setAlerts(n);setError('')}catch(e){setError(e instanceof Error?e.message:'Không tải được dữ liệu vận hành')}};
 useEffect(()=>{void load();const timer=setInterval(()=>void load(),10000);return()=>clearInterval(timer)},[]);
 const openRequests=useMemo(()=>requests.filter(r=>r.status==='NEW'||r.status==='ACKNOWLEDGED'),[requests]);
 async function requestAction(id:number,status:string){await api.updateTableRequest(id,status);await load()}
 return <section className="tables-page page-section container"><div className="staff-heading"><div><p className="eyebrow dark">SƠ ĐỒ VẬN HÀNH THỜI GIAN THỰC</p><h1>Nhà hàng · Một tầng</h1></div><span>Tự làm mới mỗi 10 giây</span></div>{error&&<p className="error">{error}</p>}
  <div className="operations-alert-strip">{alerts.filter(a=>!a.readAt).slice(0,5).map(a=><button key={a.id} onClick={async()=>{await api.readNotification(a.id);await load()}}><BellRing/><span><b>{a.title}</b><small>{a.message}</small></span><Check/></button>)}</div>
  {!!openRequests.length&&<div className="table-request-board"><h2><BellRing/> Yêu cầu từ QR tại bàn</h2>{openRequests.map(r=>{const table=tables.find(t=>t.id===r.tableId);return <article className={r.status.toLowerCase()} key={r.id}><b>{table?.code||`Bàn #${r.tableId}`}</b><span>{requestLabels[r.type]}</span><small>{r.note}</small>{r.status==='NEW'?<button onClick={()=>requestAction(r.id,'ACKNOWLEDGED')}>Nhận xử lý</button>:<button onClick={()=>requestAction(r.id,'DONE')}><Check/> Đã xong</button>}</article>})}</div>}
  <div className="table-legend">{Object.entries(serviceLabels).map(([state,label])=><span key={state}><i className={`dot ${state.toLowerCase()}`}/>{label}</span>)}<span><i className="dot urgent"/>Gọi nhân viên</span></div>
  <div className="restaurant-map"><div className="map-zone window-zone">KHU CỬA SỔ</div><div className="map-zone hall-zone">SẢNH CHÍNH</div><div className="map-zone family-zone">KHU GIA ĐÌNH / RIÊNG TƯ</div>{tables.map(table=>{const call=openRequests.some(r=>r.tableId===table.id);return <article key={table.id} className={`map-table ${table.serviceState.toLowerCase()} ${call?'urgent':''} ${table.shape.toLowerCase()}`} style={{left:`${table.layoutX}%`,top:`${table.layoutY}%`}}><b>{table.code}</b><small><Users/> {table.seats}</small><span>{call?'Đang gọi NV':serviceLabels[table.serviceState]}</span><button title="Mã QR bàn" onClick={()=>setQr(table)}><QrCode/></button></article>})}</div>
  <div className="floor-summary"><span><Armchair/> {tables.length} bàn</span><span><Users/> {tables.reduce((n,t)=>n+t.seats,0)} chỗ</span><span><BellRing/> {openRequests.length} yêu cầu đang mở</span><span><Clock3/> {tables.filter(t=>t.serviceState==='WAITING_KITCHEN').length} bàn chờ bếp</span></div>
  {qr&&<div className="qr-table-modal"><div><button className="close" onClick={()=>setQr(undefined)}>×</button><QrCode/><h2>QR phục vụ · {qr.code}</h2><img src={`https://api.qrserver.com/v1/create-qr-code/?size=260x260&data=${encodeURIComponent(`${location.origin}/ban/${qr.publicToken}`)}`} alt={`QR ${qr.code}`}/><p>In mã này và đặt trên bàn. QR chỉ nhận yêu cầu khi bàn có phiên phục vụ.</p><b>{location.origin}/ban/{qr.publicToken}</b><button onClick={()=>window.print()}>In mã QR</button></div></div>}
 </section>
}

import{BellRing,CheckCircle2,CreditCard,Droplets,UtensilsCrossed,UserRound}from'lucide-react';
import{useEffect,useState}from'react';
import{useParams}from'react-router-dom';
import{api}from'../api';
import type{GuestTable}from'../types';

const actions=[['CALL_WAITER','Gọi nhân viên',UserRound],['WATER','Xin thêm nước',Droplets],['UTENSILS','Xin dụng cụ',UtensilsCrossed],['PAYMENT','Yêu cầu thanh toán',CreditCard]] as const;
export default function TableGuestPage(){
 const{token=''}=useParams();const[table,setTable]=useState<GuestTable>();const[note,setNote]=useState('');const[message,setMessage]=useState('');const[error,setError]=useState('');
 const load=()=>api.guestTable(token).then(setTable).catch(e=>setError(e instanceof Error?e.message:'QR bàn không hợp lệ'));
 useEffect(()=>{api.guestTable(token).then(setTable).catch(e=>setError(e instanceof Error?e.message:'QR bàn không hợp lệ'))},[token]);
 async function send(type:string){setError('');try{await api.createTableRequest(token,type,note);setMessage('Yêu cầu đã được gửi. Nhân viên sẽ đến bàn ngay.');setNote('');await load()}catch(e){setError(e instanceof Error?e.message:'Không gửi được yêu cầu')}}
 if(error&&!table)return <section className="guest-table-page"><BellRing/><h1>Không thể sử dụng QR</h1><p>{error}</p></section>;
 return <section className="guest-table-page"><p className="eyebrow dark">PHỤC VỤ TẠI BÀN</p><h1>{table?.name||'Đang tải...'}</h1><p>{table?.area} · {table?.seats} ghế</p>{!table?.activeSession?<div className="guest-inactive"><p>Bàn chưa được nhân viên check-in. Vui lòng liên hệ lễ tân.</p></div>:<><textarea placeholder="Ghi chú thêm (không bắt buộc)" value={note} onChange={e=>setNote(e.target.value)}/><div className="guest-actions">{actions.map(([type,label,Icon])=><button key={type} onClick={()=>send(type)}><Icon/><b>{label}</b></button>)}</div></>}{message&&<p className="guest-success"><CheckCircle2/>{message}</p>}{error&&<p className="error">{error}</p>}{!!table?.requests.length&&<div className="guest-open-requests"><h2>Yêu cầu đang xử lý</h2>{table.requests.map(r=><p key={r.id}><BellRing/> {r.type} · {r.status==='NEW'?'Đã gửi':'Nhân viên đã nhận'}</p>)}</div>}</section>
}

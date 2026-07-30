import{useEffect,useState}from'react';
import{AlertTriangle,CheckCircle2,X}from'lucide-react';

type Props={
  title:string;
  message:string;
  confirmLabel:string;
  reasonRequired?:boolean;
  reasonLabel?:string;
  tone?:'primary'|'danger';
  busy?:boolean;
  onCancel:()=>void;
  onConfirm:(reason:string)=>void;
};

export default function ConfirmationDialog({title,message,confirmLabel,reasonRequired=false,
  reasonLabel='Lý do / ghi chú',tone='primary',busy=false,onCancel,onConfirm}:Props){
  const[reason,setReason]=useState('');
  useEffect(()=>setReason(''),[title]);
  const invalid=reasonRequired&&!reason.trim();
  return <div className="confirm-overlay" role="presentation" onMouseDown={event=>{if(event.target===event.currentTarget&&!busy)onCancel()}}>
    <section className={`confirm-dialog ${tone}`} role="dialog" aria-modal="true" aria-labelledby="confirm-title">
      <header><span>{tone==='danger'?<AlertTriangle/>:<CheckCircle2/>}</span><button type="button" aria-label="Đóng" disabled={busy} onClick={onCancel}><X/></button></header>
      <h2 id="confirm-title">{title}</h2>
      <p>{message}</p>
      {(reasonRequired||reasonLabel)&&<label>{reasonLabel}{reasonRequired&&<b> *</b>}<textarea autoFocus={reasonRequired} maxLength={400} value={reason} onChange={e=>setReason(e.target.value)} placeholder={reasonRequired?'Bắt buộc nhập để lưu lịch sử xử lý':'Có thể để trống'}/><small>{reason.length}/400 ký tự</small></label>}
      <footer><button type="button" className="cancel" disabled={busy} onClick={onCancel}>Quay lại</button><button type="button" className={`confirm ${tone}`} disabled={busy||invalid} onClick={()=>onConfirm(reason.trim())}>{busy?'Đang xử lý...':confirmLabel}</button></footer>
    </section>
  </div>;
}

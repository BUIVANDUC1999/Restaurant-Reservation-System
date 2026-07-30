import{Check,Clock3}from'lucide-react';
import type{Reservation}from'../types';

const stopped=['CANCELLED','REJECTED','NO_SHOW','EXPIRED'];
const labels:Record<string,string>={
  CANCELLED:'Đơn đã hủy',REJECTED:'Nhà hàng đã từ chối',NO_SHOW:'Khách không đến',EXPIRED:'Hết thời gian giữ bàn'
};

export default function ReservationTimeline({reservation}:{reservation:Reservation}){
  const steps=[
    {label:'Đã nhận yêu cầu',done:true,time:reservation.createdAt},
    {label:'Đã thanh toán đặt cọc',done:reservation.depositStatus==='PAID',time:reservation.depositPaidAt},
    {label:'Nhà hàng đã xác nhận',done:['CONFIRMED','CHECKED_IN','COMPLETED'].includes(reservation.status),time:reservation.confirmedAt},
    {label:'Khách đã check-in',done:['CHECKED_IN','COMPLETED'].includes(reservation.status),time:reservation.checkedInAt},
    {label:'Hoàn tất phục vụ',done:reservation.status==='COMPLETED',time:reservation.completedAt}
  ];
  const firstPending=steps.findIndex(step=>!step.done);
  return <div className="reservation-progress" aria-label="Tiến trình đặt bàn">
    <h3>Tiến trình đặt bàn</h3>
    <ol>{steps.map((step,index)=><li className={step.done?'done':index===firstPending?'current':''} key={step.label}>
      <span>{step.done?<Check/>:<Clock3/>}</span><div><b>{step.label}</b>{step.time&&<small>{new Date(step.time).toLocaleString('vi-VN')}</small>}{index===firstPending&&!stopped.includes(reservation.status)&&<small>Đang chờ xử lý</small>}</div>
    </li>)}</ol>
    {stopped.includes(reservation.status)&&<p className="reservation-stopped">{labels[reservation.status]||reservation.status}</p>}
  </div>;
}

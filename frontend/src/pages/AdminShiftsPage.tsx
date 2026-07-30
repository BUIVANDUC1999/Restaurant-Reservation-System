import {Activity, ArrowRightLeft, CalendarClock, Check, Play, Plus, UserCheck, Users, X} from 'lucide-react';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {api} from '../api';
import type {StaffShift, UserSummary, WaiterAssignmentEvent, WaiterSummary} from '../types';

const statusLabels:Record<StaffShift['status'],string>={
  SCHEDULED:'Đã lên lịch',ACTIVE:'Đang trong ca',COMPLETED:'Đã kết thúc',CANCELLED:'Đã hủy'
};
const actionLabels:Record<WaiterAssignmentEvent['action'],string>={
  CLAIM:'Tự nhận bàn',ASSIGN:'Phân công',TRANSFER:'Bàn giao',UNASSIGN:'Bỏ phân công'
};
function inputTime(date:Date){
  const local=new Date(date.getTime()-date.getTimezoneOffset()*60000);
  return local.toISOString().slice(0,16);
}
const time=(value:string)=>new Date(value).toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'});

export default function AdminShiftsPage(){
  const[shifts,setShifts]=useState<StaffShift[]>([]);
  const[staff,setStaff]=useState<UserSummary[]>([]);
  const[waiters,setWaiters]=useState<WaiterSummary[]>([]);
  const[history,setHistory]=useState<WaiterAssignmentEvent[]>([]);
  const[staffId,setStaffId]=useState('');
  const[startsAt,setStartsAt]=useState(()=>inputTime(new Date()));
  const[endsAt,setEndsAt]=useState(()=>inputTime(new Date(Date.now()+8*3600000)));
  const[busy,setBusy]=useState(false);
  const[error,setError]=useState('');

  const load=useCallback(async()=>{
    try{
      const[shiftRows,users,onDuty,events]=await Promise.all([
        api.adminShifts(),api.adminUsers(),api.waiters(),api.waiterAssignmentHistory()
      ]);
      const serviceStaff=users.filter(user=>user.role==='STAFF'&&user.active);
      setShifts(shiftRows);setStaff(serviceStaff);setWaiters(onDuty);setHistory(events);
      if(serviceStaff.length)setStaffId(current=>current||String(serviceStaff[0].id));
      setError('');
    }catch(reason){setError(reason instanceof Error?reason.message:'Không tải được dữ liệu ca làm việc')}
  },[]);
  useEffect(()=>{void load()},[load]);
  const workload=useMemo(()=>new Map(waiters.map(waiter=>[waiter.id,waiter])),[waiters]);
  const active=shifts.filter(shift=>shift.onDuty);

  async function create(){
    if(!staffId)return;
    setBusy(true);setError('');
    try{
      await api.createShift({staffId:Number(staffId),startsAt:new Date(startsAt).toISOString(),endsAt:new Date(endsAt).toISOString()});
      await load();
    }catch(reason){setError(reason instanceof Error?reason.message:'Không tạo được ca làm việc')}
    finally{setBusy(false)}
  }
  async function action(operation:()=>Promise<unknown>){
    setBusy(true);setError('');
    try{await operation();await load()}
    catch(reason){setError(reason instanceof Error?reason.message:'Không cập nhật được ca làm việc')}
    finally{setBusy(false)}
  }

  return <section className="page-section container shifts-page">
    <div className="shift-heading"><div><p className="eyebrow dark">ĐIỀU PHỐI NHÂN SỰ</p>
      <h1>Ca làm việc & tải phục vụ</h1><p>Chỉ nhân viên đang trong ca mới được nhận hoặc được phân công bàn.</p></div>
      <span><Activity/><b>{active.length}</b> nhân viên đang trong ca</span></div>
    {error&&<p className="error">{error}</p>}

    <div className="shift-overview">
      <article><UserCheck/><span><b>{active.length}</b><small>Đang trong ca</small></span></article>
      <article><Users/><span><b>{waiters.reduce((sum,row)=>sum+row.guestCount,0)}</b><small>Khách đang phụ trách</small></span></article>
      <article><CalendarClock/><span><b>{waiters.reduce((sum,row)=>sum+row.tableCount,0)}</b><small>Bàn đã phân công</small></span></article>
      <article className={waiters.some(row=>row.loadLevel==='OVERLOADED')?'danger':''}><Activity/><span>
        <b>{waiters.filter(row=>row.loadLevel==='OVERLOADED').length}</b><small>Nhân viên quá tải</small></span></article>
    </div>

    <div className="shift-layout">
      <section className="shift-create">
        <h2><Plus/> Lên ca mới</h2>
        <label>Nhân viên<select value={staffId} onChange={event=>setStaffId(event.target.value)}>
          {staff.map(user=><option value={user.id} key={user.id}>{user.fullName}</option>)}
        </select></label>
        <div><label>Bắt đầu<input type="datetime-local" value={startsAt} onChange={event=>setStartsAt(event.target.value)}/></label>
          <label>Kết thúc<input type="datetime-local" value={endsAt} onChange={event=>setEndsAt(event.target.value)}/></label></div>
        <button disabled={busy||!staffId} onClick={create}><CalendarClock/> Tạo ca làm việc</button>
        <small>Ca từ 1–16 giờ và không được trùng với ca đang lên lịch hoặc đang hoạt động.</small>
      </section>

      <section className="shift-list">
        <header><h2>Ca hôm nay</h2><small>{shifts.length} ca</small></header>
        {!shifts.length&&<p className="empty">Chưa có ca làm việc hôm nay.</p>}
        {shifts.map(shift=>{
          const load=workload.get(shift.staffId);
          return <article key={shift.id} className={`${shift.status.toLowerCase()} ${load?.loadLevel.toLowerCase()||''}`}>
            <div className="shift-person"><i>{shift.staffName.split(' ').slice(-2).map(part=>part[0]).join('')}</i>
              <span><b>{shift.staffName}</b><small>{time(shift.startsAt)} – {time(shift.endsAt)}</small></span></div>
            <em>{statusLabels[shift.status]}</em>
            <div className="shift-load">
              {load?<><span><b>{load.tableCount}</b> bàn</span><span><b>{load.guestCount}</b> khách</span>
                <strong className={load.loadLevel.toLowerCase()}>{load.loadLevel==='NORMAL'?'Ổn định':load.loadLevel==='BUSY'?'Đang bận':'Quá tải'}</strong>
                <small>{load.tableCodes.length?`Bàn ${load.tableCodes.join(', ')}`:'Chưa nhận bàn'}{load.recommended?' · Được đề xuất tiếp theo':''}</small></>
                :<small>Chưa vào ca hoặc đã kết thúc.</small>}
            </div>
            <div className="shift-actions">
              {shift.status==='SCHEDULED'&&<button disabled={busy} onClick={()=>action(()=>api.startShift(shift.id))}><Play/> Bắt đầu</button>}
              {shift.status==='ACTIVE'&&<button disabled={busy} onClick={()=>action(()=>api.completeShift(shift.id))}><Check/> Kết thúc</button>}
              {shift.status==='SCHEDULED'&&<button className="danger" disabled={busy} onClick={()=>action(()=>api.cancelShift(shift.id))}><X/> Hủy</button>}
            </div>
          </article>})}
      </section>
    </div>

    <section className="handoff-history">
      <header><div><ArrowRightLeft/><span><h2>Lịch sử phân công và bàn giao</h2>
        <p>Dữ liệu truy vết người thực hiện, người cũ, người mới và lý do.</p></span></div><b>{history.length} sự kiện gần nhất</b></header>
      {!history.length&&<p className="empty">Chưa có lịch sử phân công.</p>}
      {history.slice(0,20).map(event=><article key={event.id}>
        <time>{new Date(event.createdAt).toLocaleString('vi-VN')}</time>
        <strong>{actionLabels[event.action]} · Phiên #{event.serviceSessionId}</strong>
        <span>{event.fromStaffName||'Chưa phân công'} <ArrowRightLeft/> {event.toStaffName||'Chưa phân công'}</span>
        <small>{event.reason} · bởi {event.actor}</small>
      </article>)}
    </section>
  </section>;
}

import {AlertTriangle, Armchair, BellRing, Check, ChevronDown, Clock3, DoorOpen, History, Phone, Plus, RefreshCw, UserRoundCheck, Users, UtensilsCrossed, WalletCards, X} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import type {FormEvent} from 'react';
import {Link} from 'react-router-dom';
import {api} from '../api';
import type {WalkInMetrics, WalkInPriority, WalkInStatus, WalkInVisit} from '../types';

const statusLabel:Record<WalkInStatus,string>={
  WAITING:'Đang chờ bàn',TABLE_OFFERED:'Đã mời vào bàn',SEATED:'Đã ngồi bàn',DINING:'Đang dùng bữa',
  PAYMENT_REQUESTED:'Chờ thanh toán',CLEANING:'Chờ dọn bàn',COMPLETED:'Hoàn thành',
  LEFT:'Khách đã rời',NO_RESPONSE:'Không phản hồi',CANCELLED:'Đã hủy'
};
const priorityLabel:Record<WalkInPriority,string>={
  NORMAL:'Thông thường',ACCESSIBILITY:'Hỗ trợ tiếp cận',ELDERLY:'Người cao tuổi',MANAGER:'Quản lý ưu tiên'
};
const terminal:WalkInStatus[]=['COMPLETED','LEFT','NO_RESPONSE','CANCELLED'];
const clock=(value?:string)=>value?new Date(value).toLocaleTimeString('vi-VN',{hour:'2-digit',minute:'2-digit'}):'—';

export default function WalkInPage(){
  const[visits,setVisits]=useState<WalkInVisit[]>([]);
  const[metrics,setMetrics]=useState<WalkInMetrics>();
  const[openForm,setOpenForm]=useState(false);
  const[busy,setBusy]=useState<number|string>();
  const[error,setError]=useState('');
  const[form,setForm]=useState({customerName:'',phone:'',partySize:2,areaPreference:'',priority:'NORMAL' as WalkInPriority,priorityReason:'',quotedWaitMinutes:'',note:''});

  async function load(){
    try{const[v,m]=await Promise.all([api.walkIns(),api.walkInMetrics()]);setVisits(v);setMetrics(m);setError('')}
    catch(e){setError(e instanceof Error?e.message:'Không tải được hàng chờ')}
  }
  useEffect(()=>{void load();const timer=setInterval(()=>void load(),10000);return()=>clearInterval(timer)},[]);

  const active=useMemo(()=>visits.filter(v=>!terminal.includes(v.status)),[visits]);
  const history=useMemo(()=>visits.filter(v=>terminal.includes(v.status)).slice(0,20),[visits]);
  const critical=active.filter(v=>v.slaLevel==='CRITICAL').length;
  const waiting=active.filter(v=>v.status==='WAITING').length;

  async function create(event:FormEvent){
    event.preventDefault();setBusy('create');setError('');
    try{
      await api.createWalkIn({...form,phone:form.phone||null,quotedWaitMinutes:form.quotedWaitMinutes===''?null:Number(form.quotedWaitMinutes)});
      setForm({customerName:'',phone:'',partySize:2,areaPreference:'',priority:'NORMAL',priorityReason:'',quotedWaitMinutes:'',note:''});
      setOpenForm(false);await load();
    }catch(e){setError(e instanceof Error?e.message:'Không tiếp nhận được khách')}
    finally{setBusy(undefined)}
  }
  async function action(id:number,operation:()=>Promise<unknown>){
    setBusy(id);setError('');try{await operation();await load()}
    catch(e){setError(e instanceof Error?e.message:'Không thực hiện được thao tác')}
    finally{setBusy(undefined)}
  }
  function reviseQuote(visit:WalkInVisit){
    const value=prompt('Thời gian chờ mới (phút)',String(visit.quotedWaitMinutes));
    if(value===null)return;const minutes=Number(value);if(!Number.isFinite(minutes)||minutes<0)return;
    void action(visit.id,()=>api.reviseWalkInQuote(visit.id,minutes,'Lễ tân cập nhật ETA'));
  }

  return <section className="walkin-page page-section container">
    <div className="walkin-heading">
      <div><p className="eyebrow dark">FRONT-OF-HOUSE ORCHESTRATION</p><h1>Điều phối khách tại quán</h1>
        <p>Hàng chờ, ETA, bảo vệ lịch online và SLA từ lúc khách đến cho tới khi bàn được dọn xong.</p></div>
      <div><button onClick={()=>void load()}><RefreshCw/> Làm mới</button><button className="walkin-add" onClick={()=>setOpenForm(true)}><Plus/> Tiếp nhận khách</button></div>
    </div>
    {error&&<p className="error">{error}</p>}

    <div className="walkin-stats">
      <span><Users/><b>{waiting}</b><small>Nhóm đang chờ</small></span>
      <span><Armchair/><b>{active.filter(v=>v.status==='TABLE_OFFERED').length}</b><small>Đã mời vào bàn</small></span>
      <span><UtensilsCrossed/><b>{active.filter(v=>['SEATED','DINING'].includes(v.status)).length}</b><small>Đang phục vụ</small></span>
      <span className={critical?'critical':''}><AlertTriangle/><b>{critical}</b><small>Việc quá SLA</small></span>
    </div>
    {metrics&&<div className="walkin-kpis">
      <span><small>Chờ trung bình</small><b>{metrics.averageWaitMinutes} phút</b></span>
      <span><small>P90 thời gian chờ</small><b>{metrics.p90WaitMinutes} phút</b></span>
      <span><small>ETA chính xác ±5 phút</small><b>{metrics.quoteAccuracyPercent}%</b></span>
      <span><small>Tỷ lệ bỏ hàng chờ</small><b>{metrics.abandonmentPercent}%</b></span>
      <span><small>Dọn bàn trung bình</small><b>{metrics.averageCleaningMinutes} phút</b></span>
    </div>}

    {!!critical&&<div className="walkin-escalation"><BellRing/><div><b>Cần xử lý ngay</b>
      {active.filter(v=>v.slaLevel==='CRITICAL').map(v=><span key={v.id}>{v.code} · {v.customerName}: {v.slaMessage}</span>)}</div></div>}

    <div className="walkin-board">
      {active.map(visit=><article key={visit.id} className={`walkin-card ${visit.slaLevel.toLowerCase()} status-${visit.status.toLowerCase()}`}>
        <header><div><span>{visit.code}</span><h2>{visit.customerName}</h2><small><Users/> {visit.partySize} khách {visit.phone&&<>· <Phone/> {visit.phone}</>}</small></div>
          <div className="walkin-badges"><i>{priorityLabel[visit.priority]}</i><strong>{statusLabel[visit.status]}</strong></div></header>
        <div className="walkin-sla">
          <span><Clock3/><b>{visit.elapsedMinutes} phút</b> từ khi đến</span>
          <span className={visit.slaLevel.toLowerCase()}><i/>{visit.slaMessage}</span>
          {visit.status==='WAITING'&&<small>ETA {clock(visit.expectedSeatAt)} · đã báo chờ {visit.quotedWaitMinutes} phút</small>}
          {visit.status==='TABLE_OFFERED'&&<small>Giữ bàn đến {clock(visit.offerExpiresAt)} · đã gọi {visit.callCount} lần</small>}
        </div>
        {visit.note&&<p className="walkin-note">{visit.note}</p>}

        {visit.status==='WAITING'&&<div className="walkin-suggestions">
          <div><b>Đề xuất bàn</b><button onClick={()=>reviseQuote(visit)}>Sửa ETA</button></div>
          {visit.suggestedTables.map(table=><button key={table.id} disabled={busy===visit.id||!table.safe||new Date(table.availableAt)>new Date(Date.now()+30000)}
            className={table.safe?'safe':'unsafe'} onClick={()=>action(visit.id,()=>api.offerWalkInTable(visit.id,table.id))}>
            <span><strong>{table.code}</strong><small>{table.seats} ghế · {table.area}</small></span>
            <i>{table.reason}</i>
          </button>)}
        </div>}

        <div className="walkin-actions">
          {visit.status==='WAITING'&&<>
            <button className="secondary" onClick={()=>action(visit.id,()=>api.exitWalkIn(visit.id,'LEFT','Khách rời hàng chờ'))}>Khách rời</button>
            <button className="danger" onClick={()=>action(visit.id,()=>api.exitWalkIn(visit.id,'CANCELLED','Lễ tân hủy lượt'))}>Hủy lượt</button></>}
          {visit.status==='TABLE_OFFERED'&&<>
            <button className="primary" onClick={()=>action(visit.id,()=>api.walkInAction(visit.id,'seat'))}><UserRoundCheck/> Khách đã vào bàn</button>
            <button onClick={()=>action(visit.id,()=>api.walkInAction(visit.id,'call-again'))}><BellRing/> Gọi lại</button>
            <button className="danger" onClick={()=>action(visit.id,()=>api.exitWalkIn(visit.id,'NO_RESPONSE','Khách không phản hồi'))}>Không phản hồi</button></>}
          {visit.status==='SEATED'&&<>
            <Link to="/staff/phuc-vu"><UtensilsCrossed/> Gọi món</Link>
            <button className="primary" onClick={()=>action(visit.id,()=>api.walkInAction(visit.id,'dining'))}>Bắt đầu phục vụ</button></>}
          {visit.status==='DINING'&&<>
            <Link to="/staff/phuc-vu"><UtensilsCrossed/> Theo dõi món</Link>
            <button onClick={()=>action(visit.id,()=>api.walkInAction(visit.id,'payment'))}><WalletCards/> Khách cần thanh toán</button></>}
          {visit.status==='PAYMENT_REQUESTED'&&<>
            <Link to="/staff/thanh-toan"><WalletCards/> Mở thanh toán</Link>
            <button className="primary" onClick={()=>action(visit.id,()=>api.walkInAction(visit.id,'finish'))}><Check/> Đã thanh toán, kết thúc</button></>}
          {visit.status==='CLEANING'&&<button className="primary" onClick={()=>action(visit.id,()=>api.walkInAction(visit.id,'cleaned'))}><Check/> Bàn đã dọn xong</button>}
        </div>
        <details className="walkin-timeline"><summary><History/> Nhật ký trách nhiệm <ChevronDown/></summary>
          {visit.events.map(event=><p key={event.id}><b>{clock(event.createdAt)}</b><span>{event.action} · {event.actor}</span><small>{event.note}</small></p>)}</details>
      </article>)}
      {!active.length&&<div className="walkin-empty"><DoorOpen/><h2>Không có khách tại quán đang chờ</h2><p>Nhấn “Tiếp nhận khách” để tạo lượt Walk-in mới.</p></div>}
    </div>

    {!!history.length&&<section className="walkin-history"><h2><History/> Lịch sử gần đây</h2>{history.map(visit=><article key={visit.id}>
      <span><b>{visit.code}</b><small>{visit.customerName} · {visit.partySize} khách</small></span><i>{statusLabel[visit.status]}</i>
      {['LEFT','NO_RESPONSE'].includes(visit.status)&&<button onClick={()=>action(visit.id,()=>api.walkInAction(visit.id,'requeue',{quotedWaitMinutes:15,note:'Khách quay lại'}))}>Đưa lại hàng chờ</button>}
    </article>)}</section>}

    {openForm&&<div className="walkin-modal"><form onSubmit={create}>
      <div><span><p className="eyebrow dark">TIẾP NHẬN TẠI QUẦY</p><h2>Tạo lượt khách Walk-in</h2></span><button type="button" onClick={()=>setOpenForm(false)}><X/></button></div>
      <label>Tên khách/nhóm khách<input required maxLength={120} value={form.customerName} onChange={e=>setForm({...form,customerName:e.target.value})}/></label>
      <div className="walkin-form-grid"><label>Số điện thoại<input value={form.phone} onChange={e=>setForm({...form,phone:e.target.value})}/></label>
        <label>Số khách<input type="number" min="1" max="30" value={form.partySize} onChange={e=>setForm({...form,partySize:Number(e.target.value)})}/></label>
        <label>Khu vực mong muốn<input placeholder="Ví dụ: Cửa sổ" value={form.areaPreference} onChange={e=>setForm({...form,areaPreference:e.target.value})}/></label>
        <label>Thời gian báo khách chờ<input type="number" min="0" max="240" placeholder="Tự động tính" value={form.quotedWaitMinutes} onChange={e=>setForm({...form,quotedWaitMinutes:e.target.value})}/></label>
        <label>Mức ưu tiên<select value={form.priority} onChange={e=>setForm({...form,priority:e.target.value as WalkInPriority})}>{Object.entries(priorityLabel).map(([value,label])=><option key={value} value={value}>{label}</option>)}</select></label>
        <label>Lý do ưu tiên<input disabled={form.priority==='NORMAL'} value={form.priorityReason} onChange={e=>setForm({...form,priorityReason:e.target.value})}/></label></div>
      <label>Ghi chú<textarea value={form.note} onChange={e=>setForm({...form,note:e.target.value})}/></label>
      <button className="walkin-submit" disabled={busy==='create'}><Plus/> Tạo lượt và tính ETA</button>
    </form></div>}
  </section>;
}

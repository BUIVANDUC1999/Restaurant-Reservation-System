import {AlertTriangle, Armchair, BellRing, Check, ChevronDown, Clock3, DoorOpen, History, Phone, Plus, RefreshCw, UserRoundCheck, Users, UtensilsCrossed, WalletCards, X} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import type {FormEvent} from 'react';
import {Link} from 'react-router-dom';
import {api} from '../api';
import type {DemoScenarioInput, DemoScenarioType, MenuItem, TableOverview, WalkInMetrics, WalkInPriority, WalkInStatus, WalkInVisit} from '../types';
import {useOperationalEvents} from '../hooks/useOperationalEvents';

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
const shortSlaLabels:Record<string,string>={
  'Đang chờ đúng ETA':'Đúng giờ','Sắp đến thời gian đã hẹn':'Sắp đến lượt','Đã trễ thời gian xếp bàn':'Trễ xếp bàn',
  'Đang chờ khách vào bàn':'Chờ khách','Cần gọi khách lần nữa':'Gọi lại khách','Khách chưa gọi món':'Chưa gọi món',
  'Khách ngồi quá lâu chưa có phiếu món':'Chưa gọi món','Đang chờ thanh toán':'Chờ thanh toán',
  'Yêu cầu thanh toán sắp quá hạn':'Sắp trễ thanh toán','Yêu cầu thanh toán chưa được xử lý':'Thanh toán chậm',
  'Bàn đang được dọn':'Đang dọn','Bàn sắp quá thời gian dọn':'Sắp trễ dọn bàn','Bàn chưa được dọn đúng SLA':'Dọn bàn chậm'
};
const shortSla=(message:string)=>shortSlaLabels[message]||message.replace(/\s+phút\b/g,'p');
type DemoGroup='WALK_IN'|'RESERVATION'|'KITCHEN'|'QR'|'TABLE';
type DemoOption={type:DemoScenarioType;group:DemoGroup;title:string;description:string;tone:'normal'|'warning'|'critical'};
const demoGroupInfo:Record<DemoGroup,{title:string;description:string}>={
  WALK_IN:{title:'Khách tại quán',description:'Hàng chờ và SLA xếp bàn'},
  RESERVATION:{title:'Đặt bàn online',description:'Lịch mới, sắp đến, trễ và đặt cọc'},
  KITCHEN:{title:'Bếp & món ăn',description:'Món mới, đang nấu, chậm và đã xong'},
  QR:{title:'QR gọi phục vụ',description:'Gọi nhân viên, nước và thanh toán'},
  TABLE:{title:'Dọn & trạng thái bàn',description:'Bàn chờ dọn và quá SLA'}
};
const demoOptions:DemoOption[]=[
  {type:'WALK_IN_NORMAL',group:'WALK_IN',title:'Khách chờ bình thường',description:'Khách mới đến và còn trong ETA.',tone:'normal'},
  {type:'WALK_IN_WARNING',group:'WALK_IN',title:'Khách sắp quá ETA',description:'Đã dùng gần hết thời gian chờ được báo.',tone:'warning'},
  {type:'WALK_IN_CRITICAL',group:'WALK_IN',title:'Khách quá ETA',description:'Đã trễ xếp bàn và cần điều phối ngay.',tone:'critical'},
  {type:'RESERVATION_NEW',group:'RESERVATION',title:'Lịch đặt mới',description:'Đơn online mới, đang chờ đặt cọc.',tone:'normal'},
  {type:'RESERVATION_UPCOMING',group:'RESERVATION',title:'Khách sắp đến',description:'Lịch đã xác nhận, sắp đến giờ hẹn.',tone:'warning'},
  {type:'RESERVATION_LATE_WARNING',group:'RESERVATION',title:'Khách trễ 15 phút',description:'Mở cảnh báo xác minh khách đang đến.',tone:'warning'},
  {type:'RESERVATION_LATE_CRITICAL',group:'RESERVATION',title:'Khách trễ trên 20 phút',description:'Cần quyết định giữ hoặc giải phóng bàn.',tone:'critical'},
  {type:'RESERVATION_HOLD_EXPIRED',group:'RESERVATION',title:'Hết hạn giữ chỗ',description:'Khách chưa đặt cọc, hệ thống tự hết hạn.',tone:'critical'},
  {type:'RESERVATION_DEPOSIT_WAITING',group:'RESERVATION',title:'Cọc chờ xác nhận',description:'Đã nhận cọc nhưng nhân viên xử lý chậm.',tone:'warning'},
  {type:'KITCHEN_NEW_ORDER',group:'KITCHEN',title:'Món mới gọi',description:'Phiếu món đang chờ bếp tiếp nhận.',tone:'normal'},
  {type:'KITCHEN_PREPARING',group:'KITCHEN',title:'Món đang chế biến',description:'Bếp đã tiếp nhận và bắt đầu nấu.',tone:'normal'},
  {type:'KITCHEN_REPORTED_DELAY',group:'KITCHEN',title:'Bếp báo chậm',description:'Bếp chủ động cộng ETA và ghi lý do.',tone:'warning'},
  {type:'KITCHEN_OVERDUE_WARNING',group:'KITCHEN',title:'Món tự động quá ETA',description:'Hệ thống phát hiện món đã chậm.',tone:'warning'},
  {type:'KITCHEN_OVERDUE_CRITICAL',group:'KITCHEN',title:'Món chậm nghiêm trọng',description:'Quá ngưỡng điều phối khẩn cấp.',tone:'critical'},
  {type:'KITCHEN_READY',group:'KITCHEN',title:'Món đã xong',description:'Bếp báo xong, chờ nhân viên mang ra.',tone:'normal'},
  {type:'QR_CALL_WAITER',group:'QR',title:'Khách gọi nhân viên',description:'Yêu cầu hỗ trợ mới từ QR bàn.',tone:'normal'},
  {type:'QR_WATER',group:'QR',title:'Khách xin thêm nước',description:'Yêu cầu phục vụ từ bàn đang hoạt động.',tone:'normal'},
  {type:'QR_PAYMENT',group:'QR',title:'Khách yêu cầu thanh toán',description:'Yêu cầu thanh toán gửi từ QR bàn.',tone:'warning'},
  {type:'QR_UNANSWERED_WARNING',group:'QR',title:'QR chưa được nhận',description:'Nhân viên chưa tiếp nhận đúng SLA.',tone:'warning'},
  {type:'QR_UNANSWERED_CRITICAL',group:'QR',title:'QR quá hạn nghiêm trọng',description:'Yêu cầu bàn bị bỏ quên quá lâu.',tone:'critical'},
  {type:'TABLE_CLEANING_NORMAL',group:'TABLE',title:'Bàn đang chờ dọn',description:'Bàn vừa kết thúc lượt phục vụ.',tone:'normal'},
  {type:'TABLE_CLEANING_WARNING',group:'TABLE',title:'Dọn bàn chậm',description:'Bàn vừa vượt thời gian dọn mục tiêu.',tone:'warning'},
  {type:'TABLE_CLEANING_CRITICAL',group:'TABLE',title:'Dọn bàn quá hạn',description:'Bàn chưa sẵn sàng trong thời gian dài.',tone:'critical'}
];
const defaultDemoMinutes=(type:DemoScenarioType)=>{
  if(type==='WALK_IN_WARNING')return 15;
  if(type==='WALK_IN_CRITICAL')return 20;
  if(type==='RESERVATION_LATE_WARNING')return 16;
  if(type==='RESERVATION_LATE_CRITICAL')return 21;
  if(type==='KITCHEN_REPORTED_DELAY')return 8;
  if(type==='KITCHEN_OVERDUE_CRITICAL'||type==='QR_UNANSWERED_CRITICAL')return 12;
  if(type.includes('CLEANING'))return 18;
  return 4;
};
const initialDemoForm:DemoScenarioInput={type:'KITCHEN_OVERDUE_WARNING',customerName:'Gia đình demo',phone:'0901000001',email:'',notifyEmail:false,partySize:4,areaPreference:'',priority:'NORMAL',priorityReason:'',minutes:4,tableId:null,menuItemId:null,reason:'Bếp đang đông',note:'Tình huống tự tạo để trình bày'};

export default function WalkInPage(){
  const[visits,setVisits]=useState<WalkInVisit[]>([]);
  const[metrics,setMetrics]=useState<WalkInMetrics>();
  const[tables,setTables]=useState<TableOverview[]>([]);
  const[menuItems,setMenuItems]=useState<MenuItem[]>([]);
  const[openForm,setOpenForm]=useState(false);
  const[openDemoForm,setOpenDemoForm]=useState(false);
  const[demoGroup,setDemoGroup]=useState<DemoGroup>('KITCHEN');
  const[busy,setBusy]=useState<number|string>();
  const[error,setError]=useState('');
  const[notice,setNotice]=useState('');
  const[form,setForm]=useState({customerName:'',phone:'',partySize:2,areaPreference:'',priority:'NORMAL' as WalkInPriority,priorityReason:'',quotedWaitMinutes:'',note:''});
  const[demoForm,setDemoForm]=useState<DemoScenarioInput>(initialDemoForm);

  async function load(){
    try{const[v,m,t,dishes]=await Promise.all([api.walkIns(),api.walkInMetrics(),api.tableOverview(),api.staffMenu()]);setVisits(v);setMetrics(m);setTables(t);setMenuItems(dishes);setError('')}
    catch(e){setError(e instanceof Error?e.message:'Không tải được hàng chờ')}
  }
  useOperationalEvents(()=>void load());
  useEffect(()=>{void load();const timer=setInterval(()=>void load(),10000);return()=>clearInterval(timer)},[]);

  const active=useMemo(()=>visits.filter(v=>!terminal.includes(v.status)),[visits]);
  const history=useMemo(()=>visits.filter(v=>terminal.includes(v.status)).slice(0,20),[visits]);
  const critical=active.filter(v=>v.slaLevel==='CRITICAL').length;
  const waiting=active.filter(v=>v.status==='WAITING').length;
  const servingTables=tables.filter(table=>!!table.serviceSessionId);
  const servingGuests=servingTables.reduce((total,table)=>total+(table.partySize||0),0);
  const selectedDemo=demoOptions.find(option=>option.type===demoForm.type)??demoOptions[0];
  const demoNeedsTable=['KITCHEN','QR','TABLE'].includes(selectedDemo.group);
  const demoNeedsMenu=selectedDemo.group==='KITCHEN';
  const demoUsesCustomer=selectedDemo.group!=='TABLE';
  const demoUsesMinutes=['WALK_IN_WARNING','WALK_IN_CRITICAL','RESERVATION_LATE_WARNING','RESERVATION_LATE_CRITICAL','RESERVATION_HOLD_EXPIRED','RESERVATION_DEPOSIT_WAITING','KITCHEN_REPORTED_DELAY','KITCHEN_OVERDUE_WARNING','KITCHEN_OVERDUE_CRITICAL','QR_UNANSWERED_WARNING','QR_UNANSWERED_CRITICAL','TABLE_CLEANING_WARNING','TABLE_CLEANING_CRITICAL'].includes(demoForm.type);
  const demoUsesReason=['KITCHEN_REPORTED_DELAY','KITCHEN_OVERDUE_WARNING','KITCHEN_OVERDUE_CRITICAL'].includes(demoForm.type);

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
  async function createDemo(event:FormEvent){
    event.preventDefault();
    setBusy('demo');setError('');
    try{
      const result=await api.createDemoScenario(demoForm);
      setNotice(`${result.title}: ${result.message}`);
      setDemoForm(initialDemoForm);setOpenDemoForm(false);await load();
    }
    catch(e){setError(e instanceof Error?e.message:'Không tạo được dữ liệu demo')}
    finally{setBusy(undefined)}
  }

  return <section className="walkin-page page-section container">
    <div className="walkin-heading">
      <div><p className="eyebrow dark">FRONT-OF-HOUSE ORCHESTRATION</p><h1>Điều phối khách tại quán</h1>
        <p>Theo dõi hàng chờ và thời gian phục vụ.</p></div>
      <div><button onClick={()=>setOpenDemoForm(true)}><History/> Tạo tình huống demo</button><button onClick={()=>void load()}><RefreshCw/> Làm mới</button><button className="walkin-add" onClick={()=>setOpenForm(true)}><Plus/> Tiếp nhận khách</button></div>
    </div>
    {error&&<p className="error">{error}</p>}
    {notice&&<p className="walkin-success"><Check/> {notice}<button type="button" onClick={()=>setNotice('')}>Đóng</button></p>}

    <div className="walkin-stats">
      <span><Users/><b>{waiting}</b><small>Nhóm đang chờ</small></span>
      <span><Armchair/><b>{active.filter(v=>v.status==='TABLE_OFFERED').length}</b><small>Đã mời vào bàn</small></span>
      <span><UtensilsCrossed/><b>{servingTables.length}</b><small>Bàn đang phục vụ · {servingGuests} khách</small></span>
      <span className={critical?'critical':''}><AlertTriangle/><b>{critical}</b><small>Việc quá giờ</small></span>
    </div>
    {!!servingTables.length&&<div className="walkin-serving-tables"><div><Armchair/><span><b>Các bàn đang phục vụ</b><small>Bao gồm khách đặt online và khách đến trực tiếp</small></span></div>
      <section>{servingTables.map(table=><Link to="/staff/ban" key={table.id}><b>{table.code}</b><span>{table.customerName||'Khách tại bàn'} · {table.partySize||0} khách</span><small>{table.assignedStaffName||'Chưa phân công nhân viên'}</small></Link>)}</section>
    </div>}
    {metrics&&<div className="walkin-kpis">
      <span><small>Chờ trung bình</small><b>{metrics.averageWaitMinutes} phút</b></span>
      <span><small>P90 thời gian chờ</small><b>{metrics.p90WaitMinutes} phút</b></span>
      <span><small>ETA chính xác ±5 phút</small><b>{metrics.quoteAccuracyPercent}%</b></span>
      <span><small>Tỷ lệ bỏ hàng chờ</small><b>{metrics.abandonmentPercent}%</b></span>
      <span><small>Dọn bàn trung bình</small><b>{metrics.averageCleaningMinutes} phút</b></span>
    </div>}

    {!!critical&&<div className="walkin-escalation"><BellRing/><div><b>Cần xử lý ngay</b>
      {active.filter(v=>v.slaLevel==='CRITICAL').map(v=><span key={v.id}>{v.code} · {v.customerName} · {shortSla(v.slaMessage)}</span>)}</div></div>}

    <div className="walkin-board">
      {active.map(visit=><article key={visit.id} className={`walkin-card ${visit.slaLevel.toLowerCase()} status-${visit.status.toLowerCase()}`}>
        <header><div><span>{visit.code}</span><h2>{visit.customerName}</h2><small><Users/> {visit.partySize} khách {visit.phone&&<>· <Phone/> {visit.phone}</>}</small></div>
          <div className="walkin-badges"><i>{priorityLabel[visit.priority]}</i><strong>{statusLabel[visit.status]}</strong></div></header>
        <div className="walkin-sla">
          <span><Clock3/><b>{visit.elapsedMinutes}p</b></span>
          <span className={visit.slaLevel.toLowerCase()}><i/>{shortSla(visit.slaMessage)}</span>
          {visit.status==='WAITING'&&<small>ETA {clock(visit.expectedSeatAt)} · chờ {visit.quotedWaitMinutes}p</small>}
          {visit.status==='TABLE_OFFERED'&&<small>Giữ đến {clock(visit.offerExpiresAt)} · gọi {visit.callCount} lần</small>}
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

    {openDemoForm&&<div className="walkin-modal"><form className="walkin-demo-form demo-studio" onSubmit={createDemo}>
      <div><span><p className="eyebrow dark">DEMO SCENARIO STUDIO</p><h2>Tự tạo tình huống toàn hệ thống</h2></span><button type="button" onClick={()=>setOpenDemoForm(false)}><X/></button></div>
      <p className="walkin-demo-note">Chọn đúng nhóm nghiệp vụ, chọn một tình huống rồi nhập dữ liệu. Không có dữ liệu nào được tạo trước khi bạn bấm “Xác nhận tạo”.</p>

      <nav className="demo-group-tabs">{(Object.keys(demoGroupInfo) as DemoGroup[]).map(group=><button type="button" key={group} className={demoGroup===group?'selected':''} onClick={()=>{const next=demoOptions.find(option=>option.group===group)!;setDemoGroup(group);setDemoForm({...demoForm,type:next.type,minutes:defaultDemoMinutes(next.type)})}}>
        <b>{demoGroupInfo[group].title}</b><small>{demoGroupInfo[group].description}</small>
      </button>)}</nav>

      <fieldset className="demo-scenario-picker"><legend>Tình huống trong nhóm {demoGroupInfo[demoGroup].title}</legend>
        {demoOptions.filter(option=>option.group===demoGroup).map(option=><button type="button" key={option.type}
          className={`${option.tone} ${demoForm.type===option.type?'selected':''}`}
          onClick={()=>setDemoForm({...demoForm,type:option.type,minutes:defaultDemoMinutes(option.type)})}>
          <span>{option.tone==='normal'?<Check/>:<AlertTriangle/>}<b>{option.title}</b></span><small>{option.description}</small>
        </button>)}
      </fieldset>

      <section className="demo-studio-fields"><h3>Thông tin tình huống</h3><div className="walkin-form-grid">
        {demoUsesCustomer&&<><label>Tên khách/nhóm khách<input required maxLength={120} value={demoForm.customerName} onChange={e=>setDemoForm({...demoForm,customerName:e.target.value})}/></label>
          <label>Số điện thoại<input pattern="[0-9+ ]{9,15}" value={demoForm.phone} onChange={e=>setDemoForm({...demoForm,phone:e.target.value})}/></label>
          <label>Số khách<input type="number" min="1" max="30" value={demoForm.partySize} onChange={e=>setDemoForm({...demoForm,partySize:Number(e.target.value)})}/></label></>}
        {selectedDemo.group==='WALK_IN'&&<><label>Khu vực mong muốn<input placeholder="Ví dụ: Cửa sổ" value={demoForm.areaPreference} onChange={e=>setDemoForm({...demoForm,areaPreference:e.target.value})}/></label>
          <label>Mức ưu tiên<select value={demoForm.priority} onChange={e=>setDemoForm({...demoForm,priority:e.target.value as WalkInPriority})}>{Object.entries(priorityLabel).map(([value,label])=><option key={value} value={value}>{label}</option>)}</select></label>
          <label>Lý do ưu tiên<input required={demoForm.priority!=='NORMAL'} disabled={demoForm.priority==='NORMAL'} value={demoForm.priorityReason} onChange={e=>setDemoForm({...demoForm,priorityReason:e.target.value})}/></label></>}
        {selectedDemo.group==='RESERVATION'&&<><label>Email nhận thử thông báo<input type="email" placeholder="Không bắt buộc" value={demoForm.email} onChange={e=>setDemoForm({...demoForm,email:e.target.value})}/></label>
          <label className="demo-checkbox"><input type="checkbox" checked={demoForm.notifyEmail} disabled={!demoForm.email} onChange={e=>setDemoForm({...demoForm,notifyEmail:e.target.checked})}/><span>Gửi email demo cho khách</span></label></>}
        {demoNeedsTable&&<label>Chọn bàn<select value={demoForm.tableId??''} onChange={e=>setDemoForm({...demoForm,tableId:e.target.value?Number(e.target.value):null})}><option value="">Hệ thống tự chọn bàn phù hợp</option>{tables.map(table=><option key={table.id} value={table.id}>{table.code} · {table.seats} ghế · {table.status}{table.customerName?` · ${table.customerName}`:''}</option>)}</select><small>Với bếp/QR, có thể chọn bàn đang phục vụ. Với dọn bàn, hãy chọn bàn trống.</small></label>}
        {demoNeedsMenu&&<label>Chọn món<select value={demoForm.menuItemId??''} onChange={e=>setDemoForm({...demoForm,menuItemId:e.target.value?Number(e.target.value):null})}><option value="">Tự chọn món đang phục vụ</option>{menuItems.filter(item=>item.available).map(item=><option key={item.id} value={item.id}>{item.name} · {item.preparationMinutes} phút</option>)}</select></label>}
        {demoUsesMinutes&&<label>Số phút mô phỏng<input type="number" min="1" max="240" value={demoForm.minutes} onChange={e=>setDemoForm({...demoForm,minutes:Number(e.target.value)})}/><small>Thời gian chờ, số phút chậm hoặc phần ETA cộng thêm tùy tình huống.</small></label>}
        {demoUsesReason&&<label>Lý do món chậm<input maxLength={300} value={demoForm.reason} onChange={e=>setDemoForm({...demoForm,reason:e.target.value})}/></label>}
      </div>
      <label>Ghi chú tình huống<textarea maxLength={500} value={demoForm.note} onChange={e=>setDemoForm({...demoForm,note:e.target.value})}/></label></section>

      <div className={`walkin-demo-summary ${selectedDemo.tone}`}><Clock3/><span><b>Sẽ tạo: {selectedDemo.title}</b><small>{selectedDemo.description} · Sau khi tạo, mở đúng màn hình nghiệp vụ để kiểm tra.</small></span></div>
      <div className="walkin-demo-actions"><button type="button" onClick={()=>setOpenDemoForm(false)}>Hủy</button><button className="walkin-submit" disabled={busy==='demo'}><Plus/> {busy==='demo'?'Đang tạo...':'Xác nhận tạo tình huống'}</button></div>
    </form></div>}

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

import{Armchair,BellRing,Clock3,Search,UserRound,Users}from'lucide-react';
import{useEffect,useMemo,useState}from'react';
import{Link}from'react-router-dom';
import{api}from'../api';
import type{TableOverview}from'../types';

const statusLabels:Record<string,string>={AVAILABLE:'Bàn trống',RESERVED:'Đã đặt',OCCUPIED:'Có khách',NEEDS_CLEANING:'Cần dọn',INACTIVE:'Tạm ngưng'};
const serviceLabels:Record<TableOverview['serviceState'],string>={EMPTY:'Bàn trống',RESERVED:'Khách sắp đến',DINING:'Khách đang dùng bữa',WAITING_KITCHEN:'Đang chờ bếp',NEEDS_SERVING:'Cần phục vụ món',NEEDS_CLEANING:'Cần dọn bàn',INACTIVE:'Tạm ngưng'};
const statuses=Object.keys(statusLabels);

export default function TablesPage(){
  const[tables,setTables]=useState<TableOverview[]>([]);const[floor,setFloor]=useState('Tầng 1');const[search,setSearch]=useState('');const[error,setError]=useState('');
  const load=()=>api.tableOverview().then(data=>{setTables(data);setError('')}).catch(e=>setError(e instanceof Error?e.message:'Không tải được bàn'));
  useEffect(()=>{void load();const timer=setInterval(()=>void load(),10000);return()=>clearInterval(timer)},[]);
  const shown=useMemo(()=>{const keyword=search.trim().toLowerCase();return tables.filter(table=>(keyword||table.floor===floor)&&(!keyword||table.code.toLowerCase().includes(keyword)||table.name.toLowerCase().includes(keyword)||table.area.toLowerCase().includes(keyword)))},[tables,floor,search]);
  async function update(id:number,status:string){try{await api.updateTableStatus(id,status);await load()}catch(e){setError(e instanceof Error?e.message:'Không cập nhật được')}}
  return <section className="tables-page page-section container"><div className="staff-heading"><div><p className="eyebrow dark">TRẠNG THÁI PHỤC VỤ GẦN THỜI GIAN THỰC</p><h1>Tra cứu & sơ đồ bàn</h1></div><div className="floor-tabs"><button className={floor==='Tầng 1'?'active':''} onClick={()=>{setFloor('Tầng 1');setSearch('')}}>Tầng 1</button><button className={floor==='Tầng 2'?'active':''} onClick={()=>{setFloor('Tầng 2');setSearch('')}}>Tầng 2</button></div></div>
    <div className="table-search"><Search/><input aria-label="Tìm bàn" placeholder="Nhập mã bàn, tên bàn hoặc khu vực, ví dụ T1-02..." value={search} onChange={event=>setSearch(event.target.value)}/><span>{search?`Tìm thấy ${shown.length} bàn`:'Tự động làm mới mỗi 10 giây'}</span></div>
    <div className="table-legend">{Object.entries(serviceLabels).map(([state,label])=><span key={state}><i className={`dot ${state.toLowerCase()}`}/>{label}</span>)}</div>{error&&<p className="error">{error}</p>}
    <div className="floor-summary"><b>{search?'Kết quả tìm kiếm':floor}</b><span><Armchair size={17}/>{shown.length} bàn</span><span><Users size={17}/>{shown.reduce((n,t)=>n+t.seats,0)} chỗ</span><span><BellRing size={17}/>{shown.filter(t=>t.serviceState==='NEEDS_SERVING').length} bàn cần phục vụ</span></div>
    <div className="floor-plan table-overview-grid">{shown.map(table=><article className={`table-tile ${table.serviceState.toLowerCase()}`} key={table.id}><div className="table-shape"><Armchair/><b>{table.code}</b><span>{table.seats} ghế</span></div><h3>{table.name}</h3><p>{table.floor} • {table.area}</p><strong className={`service-state ${table.serviceState.toLowerCase()}`}>{serviceLabels[table.serviceState]}</strong>
      {table.customerName&&<div className="table-guest"><span><UserRound/><b>{table.customerName}</b><small>{table.customerPhone} • {table.partySize} khách</small></span><span><Clock3/><b>{table.reservationCode}</b><small>Phiên #{table.serviceSessionId||'—'}</small></span>{table.readyOrderCount>0&&<div className="table-alert"><BellRing/><b>{table.readyOrderCount} phiếu món đang chờ mang ra</b></div>}{table.openOrderCount>0&&<Link to="/staff/phuc-vu">Xem {table.openOrderCount} phiếu đang mở →</Link>}</div>}
      {table.serviceState==='NEEDS_CLEANING'&&<p className="cleaning-note">Khách đã rời bàn — cần dọn trước lượt tiếp theo.</p>}
      <select aria-label={`Trạng thái ${table.code}`} value={table.status} onChange={event=>update(table.id,event.target.value)}>{statuses.map(status=><option value={status} key={status}>{statusLabels[status]}</option>)}</select></article>)}{!shown.length&&<div className="table-search-empty"><Search/><h2>Không tìm thấy bàn</h2><p>Hãy thử mã như T1-01, T2-05 hoặc tên khu vực.</p></div>}</div>
  </section>
}

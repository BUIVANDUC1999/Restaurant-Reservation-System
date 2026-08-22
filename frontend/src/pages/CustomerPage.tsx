import {CalendarCheck, CalendarDays, Clock3, LogOut, Mail, Search, ShieldCheck, UserRound, UsersRound} from 'lucide-react';
import {useEffect, useState} from 'react';
import {Link, Navigate} from 'react-router-dom';
import {api} from '../api';
import {useAuth} from '../auth';
import type {Reservation} from '../types';

const terminalStatuses=['COMPLETED','CANCELLED','REJECTED','NO_SHOW','EXPIRED'];
const statusLabels:Record<string,string>={PENDING:'Chờ thanh toán/xác nhận',CONFIRMED:'Đã xác nhận',CHECKED_IN:'Đang phục vụ',COMPLETED:'Hoàn tất',CANCELLED:'Đã hủy',REJECTED:'Đã từ chối',NO_SHOW:'Không đến',EXPIRED:'Hết hạn'};

export default function CustomerPage(){
  const{user,logout}=useAuth();
  const[reservations,setReservations]=useState<Reservation[]>([]);
  const[loading,setLoading]=useState(true);
  const[error,setError]=useState('');
  useEffect(()=>{api.customerReservations().then(setReservations).catch(err=>setError(err instanceof Error?err.message:'Không tải được lịch đặt bàn')).finally(()=>setLoading(false));},[]);
  if(!user)return <Navigate to="/dang-nhap" replace/>;
  if(user.role!=='CUSTOMER')return <Navigate to={user.role==='ADMIN'?'/admin':user.role==='KITCHEN'?'/bep':'/staff'} replace/>;
  const active=reservations.filter(item=>!terminalStatuses.includes(item.status));
  const upcoming=active[0];
  return <section className="customer-page page-section container"><div className="customer-hero"><div className="avatar"><UserRound/></div><div><p className="eyebrow">DASHBOARD KHÁCH HÀNG</p><h1>Xin chào, {user.fullName}</h1><p>Theo dõi lịch đặt bàn và chuẩn bị cho trải nghiệm tại Khám Phá Việt.</p></div></div>
    {error&&<p className="error customer-dashboard-error">{error}</p>}
    <div className="customer-grid customer-dashboard-metrics"><article><ShieldCheck/><span><small>Vai trò</small><b>Khách hàng</b></span></article><article><Mail/><span><small>Email nhận thông báo</small><b>{user.email}</b></span></article><article><CalendarCheck/><span><small>Đơn đang hoạt động</small><b>{loading?'—':active.length}</b></span></article><article><CalendarDays/><span><small>Tổng lịch đã đặt</small><b>{loading?'—':reservations.length}</b></span></article></div>
    <div className="customer-dashboard-layout"><section className="customer-upcoming"><header><div><p className="eyebrow dark">LỊCH GẦN NHẤT</p><h2>{upcoming?'Thông tin bàn đã đặt':'Chưa có lịch sắp tới'}</h2></div><Link to="/dat-ban">Đặt bàn mới →</Link></header>
      {upcoming?<article><div className="customer-reservation-date"><CalendarDays/><b>{upcoming.reservationDate.split('-').reverse().join('/')}</b><span>{upcoming.reservationTime.slice(0,5)}</span></div><div><h3>{upcoming.code}</h3><p><UsersRound/> {upcoming.partySize} khách · {upcoming.assignedTables.map(table=>table.code).join(', ')||'Nhà hàng đang xếp bàn'}</p><i className={upcoming.status.toLowerCase()}>{statusLabels[upcoming.status]||upcoming.status}</i></div><Link to="/tra-cuu"><Search/> Tra cứu chi tiết</Link></article>:<div className="customer-empty"><CalendarDays/><p>Bạn chưa có đơn đặt bàn nào đang hoạt động.</p><Link className="btn btn-gold" to="/dat-ban">Đặt bàn ngay</Link></div>}
    </section>
    <section className="customer-history"><header><div><p className="eyebrow dark">LỊCH SỬ CỦA TÔI</p><h2>Các đơn gần đây</h2></div><span>{reservations.length} đơn</span></header><div>{reservations.slice(0,5).map(item=><article key={item.id}><Clock3/><span><b>{item.code} · {item.customerName}</b><small>{item.reservationDate.split('-').reverse().join('/')} lúc {item.reservationTime.slice(0,5)} · {item.partySize} khách</small></span><i className={item.status.toLowerCase()}>{statusLabels[item.status]||item.status}</i></article>)}{!loading&&!reservations.length&&<p className="role-empty">Chưa có lịch sử đặt bàn gắn với email này.</p>}</div></section></div>
    <div className="customer-actions"><Link className="btn btn-gold" to="/dat-ban"><CalendarDays/> Đặt bàn mới</Link><Link className="btn btn-green" to="/tra-cuu"><Search/> Tra cứu bằng mã đơn</Link><button className="customer-logout" onClick={logout}><LogOut size={17}/> Đăng xuất</button></div>
  </section>;
}

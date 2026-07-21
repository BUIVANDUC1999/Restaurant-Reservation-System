import {Activity,Banknote,CalendarCheck,ChefHat,TableProperties,UserCog,Users,UsersRound,UtensilsCrossed} from 'lucide-react';
import {useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {api} from '../api';
import type {UserStats} from '../types';

export default function AdminDashboardPage(){
  const[stats,setStats]=useState<UserStats>();
  const[error,setError]=useState('');
  useEffect(()=>{api.adminUserStats().then(setStats).catch(err=>setError(err instanceof Error?err.message:'Không tải được thống kê'))},[]);
  return <section className="page-section container admin-page">
    <p className="eyebrow dark">TRUNG TÂM QUẢN TRỊ</p><h1>Tổng quan hệ thống</h1>
    <p className="page-lead">Theo dõi tài khoản và truy cập nhanh các khu vực vận hành nhà hàng.</p>
    {error&&<p className="error">{error}</p>}
    <div className="admin-stats">
      <article><Users/><span><b>{stats?.totalCount??'—'}</b><small>Tổng tài khoản</small></span></article>
      <article><UserCog/><span><b>{stats?.employeeCount??'—'}</b><small>Tài khoản nhân viên</small></span></article>
      <article><UsersRound/><span><b>{stats?.customerCount??'—'}</b><small>Tài khoản khách hàng</small></span></article>
      <article><Activity/><span><b>{stats?.activeCount??'—'}</b><small>Tài khoản hoạt động</small></span></article>
    </div>
    <div className="admin-actions">
      <Link to="/admin/tai-khoan"><UserCog/><div><h2>Quản lý tài khoản</h2><p>Xem số lượng và danh sách Admin, nhân viên, khách hàng.</p></div><b>→</b></Link>
      <Link to="/staff"><CalendarCheck/><div><h2>Quản lý đặt bàn</h2><p>Xác nhận yêu cầu, món đặt trước và tiếp nhận khách.</p></div><b>→</b></Link>
      <Link to="/staff/ban"><TableProperties/><div><h2>Quản lý bàn</h2><p>Theo dõi sơ đồ và trạng thái bàn theo thời gian thực.</p></div><b>→</b></Link>
      <Link to="/staff/thuc-don"><UtensilsCrossed/><div><h2>Quản lý món ăn</h2><p>Thêm món, sửa giá, hình ảnh, danh mục và trạng thái phục vụ.</p></div><b>→</b></Link>
      <Link to="/bep"><ChefHat/><div><h2>Điều phối bếp</h2><p>Theo dõi phiếu mới, món đang chế biến và món sẵn sàng.</p></div><b>→</b></Link>
      <Link to="/staff/thanh-toan"><Banknote/><div><h2>Thanh toán</h2><p>Nhân viên lập hóa đơn, giảm giá và xác nhận thanh toán theo bàn.</p></div><b>→</b></Link>
    </div>
  </section>
}

import{LogOut,Menu,Phone,X}from'lucide-react';
import{useState}from'react';
import{Link,NavLink}from'react-router-dom';
import{useAuth}from'../auth';

export default function Header(){
  const[open,setOpen]=useState(false);const{user,logout}=useAuth();const isAdmin=user?.role==='ADMIN';const canOperate=isAdmin||user?.role==='STAFF';const canKitchen=isAdmin||user?.role==='KITCHEN';const canCashier=isAdmin||user?.role==='CASHIER';
  return <><div className="topbar"><span>Nhà hàng Khám Phá Việt — Nơi thưởng thức hương vị quê hương</span><a href="tel:0984353577"><Phone size={15}/> 0984 353 577</a></div><header className="header"><Link className="brand" to="/"><span className="brand-mark">KV</span><span><b>KHÁM PHÁ VIỆT</b><small>Ẩm thực Tây Bắc</small></span></Link><button className="menu-toggle" onClick={()=>setOpen(!open)} aria-label="Mở menu">{open?<X/>:<Menu/>}</button><nav className={open?'open':''} onClick={()=>setOpen(false)}><NavLink to="/">Trang chủ</NavLink><NavLink to="/thuc-don">Thực đơn</NavLink><NavLink to="/tra-cuu">Tra cứu</NavLink>{isAdmin&&<NavLink to="/admin">Quản trị</NavLink>}{isAdmin&&<NavLink to="/admin/tai-khoan">Tài khoản</NavLink>}{canOperate&&<NavLink to="/staff">Đặt bàn</NavLink>}{canOperate&&<NavLink to="/staff/phuc-vu">Phục vụ</NavLink>}{canOperate&&<NavLink to="/staff/ban">Sơ đồ bàn</NavLink>}{canKitchen&&<NavLink to="/bep">Bếp</NavLink>}{canCashier&&<NavLink to="/thu-ngan">Thu ngân</NavLink>}{user?.role==='CUSTOMER'&&<NavLink to="/tai-khoan">Tài khoản</NavLink>}{!user&&<NavLink to="/dang-nhap">Đăng nhập</NavLink>}{user?<button className="nav-logout" onClick={logout} title="Đăng xuất"><LogOut size={17}/></button>:null}<Link className="btn btn-gold" to="/dat-ban">Đặt bàn</Link></nav></header></>;
}

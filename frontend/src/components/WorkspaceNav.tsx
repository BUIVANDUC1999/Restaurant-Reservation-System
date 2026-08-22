import {
  Banknote, CalendarCheck, CalendarClock, ChefHat, ChevronLeft, ChevronRight, LayoutDashboard,
  TableProperties, UserCog, UsersRound, UtensilsCrossed
} from 'lucide-react';
import {useState} from 'react';
import {NavLink} from 'react-router-dom';
import {useAuth} from '../auth';
import type {AuthUser} from '../types';

const roleLabels:Record<AuthUser['role'],string>={
  ADMIN:'Quản trị viên',STAFF:'Nhân viên phục vụ',KITCHEN:'Nhân viên bếp',CUSTOMER:'Khách hàng'
};
const entries={
  ADMIN:[
    ['/admin','Tổng quan',LayoutDashboard],['/admin/tai-khoan','Tài khoản',UserCog],
    ['/admin/ca-lam-viec','Ca làm việc',CalendarClock],
    ['/staff','Dashboard phục vụ',LayoutDashboard],['/staff/dat-ban','Đặt bàn',CalendarCheck],['/staff/walk-in','Khách tại quán',UsersRound],
    ['/staff/phuc-vu','Phục vụ',UtensilsCrossed],['/staff/ban','Bàn của tôi',TableProperties],
    ['/staff/thuc-don','Quản lý món',UtensilsCrossed],['/staff/thanh-toan','Thanh toán',Banknote],
    ['/bep','Dashboard bếp',LayoutDashboard],['/bep/dieu-phoi','Điều phối bếp',ChefHat]
  ],
  STAFF:[
    ['/staff','Tổng quan',LayoutDashboard],['/staff/dat-ban','Đặt bàn',CalendarCheck],['/staff/walk-in','Khách tại quán',UsersRound],
    ['/staff/phuc-vu','Phục vụ',UtensilsCrossed],['/staff/ban','Sơ đồ bàn',TableProperties],
    ['/staff/thuc-don','Quản lý món',UtensilsCrossed],['/staff/thanh-toan','Thanh toán',Banknote]
  ],
  KITCHEN:[['/bep','Tổng quan',LayoutDashboard],['/bep/dieu-phoi','Điều phối bếp',ChefHat]],
  CUSTOMER:[]
} satisfies Record<AuthUser['role'],Array<[string,string,typeof LayoutDashboard]>>;

export default function WorkspaceNav(){
  const{user}=useAuth();
  const[collapsed,setCollapsed]=useState(()=>localStorage.getItem('workspace_nav_collapsed')==='true');
  if(!user||user.role==='CUSTOMER')return null;
  function toggle(){
    setCollapsed(value=>{localStorage.setItem('workspace_nav_collapsed',String(!value));return !value});
  }
  return <aside className={`workspace-nav ${collapsed?'collapsed':''}`}>
    <div className="workspace-user"><span>{user.fullName.split(' ').slice(-2).map(part=>part[0]).join('')}</span>
      <div><b>{user.fullName}</b><small>{roleLabels[user.role]}</small></div></div>
    <nav>{entries[user.role].map(([path,label,Icon])=>
      <NavLink key={path} to={path} end={path==='/staff'||path==='/admin'||path==='/bep'} title={label}>
        <Icon/><span>{label}</span>
      </NavLink>)}</nav>
    <button className="workspace-collapse" onClick={toggle}>
      {collapsed?<ChevronRight/>:<ChevronLeft/>}<span>Thu gọn</span>
    </button>
  </aside>;
}

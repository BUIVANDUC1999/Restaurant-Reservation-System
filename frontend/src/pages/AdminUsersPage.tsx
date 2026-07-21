import {Search,ShieldCheck,UserCog,UsersRound} from 'lucide-react';
import {useEffect,useMemo,useState} from 'react';
import {api} from '../api';
import type {AuthUser,UserStats,UserSummary} from '../types';

const labels:Record<AuthUser['role'],string>={ADMIN:'Quản trị viên',STAFF:'Nhân viên phục vụ',KITCHEN:'Nhân viên bếp',CASHIER:'Thu ngân',CUSTOMER:'Khách hàng'};
type Filter='ALL'|'EMPLOYEE'|'CUSTOMER'|'ADMIN';

export default function AdminUsersPage(){
  const[users,setUsers]=useState<UserSummary[]>([]);const[stats,setStats]=useState<UserStats>();
  const[filter,setFilter]=useState<Filter>('ALL');const[search,setSearch]=useState('');const[error,setError]=useState('');
  useEffect(()=>{Promise.all([api.adminUsers(),api.adminUserStats()]).then(([list,data])=>{setUsers(list);setStats(data)}).catch(err=>setError(err instanceof Error?err.message:'Không tải được tài khoản'))},[]);
  const filtered=useMemo(()=>users.filter(user=>{
    const byRole=filter==='ALL'||filter==='CUSTOMER'&&user.role==='CUSTOMER'||filter==='ADMIN'&&user.role==='ADMIN'||filter==='EMPLOYEE'&&['STAFF','KITCHEN','CASHIER'].includes(user.role);
    const keyword=search.trim().toLowerCase();return byRole&&(!keyword||user.fullName.toLowerCase().includes(keyword)||user.email.toLowerCase().includes(keyword));
  }),[users,filter,search]);
  return <section className="page-section container admin-page">
    <p className="eyebrow dark">QUẢN TRỊ HỆ THỐNG</p><h1>Danh sách tài khoản</h1>
    <p className="page-lead">Thông tin đăng nhập được bảo vệ; trang này không hiển thị mật khẩu.</p>
    {error&&<p className="error">{error}</p>}
    <div className="account-summary"><span><UserCog/><b>{stats?.employeeCount??'—'}</b> nhân viên</span><span><UsersRound/><b>{stats?.customerCount??'—'}</b> khách hàng</span><span><ShieldCheck/><b>{stats?.adminCount??'—'}</b> quản trị viên</span></div>
    <div className="account-tools"><div><Search/><input aria-label="Tìm tài khoản" placeholder="Tìm theo họ tên hoặc email..." value={search} onChange={event=>setSearch(event.target.value)}/></div><div className="filters account-filters">{([['ALL','Tất cả'],['EMPLOYEE','Nhân viên'],['CUSTOMER','Khách hàng'],['ADMIN','Admin']] as const).map(([value,label])=><button className={filter===value?'active':''} onClick={()=>setFilter(value)} key={value}>{label}</button>)}</div></div>
    <div className="table-wrap"><table className="account-table"><thead><tr><th>Họ tên</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th>Ngày tạo</th></tr></thead><tbody>{filtered.map(user=><tr key={user.id}><td><b>{user.fullName}</b></td><td>{user.email}</td><td><span className={`role-badge ${user.role.toLowerCase()}`}>{labels[user.role]}</span></td><td><span className={`active-badge ${user.active?'on':'off'}`}>{user.active?'Đang hoạt động':'Đã khóa'}</span></td><td>{new Intl.DateTimeFormat('vi-VN').format(new Date(user.createdAt))}</td></tr>)}</tbody></table>{!filtered.length&&<p className="empty">Không tìm thấy tài khoản phù hợp.</p>}</div>
  </section>
}

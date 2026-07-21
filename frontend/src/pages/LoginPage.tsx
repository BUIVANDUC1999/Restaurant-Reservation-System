import{FormEvent,useState}from'react';
import{Banknote,ChefHat,Crown,LockKeyhole,UserRound,UsersRound}from'lucide-react';
import{Link,useLocation,useNavigate}from'react-router-dom';
import{api}from'../api';
import{useAuth}from'../auth';

const demoAccounts=[
  {role:'ADMIN',label:'Quản trị viên',description:'Toàn quyền quản lý hệ thống',email:'admin@khamphaviet.vn',password:'Admin@123',icon:Crown},
  {role:'STAFF',label:'Nhân viên',description:'Quản lý đặt bàn và sơ đồ bàn',email:'staff@khamphaviet.vn',password:'Staff@123',icon:UsersRound},
  {role:'KITCHEN',label:'Nhân viên bếp',description:'Tiếp nhận và cập nhật phiếu món',email:'kitchen@khamphaviet.vn',password:'Kitchen@123',icon:ChefHat},
  {role:'CASHIER',label:'Thu ngân',description:'Lập hóa đơn và xác nhận thanh toán',email:'cashier@khamphaviet.vn',password:'Cashier@123',icon:Banknote},
  {role:'CUSTOMER',label:'Khách hàng',description:'Đặt bàn và quản lý tài khoản',email:'customer@khamphaviet.vn',password:'Customer@123',icon:UserRound},
] as const;

export default function LoginPage(){
  const[email,setEmail]=useState<string>(demoAccounts[0].email);const[password,setPassword]=useState<string>(demoAccounts[0].password);
  const[selected,setSelected]=useState<string>('ADMIN');const[error,setError]=useState('');const[loading,setLoading]=useState(false);
  const{save}=useAuth();const navigate=useNavigate();const location=useLocation();
  function choose(account:typeof demoAccounts[number]){setSelected(account.role);setEmail(account.email);setPassword(account.password);setError('')}
  async function submit(e:FormEvent){e.preventDefault();setLoading(true);setError('');try{const user=await api.login(email,password);save(user);const requested=(location.state as{from?:string})?.from;const home=user.role==='ADMIN'?'/admin':user.role==='CUSTOMER'?'/tai-khoan':user.role==='KITCHEN'?'/bep':user.role==='CASHIER'?'/thu-ngan':'/staff';navigate(requested||home,{replace:true})}catch(err){setError(err instanceof Error?err.message:'Đăng nhập thất bại')}finally{setLoading(false)}}
  return <section className="login-page"><div className="login-shell"><div className="demo-side"><p className="eyebrow">TÀI KHOẢN TRÌNH DIỄN</p><h1>Chọn vai trò để demo</h1><p>Mỗi tài khoản được phân quyền và chuyển đến khu vực phù hợp sau khi đăng nhập.</p><div className="demo-accounts">{demoAccounts.map(account=>{const Icon=account.icon;return <button type="button" className={selected===account.role?'selected':''} onClick={()=>choose(account)} key={account.role}><span><Icon/></span><div><b>{account.label}</b><small>{account.description}</small></div><i>→</i></button>})}</div></div><form className="login-card" onSubmit={submit}><div className="login-icon"><LockKeyhole/></div><p className="eyebrow dark">ĐĂNG NHẬP HỆ THỐNG</p><h1>Chào mừng trở lại</h1><p>Thông tin của vai trò được chọn đã được điền sẵn để bạn trình diễn nhanh.</p><label>Email<input type="email" value={email} onChange={e=>setEmail(e.target.value)} required/></label><label>Mật khẩu<input type="password" value={password} onChange={e=>setPassword(e.target.value)} required/></label>{error&&<p className="error">{error}</p>}<button className="btn btn-green" disabled={loading}>{loading?'Đang đăng nhập...':'Đăng nhập'}</button><Link className="register-link" to="/dang-ky">Chưa có tài khoản? Đăng ký khách hàng →</Link><small>Chỉ sử dụng các tài khoản demo trong môi trường học tập.</small></form></div></section>
}

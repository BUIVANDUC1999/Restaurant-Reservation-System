import{FormEvent,useState}from'react';
import{UserPlus}from'lucide-react';
import{Link,useNavigate}from'react-router-dom';
import{api}from'../api';
import{useAuth}from'../auth';

export default function RegisterPage(){
  const[form,setForm]=useState({fullName:'',email:'',phone:'',password:'',confirmPassword:''});const[error,setError]=useState('');const[loading,setLoading]=useState(false);const{save}=useAuth();const navigate=useNavigate();
  const change=(key:string,value:string)=>setForm(current=>({...current,[key]:value}));
  async function submit(event:FormEvent){event.preventDefault();if(form.password!==form.confirmPassword){setError('Mật khẩu xác nhận chưa khớp');return}setLoading(true);setError('');try{const user=await api.register({fullName:form.fullName,email:form.email,phone:form.phone,password:form.password});save(user);navigate('/tai-khoan',{replace:true})}catch(err){setError(err instanceof Error?err.message:'Không đăng ký được tài khoản')}finally{setLoading(false)}}
  return <section className="register-page"><form className="register-card" onSubmit={submit}><div className="login-icon"><UserPlus/></div><p className="eyebrow dark">THÀNH VIÊN KHÁM PHÁ VIỆT</p><h1>Đăng ký khách hàng</h1><p>Tạo tài khoản để quản lý thông tin và sử dụng các tiện ích đặt bàn.</p><div className="register-grid"><label>Họ và tên<input required maxLength={120} value={form.fullName} onChange={e=>change('fullName',e.target.value)} placeholder="Nguyễn Văn An"/></label><label>Số điện thoại<input required pattern="[0-9+ ]{9,15}" value={form.phone} onChange={e=>change('phone',e.target.value)} placeholder="0984 353 577"/></label><label className="wide">Email<input required type="email" value={form.email} onChange={e=>change('email',e.target.value)} placeholder="ban@example.com"/></label><label>Mật khẩu<input required minLength={8} type="password" value={form.password} onChange={e=>change('password',e.target.value)} placeholder="Tối thiểu 8 ký tự"/></label><label>Xác nhận mật khẩu<input required minLength={8} type="password" value={form.confirmPassword} onChange={e=>change('confirmPassword',e.target.value)}/></label></div>{error&&<p className="error">{error}</p>}<button className="btn btn-green" disabled={loading}>{loading?'Đang tạo tài khoản...':'Đăng ký và đăng nhập'}</button><Link to="/dang-nhap">Đã có tài khoản? Quay lại đăng nhập</Link></form></section>
}

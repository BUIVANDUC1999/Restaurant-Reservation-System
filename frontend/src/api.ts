import type {AuthUser,MenuItem,Reservation,RestaurantTable,UserStats,UserSummary} from './types'
const BASE=import.meta.env.VITE_API_URL||'/api/v1'
const token=()=>{try{return JSON.parse(localStorage.getItem('restaurant_auth')||'null')?.accessToken}catch{return null}}
async function request<T>(path:string,options?:RequestInit):Promise<T>{const auth=token();const response=await fetch(`${BASE}${path}`,{headers:{'Content-Type':'application/json',...(auth?{Authorization:`Bearer ${auth}`}:{}) ,...options?.headers},...options});const text=await response.text();let data:null|Record<string,unknown>=null;try{data=text?JSON.parse(text):null}catch{data=null}if(!response.ok)throw new Error(typeof data?.message==='string'?data.message:(response.status===401||response.status===403?'Phiên đăng nhập không hợp lệ':'Có lỗi xảy ra'));return data as T}
export const api={
  login:(email:string,password:string)=>request<AuthUser>('/auth/login',{method:'POST',body:JSON.stringify({email,password})}),
  adminUserStats:()=>request<UserStats>('/admin/users/stats'),
  adminUsers:()=>request<UserSummary[]>('/admin/users'),
  menu:()=>request<MenuItem[]>('/menu/items'),
  createReservation:(body:unknown)=>request<Reservation>('/reservations',{method:'POST',body:JSON.stringify(body)}),
  lookup:(code:string,phone:string)=>request<Reservation>(`/reservations/lookup?code=${encodeURIComponent(code)}&phone=${encodeURIComponent(phone)}`),
  staffReservations:()=>request<Reservation[]>('/staff/reservations'),
  updateStatus:(id:number,status:string)=>request<Reservation>(`/staff/reservations/${id}/status`,{method:'PATCH',body:JSON.stringify({status})}),
  confirmPreOrder:(id:number)=>request<Reservation>(`/staff/reservations/${id}/preorder/confirm`,{method:'POST'}),
  assignTables:(id:number,tableIds:number[])=>request<Reservation>(`/staff/reservations/${id}/tables`,{method:'PUT',body:JSON.stringify({tableIds})}),
  checkIn:(id:number)=>request<Reservation>(`/staff/reservations/${id}/check-in`,{method:'POST'}),
  completeService:(id:number)=>request<Reservation>(`/staff/reservations/${id}/complete`,{method:'POST'}),
  tables:()=>request<RestaurantTable[]>('/staff/tables'),
  updateTableStatus:(id:number,status:string)=>request<RestaurantTable>(`/staff/tables/${id}/status`,{method:'PATCH',body:JSON.stringify({status})}),
  createTable:(body:unknown)=>request<RestaurantTable>('/staff/tables',{method:'POST',body:JSON.stringify(body)})
}

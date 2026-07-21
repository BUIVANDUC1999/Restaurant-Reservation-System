import type {AuthUser,Checkout,DiningOrder,MenuItem,PayPalConfig,PayPalOrder,Reservation,RestaurantTable,TableOverview,UserStats,UserSummary} from './types'
const BASE=import.meta.env.VITE_API_URL||'/api/v1'
const token=()=>{try{return JSON.parse(localStorage.getItem('restaurant_auth')||'null')?.accessToken}catch{return null}}
async function request<T>(path:string,options?:RequestInit):Promise<T>{const auth=token();const response=await fetch(`${BASE}${path}`,{headers:{'Content-Type':'application/json',...(auth?{Authorization:`Bearer ${auth}`}:{}) ,...options?.headers},...options});const text=await response.text();let data:null|Record<string,unknown>=null;try{data=text?JSON.parse(text):null}catch{data=null}if(!response.ok)throw new Error(typeof data?.message==='string'?data.message:(response.status===401||response.status===403?'Phiên đăng nhập không hợp lệ':'Có lỗi xảy ra'));return data as T}
export const api={
  login:(email:string,password:string)=>request<AuthUser>('/auth/login',{method:'POST',body:JSON.stringify({email,password})}),
  register:(body:unknown)=>request<AuthUser>('/auth/register',{method:'POST',body:JSON.stringify(body)}),
  adminUserStats:()=>request<UserStats>('/admin/users/stats'),
  adminUsers:()=>request<UserSummary[]>('/admin/users'),
  menu:()=>request<MenuItem[]>('/menu/items'),
  staffMenu:()=>request<MenuItem[]>('/staff/menu/items'),
  createMenuItem:(body:unknown)=>request<MenuItem>('/staff/menu/items',{method:'POST',body:JSON.stringify(body)}),
  updateMenuItem:(id:number,body:unknown)=>request<MenuItem>(`/staff/menu/items/${id}`,{method:'PUT',body:JSON.stringify(body)}),
  setMenuAvailability:(id:number,available:boolean)=>request<MenuItem>(`/staff/menu/items/${id}/availability`,{method:'PATCH',body:JSON.stringify({available})}),
  createReservation:(body:unknown)=>request<Reservation>('/reservations',{method:'POST',body:JSON.stringify(body)}),
  lookup:(code:string,phone:string)=>request<Reservation>(`/reservations/lookup?code=${encodeURIComponent(code)}&phone=${encodeURIComponent(phone)}`),
  staffReservations:()=>request<Reservation[]>('/staff/reservations'),
  updateStatus:(id:number,status:string)=>request<Reservation>(`/staff/reservations/${id}/status`,{method:'PATCH',body:JSON.stringify({status})}),
  confirmPreOrder:(id:number)=>request<Reservation>(`/staff/reservations/${id}/preorder/confirm`,{method:'POST'}),
  assignTables:(id:number,tableIds:number[])=>request<Reservation>(`/staff/reservations/${id}/tables`,{method:'PUT',body:JSON.stringify({tableIds})}),
  checkIn:(id:number)=>request<Reservation>(`/staff/reservations/${id}/check-in`,{method:'POST'}),
  completeService:(id:number)=>request<Reservation>(`/staff/reservations/${id}/complete`,{method:'POST'}),
  tables:()=>request<RestaurantTable[]>('/staff/tables'),
  tableOverview:()=>request<TableOverview[]>('/staff/tables/overview'),
  updateTableStatus:(id:number,status:string)=>request<RestaurantTable>(`/staff/tables/${id}/status`,{method:'PATCH',body:JSON.stringify({status})}),
  createTable:(body:unknown)=>request<RestaurantTable>('/staff/tables',{method:'POST',body:JSON.stringify(body)}),
  sessionOrders:(sessionId:number)=>request<DiningOrder[]>(`/staff/service-sessions/${sessionId}/orders`),
  createOrder:(sessionId:number,body:unknown)=>request<DiningOrder>(`/staff/service-sessions/${sessionId}/orders`,{method:'POST',body:JSON.stringify(body)}),
  serveOrder:(id:number)=>request<DiningOrder>(`/staff/orders/${id}/served`,{method:'PATCH'}),
  cancelOrder:(id:number)=>request<DiningOrder>(`/staff/orders/${id}/cancel`,{method:'PATCH'}),
  kitchenOrders:()=>request<DiningOrder[]>('/kitchen/orders'),
  updateKitchenOrder:(id:number,status:'PREPARING'|'READY')=>request<DiningOrder>(`/kitchen/orders/${id}/status`,{method:'PATCH',body:JSON.stringify({status})}),
  checkouts:()=>request<Checkout[]>('/staff/checkouts'),
  payCheckout:(sessionId:number,body:unknown)=>request<Checkout>(`/staff/checkouts/${sessionId}/pay`,{method:'POST',body:JSON.stringify(body)}),
  paypalConfig:()=>request<PayPalConfig>('/staff/checkouts/paypal/config'),
  createPayPalOrder:(sessionId:number,discountAmount:number)=>request<PayPalOrder>(`/staff/checkouts/${sessionId}/paypal/orders`,{method:'POST',body:JSON.stringify({discountAmount})}),
  capturePayPalOrder:(sessionId:number,orderId:string,discountAmount:number)=>request<Checkout>(`/staff/checkouts/${sessionId}/paypal/orders/${encodeURIComponent(orderId)}/capture`,{method:'POST',body:JSON.stringify({discountAmount})})
}

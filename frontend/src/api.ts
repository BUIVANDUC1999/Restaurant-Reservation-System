import type {MenuItem,Reservation} from './types'
const BASE=import.meta.env.VITE_API_URL||'/api/v1'
async function request<T>(path:string,options?:RequestInit):Promise<T>{const response=await fetch(`${BASE}${path}`,{headers:{'Content-Type':'application/json',...options?.headers},...options});const data=await response.json();if(!response.ok)throw new Error(data.message||'Có lỗi xảy ra');return data}
export const api={
  menu:()=>request<MenuItem[]>('/menu/items'),
  createReservation:(body:unknown)=>request<Reservation>('/reservations',{method:'POST',body:JSON.stringify(body)}),
  lookup:(code:string,phone:string)=>request<Reservation>(`/reservations/lookup?code=${encodeURIComponent(code)}&phone=${encodeURIComponent(phone)}`),
  staffReservations:()=>request<Reservation[]>('/staff/reservations'),
  updateStatus:(id:number,status:string)=>request<Reservation>(`/staff/reservations/${id}/status`,{method:'PATCH',body:JSON.stringify({status})})
}


/* eslint-disable react-refresh/only-export-components */
import{createContext,useContext,useState,type ReactNode}from'react';import type{AuthUser}from'./types'
type AuthContextValue={user:AuthUser|null;save:(user:AuthUser)=>void;logout:()=>void}
const AuthContext=createContext<AuthContextValue|null>(null)
export function AuthProvider({children}:{children:ReactNode}){const[user,setUser]=useState<AuthUser|null>(()=>{try{return JSON.parse(localStorage.getItem('restaurant_auth')||'null')}catch{return null}});const save=(value:AuthUser)=>{localStorage.setItem('restaurant_auth',JSON.stringify(value));setUser(value)};const logout=()=>{localStorage.removeItem('restaurant_auth');setUser(null)};return <AuthContext.Provider value={{user,save,logout}}>{children}</AuthContext.Provider>}
export function useAuth(){const value=useContext(AuthContext);if(!value)throw new Error('AuthProvider is missing');return value}


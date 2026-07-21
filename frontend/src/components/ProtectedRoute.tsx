import type{ReactNode}from'react';import{Navigate,useLocation}from'react-router-dom';import{useAuth}from'../auth'
export default function ProtectedRoute({children}:{children:ReactNode}){const{user}=useAuth();const location=useLocation();return user?<>{children}</>:<Navigate to="/dang-nhap" replace state={{from:location.pathname}}/>}


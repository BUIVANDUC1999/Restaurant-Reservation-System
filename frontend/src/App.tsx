import {Route,Routes} from 'react-router-dom'
import Header from './components/Header'
import Footer from './components/Footer'
import HomePage from './pages/HomePage'
import MenuPage from './pages/MenuPage'
import ReservationPage from './pages/ReservationPage'
import LookupPage from './pages/LookupPage'
import DashboardPage from './pages/DashboardPage'
export default function App(){return <><Header/><main><Routes><Route path="/" element={<HomePage/>}/><Route path="/thuc-don" element={<MenuPage/>}/><Route path="/dat-ban" element={<ReservationPage/>}/><Route path="/tra-cuu" element={<LookupPage/>}/><Route path="/staff" element={<DashboardPage/>}/></Routes></main><Footer/></>}


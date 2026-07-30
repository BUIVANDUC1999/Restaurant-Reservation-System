import{useEffect,useRef,useState}from'react';
import{api}from'../api';
import type{PayPalConfig}from'../types';

type Buttons={render:(target:HTMLElement)=>Promise<void>;close?:()=>Promise<void>};
declare global{interface Window{paypal?:{Buttons:(options:Record<string,unknown>)=>Buttons}}}

let sdk:Promise<void>|undefined;
function load(config:PayPalConfig){
  if(window.paypal)return Promise.resolve();
  if(sdk)return sdk;
  sdk=new Promise((resolve,reject)=>{
    const script=document.createElement('script');
    script.src=`https://www.paypal.com/sdk/js?client-id=${encodeURIComponent(config.clientId)}&currency=${config.currency}&intent=capture&components=buttons`;
    script.async=true;script.onload=()=>resolve();script.onerror=reject;document.head.appendChild(script);
  });
  return sdk;
}

export default function DepositPayment({code,phone,amount,onPaid}:{code:string;phone:string;amount:number;onPaid:()=>void}){
  const[config,setConfig]=useState<PayPalConfig>();
  const[error,setError]=useState('');
  const storageKey=`paypal-deposit-order:${code}`;
  const[pendingOrderId,setPendingOrderId]=useState(()=>localStorage.getItem(storageKey)||'');
  const target=useRef<HTMLDivElement>(null);
  useEffect(()=>{api.depositPayPalConfig(code).then(setConfig).catch(e=>setError(e instanceof Error?e.message:'Không tải được cấu hình PayPal'))},[code]);
  useEffect(()=>setPendingOrderId(localStorage.getItem(storageKey)||''),[storageKey]);
  useEffect(()=>{
    if(!config?.enabled||!target.current)return;
    let active=true;let buttons:Buttons|undefined;target.current.innerHTML='';
    load(config).then(async()=>{
      if(!active||!target.current||!window.paypal)return;
      buttons=window.paypal.Buttons({
        style:{layout:'vertical',shape:'rect',label:'paypal'},
        createOrder:async()=>{
          const order=await api.createDepositPayPal(code,phone);
          localStorage.setItem(storageKey,order.orderId);setPendingOrderId(order.orderId);
          return order.orderId;
        },
        onApprove:async(data:{orderID:string})=>{
          try{
            await api.captureDepositPayPal(code,phone,data.orderID);
            localStorage.removeItem(storageKey);setPendingOrderId('');onPaid();
          }
          catch(reason){setError(reason instanceof Error?reason.message:'Backend không xác nhận được giao dịch PayPal')}
        },
        onCancel:()=>setError('Bạn đã hủy thanh toán PayPal Sandbox'),
        onError:(reason:unknown)=>setError(reason instanceof Error?reason.message:'PayPal Sandbox từ chối giao dịch')
      });
      await buttons.render(target.current);
    }).catch(()=>setError('Không khởi tạo được PayPal Sandbox'));
    return()=>{active=false;if(buttons?.close)void buttons.close()};
  },[config,code,phone,onPaid,storageKey]);
  async function reconcile(){
    if(!pendingOrderId)return;
    setError('');
    try{
      await api.captureDepositPayPal(code,phone,pendingOrderId);
      localStorage.removeItem(storageKey);setPendingOrderId('');onPaid();
    }catch(reason){setError(reason instanceof Error?reason.message:'Không đối soát được giao dịch PayPal')}
  }
  return <div className="deposit-box">
    <h2>Thanh toán đặt cọc bằng PayPal Sandbox</h2>
    <strong>{Number(amount).toLocaleString('vi-VN')} ₫</strong>
    <p>Đây là giao dịch thử nghiệm, không sử dụng tiền thật.</p>
    {!config&&<small>Đang kiểm tra PayPal Sandbox...</small>}
    {config&&!config.enabled&&<p className="error">PayPal Sandbox chưa được cấu hình.</p>}
    {config?.enabled&&<div ref={target}/>}
    {config?.enabled&&<small>Quy đổi thử nghiệm: 1 USD = {Number(config.vndPerUsd).toLocaleString('vi-VN')} ₫</small>}
    {config?.enabled&&pendingOrderId&&
      <button type="button" className="btn btn-green" onClick={reconcile}>Đối soát lại giao dịch vừa thanh toán</button>}
    {error&&<p className="error">{error}</p>}
  </div>;
}

import {useEffect, useRef, useState} from 'react';
import {api} from '../api';
import type {PayPalConfig} from '../types';

type PayPalButtons = {render:(target:HTMLElement)=>Promise<void>;close?:()=>Promise<void>};

declare global {
  interface Window { paypal?: {Buttons:(options:Record<string,unknown>)=>PayPalButtons} }
}

let sdkPromise:Promise<void>|undefined;
function loadSdk(config:PayPalConfig) {
  if (window.paypal) return Promise.resolve();
  if (sdkPromise) return sdkPromise;
  sdkPromise = new Promise<void>((resolve,reject)=>{
    const script=document.createElement('script');
    script.src=`https://www.paypal.com/sdk/js?client-id=${encodeURIComponent(config.clientId)}&currency=${encodeURIComponent(config.currency)}&intent=capture&components=buttons`;
    script.async=true;script.onload=()=>resolve();script.onerror=()=>reject(new Error('Không tải được PayPal JavaScript SDK'));
    document.head.appendChild(script);
  });
  return sdkPromise;
}

export default function PayPalSandboxButton({sessionId,discountAmount,disabled,onPaid}:{sessionId:number;discountAmount:number;disabled:boolean;onPaid:()=>Promise<void>}) {
  const target=useRef<HTMLDivElement>(null);const[config,setConfig]=useState<PayPalConfig>();const[error,setError]=useState('');
  useEffect(()=>{api.paypalConfig().then(setConfig).catch(e=>setError(e instanceof Error?e.message:'Không tải được cấu hình PayPal'))},[]);
  useEffect(()=>{
    if(!config?.enabled||disabled||!target.current)return;
    let active=true;let buttons:PayPalButtons|undefined;target.current.innerHTML='';
    loadSdk(config).then(async()=>{
      if(!active||!target.current||!window.paypal)return;
      buttons=window.paypal.Buttons({style:{layout:'vertical',shape:'rect',label:'paypal'},createOrder:async()=>(await api.createPayPalOrder(sessionId,discountAmount)).orderId,onApprove:async(data:{orderID:string})=>{await api.capturePayPalOrder(sessionId,data.orderID,discountAmount);await onPaid()},onCancel:()=>setError('Người dùng đã hủy thanh toán PayPal Sandbox'),onError:(reason:unknown)=>setError(reason instanceof Error?reason.message:'Thanh toán PayPal không thành công')});
      await buttons.render(target.current);
    }).catch(e=>setError(e instanceof Error?e.message:'Không khởi tạo được PayPal'));
    return()=>{active=false;if(buttons?.close)void buttons.close()};
  },[config,sessionId,discountAmount,disabled,onPaid]);
  if(!config)return <small>Đang kiểm tra PayPal Sandbox...</small>;
  if(!config.enabled)return <small>PayPal Sandbox chưa được cấu hình. Hãy thêm Client ID và Client Secret vào backend.</small>;
  if(disabled)return <small>Chỉ có thể thanh toán PayPal khi tất cả món đã được phục vụ.</small>;
  return <div className="paypal-sandbox"><div ref={target}/><small>Sandbox • Quy đổi theo 1 USD = {Number(config.vndPerUsd).toLocaleString('vi-VN')}đ</small>{error&&<p className="error">{error}</p>}</div>;
}

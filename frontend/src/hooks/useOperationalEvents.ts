import {useEffect,useRef,useState} from 'react';

type StaffEvent={type:string;title:string;message:string;referenceId?:number;createdAt:string};
const BASE=import.meta.env.VITE_API_URL||'/api/v1';

export function useOperationalEvents(onEvent:()=>void){
  const callback=useRef(onEvent);
  const [connected,setConnected]=useState(false);
  const [lastEvent,setLastEvent]=useState<StaffEvent>();
  callback.current=onEvent;
  useEffect(()=>{
    let stopped=false;
    let controller:AbortController|undefined;
    let reconnect:number|undefined;
    async function connect(){
      const auth=JSON.parse(localStorage.getItem('restaurant_auth')||'null');
      if(!auth?.accessToken)return;
      controller=new AbortController();
      try{
        const response=await fetch(`${BASE}/operations/events/stream`,{
          headers:{Authorization:`Bearer ${auth.accessToken}`,Accept:'text/event-stream'},
          signal:controller.signal
        });
        if(!response.ok||!response.body)throw new Error('stream unavailable');
        setConnected(true);
        const reader=response.body.getReader(),decoder=new TextDecoder();
        let buffer='';
        while(!stopped){
          const {done,value}=await reader.read();if(done)break;
          buffer+=decoder.decode(value,{stream:true});
          const blocks=buffer.split('\n\n');buffer=blocks.pop()||'';
          for(const block of blocks){
            const raw=block.split('\n').find(line=>line.startsWith('data:'));
            if(!raw)continue;
            const event=JSON.parse(raw.slice(5).trim()) as StaffEvent;
            if(event.type==='CONNECTED')continue;
            setLastEvent(event);callback.current();
            if(document.hidden&&Notification.permission==='granted'){
              const notification=new Notification(event.title,{body:event.message});
              notification.onclick=()=>{
                window.focus();
                window.location.href=event.type==='TABLE_CALL'?'/staff/ban':event.type.includes('RESERVATION')?'/staff/dat-ban':'/staff';
                notification.close();
              };
            }
          }
        }
      }catch(error){if(!stopped&&!(error instanceof DOMException&&error.name==='AbortError'))setConnected(false)}
      if(!stopped)reconnect=window.setTimeout(connect,3000);
    }
    void connect();
    return()=>{stopped=true;controller?.abort();if(reconnect)clearTimeout(reconnect)};
  },[]);
  return{connected,lastEvent};
}

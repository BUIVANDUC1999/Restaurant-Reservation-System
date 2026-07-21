export type MenuItem={id:number;name:string;category:string;price:number;description:string;imageUrl:string;featured:boolean}
export type Reservation={id:number;code:string;customerName:string;phone:string;email?:string;reservationDate:string;timeSlot:'LUNCH'|'DINNER';partySize:number;preferredFloor?:string;note?:string;status:string;createdAt:string}


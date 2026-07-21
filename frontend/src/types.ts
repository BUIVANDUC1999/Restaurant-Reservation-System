export type MenuItem={id:number;name:string;category:string;price:number;description:string;imageUrl:string;featured:boolean}
export type PreOrderItem={id:number;menuItemId:number;itemName:string;unitPrice:number;quantity:number;status:'REQUESTED'|'CONFIRMED'|'CANCELLED';lineTotal:number}
export type AssignedTable={id:number;code:string;name:string;floor:string;area:string;seats:number;status:RestaurantTable['status']}
export type Reservation={id:number;code:string;customerName:string;phone:string;email?:string;reservationDate:string;timeSlot:'LUNCH'|'DINNER';partySize:number;preferredFloor?:string;note?:string;status:string;createdAt:string;preOrderItems:PreOrderItem[];assignedTables:AssignedTable[];serviceSessionId?:number}
export type AuthUser={accessToken:string;id:number;fullName:string;email:string;role:'ADMIN'|'STAFF'|'KITCHEN'|'CASHIER'|'CUSTOMER'}
export type RestaurantTable={id:number;code:string;name:string;floor:string;area:string;seats:number;status:'AVAILABLE'|'RESERVED'|'OCCUPIED'|'NEEDS_CLEANING'|'INACTIVE';active:boolean}

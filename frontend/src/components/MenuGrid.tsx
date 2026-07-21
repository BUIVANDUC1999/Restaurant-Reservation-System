import type{MenuItem}from'../types'
export default function MenuGrid({items}:{items:MenuItem[]}){return <div className="menu-grid">{items.map(item=><article className="dish-card" key={item.id}><img src={item.imageUrl} alt={item.name}/><div className="dish-card-body"><span>{item.category}</span><h3>{item.name}</h3><p>{item.description}</p><strong>{Number(item.price).toLocaleString('vi-VN')}đ</strong></div></article>)}</div>}


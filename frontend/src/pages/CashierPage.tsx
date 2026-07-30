import {CheckCircle2, CreditCard, ReceiptText, RefreshCw} from 'lucide-react';
import {useEffect, useState} from 'react';
import {api} from '../api';
import type {Checkout} from '../types';
import PayPalSandboxButton from '../components/PayPalSandboxButton';

export default function CashierPage() {
  const [rows, setRows] = useState<Checkout[]>([]);
  const [discounts, setDiscounts] = useState<Record<number, number>>({});
  const [error, setError] = useState('');

  const load = () => api.checkouts()
    .then(data => { setRows(data); setError(''); })
    .catch(e => setError(e instanceof Error ? e.message : 'Không tải được hóa đơn'));

  useEffect(() => { void load(); }, []);

  return <section className="page-section container cashier-page">
    <div className="cashier-heading">
      <div><p className="eyebrow dark">KHU VỰC NHÂN VIÊN</p><h1>Hóa đơn & thanh toán</h1><p>Nhân viên xác nhận thanh toán trước khi hoàn tất lượt khách.</p></div>
      <button onClick={() => void load()}><RefreshCw/> Làm mới</button>
    </div>
    {error && <p className="error">{error}</p>}
    <div className="checkout-grid">
      {rows.map(row => <article className={row.paid ? 'paid' : ''} key={row.serviceSessionId}>
        <header><div><ReceiptText/><span><b>{row.invoiceCode || `Phiên #${row.serviceSessionId}`}</b><small>{row.tableCodes.join(', ')} • {row.customerName} • {row.partySize} khách</small></span></div>{row.paid && <strong><CheckCircle2/> Đã thanh toán</strong>}</header>
        <div className="invoice-lines">{row.items.map((item, index) => <div key={`${item.itemName}-${index}`}><span>{item.quantity}× {item.itemName}</span><b>{Number(item.lineTotal).toLocaleString('vi-VN')}đ</b></div>)}{!row.items.length && <p>Chưa có món đã phục vụ.</p>}</div>
        <dl>
          <div><dt>Tạm tính</dt><dd>{Number(row.subtotal).toLocaleString('vi-VN')}đ</dd></div>
          {row.paid && <div><dt>Giảm giá</dt><dd>-{Number(row.discountAmount).toLocaleString('vi-VN')}đ</dd></div>}
          <div className="invoice-total"><dt>Tổng thanh toán</dt><dd>{Number(row.paid ? row.totalAmount : Math.max(0, row.subtotal - (discounts[row.serviceSessionId] || 0))).toLocaleString('vi-VN')}đ</dd></div>
        </dl>
        {!row.paid && <div className="payment-form">
          <label>Giảm giá<input type="number" min="0" max={row.subtotal} value={discounts[row.serviceSessionId] || 0} onChange={e => setDiscounts(v => ({...v, [row.serviceSessionId]: Number(e.target.value)}))}/></label>
          <strong>Phương thức: PayPal Sandbox</strong>
          <PayPalSandboxButton sessionId={row.serviceSessionId} discountAmount={discounts[row.serviceSessionId] || 0} disabled={row.openOrderCount > 0} onPaid={async () => { await load(); }}/>
          {row.openOrderCount > 0 && <small>Còn {row.openOrderCount} phiếu món chưa phục vụ.</small>}
        </div>}
        {row.paid && <footer><CreditCard/> {row.paymentMethod === 'PAYPAL' ? 'PayPal Sandbox' : row.paymentMethod} • {row.paidAt && new Date(row.paidAt).toLocaleString('vi-VN')}</footer>}
      </article>)}
      {!rows.length && <div className="empty"><ReceiptText/><h2>Chưa có bàn chờ thanh toán</h2></div>}
    </div>
  </section>;
}

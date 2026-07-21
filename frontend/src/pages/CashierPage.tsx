import {Banknote, CheckCircle2, CreditCard, ReceiptText, RefreshCw} from 'lucide-react';
import {useEffect, useState} from 'react';
import {api} from '../api';
import type {Checkout} from '../types';

const methods = [['CASH', 'Tiền mặt'], ['BANK_TRANSFER', 'Chuyển khoản'], ['QR', 'Mã QR'], ['CARD', 'Thẻ']] as const;

export default function CashierPage() {
  const [rows, setRows] = useState<Checkout[]>([]);
  const [discounts, setDiscounts] = useState<Record<number, number>>({});
  const [paymentMethods, setPaymentMethods] = useState<Record<number, string>>({});
  const [busy, setBusy] = useState<number>();
  const [error, setError] = useState('');

  const load = () => api.checkouts()
    .then(data => { setRows(data); setError(''); })
    .catch(e => setError(e instanceof Error ? e.message : 'Không tải được hóa đơn'));

  useEffect(() => { void load(); }, []);

  async function pay(row: Checkout) {
    setBusy(row.serviceSessionId);
    try {
      await api.payCheckout(row.serviceSessionId, {
        method: paymentMethods[row.serviceSessionId] || 'CASH',
        discountAmount: discounts[row.serviceSessionId] || 0,
      });
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thanh toán được');
    } finally {
      setBusy(undefined);
    }
  }

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
          <label>Phương thức<select value={paymentMethods[row.serviceSessionId] || 'CASH'} onChange={e => setPaymentMethods(v => ({...v, [row.serviceSessionId]: e.target.value}))}>{methods.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>
          <button disabled={busy === row.serviceSessionId || row.openOrderCount > 0} onClick={() => pay(row)}><Banknote/> Xác nhận thanh toán</button>
          {row.openOrderCount > 0 && <small>Còn {row.openOrderCount} phiếu món chưa phục vụ.</small>}
        </div>}
        {row.paid && <footer><CreditCard/> {methods.find(([value]) => value === row.paymentMethod)?.[1]} • {row.paidAt && new Date(row.paidAt).toLocaleString('vi-VN')}</footer>}
      </article>)}
      {!rows.length && <div className="empty"><ReceiptText/><h2>Chưa có bàn chờ thanh toán</h2></div>}
    </div>
  </section>;
}

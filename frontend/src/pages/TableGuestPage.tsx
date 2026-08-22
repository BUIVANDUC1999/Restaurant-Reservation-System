import {BellRing, CheckCircle2, ChefHat, CreditCard, Droplets, MapPin, UtensilsCrossed, UserRound} from 'lucide-react';
import {useCallback, useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import {api} from '../api';
import type {GuestTable, TableRequest} from '../types';

const activeActions = [
  ['CALL_WAITER', 'Gọi nhân viên đặt món', ChefHat],
  ['WATER', 'Xin thêm nước', Droplets],
  ['UTENSILS', 'Xin dụng cụ', UtensilsCrossed],
  ['PAYMENT', 'Yêu cầu thanh toán', CreditCard]
] as const;
const requestLabels: Record<TableRequest['type'], string> = {
  CALL_WAITER: 'Gọi nhân viên', WATER: 'Xin thêm nước',
  UTENSILS: 'Xin dụng cụ', PAYMENT: 'Yêu cầu thanh toán'
};

export default function TableGuestPage() {
  const {token = ''} = useParams();
  const [table, setTable] = useState<GuestTable>();
  const [note, setNote] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try { setTable(await api.guestTable(token)); setError(''); }
    catch (exception) { setError(exception instanceof Error ? exception.message : 'QR bàn không hợp lệ'); }
  }, [token]);

  useEffect(() => {
    void load();
    const timer = setInterval(() => void load(), 8000);
    return () => clearInterval(timer);
  }, [load]);

  async function send(type: TableRequest['type']) {
    setBusy(true); setError(''); setMessage('');
    try {
      const created = await api.createTableRequest(token, type, note);
      setMessage(table?.activeSession
        ? `Đã gửi yêu cầu #${created.id}. Nhân viên phụ trách sẽ hỗ trợ tại bàn.`
        : `Đã gửi yêu cầu #${created.id}. Khách không cần đăng nhập; nhân viên sẽ đến hỗ trợ nhận bàn.`);
      setNote('');
      await load();
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Không gửi được yêu cầu');
    } finally { setBusy(false); }
  }

  if (error && !table) return <section className="guest-table-page guest-table-error">
    <BellRing/><h1>Không thể sử dụng QR</h1><p>{error}</p>
  </section>;

  const actions = table?.activeSession
    ? activeActions
    : [['CALL_WAITER', 'Gọi nhân viên hỗ trợ', UserRound] as const];

  return <section className="guest-table-page">
    <header className="guest-table-brand"><span>KV</span><div><b>KHÁM PHÁ VIỆT</b><small>Phục vụ tại bàn</small></div></header>
    <div className="guest-table-location"><MapPin/><div><small>VỊ TRÍ CỦA BẠN</small><h1>{table?.name || 'Đang tải...'}</h1>
      <p>{table?.area} · {table?.seats} ghế</p></div></div>

    {table?.activeSession ? <div className="guest-staff-card"><UserRound/><span><small>NHÂN VIÊN PHỤ TRÁCH</small>
      <b>{table.assignedStaffName || 'Đang điều phối nhân viên'}</b></span></div>
      : <div className="guest-inactive"><UserRound/><div><b>Bạn vừa đến bàn?</b>
        <p>Không cần đăng nhập. Phiên phục vụ sẽ được mở sau khi nhân viên xác nhận nhận khách tại bàn.</p></div></div>}

    <label className="guest-note">Ghi chú cho nhân viên
      <textarea maxLength={300} placeholder="Ví dụ: cần thực đơn, bàn có trẻ nhỏ..." value={note}
        onChange={event => setNote(event.target.value)}/>
    </label>

    <div className={`guest-actions ${table?.activeSession ? '' : 'single'}`}>
      {actions.map(([type, label, Icon]) => <button disabled={busy || !table} key={type} onClick={() => void send(type)}>
        <Icon/><b>{busy ? 'Đang gửi...' : label}</b>
      </button>)}
    </div>

    {message && <p className="guest-success"><CheckCircle2/>{message}</p>}
    {error && <p className="error">{error}</p>}
    {!!table?.requests.length && <div className="guest-open-requests"><h2>Yêu cầu đang xử lý</h2>
      {table.requests.map(request => <p key={request.id}><BellRing/>
        <span><b>{requestLabels[request.type]}</b><small>{request.status === 'NEW' ? 'Đã gửi' : 'Nhân viên đã nhận'}</small></span>
      </p>)}
    </div>}
    <footer>Trang QR công khai · Không yêu cầu tài khoản</footer>
  </section>;
}

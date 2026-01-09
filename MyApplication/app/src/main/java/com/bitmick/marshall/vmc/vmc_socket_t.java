package com.bitmick.marshall.vmc;

import com.bitmick.marshall.models.MsgObject;
import com.bitmick.marshall.vmc.marshall_t;
import com.bitmick.utils.Log;
/* loaded from: classes.dex */
public class vmc_socket_t extends vmc_client_t {
    public static final String TAG = "vmc_socket_t";
    static long last_tick = 0;
    private static boolean m_transfer_paused = false;
    private static final int vmc_socket_event_close_req_e = 3;
    private static final int vmc_socket_event_closed_e = 4;
    private static final int vmc_socket_event_error_e = 7;
    private static final int vmc_socket_event_init_done_e = 0;
    private static final int vmc_socket_event_open_req_e = 1;
    private static final int vmc_socket_event_opened_e = 2;
    private static final int vmc_socket_event_receive_e = 6;
    private static final int vmc_socket_event_transmit_e = 5;
    private static final int vmc_socket_state_close_req_e = 4;
    private static final int vmc_socket_state_closed_e = 5;
    private static final int vmc_socket_state_error_e = 6;
    private static final int vmc_socket_state_idle_e = 1;
    private static final int vmc_socket_state_init_e = 0;
    private static final int vmc_socket_state_open_req_e = 2;
    private static final int vmc_socket_state_opened_e = 3;
    public static final int vmc_socket_type_ftp_e = 2;
    public static final int vmc_socket_type_tcp_e = 0;
    private socket_callbacks_t m_callbacks;
    private socket_msg_t m_pending_tx_msg;
    private int m_pending_tx_msg_retries;
    private long m_pending_tx_msg_timeout;
    private int m_socket_state;
    private static String[] m_states_str = {"init", "idle", "open_req", "opened", "close_req", "closed", "error"};
    private static String[] m_events_str = {"init_done", "open_req", "opened", "close_req", "closed", "transmit", "receive", "error"};

    /* loaded from: classes.dex */
    public interface socket_callbacks_t {
        void onFTPDone(boolean z);

        void onReady();

        boolean onReceive(byte[] bArr, int i);

        void onSocketClosed();

        void onSocketError();

        void onSocketOpened();

        void onTimerTick(long j);

        void onTransmitDone(boolean z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class socket_msg_t {
        public byte[] buffer;
        public int len;
        public int offset;

        private socket_msg_t() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class socket_params_t {
        public String ip_addr;
        public Object meta;
        public int mtu;
        public short port;
        public int timeout;
        public short type;

        private socket_params_t() {
        }
    }

    public vmc_socket_t() {
        super(TAG);
        init();
    }

    @Override // com.bitmick.marshall.vmc.vmc_client_t
    public void onReady(vmc_link vmc_linkVar) {
        this.m_vmc_link = vmc_linkVar;
        int i = this.m_socket_state;
        if (i != 0 && i != 1) {
            close_socket();
        } else {
            notify(0, null);
        }
    }

    @Override // com.bitmick.marshall.vmc.vmc_client_t
    public void onCommError() {
        socket_callbacks_t socket_callbacks_tVar = this.m_callbacks;
        if (socket_callbacks_tVar != null) {
            socket_callbacks_tVar.onSocketError();
        }
        init();
    }

    @Override // com.bitmick.marshall.vmc.vmc_client_t
    public void onTimerTick(long j) {
        super.onTimerTick(j);
        socket_callbacks_t socket_callbacks_tVar = this.m_callbacks;
        if (socket_callbacks_tVar != null) {
            socket_callbacks_tVar.onTimerTick(j);
        }
        if (m_transfer_paused) {
            m_transfer_paused = false;
            socket_flow_control(false);
        }
        long j2 = j - last_tick;
        last_tick = j;
        long j3 = this.m_pending_tx_msg_timeout;
        if (j3 > 0) {
            long j4 = j3 - j2;
            this.m_pending_tx_msg_timeout = j4;
            if (j4 <= 0) {
                Log.d(TAG, String.format("retransmitting data packet. %d bytes", Integer.valueOf(this.m_pending_tx_msg.len)));
                int i = this.m_pending_tx_msg_retries - 1;
                this.m_pending_tx_msg_retries = i;
                if (i > 0) {
                    socket_msg_t socket_msg_tVar = this.m_pending_tx_msg;
                    if (socket_msg_tVar != null) {
                        transmit(socket_msg_tVar.buffer, this.m_pending_tx_msg.offset, this.m_pending_tx_msg.len);
                        return;
                    }
                    return;
                }
                socket_callbacks_t socket_callbacks_tVar2 = this.m_callbacks;
                if (socket_callbacks_tVar2 != null) {
                    socket_callbacks_tVar2.onTransmitDone(false);
                }
            }
        }
    }

    @Override // com.bitmick.marshall.vmc.vmc_client_t
    public boolean handleMarshallMessage(marshall_t.msg_marshall_t msg_marshall_tVar) {
        byte b = msg_marshall_tVar.func_code;
        if (b == 11) {
            marshall_t.msg_status_t msg_status_tVar = (marshall_t.msg_status_t) msg_marshall_tVar.body;
            Log.d(TAG, String.format("received status: %d", Byte.valueOf(msg_status_tVar.status)));
            if (msg_status_tVar.status == 6) {
                boolean z = msg_status_tVar.data[0] == 0;
                socket_callbacks_t socket_callbacks_tVar = this.m_callbacks;
                if (socket_callbacks_tVar != null) {
                    socket_callbacks_tVar.onFTPDone(z);
                }
                close_socket();
            }
        } else if (b == 32) {
            marshall_t.msg_modem_status_t msg_modem_status_tVar = (marshall_t.msg_modem_status_t) msg_marshall_tVar.body;
            String str = TAG;
            Log.d(str, String.format("received modem status: %d, socket state: %d", Integer.valueOf(msg_modem_status_tVar.status & 255), Integer.valueOf(this.m_socket_state)));
            byte b2 = msg_modem_status_tVar.status;
            if (b2 == 0) {
                int i = this.m_socket_state;
                if (i == 2) {
                    notify(2, null);
                } else if (i == 3) {
                    this.m_pending_tx_msg = null;
                    this.m_pending_tx_msg_retries = 0;
                    socket_callbacks_t socket_callbacks_tVar2 = this.m_callbacks;
                    if (socket_callbacks_tVar2 != null) {
                        socket_callbacks_tVar2.onTransmitDone(true);
                    }
                } else if (i == 4) {
                    notify(4, null);
                }
            } else if (b2 == 1) {
                Log.d(str, "socket fail");
                notify(7, null);
            } else if (b2 == 2) {
                Log.d(str, "socket busy");
                int i2 = this.m_socket_state;
                if (i2 == 2) {
                    notify(7, null);
                } else if (i2 == 3) {
                    marshall_t.msg_modem_status_t msg_modem_status_tVar2 = (marshall_t.msg_modem_status_t) msg_marshall_tVar.body;
                    if (msg_modem_status_tVar2.extra > 0) {
                        this.m_pending_tx_msg_timeout = msg_modem_status_tVar2.extra * 1000;
                    }
                }
            } else if (b2 == 5) {
                notify(4, null);
            }
        } else if (b == 36) {
            notify(6, msg_marshall_tVar.body);
        }
        return false;
    }

    private boolean change_state(int i, Object obj) {
        String str = TAG;
        String[] strArr = m_states_str;
        Log.d(str, String.format("changed state, from [%s] to [%s]", strArr[this.m_socket_state], strArr[i]));
        this.m_socket_state = i;
        switch (i) {
            case 1:
                socket_callbacks_t socket_callbacks_tVar = this.m_callbacks;
                if (socket_callbacks_tVar != null) {
                    socket_callbacks_tVar.onReady();
                    break;
                }
                break;
            case 2:
                marshall_t.msg_open_socket_t msg_open_socket_tVar = new marshall_t.msg_open_socket_t();
                socket_params_t socket_params_tVar = (socket_params_t) obj;
                msg_open_socket_tVar.dest = (byte) 1;
                msg_open_socket_tVar.protocol = (byte) socket_params_tVar.type;
                msg_open_socket_tVar.port = socket_params_tVar.port;
                msg_open_socket_tVar.ip_addr = socket_params_tVar.ip_addr;
                if (this.m_vmc_link.get_config().prot_ver_major >= 2) {
                    msg_open_socket_tVar.socket_mtu = (short) socket_params_tVar.mtu;
                    msg_open_socket_tVar.socket_timeout = (short) socket_params_tVar.timeout;
                }
                Log.d(str, String.format("open url: %s mtu: %d", socket_params_tVar.ip_addr, Short.valueOf(msg_open_socket_tVar.socket_mtu)));
                transmit_marshall(marshall_t.request((byte) 33, (byte) 1, msg_open_socket_tVar));
                break;
            case 3:
                socket_callbacks_t socket_callbacks_tVar2 = this.m_callbacks;
                if (socket_callbacks_tVar2 != null) {
                    socket_callbacks_tVar2.onSocketOpened();
                    break;
                }
                break;
            case 4:
                transmit_marshall(marshall_t.request((byte) 34, (byte) 0, new marshall_t.msg_close_socket_t()));
                break;
            case 5:
                socket_callbacks_t socket_callbacks_tVar3 = this.m_callbacks;
                if (socket_callbacks_tVar3 != null) {
                    socket_callbacks_tVar3.onSocketClosed();
                    break;
                }
                break;
            case 6:
                socket_callbacks_t socket_callbacks_tVar4 = this.m_callbacks;
                if (socket_callbacks_tVar4 != null) {
                    socket_callbacks_tVar4.onSocketError();
                    break;
                }
                break;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.bitmick.marshall.vmc.vmc_client_t
    public boolean handleMessage(MsgObject msgObject) {
        boolean z;
        super.handleMessage(msgObject);
        if (msgObject.id < m_events_str.length && msgObject.id != 6) {
            Log.d(TAG, String.format("received event: [%s], state: [%s]", m_events_str[msgObject.id], m_states_str[this.m_socket_state]));
        }
        do {
            switch (this.m_socket_state) {
                case 0:
                    if (msgObject.id == 0) {
                        z = change_state(1, null);
                        continue;
                    }
                    break;
                case 1:
                    int i = msgObject.id;
                    if (i == 1) {
                        z = change_state(2, msgObject.data);
                        continue;
                    } else if (i == 3) {
                        z = change_state(4, null);
                        continue;
                    } else if (i == 6) {
                        Log.d(TAG, "received data, without opened socket. closing...");
                        z = change_state(4, null);
                        continue;
                    }
                    break;
                case 2:
                    int i2 = msgObject.id;
                    if (i2 == 2) {
                        z = change_state(3, null);
                        continue;
                    } else if (i2 == 3) {
                        z = change_state(4, null);
                        continue;
                    } else if (i2 == 7) {
                        z = change_state(6, null);
                        continue;
                    }
                    break;
                case 3:
                    int i3 = msgObject.id;
                    if (i3 == 3) {
                        z = change_state(4, null);
                        continue;
                    } else if (i3 == 4) {
                        z = change_state(5, null);
                        continue;
                    } else if (i3 == 5) {
                        socket_msg_t socket_msg_tVar = (socket_msg_t) msgObject.data;
                        this.m_pending_tx_msg = socket_msg_tVar;
                        byte[] bArr = new byte[socket_msg_tVar.len];
                        System.arraycopy(socket_msg_tVar.buffer, socket_msg_tVar.offset, bArr, 0, socket_msg_tVar.len);
                        transmit_marshall(marshall_t.request((byte) 35, (byte) 0, new marshall_t.msg_socket_transfer_data_t(bArr)));
                        Log.d(TAG, String.format("transmitted %d bytes", Integer.valueOf(socket_msg_tVar.len)));
                        break;
                    } else if (i3 == 6) {
                        marshall_t.msg_socket_transfer_data_t msg_socket_transfer_data_tVar = (marshall_t.msg_socket_transfer_data_t) msgObject.data;
                        socket_callbacks_t socket_callbacks_tVar = this.m_callbacks;
                        if (socket_callbacks_tVar != null) {
                            socket_callbacks_tVar.onReceive(msg_socket_transfer_data_tVar.data, msg_socket_transfer_data_tVar.data.length);
                            break;
                        }
                    } else if (i3 == 7) {
                        z = change_state(6, null);
                        continue;
                    }
                    break;
                case 4:
                    int i4 = msgObject.id;
                    if (i4 == 4) {
                        z = change_state(5, null);
                        continue;
                    } else if (i4 == 7) {
                        z = change_state(6, null);
                        continue;
                    }
                    break;
                case 5:
                    z = change_state(1, null);
                    continue;
                case 6:
                    int i5 = msgObject.id;
                    if (i5 == 4) {
                        z = change_state(1, null);
                        continue;
                    } else if (i5 == 7) {
                        z = change_state(1, null);
                        continue;
                    }
                    break;
            }
            z = false;
            continue;
        } while (z);
        return true;
    }

    private void init() {
        this.m_vmc_link = null;
        this.m_pending_tx_msg_timeout = 0L;
        this.m_socket_state = 0;
    }

    private void transmit_marshall(marshall_t.msg_marshall_t msg_marshall_tVar) {
        if (this.m_vmc_link != null) {
            this.m_vmc_link.transmit_marshall(msg_marshall_tVar);
        }
    }

    private void socket_flow_control(boolean z) {
        marshall_t.msg_modem_rx_control_t msg_modem_rx_control_tVar = new marshall_t.msg_modem_rx_control_t();
        msg_modem_rx_control_tVar.control = z ? (byte) 1 : (byte) 0;
        transmit_marshall(marshall_t.request((byte) 35, (byte) 0, msg_modem_rx_control_tVar));
        m_transfer_paused = true;
    }

    public final void register_callbacks(socket_callbacks_t socket_callbacks_tVar) {
        this.m_callbacks = socket_callbacks_tVar;
    }

    public final boolean is_ready() {
        return this.m_socket_state == 1;
    }

    public final boolean is_opened() {
        return this.m_socket_state == 3;
    }

    public final void open_socket(int i, String str, short s) {
        open_socket(i, str, s, -1, -1);
    }

    public final void open_socket(int i, String str, short s, int i2, int i3) {
        socket_params_t socket_params_tVar = new socket_params_t();
        socket_params_tVar.type = (short) i;
        socket_params_tVar.ip_addr = str;
        socket_params_tVar.port = s;
        socket_params_tVar.meta = null;
        socket_params_tVar.mtu = i2;
        socket_params_tVar.timeout = i3;
        notify(1, socket_params_tVar);
    }

    public final void transmit(byte[] bArr, int i, int i2) {
        socket_msg_t socket_msg_tVar = new socket_msg_t();
        socket_msg_tVar.buffer = bArr;
        socket_msg_tVar.offset = i;
        socket_msg_tVar.len = i2;
        this.m_pending_tx_msg_retries = 3;
        notify(5, socket_msg_tVar);
    }

    public final void close_socket() {
        if (is_ready()) {
            return;
        }
        notify(3, null);
    }
}

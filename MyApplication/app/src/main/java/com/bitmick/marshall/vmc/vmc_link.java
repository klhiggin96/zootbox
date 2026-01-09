package com.bitmick.marshall.vmc;

import com.bitmick.marshall.interfaces.lowlevel_i;
import com.bitmick.marshall.drivers.serial_port_i;
import com.bitmick.marshall.models.MsgObject;
import com.bitmick.marshall.models.vmc_configuration;
import com.bitmick.marshall.vmc.marshall_t;
import com.bitmick.marshall.vmc.mdb_req_t;
import com.bitmick.utils.Log;
import com.bitmick.utils.StringUtils;
import com.bitmick.utils.Utils;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
/* loaded from: classes.dex */
public class vmc_link implements lowlevel_i.link_events_t {
    public static final int MARSHALL_NO_COMM_TIMEOUT = 15000;
    public static final int MARSHALL_RETRANSMIT_KA_INTERVAL = 1000;
    private static final int MAX_QUEUE_SIZE = 100;
    public static final byte PERIPHERAL_SUB_TYPE = 1;
    public static final String TAG = "vmc_link";
    public static final int TIMER_TICK_MS = 50;
    private static byte[] m_tx_buf = null;
    private static final int vmc_link_state_config_e = 2;
    private static final int vmc_link_state_error_e = 4;
    private static final int vmc_link_state_init_e = 0;
    private static final int vmc_link_state_ready_e = 3;
    private static final int vmc_link_state_reset_e = 1;
    private vmc_configuration m_app_config;
    private ArrayList<vmc_client_t> m_clients;
    private marshall_t.msg_config_t m_config;
    private lowlevel_i m_i_f;
    private serial_port_i m_serial_port;
    private boolean m_is_marshall_online;
    private Timer m_keepalive_timer;
    private long m_last_tick_timestamp;
    private vpos_link_stats_t m_link_stats;
    private int m_marshall_link_state;
    private LinkedList<marshall_t.msg_marshall_t> m_marshall_queue;
    private BlockingQueue<MsgObject> m_msg_queue;
    private marshall_t.msg_marshall_t m_pending_tx_msg;
    private int m_pending_tx_msg_retries;
    private long m_receive_timestamp;
    private volatile int m_rx_msg_id;
    private long m_transmit_timestamp;
    private volatile byte m_tx_msg_id;
    private vmc_link_events_t m_vmc_events;
    private Thread m_vmc_thread;
    private boolean m_vmc_thread_running;
    private vpos_config_t m_vpos_config;

    /* loaded from: classes.dex */
    public interface vmc_link_events_t {
        void onCommError();

        void onReady(vpos_config_t vpos_config_tVar);
    }

    /* loaded from: classes.dex */
    public static class vpos_config_t {
        public byte[] acquirer_terminal_id;
        public byte[] country_code;
        public byte[] currency_code;
        public byte decimal_place;
        public byte language;
        public byte[] machine_location;
        public int machine_type;
        public byte[] merchant_id;
        public byte prot_ver_major;
        public byte prot_ver_minor;
        public byte[] vpos_serial;
    }

    static /* synthetic */ int access$810(vmc_link vmc_linkVar) {
        int i = vmc_linkVar.m_pending_tx_msg_retries;
        vmc_linkVar.m_pending_tx_msg_retries = i - 1;
        return i;
    }

    /* loaded from: classes.dex */
    public class vpos_link_stats_t {
        public int comm_loss;
        public int crc_error;
        public int retrans;

        public vpos_link_stats_t() {
        }
    }

    public vmc_link() {
        init();
    }

    public vmc_link set_events(vmc_link_events_t vmc_link_events_tVar) {
        this.m_vmc_events = vmc_link_events_tVar;
        return this;
    }

    public void start() {
        if (this.m_app_config.always_idle) {
            this.m_app_config.reader_always_on = true;
        }
        Iterator<vmc_client_t> it = this.m_clients.iterator();
        while (it.hasNext()) {
            it.next().start();
        }
        if (this.m_app_config.debug) {
            Log.level(255);
        } else {
            Log.level(0);
        }
        Thread thread = new Thread(new VMCRunnable());
        this.m_vmc_thread = thread;
        thread.setPriority(10);
        this.m_vmc_thread.start();

        if (this.m_serial_port != null) {
            Log.d(TAG, "Starting serial port polling thread (FIX 3)");
            Thread serialThread = new Thread(new SerialPortPollingRunnable());
            serialThread.setPriority(10);
            serialThread.start();
        } else {
            this.m_i_f.init(this.m_app_config.port_vpos, Integer.valueOf(this.m_app_config.port_vpos_baud));
            this.m_i_f.register_link_events(this);
            this.m_i_f.start();
        }
        Log.d(TAG, String.format("marshall java sdk version %s", vmc_framework.get_version()));
    }

    public void stop() {
        Iterator<vmc_client_t> it = this.m_clients.iterator();
        while (it.hasNext()) {
            it.next().stop();
        }
        keepalive_stop();
        if (this.m_vmc_thread != null) {
            notify(65535, null);
            this.m_vmc_thread = null;
        }
        this.m_i_f.stop();
    }

    @Override // com.bitmick.marshall.interfaces.lowlevel_i.link_events_t
    public boolean onReceive(byte[] bArr, int i, int i2) {
        int i3;
        marshall_t.msg_marshall_t msg_marshall_tVar;
        marshall_t.msg_marshall_t msg_marshall_tVar2 = new marshall_t.msg_marshall_t();
        boolean z = msg_marshall_tVar2.decode(bArr, i, i2) > 0;
        if (z) {
            boolean z2 = (msg_marshall_tVar2.options & 1) != 0;
            int i4 = (msg_marshall_tVar2.options & 12) >> 2;
            int i5 = msg_marshall_tVar2.id & 255;
            dump_packet("rx", msg_marshall_tVar2, bArr, i, i2, false);
            if (msg_marshall_tVar2.func_code == 0) {
                if (((marshall_t.msg_response_t) msg_marshall_tVar2.body).value == 0 && (msg_marshall_tVar = this.m_pending_tx_msg) != null) {
                    if (msg_marshall_tVar.id != msg_marshall_tVar2.id) {
                        Log.e(TAG, String.format("spurious ack!. waiting for %d, received %d", Byte.valueOf(this.m_pending_tx_msg.id), Byte.valueOf(msg_marshall_tVar2.id)));
                    }
                    discard_last_tx_message_trigger_next();
                }
            } else {
                if (z2) {
                    transmit_marshall_raw(marshall_t.response((byte) 0, (byte) 0), msg_marshall_tVar2.id, true);
                }
                if (i4 != 0 && this.m_rx_msg_id > i5) {
                    Log.e(TAG, String.format("discarding duplicate frame: %d, retrans: %d", Integer.valueOf(this.m_rx_msg_id), Integer.valueOf(i4)));
                    return false;
                }
                if (is_ready() && (i3 = i5 - this.m_rx_msg_id) != 0) {
                    Log.e(TAG, String.format("not consecutive message. func_code %d received id: %d, expected id: %d, delta: %d", Byte.valueOf(msg_marshall_tVar2.func_code), Integer.valueOf(i5), Integer.valueOf(this.m_rx_msg_id), Integer.valueOf(i3)));
                }
                this.m_rx_msg_id = (i5 + 1) & 255;
            }
            this.m_receive_timestamp = Utils.currentTimeMillis();
            notify(1, msg_marshall_tVar2);
        } else {
            dump_packet("rx", msg_marshall_tVar2, bArr, i, i2, true);
            this.m_link_stats.crc_error++;
            this.m_i_f.reset();
        }
        return z;
    }

    private void init() {
        this.m_msg_queue = new ArrayBlockingQueue(100);
        this.m_clients = new ArrayList<>();
        marshall_init_variables();
        m_tx_buf = new byte[512];
        this.m_link_stats = new vpos_link_stats_t();
    }

    private void marshall_init_variables() {
        this.m_rx_msg_id = 0;
        this.m_tx_msg_id = (byte) 0;
        this.m_config = new marshall_t.msg_config_t();
        this.m_transmit_timestamp = 0L;
        this.m_is_marshall_online = false;
        this.m_pending_tx_msg = null;
        this.m_pending_tx_msg_retries = 3;
        this.m_marshall_queue = new LinkedList<>();
        this.m_marshall_link_state = 0;
    }

    private void tx_queue_reset() {
        synchronized (this.m_marshall_queue) {
            this.m_marshall_queue.clear();
        }
    }

    private boolean tx_queue_is_empty() {
        boolean isEmpty;
        synchronized (this.m_marshall_queue) {
            isEmpty = this.m_marshall_queue.isEmpty();
        }
        return isEmpty;
    }

    private marshall_t.msg_marshall_t tx_queue_peek() {
        marshall_t.msg_marshall_t peekFirst;
        synchronized (this.m_marshall_queue) {
            peekFirst = !tx_queue_is_empty() ? this.m_marshall_queue.peekFirst() : null;
        }
        return peekFirst;
    }

    private void tx_queue_enqueue(marshall_t.msg_marshall_t msg_marshall_tVar) {
        synchronized (this.m_marshall_queue) {
            this.m_marshall_queue.addLast(msg_marshall_tVar);
        }
    }

    private marshall_t.msg_marshall_t tx_queue_dequeue() {
        marshall_t.msg_marshall_t msg_marshall_tVar;
        synchronized (this.m_marshall_queue) {
            msg_marshall_tVar = null;
            try {
                if (!tx_queue_is_empty()) {
                    msg_marshall_tVar = this.m_marshall_queue.removeFirst();
                }
            } catch (Exception unused) {
            }
        }
        return msg_marshall_tVar;
    }

    private void discard_last_tx_message_trigger_next() {
        tx_queue_dequeue();
        this.m_pending_tx_msg = null;
        this.m_pending_tx_msg_retries = 3;
        if (tx_queue_is_empty()) {
            return;
        }
        notify(2, null);
    }

    private void keepalive_start() {
        if (this.m_keepalive_timer == null) {
            Timer timer = new Timer(true);
            this.m_keepalive_timer = timer;
            timer.scheduleAtFixedRate(new KeepaliveTimerTask(), 0L, 50L);
            this.m_last_tick_timestamp = Utils.currentTimeMillis();
        }
    }

    private void keepalive_stop() {
        Timer timer = this.m_keepalive_timer;
        if (timer != null) {
            timer.cancel();
            this.m_keepalive_timer = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean marshall_link_change_state(int i) {
        if (i == 0) {
            keepalive_stop();
            marshall_init_variables();
            Iterator<vmc_client_t> it = this.m_clients.iterator();
            while (it.hasNext()) {
                it.next().onCommError();
            }
        } else if (i == 2) {
            keepalive_start();
        } else if (i == 3) {
            Log.d(TAG, String.format("vmc is online. time: %s", new Date()));
            if (this.m_is_marshall_online) {
                this.m_link_stats.comm_loss++;
            }
            this.m_is_marshall_online = true;
            Iterator<vmc_client_t> it2 = this.m_clients.iterator();
            while (it2.hasNext()) {
                it2.next().onReady(this);
            }
            vmc_link_events_t vmc_link_events_tVar = this.m_vmc_events;
            if (vmc_link_events_tVar != null) {
                vmc_link_events_tVar.onReady(this.m_vpos_config);
            }
        }
        this.m_marshall_link_state = i;
        return true;
    }

    private void marshall_link_state(marshall_t.msg_marshall_t msg_marshall_tVar) {
        byte b;
        int i = this.m_marshall_link_state;
        if (i == 0) {
            if (msg_marshall_tVar.func_code != 1) {
                return;
            }
            marshall_t.msg_fw_info_t msg_fw_info_tVar = new marshall_t.msg_fw_info_t();
            msg_fw_info_tVar.prot_ver = (short) 512;
            msg_fw_info_tVar.periph_type = (byte) this.m_app_config.machine_type;
            msg_fw_info_tVar.periph_sub_type = (byte) 1;
            msg_fw_info_tVar.periph_cap_bitmap = (short) 0;
            msg_fw_info_tVar.model = StringUtils.padRight(this.m_app_config.model, 19).substring(0, 19);
            msg_fw_info_tVar.serial = StringUtils.padRight(this.m_app_config.serial, 19).substring(0, 19);
            msg_fw_info_tVar.app_sw_ver = StringUtils.padRight(this.m_app_config.sw_ver, 19).substring(0, 19);
            msg_fw_info_tVar.vmc_hw_ver = StringUtils.padRight(this.m_app_config.hw_ver, 19).substring(0, 19);
            msg_fw_info_tVar.vmc_manuf_code = StringUtils.padRight(this.m_app_config.manuf_code, 19).substring(0, 19);
            msg_fw_info_tVar.marshall_sdk_version = StringUtils.padRight(this.m_app_config.marshall_sdk_version, 19).substring(0, 19);
            if (this.m_app_config.multi_session_support) {
                msg_fw_info_tVar.periph_cap_bitmap = (short) (msg_fw_info_tVar.periph_cap_bitmap | 64);
            }
            if (this.m_app_config.price_not_final_support) {
                msg_fw_info_tVar.periph_cap_bitmap = (short) (msg_fw_info_tVar.periph_cap_bitmap | 256);
            }
            if (this.m_app_config.mag_card_approved_by_vmc_support) {
                msg_fw_info_tVar.periph_cap_bitmap = (short) (512 | msg_fw_info_tVar.periph_cap_bitmap);
            }
            if (this.m_app_config.mifare_approved_by_vmc_support) {
                msg_fw_info_tVar.periph_cap_bitmap = (short) (msg_fw_info_tVar.periph_cap_bitmap | marshall_t.msg_fw_info_t.fw_info_periph_cap_bitmap_mifare_card_approved_by_vmc);
            }
            transmit_marshall(marshall_t.firmware_info((byte) 0, msg_fw_info_tVar));
            this.m_rx_msg_id = 0;
            marshall_link_change_state(1);
        } else if (i != 1) {
            if (i == 2) {
                if (msg_marshall_tVar.func_code == 1) {
                    marshall_link_change_state(0);
                } else {
                    marshall_link_change_state(3);
                }
            } else if (i == 3 && (b = msg_marshall_tVar.func_code) != 0) {
                if (b == 1) {
                    marshall_link_change_state(0);
                    return;
                }
                Iterator<vmc_client_t> it = this.m_clients.iterator();
                while (it.hasNext() && !it.next().handleMarshallMessage(msg_marshall_tVar)) {
                }
            }
        } else {
            byte b2 = msg_marshall_tVar.func_code;
            if (b2 == 1) {
                marshall_link_change_state(0);
            } else if (b2 != 6) {
            } else {
                this.m_config = (marshall_t.msg_config_t) msg_marshall_tVar.body;
                vpos_config_t vpos_config_tVar = new vpos_config_t();
                this.m_vpos_config = vpos_config_tVar;
                vpos_config_tVar.prot_ver_major = this.m_config.prot_ver_major;
                this.m_vpos_config.prot_ver_minor = this.m_config.prot_ver_minor;
                this.m_vpos_config.country_code = this.m_config.country_code;
                this.m_vpos_config.currency_code = this.m_config.currency_code;
                this.m_vpos_config.decimal_place = this.m_config.decimal_place;
                this.m_vpos_config.language = this.m_config.language;
                this.m_vpos_config.vpos_serial = this.m_config.vpos_serial;
                if (this.m_vpos_config.prot_ver_major >= 1) {
                    this.m_vpos_config.merchant_id = this.m_config.merchant_id;
                    this.m_vpos_config.acquirer_terminal_id = this.m_config.acquirer_terminal_id;
                    this.m_vpos_config.machine_location = this.m_config.machine_location;
                }
                marshall_link_change_state(2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean handle_message(MsgObject msgObject) {
        int i = msgObject.id;
        if (i == 1) {
            marshall_link_state((marshall_t.msg_marshall_t) msgObject.data);
        } else if (i == 2) {
            marshall_t.msg_marshall_t tx_queue_peek = tx_queue_peek();
            if (this.m_pending_tx_msg != null) {
                Log.e(TAG, "trying to transmit while did not get message back!");
            }
            if (tx_queue_peek == null) {
                Log.e(TAG, "requested to transmit from null queue!");
            } else {
                if (tx_queue_peek.func_code == Byte.MIN_VALUE) {
                    Log.d(TAG, String.format("transmitting mdb command: %d", Byte.valueOf(((mdb_req_t.msg_mdb_req_t) tx_queue_peek.body).command)));
                }
                transmit_marshall_raw(tx_queue_peek, this.m_tx_msg_id, true);
                this.m_tx_msg_id = (byte) (this.m_tx_msg_id + 1);
                if ((tx_queue_peek.options & 1) == 0) {
                    discard_last_tx_message_trigger_next();
                } else {
                    this.m_pending_tx_msg = tx_queue_peek;
                }
            }
        } else if (i == 65535) {
            this.m_vmc_thread_running = false;
        }
        return true;
    }

    private void notify(int i, Object obj) {
        MsgObject msgObject = new MsgObject();
        msgObject.id = i;
        msgObject.data = obj;
        try {
            this.m_msg_queue.add(msgObject);
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    /* loaded from: classes.dex */
    private class SerialPortPollingRunnable implements Runnable {
        private SerialPortPollingRunnable() {
        }

        @Override
        public void run() {
            byte[] pollingBuffer = new byte[16384];
            int head = 0;
            int tail = 0;
            while (vmc_link.this.m_vmc_thread_running) {
                try {
                    int available = vmc_link.this.m_serial_port.available();
                    if (available > 0) {
                        int maxToRead = Math.min(available, pollingBuffer.length - head);
                        if (maxToRead > 0) {
                            byte[] temp = new byte[maxToRead];
                            int bytesRead = vmc_link.this.m_serial_port.read(temp, maxToRead);
                            if (bytesRead > 0) {
                                System.arraycopy(temp, 0, pollingBuffer, head, bytesRead);
                                head += bytesRead;

                                // Perform Marshall packet framing
                                while (head != tail) {
                                    if (head - tail < 2) break;
                                    
                                    // Little-endian length prefix + 2
                                    int packetLen = ((pollingBuffer[tail] & 0xFF) | ((pollingBuffer[tail + 1] & 0xFF) << 8)) + 2;
                                    
                                    if (packetLen < 9 || packetLen > 512) {
                                        // Invalid length, reset buffer
                                        Log.w(TAG, "Invalid packet length: " + packetLen + " (bytes: 0x" +
                                            Integer.toHexString(pollingBuffer[tail] & 0xFF) + " 0x" +
                                            Integer.toHexString(pollingBuffer[tail + 1] & 0xFF) + "), resetting");
                                        head = 0;
                                        tail = 0;
                                        break;
                                    }
                                    
                                    if (head - tail >= packetLen) {
                                        vmc_link.this.onReceive(pollingBuffer, tail, packetLen);
                                        tail += packetLen;
                                        
                                        // Reset buffer if caught up and near end
                                        if (pollingBuffer.length - tail <= 512 && tail == head) {
                                            head = 0;
                                            tail = 0;
                                        }
                                    } else {
                                        // Wait for more data
                                        break;
                                    }
                                }
                            }
                        } else {
                            // Buffer full but no packet found, reset
                            head = 0;
                            tail = 0;
                        }
                    }
                    Thread.sleep(5);
                } catch (Exception e) {
                    Log.e(TAG, "Serial polling error: " + e.getMessage());
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private class VMCRunnable implements Runnable {
        private VMCRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            vmc_link.this.m_vmc_thread_running = true;
            while (vmc_link.this.m_vmc_thread_running) {
                try {
                    vmc_link.this.handle_message((MsgObject) vmc_link.this.m_msg_queue.take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void transmit_marshall_lowlevel(marshall_t.msg_marshall_t msg_marshall_tVar) {
        byte[] bArr = m_tx_buf;
        int encode = msg_marshall_tVar.encode(bArr, 0, bArr.length);
        if (this.m_serial_port != null) {
            byte[] txData = new byte[encode];
            System.arraycopy(m_tx_buf, 0, txData, 0, encode);
            this.m_serial_port.write(txData);
        } else {
            this.m_i_f.transmit(m_tx_buf, encode);
        }
        dump_packet("tx", msg_marshall_tVar, m_tx_buf, 0, encode, false);
        this.m_transmit_timestamp = Utils.currentTimeMillis();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void transmit_marshall_raw(marshall_t.msg_marshall_t msg_marshall_tVar, byte b, boolean z) {
        if (z) {
            msg_marshall_tVar.source = this.m_config.dev_src_id;
            msg_marshall_tVar.source_lsb = (byte) this.m_app_config.serial.charAt(0);
            msg_marshall_tVar.dest = (byte) 0;
            msg_marshall_tVar.dest_lsb = this.m_config.vpos_serial[0];
            msg_marshall_tVar.id = b;
            transmit_marshall_lowlevel(msg_marshall_tVar);
            return;
        }
        boolean tx_queue_is_empty = tx_queue_is_empty();
        tx_queue_enqueue(msg_marshall_tVar);
        if (tx_queue_is_empty) {
            notify(2, null);
        }
    }

    public final vmc_client_t register_vmc_client(vmc_client_t vmc_client_tVar) {
        this.m_clients.add(vmc_client_tVar);
        return vmc_client_tVar;
    }

    public vmc_link set_lowlevel(lowlevel_i lowlevel_iVar) {
        this.m_i_f = lowlevel_iVar;
        return this;
    }

    public vmc_link set_serial_port(serial_port_i port) {
        this.m_serial_port = port;
        return this;
    }

    public vmc_link configure(vmc_configuration vmc_configurationVar) {
        this.m_app_config = vmc_configurationVar;
        return this;
    }

    public vmc_configuration get_configuration() {
        return this.m_app_config;
    }

    public final void transmit_marshall(marshall_t.msg_marshall_t msg_marshall_tVar) {
        transmit_marshall_raw(msg_marshall_tVar, this.m_tx_msg_id, false);
    }

    public final boolean is_ready() {
        return this.m_is_marshall_online;
    }

    public vpos_config_t get_config() {
        return this.m_vpos_config;
    }

    public int get_marshall_version() {
        vpos_config_t vpos_config_tVar = this.m_vpos_config;
        if (vpos_config_tVar != null) {
            return (vpos_config_tVar.prot_ver_major * marshall_t.marshalll_display_control_button_id_left_arrow) + this.m_vpos_config.prot_ver_minor;
        }
        return 512;
    }

    public vpos_link_stats_t get_stats() {
        return this.m_link_stats;
    }

    /* loaded from: classes.dex */
    public class KeepaliveTimerTask extends TimerTask {
        public KeepaliveTimerTask() {
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            long currentTimeMillis = Utils.currentTimeMillis();
            long j = currentTimeMillis - vmc_link.this.m_transmit_timestamp;
            long j2 = currentTimeMillis - vmc_link.this.m_receive_timestamp;
            int i = vmc_link.this.m_marshall_link_state;
            if (i != 0 && i != 1 && j > 1000) {
                if (vmc_link.this.m_pending_tx_msg != null) {
                    if (vmc_link.this.m_pending_tx_msg_retries <= 0) {
                        vmc_link.this.m_pending_tx_msg_retries = 3;
                    } else {
                        Log.e(vmc_link.TAG, String.format("retransmitting... last_rx: %d, last_tx: %d, attempts left: #%d", Long.valueOf(j2), Long.valueOf(j), Integer.valueOf(vmc_link.this.m_pending_tx_msg_retries)));
                        vmc_link.access$810(vmc_link.this);
                        byte b = (byte) (3 - vmc_link.this.m_pending_tx_msg_retries);
                        marshall_t.msg_marshall_t msg_marshall_tVar = vmc_link.this.m_pending_tx_msg;
                        msg_marshall_tVar.options = (byte) (msg_marshall_tVar.options & (-13));
                        vmc_link.this.m_pending_tx_msg.options = (byte) ((b << 2) | vmc_link.this.m_pending_tx_msg.options);
                        vmc_link vmc_linkVar = vmc_link.this;
                        vmc_linkVar.transmit_marshall_raw(vmc_linkVar.m_pending_tx_msg, vmc_link.this.m_pending_tx_msg.id, true);
                        vmc_link.this.m_link_stats.retrans++;
                    }
                } else {
                    vmc_link.this.transmit_marshall(marshall_t.keepalive());
                }
            }
            if (vmc_link.this.m_is_marshall_online) {
                long j3 = currentTimeMillis - vmc_link.this.m_last_tick_timestamp;
                if (j3 > 500) {
                    Log.d(vmc_link.TAG, String.format("warning! sdk is not getting enough cpu! (starved for %dms", Long.valueOf(j3)));
                }
                vmc_link.this.m_last_tick_timestamp = currentTimeMillis;
                int i2 = vmc_link.this.m_config.keepalive_interval_ms;
                int i3 = vmc_link.MARSHALL_NO_COMM_TIMEOUT;
                if (i2 > 15000) {
                    i3 = vmc_link.this.m_config.keepalive_interval_ms;
                }
                if (j2 > i3) {
                    Log.e(vmc_link.TAG, "link down");
                    vmc_link.this.marshall_link_change_state(0);
                    if (vmc_link.this.m_vmc_events != null) {
                        vmc_link.this.m_vmc_events.onCommError();
                    }
                    Iterator it = vmc_link.this.m_clients.iterator();
                    while (it.hasNext()) {
                        ((vmc_client_t) it.next()).onCommError();
                    }
                    vmc_link.this.m_link_stats.comm_loss++;
                }
            }
            Iterator it2 = vmc_link.this.m_clients.iterator();
            while (it2.hasNext()) {
                ((vmc_client_t) it2.next()).onTimerTick(currentTimeMillis);
            }
            vmc_link.this.m_i_f.onLinkTimerTick(currentTimeMillis);
        }
    }

    private void dump_packet(String str, marshall_t.msg_marshall_t msg_marshall_tVar, byte[] bArr, int i, int i2, boolean z) {
        if (this.m_app_config.dump_packets_level != 2 ? this.m_app_config.dump_packets_level == 1 : !(this.m_pending_tx_msg == null ? msg_marshall_tVar.func_code == 7 : msg_marshall_tVar.func_code != 0 || this.m_pending_tx_msg.func_code == 7)) {
            z = true;
        }
        if (z) {
            Utils.dumpPacket(str, bArr, i, i2);
        }
    }
}

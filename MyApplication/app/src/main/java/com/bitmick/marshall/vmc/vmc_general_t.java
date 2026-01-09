package com.bitmick.marshall.vmc;

import com.bitmick.marshall.vmc.marshall_t;
import com.bitmick.utils.Log;
import java.util.Iterator;
/* loaded from: classes.dex */
public class vmc_general_t extends vmc_client_t {
    private static final String TAG = "vmc_general_t";
    private GeneralEvents m_general_events;

    /* loaded from: classes.dex */
    public interface GeneralEvents {
        void onDCSParamChange(int i, byte[] bArr);

        void onDisplayEventButtonPressed(byte b);

        void onDisplayEventTextInput(String str);

        void onTimeUpdate(byte b, byte b2, byte b3, byte b4, byte b5, byte b6);

        void onTransferData(byte[] bArr);
    }

    public vmc_general_t() {
        super(TAG);
    }

    @Override // com.bitmick.marshall.vmc.vmc_client_t
    public void onReady(vmc_link vmc_linkVar) {
        this.m_vmc_link = vmc_linkVar;
    }

    @Override // com.bitmick.marshall.vmc.vmc_client_t
    public void onCommError() {
        this.m_vmc_link = null;
    }

    @Override // com.bitmick.marshall.vmc.vmc_client_t
    public boolean handleMarshallMessage(marshall_t.msg_marshall_t msg_marshall_tVar) {
        GeneralEvents generalEvents;
        byte b = msg_marshall_tVar.func_code;
        if (b == 9) {
            Log.d(TAG, String.format("display message status: %d", Byte.valueOf(((marshall_t.msg_display_status_t) msg_marshall_tVar.body).status)));
        } else if (b == 10) {
            marshall_t.msg_transfer_data_t msg_transfer_data_tVar = (marshall_t.msg_transfer_data_t) msg_marshall_tVar.body;
            if (msg_transfer_data_tVar.excel_data != null && (generalEvents = this.m_general_events) != null) {
                generalEvents.onTransferData(msg_transfer_data_tVar.excel_data);
            }
        } else if (b == 13) {
            marshall_t.msg_time_rsp_t msg_time_rsp_tVar = (marshall_t.msg_time_rsp_t) msg_marshall_tVar.body;
            GeneralEvents generalEvents2 = this.m_general_events;
            if (generalEvents2 != null) {
                generalEvents2.onTimeUpdate(msg_time_rsp_tVar.year, msg_time_rsp_tVar.month, msg_time_rsp_tVar.day, msg_time_rsp_tVar.hours, msg_time_rsp_tVar.minutes, msg_time_rsp_tVar.seconds);
            }
        } else if (b == 14) {
            marshall_t.msg_set_periph_param_t msg_set_periph_param_tVar = (marshall_t.msg_set_periph_param_t) msg_marshall_tVar.body;
            GeneralEvents generalEvents3 = this.m_general_events;
            if (generalEvents3 != null) {
                generalEvents3.onDCSParamChange(msg_set_periph_param_tVar.dcs_code, msg_set_periph_param_tVar.data);
            }
        } else if (b == 21) {
            marshall_t.msg_display_event_t msg_display_event_tVar = (marshall_t.msg_display_event_t) msg_marshall_tVar.body;
            Log.d(TAG, String.format("display event: %d clicks", Integer.valueOf(msg_display_event_tVar.buttons.size())));
            if (this.m_general_events != null) {
                Iterator<Byte> it = msg_display_event_tVar.buttons.iterator();
                while (it.hasNext()) {
                    this.m_general_events.onDisplayEventButtonPressed(it.next().byteValue());
                }
                Iterator<String> it2 = msg_display_event_tVar.text.iterator();
                while (it2.hasNext()) {
                    this.m_general_events.onDisplayEventTextInput(it2.next());
                }
            }
        }
        return false;
    }

    public void register_callbacks(GeneralEvents generalEvents) {
        this.m_general_events = generalEvents;
    }

    public void alert(short s, String str) {
        if (this.m_vmc_link != null) {
            this.m_vmc_link.transmit_marshall(marshall_t.request((byte) 49, (byte) 1, new marshall_t.msg_alert_t(s, str)));
        }
    }

    public void set_param(short s, byte[] bArr) {
        this.m_vmc_link.transmit_marshall(marshall_t.request((byte) 14, (byte) 1, new marshall_t.msg_set_periph_param_t(s, bArr)));
    }

    public void request_time() {
        this.m_vmc_link.transmit_marshall(marshall_t.request((byte) 12, (byte) 1, new marshall_t.msg_time_req_t()));
    }

    public void set_display_text(String str, short s, short s2) {
        this.m_vmc_link.transmit_marshall(marshall_t.request((byte) 8, (byte) 1, new marshall_t.msg_display_msg_t(str, s, s2)));
    }

    public Object display_control_create_layout(int i) {
        return new marshall_t.msg_display_control_t.layout_t(i);
    }

    public Object display_control_layout_add_label(Object obj, int i, int i2, String str) {
        if (obj instanceof marshall_t.msg_display_control_t.layout_t) {
            ((marshall_t.msg_display_control_t.layout_t) obj).add(new marshall_t.msg_display_control_t.label_t(i, i2, str));
            return obj;
        }
        return null;
    }

    public Object display_control_layout_add_button(Object obj, int i) {
        if (obj instanceof marshall_t.msg_display_control_t.layout_t) {
            ((marshall_t.msg_display_control_t.layout_t) obj).add((byte) i);
            return obj;
        }
        return null;
    }

    public void display_control_apply_layout(Object obj) {
        if (this.m_vmc_link == null || !(obj instanceof marshall_t.msg_display_control_t.layout_t)) {
            return;
        }
        marshall_t.msg_display_control_t msg_display_control_tVar = new marshall_t.msg_display_control_t();
        msg_display_control_tVar.add((marshall_t.msg_display_control_t.layout_t) obj);
        this.m_vmc_link.transmit_marshall(marshall_t.request((byte) 20, (byte) 1, msg_display_control_tVar));
    }
}

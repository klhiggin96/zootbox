package com.bitmick.marshall.vmc;

import com.bitmick.marshall.vmc.marshall_t;
import com.bitmick.marshall.vmc.mdb_t;
import com.bitmick.utils.ByteArrayUtils;
import com.bitmick.utils.Log;
/* loaded from: classes.dex */
public class mdb_rsp_t extends mdb_t {
    public static final String TAG = "mdb_rsp_t";
    public static final byte mdb_response_expansion_diagnostics_response = -1;
    public static final byte mdb_response_expansion_ok_to_send = 30;
    public static final byte mdb_response_expansion_pripheral_id = 9;
    public static final byte mdb_response_expansion_retry_deny = 28;
    public static final byte mdb_response_expansion_send_block = 29;
    public static final byte mdb_response_expansion_user_file_data = 16;
    public static final byte mdb_response_poll_begin_session = 3;
    public static final byte mdb_response_poll_cancelled = 8;
    public static final byte mdb_response_poll_cmd_out_of_seq = 11;
    public static final byte mdb_response_poll_data_entry_cancel = 19;
    public static final byte mdb_response_poll_data_entry_req = 18;
    public static final byte mdb_response_poll_diagnostics_response = -1;
    public static final byte mdb_response_poll_display_req = 2;
    public static final byte mdb_response_poll_end_session = 7;
    public static final byte mdb_response_poll_ftl_req_to_rcv = 27;
    public static final byte mdb_response_poll_ftl_retry_deny = 28;
    public static final byte mdb_response_poll_ftl_send_block = 29;
    public static final byte mdb_response_poll_just_reset = 0;
    public static final byte mdb_response_poll_malfunction_error = 10;
    public static final byte mdb_response_poll_ok_to_send = 30;
    public static final byte mdb_response_poll_peripheral_id = 9;
    public static final byte mdb_response_poll_reader_config_data = 1;
    public static final byte mdb_response_poll_req_to_send = 31;
    public static final byte mdb_response_poll_revalue_approved = 13;
    public static final byte mdb_response_poll_revalue_denied = 14;
    public static final byte mdb_response_poll_revalue_limit_amount = 15;
    public static final byte mdb_response_poll_selection_request = 20;
    public static final byte mdb_response_poll_session_cancel_req = 4;
    public static final byte mdb_response_poll_time_date_req = 17;
    public static final byte mdb_response_poll_user_file_data = 16;
    public static final byte mdb_response_poll_vend_approved = 5;
    public static final byte mdb_response_poll_vend_denied = 6;
    public static final byte mdb_response_reader_cancelled = 8;
    public static final byte mdb_response_reader_state_rsp = -120;
    public static final byte mdb_response_revalue_approved = 13;
    public static final byte mdb_response_revalue_denied = 14;
    public static final byte mdb_response_revalue_limit_amount = 15;
    public static final byte mdb_response_sessions_status = -122;
    public static final byte mdb_response_setup_reader_config_data = 1;
    public static final byte mdb_response_vend_approved = 5;
    public static final byte mdb_response_vend_denied = 6;
    public static final byte mdb_response_vend_end_session = 7;

    /* loaded from: classes.dex */
    public static class msg_mdb_rsp_t extends marshall_t.msg_i {
        public marshall_t.msg_i body;
        public byte response;

        public msg_mdb_rsp_t() {
        }

        public msg_mdb_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            this.response = bArr[i];
            Log.d(mdb_rsp_t.TAG, String.format("received mdb: %d", Byte.valueOf(this.response)));
            byte b = this.response;
            if (b == -122) {
                this.body = new mdb_t.msg_sessions_status_t();
            } else if (b != -120) {
                switch (b) {
                    case 0:
                        break;
                    case 1:
                        this.body = new msg_setup_config_data_rsp_t();
                        break;
                    case 2:
                        this.body = new msg_display_request_rsp_t();
                        break;
                    case 3:
                        this.body = new msg_begin_session_rsp_t();
                        break;
                    case 4:
                        this.body = new msg_session_cancel_req_rsp_t();
                        break;
                    case 5:
                        this.body = new msg_vend_approved_rsp_t();
                        break;
                    case 6:
                        this.body = new msg_vend_denied_rsp_t();
                        break;
                    case 7:
                        this.body = new msg_end_session_rsp_t();
                        break;
                    case 8:
                        this.body = new msg_cancelled_rsp_t();
                        break;
                    case 9:
                        this.body = new msg_peripheral_id_rsp_t();
                        break;
                    case 10:
                        this.body = new msg_malfunction_error_rsp_t();
                        break;
                    case 11:
                        this.body = new msg_cmd_out_of_seq_rsp_t();
                        break;
                    default:
                        switch (b) {
                            case 13:
                                this.body = new msg_revalue_approved_rsp_t();
                                break;
                            case 14:
                                this.body = new msg_revalue_denied_rsp_t();
                                break;
                            case 15:
                                this.body = new msg_limit_amount_rsp_t();
                                break;
                            case 16:
                            case 17:
                            case 18:
                            case 19:
                                break;
                            case 20:
                                this.body = new msg_remote_selection_t();
                                break;
                            default:
                                switch (b) {
                                    case 27:
                                        this.body = new mdb_t.msg_ftl_req_to_receive_t();
                                        break;
                                    case 28:
                                        break;
                                    case 29:
                                        this.body = new mdb_t.msg_ftl_send_block_t();
                                        break;
                                    case 30:
                                        this.body = new mdb_t.msg_ftl_ok_to_send_t();
                                        break;
                                    case 31:
                                        this.body = new mdb_t.msg_ftl_req_to_send_t();
                                        break;
                                    default:
                                        String str = mdb_rsp_t.TAG;
                                        Log.d(str, "not implemented : " + String.valueOf((int) this.response));
                                        break;
                                }
                        }
                }
            } else {
                this.body = new mdb_t.msg_reader_state_t();
            }
            int i4 = i2 - (i3 - i);
            marshall_t.msg_i msg_iVar = this.body;
            if (msg_iVar != null) {
                i3 += msg_iVar.decode(bArr, i3, i4);
            }
            return i3 - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.response;
            marshall_t.msg_i msg_iVar = this.body;
            if (msg_iVar != null) {
                i3 += msg_iVar.encode(bArr, i3, i2 - i3);
            }
            return i3 - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_setup_config_data_rsp_t extends marshall_t.msg_i {
        public byte app_max_response_time;
        public byte country_code_high;
        public byte country_code_low;
        public byte decimal_places;
        public byte misc_options;
        public byte reader_feature_level;
        public byte scale_factor;

        public msg_setup_config_data_rsp_t() {
        }

        public msg_setup_config_data_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            this.reader_feature_level = bArr[i];
            int i4 = i3 + 1;
            this.country_code_high = bArr[i3];
            int i5 = i4 + 1;
            this.country_code_low = bArr[i4];
            int i6 = i5 + 1;
            this.scale_factor = bArr[i5];
            int i7 = i6 + 1;
            this.decimal_places = bArr[i6];
            int i8 = i7 + 1;
            this.app_max_response_time = bArr[i7];
            this.misc_options = bArr[i8];
            return (i8 + 1) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.reader_feature_level;
            int i4 = i3 + 1;
            bArr[i3] = this.country_code_high;
            int i5 = i4 + 1;
            bArr[i4] = this.country_code_low;
            int i6 = i5 + 1;
            bArr[i5] = this.scale_factor;
            int i7 = i6 + 1;
            bArr[i6] = this.decimal_places;
            int i8 = i7 + 1;
            bArr[i7] = this.app_max_response_time;
            bArr[i8] = this.misc_options;
            return (i8 + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_display_request_rsp_t extends marshall_t.msg_i {
        public byte[] display_data;
        public byte display_time;

        public msg_display_request_rsp_t() {
        }

        public msg_display_request_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            byte[] bArr2 = new byte[i2];
            this.display_data = bArr2;
            int i3 = i + 1;
            this.display_time = bArr[i];
            return (i3 + ByteArrayUtils.byteArrToSubByteArray(bArr, i3, bArr2)) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.display_time;
            return (i3 + ByteArrayUtils.subByteArrToByteArray(bArr, i3, this.display_data)) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_begin_session_rsp_t extends marshall_t.msg_i {
        public short funds_available;

        public msg_begin_session_rsp_t() {
        }

        public msg_begin_session_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            this.funds_available = ByteArrayUtils.byteArrToShort(bArr, i);
            return (i + 2) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return (ByteArrayUtils.shortToByteArray(bArr, i, this.funds_available) + i) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_session_cancel_req_rsp_t extends marshall_t.msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_session_cancel_req_rsp_t() {
        }

        public msg_session_cancel_req_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_vend_approved_rsp_t extends marshall_t.msg_i {
        public short vend_amount;

        public msg_vend_approved_rsp_t() {
        }

        public msg_vend_approved_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            this.vend_amount = ByteArrayUtils.byteArrToShort(bArr, i);
            return (i + 2) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return (ByteArrayUtils.byteArrToShort(bArr, i) + i) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_vend_denied_rsp_t extends marshall_t.msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_vend_denied_rsp_t() {
        }

        public msg_vend_denied_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_end_session_rsp_t extends marshall_t.msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_end_session_rsp_t() {
        }

        public msg_end_session_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_cancelled_rsp_t extends marshall_t.msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_cancelled_rsp_t() {
        }

        public msg_cancelled_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_peripheral_id_rsp_t extends marshall_t.msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_peripheral_id_rsp_t() {
        }

        public msg_peripheral_id_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_malfunction_error_rsp_t extends marshall_t.msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_malfunction_error_rsp_t() {
        }

        public msg_malfunction_error_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_cmd_out_of_seq_rsp_t extends marshall_t.msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_cmd_out_of_seq_rsp_t() {
        }

        public msg_cmd_out_of_seq_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_revalue_approved_rsp_t extends marshall_t.msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_revalue_approved_rsp_t() {
        }

        public msg_revalue_approved_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_revalue_denied_rsp_t extends marshall_t.msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_revalue_denied_rsp_t() {
        }

        public msg_revalue_denied_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_limit_amount_rsp_t extends marshall_t.msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_limit_amount_rsp_t() {
        }

        public msg_limit_amount_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_remote_selection_t extends marshall_t.msg_i {
        short funds_available;
        short item_number;
        int item_options;
        short payment_data;
        short payment_media_id;
        byte payment_type;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_remote_selection_t() {
        }

        public msg_remote_selection_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            this.funds_available = ByteArrayUtils.byteArrToShort(bArr, i);
            int i3 = i + 2;
            this.payment_media_id = ByteArrayUtils.byteArrToShort(bArr, i3);
            int i4 = i3 + 2;
            int i5 = i4 + 1;
            this.payment_type = bArr[i4];
            this.payment_data = ByteArrayUtils.byteArrToShort(bArr, i5);
            int i6 = i5 + 2;
            this.item_number = ByteArrayUtils.byteArrToShort(bArr, i6);
            int i7 = i6 + 2;
            this.item_options = ByteArrayUtils.byteArrToInteger(bArr, i7);
            return (i7 + 4) - i;
        }
    }
}

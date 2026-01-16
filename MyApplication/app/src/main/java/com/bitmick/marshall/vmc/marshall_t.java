package com.bitmick.marshall.vmc;

import com.bitmick.marshall.vmc.mdb_req_t;
import com.bitmick.marshall.vmc.mdb_rsp_t;
import com.bitmick.utils.ByteArrayUtils;
import com.bitmick.utils.Log;
import com.bitmick.utils.StringUtils;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class marshall_t {
    public static int MARSHALL_MSG_MAX_SIZE = 512;
    public static final short POLYNOMIAL_CCITT = 4129;
    public static final short SEED_CCITT = 0;
    private static String TAG = "marshall_t";
    public static final byte entry_type_ble_e = 11;
    public static final byte entry_type_cnous_card_e = 7;
    public static final byte entry_type_contact_card_e = 3;
    public static final byte entry_type_contactless_card_e = 2;
    public static final byte entry_type_hid_card_e = 5;
    public static final byte entry_type_keypad_e = 8;
    public static final byte entry_type_mifare_card_e = 4;
    public static final byte entry_type_mobile_e = 9;
    public static final byte entry_type_msr_swipe_card_e = 1;
    public static final byte entry_type_nfc_card_e = 6;
    public static final byte entry_type_qr_e = 10;
    public static final byte getMarshall_ev_ext_status_notification_suspended_evse = 0;
    public static final byte marshall_addr_bcast = -1;
    public static final byte marshall_display_control_button_id_tag = 3;
    public static final byte marshall_display_control_label_tag = 2;
    public static final byte marshall_display_control_layout_id_tag = 1;
    public static final int marshall_ev_ext_charger_fw_max_len = 12;
    public static final byte marshall_ev_ext_charger_state_charging_1_phase = 1;
    public static final byte marshall_ev_ext_charger_state_charging_3_phase = 2;
    public static final byte marshall_ev_ext_charger_state_not_charging = 0;
    public static final byte marshall_ev_ext_charging_state_charging_1phase_e = 1;
    public static final byte marshall_ev_ext_charging_state_charging_3phase_e = 2;
    public static final byte marshall_ev_ext_charging_state_not_charging_e = 0;
    public static final byte marshall_ev_ext_command_keep_current_state_e = 0;
    public static final byte marshall_ev_ext_command_start_1phase_e = 2;
    public static final byte marshall_ev_ext_command_start_3phase_e = 3;
    public static final byte marshall_ev_ext_command_stop_charging_e = 1;
    public static final byte marshall_ev_ext_error_error_on_pe_n_wires_e = 8;
    public static final byte marshall_ev_ext_error_error_on_rcd_e = 4;
    public static final byte marshall_ev_ext_error_ok_e = 0;
    public static final byte marshall_ev_ext_error_over_current_e = 32;
    public static final byte marshall_ev_ext_error_over_voltage_e = 16;
    public static final byte marshall_ev_ext_error_relay_cant_close_e = 2;
    public static final byte marshall_ev_ext_error_relay_stuck_closed_e = 1;
    public static final byte marshall_ev_ext_error_temp_too_high_e = 64;
    public static final byte marshall_ev_ext_error_unsupported_charging_mode_e = Byte.MIN_VALUE;
    public static final byte marshall_ev_ext_ev_state_connected = 1;
    public static final byte marshall_ev_ext_ev_state_error_state = 4;
    public static final byte marshall_ev_ext_ev_state_needs_to_ventilate = 3;
    public static final byte marshall_ev_ext_ev_state_not_connected = 0;
    public static final byte marshall_ev_ext_ev_state_wants_to_charge = 2;
    public static final byte marshall_ev_ext_grid_measurement_1000_imp_kwh = 8;
    public static final byte marshall_ev_ext_grid_measurement_100_imp_kwh = 3;
    public static final byte marshall_ev_ext_grid_measurement_10_imp_kwh = 2;
    public static final byte marshall_ev_ext_grid_measurement_1600_imp_kwh = 9;
    public static final byte marshall_ev_ext_grid_measurement_2000_imp_kwh = 10;
    public static final byte marshall_ev_ext_grid_measurement_200_imp_kwh = 4;
    public static final byte marshall_ev_ext_grid_measurement_400_imp_kwh = 5;
    public static final byte marshall_ev_ext_grid_measurement_500_imp_kwh = 6;
    public static final byte marshall_ev_ext_grid_measurement_800_imp_kwh = 7;
    public static final byte marshall_ev_ext_grid_measurement_internal = 1;
    public static final byte marshall_ev_ext_grid_measurement_no_change = 0;
    public static final byte marshall_ev_ext_grid_type_it = 2;
    public static final byte marshall_ev_ext_grid_type_no_change = 0;
    public static final byte marshall_ev_ext_grid_type_tn_s = 1;
    public static final byte marshall_ev_ext_grid_type_us_it = 3;
    public static final byte marshall_ev_ext_reason_deauthorized = 4;
    public static final byte marshall_ev_ext_reason_emergency_stop = 3;
    public static final byte marshall_ev_ext_reason_ev_disconnected = 5;
    public static final byte marshall_ev_ext_reason_hard_reset = 9;
    public static final byte marshall_ev_ext_reason_local = 1;
    public static final byte marshall_ev_ext_reason_none = 0;
    public static final byte marshall_ev_ext_reason_other = 11;
    public static final byte marshall_ev_ext_reason_powerloss = 6;
    public static final byte marshall_ev_ext_reason_reboot = 7;
    public static final byte marshall_ev_ext_reason_remote = 2;
    public static final byte marshall_ev_ext_reason_soft_reset = 8;
    public static final byte marshall_ev_ext_reason_unlock_command = 10;
    public static final byte marshall_ev_ext_relay_close_e = 2;
    public static final byte marshall_ev_ext_relay_keep_current_state_e = 0;
    public static final byte marshall_ev_ext_relay_open_e = 1;
    public static final byte marshall_ev_ext_status_notification_available = 0;
    public static final byte marshall_ev_ext_status_notification_charging = 0;
    public static final byte marshall_ev_ext_status_notification_faulted = 0;
    public static final byte marshall_ev_ext_status_notification_finishing = 0;
    public static final byte marshall_ev_ext_status_notification_preparing = 0;
    public static final byte marshall_ev_ext_status_notification_reserved = 0;
    public static final byte marshall_ev_ext_status_notification_suspended = 0;
    public static final byte marshall_ev_ext_status_notification_unavailable = 0;
    public static final byte marshall_ev_ext_vehicle_state_connected_e = 1;
    public static final byte marshall_ev_ext_vehicle_state_error_e = 4;
    public static final byte marshall_ev_ext_vehicle_state_needs_to_ventilate_e = 3;
    public static final byte marshall_ev_ext_vehicle_state_not_connected_e = 0;
    public static final byte marshall_ev_ext_vehicle_state_wants_to_charge_e = 2;
    public static final byte marshall_ev_ext_warning_high_temp_slow_charge_e = 8;
    public static final byte marshall_ev_ext_warning_incoming_packer_error_e = 16;
    public static final byte marshall_ev_ext_warning_low_voltage_missing_phase_e = 2;
    public static final byte marshall_ev_ext_warning_ok_e = 0;
    public static final byte marshall_ev_ext_warning_problem_w_grid_e = 4;
    public static final byte marshall_ev_ext_warning_relay_phase_2_3_cant_close_e = 1;
    public static final byte marshall_func_code_alert = 49;
    public static final byte marshall_func_code_close_socket = 34;
    public static final byte marshall_func_code_commodore_ext_commodore_tlv = 80;
    public static final byte marshall_func_code_config = 6;
    public static final byte marshall_func_code_display_control = 20;
    public static final byte marshall_func_code_display_event = 21;
    public static final byte marshall_func_code_display_msg = 8;
    public static final byte marshall_func_code_display_status = 9;
    public static final byte marshall_func_code_ereceipt_e = 17;
    public static final byte marshall_func_code_ev_ext_ev_boot_notification = -96;
    public static final byte marshall_func_code_ev_ext_ev_charging = -30;
    public static final byte marshall_func_code_ev_ext_ev_diag_response = -91;
    public static final byte marshall_func_code_ev_ext_ev_guid_request = 18;
    public static final byte marshall_func_code_ev_ext_ev_guid_response = 19;
    public static final byte marshall_func_code_ev_ext_ev_info_req = -32;
    public static final byte marshall_func_code_ev_ext_ev_info_rsp = -31;
    public static final byte marshall_func_code_ev_ext_ev_meter_value = -92;
    public static final byte marshall_func_code_ev_ext_ev_start_notification = -94;
    public static final byte marshall_func_code_ev_ext_ev_status = -29;
    public static final byte marshall_func_code_ev_ext_ev_status_notification = -95;
    public static final byte marshall_func_code_ev_ext_ev_stop_notification = -93;
    public static final byte marshall_func_code_ev_ext_grid_setup_req = -28;
    public static final byte marshall_func_code_ev_ext_grid_setup_rsp = -27;
    public static final byte marshall_func_code_fw_info = 5;
    public static final byte marshall_func_code_get_time_req = 12;
    public static final byte marshall_func_code_get_time_rsp = 13;
    public static final byte marshall_func_code_keepalive = 7;
    public static final byte marshall_func_code_mdb_command = Byte.MIN_VALUE;
    public static final byte marshall_func_code_modem_rx_control = 37;
    public static final byte marshall_func_code_modem_status = 32;
    public static final byte marshall_func_code_open_socket = 33;
    public static final byte marshall_func_code_reader_command = -111;
    public static final byte marshall_func_code_reset = 1;
    public static final byte marshall_func_code_response = 0;
    public static final byte marshall_func_code_set_periph_param = 14;
    public static final byte marshall_func_code_socket_rcv_data = 36;
    public static final byte marshall_func_code_socket_send_data = 35;
    public static final byte marshall_func_code_status = 11;
    public static final byte marshall_func_code_trace = 48;
    public static final byte marshall_func_code_trasfer_data = 10;
    public static final byte marshall_packet_option_ack_mask = 1;
    public static final byte marshall_packet_option_encrypt_mask = 2;
    public static final byte marshall_packet_option_none = 0;
    public static final byte marshall_packet_option_retry_num_mask = 12;
    public static final byte marshall_packet_option_rfu_mask = -16;
    public static final byte marshall_transfer_data_card_bin = 5;
    public static final byte marshall_transfer_data_card_bin_hash = 6;
    public static final byte marshall_transfer_data_card_entry_mode = 4;
    public static final byte marshall_transfer_data_card_type = 3;
    public static final byte marshall_transfer_data_cc_last_4_digits = 13;
    public static final byte marshall_transfer_data_choose_prod_timeout = 2;
    public static final byte marshall_transfer_data_com_status = 9;
    public static final byte marshall_transfer_data_ftl_data = 10;
    public static final byte marshall_transfer_data_product_code = 14;
    public static final byte marshall_transfer_data_product_price = 15;
    public static final byte marshall_transfer_data_prop_card_uid = 7;
    public static final byte marshall_transfer_data_transaction_id = 1;
    public static final byte marshall_transfer_data_vmc_auth_status = 8;
    public static final short marshall_version = 512;
    public static final byte marshalll_display_control_button_id_back = 104;
    public static final byte marshalll_display_control_button_id_cancel = 103;
    public static final byte marshalll_display_control_button_id_enter_ok = 105;
    public static final byte marshalll_display_control_button_id_left_arrow = 100;
    public static final byte marshalll_display_control_button_id_menu = 101;
    public static final byte marshalll_display_control_button_id_right_arrow = 102;
    public static final byte marshalll_display_control_button_id_touch = 106;
    public static final byte status_ftp_status = 6;
    public static final byte status_machine_status = 5;
    public static final byte status_settlement = 4;
    public static final byte status_unexpected_error = 0;
    public static final byte status_unexpected_out_of_seq = 2;
    public static final byte status_unexpected_pending_3rd_party_rcvd = 3;
    public static final byte status_unexpected_timeout = 1;
    public static final byte status_vpos_call_your_bank = 54;
    public static final byte status_vpos_card_error = 56;
    public static final byte status_vpos_card_read_ok_please_remove_card = 73;
    public static final byte status_vpos_insert_card = 61;
    public static final byte status_vpos_insert_or_swipe_another_card = 84;
    public static final byte status_vpos_not_accepted = 62;
    public static final byte status_vpos_please_insert_card = 79;
    public static final byte status_vpos_please_insert_or_swipe_card = 74;
    public static final byte status_vpos_please_present_one_card_only = 75;
    public static final byte status_vpos_please_remove_card = 66;
    public static final byte status_vpos_please_use_chip = 67;
    public static final byte status_vpos_please_use_mag = 68;
    public static final byte status_vpos_present_card = 71;
    public static final byte status_vpos_present_card_again = 83;
    public static final byte status_vpos_processing_error = 65;
    public static final byte status_vpos_see_phone_for_instructions = 82;
    public static final byte status_vpos_try_again = 69;
    public static final byte status_vpos_try_another_card = 78;
    public static final byte vpos_address = 0;

    /* loaded from: classes.dex */
    public static class evse_diag_rec_t {
        byte charging_state;
        byte connector_id;
        short error;
        byte[] reserved;
        byte state;
        byte warning;
    }

    /* loaded from: classes.dex */
    public static class evse_rec_t {
        byte connector_id;
        String hw_version;
        String reserved;
        String serial;
        String sw_version;
    }

    /* loaded from: classes.dex */
    public static abstract class msg_i {
        public abstract int decode(byte[] bArr, int i, int i2);

        public abstract int encode(byte[] bArr, int i, int i2);
    }

    private static short CRC_CCITT(short s, byte b) {
        short s2 = (short) ((((short) (b & 255)) ^ (s >> 8)) << 8);
        short s3 = 0;
        for (byte b2 = 0; b2 < 8; b2 = (byte) (b2 + 1)) {
            s3 = (short) (((s3 ^ s2) & 32768) != 0 ? (s3 << 1) ^ 4129 : s3 << 1);
            s2 = (short) (s2 << 1);
        }
        return (short) ((s << 8) ^ s3);
    }

    /* loaded from: classes.dex */
    public static class msg_marshall_t extends msg_i {
        public msg_i body;
        public short crc16;
        public byte dest;
        public byte dest_lsb;
        public byte func_code;
        public byte id;
        public byte options;
        public short packet_len;
        public byte source;
        public byte source_lsb;

        public msg_marshall_t() {
        }

        public msg_marshall_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            short byteArrToShort = ByteArrayUtils.byteArrToShort(bArr, i);
            this.packet_len = byteArrToShort;
            int i3 = i + 2;
            int i4 = i3 + 1;
            this.options = bArr[i3];
            int i5 = i4 + 1;
            this.id = bArr[i4];
            int i6 = i5 + 1;
            this.source = bArr[i5];
            int i7 = i6 + 1;
            this.source_lsb = bArr[i6];
            int i8 = i7 + 1;
            this.dest = bArr[i7];
            int i9 = i8 + 1;
            this.dest_lsb = bArr[i8];
            int i10 = i9 + 1;
            this.func_code = bArr[i9];
            if (byteArrToShort < 0) {
                return -1;
            }
            this.crc16 = ByteArrayUtils.byteArrToShort(bArr, byteArrToShort + i);
            int Calc_CRC_CCITT = marshall_t.Calc_CRC_CCITT(bArr, i, this.packet_len, (short) 0) & 0xFFFF;
            int i11 = 65535 & this.crc16;
            if (i11 != Calc_CRC_CCITT) {
                Log.d(marshall_t.TAG, String.format("wrong crc on packet(%02x), index: %d, length: %d, crc_rx: %04x, crc_calc: %04x", Integer.valueOf(this.id & 255), Integer.valueOf(i), Short.valueOf(this.packet_len), Integer.valueOf(i11), Integer.valueOf(Calc_CRC_CCITT)));
                return -1;
            }
            this.body = null;
            byte b = this.func_code;
            if (b == Byte.MIN_VALUE) {
                this.body = new msg_mdb_t();
            } else if (b == -111) {
                this.body = new msg_reader_t();
            } else if (b == 17) {
                this.body = new msg_ereceipt_t();
            } else if (b == 80) {
                this.body = new msg_ext_commodore_tlv_t();
            } else if (b == 0) {
                this.body = new msg_response_t();
            } else if (b == 1) {
                this.body = new msg_reset_t();
            } else if (b == 48) {
                this.body = new msg_trace_t();
            } else if (b != 49) {
                switch (b) {
                    case 5:
                        this.body = new msg_fw_info_t();
                        break;
                    case 6:
                        this.body = new msg_config_t();
                        break;
                    case 7:
                        this.body = new msg_keepalive_t();
                        break;
                    case 8:
                        Log.d(marshall_t.TAG, "unsupported: marshall_func_code_display_msg");
                        break;
                    case 9:
                        this.body = new msg_display_status_t();
                        break;
                    case 10:
                        this.body = new msg_transfer_data_t();
                        break;
                    case 11:
                        this.body = new msg_status_t();
                        break;
                    case 12:
                        Log.d(marshall_t.TAG, "unsupported: marshall_func_code_get_time_req");
                        break;
                    case 13:
                        this.body = new msg_time_rsp_t();
                        break;
                    case 14:
                        this.body = new msg_set_periph_param_t();
                        break;
                    default:
                        switch (b) {
                            case 19:
                                this.body = new msg_ev_ext_guid_response();
                                break;
                            case 20:
                                this.body = new msg_display_control_t();
                                break;
                            case 21:
                                this.body = new msg_display_event_t();
                                break;
                            default:
                                switch (b) {
                                    case 32:
                                        this.body = new msg_modem_status_t();
                                        break;
                                    case 33:
                                        this.body = new msg_open_socket_t();
                                        break;
                                    case 34:
                                        this.body = new msg_close_socket_t();
                                        break;
                                    case 35:
                                        this.body = new msg_socket_transfer_data_t();
                                        break;
                                    case 36:
                                        this.body = new msg_socket_transfer_data_t();
                                        break;
                                    case 37:
                                        break;
                                    default:
                                        Log.d(marshall_t.TAG, String.format("unsupported msg func_code: %d", Byte.valueOf(this.func_code)));
                                        break;
                                }
                        }
                }
            } else {
                this.body = new msg_alert_t();
            }
            int i12 = this.packet_len - (i10 - i);
            msg_i msg_iVar = this.body;
            if (msg_iVar != null) {
                i10 += msg_iVar.decode(bArr, i10, i12);
            }
            return i10 - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int shortToByteArray = ByteArrayUtils.shortToByteArray(bArr, i, this.packet_len) + i;
            int i3 = shortToByteArray + 1;
            bArr[shortToByteArray] = this.options;
            int i4 = i3 + 1;
            bArr[i3] = this.id;
            int i5 = i4 + 1;
            bArr[i4] = this.source;
            int i6 = i5 + 1;
            bArr[i5] = this.source_lsb;
            int i7 = i6 + 1;
            bArr[i6] = this.dest;
            int i8 = i7 + 1;
            bArr[i7] = this.dest_lsb;
            int i9 = i8 + 1;
            bArr[i8] = this.func_code;
            msg_i msg_iVar = this.body;
            if (msg_iVar != null) {
                i9 += msg_iVar.encode(bArr, i9, i2);
            }
            short s = (short) i9;
            this.packet_len = s;
            ByteArrayUtils.shortToByteArray(bArr, i, s);
            short Calc_CRC_CCITT = marshall_t.Calc_CRC_CCITT(bArr, i, i9, (short) 0);
            this.crc16 = Calc_CRC_CCITT;
            return (i9 + ByteArrayUtils.shortToByteArray(bArr, i9, Calc_CRC_CCITT)) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_response_t extends msg_i {
        public static final byte response_ack = 0;
        public static final byte response_crc_err = 1;
        public static final byte response_non_cons_id = 3;
        public static final byte response_unexp_error = 10;
        public static final byte response_wrong_rcp = 2;
        public byte value;

        public msg_response_t(byte b) {
            this.value = b;
        }

        public msg_response_t() {
        }

        public msg_response_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            this.value = bArr[i];
            return (i + 1) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            bArr[i] = this.value;
            return (i + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_reset_t extends msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_reset_t() {
        }

        public msg_reset_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            Log.d(marshall_t.TAG, "received reset");
            return i - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_fw_info_t extends msg_i {
        public static final short fw_info_periph_cap_bitmap_contact = 2;
        public static final short fw_info_periph_cap_bitmap_contactless = 4;
        public static final short fw_info_periph_cap_bitmap_display = 16;
        public static final short fw_info_periph_cap_bitmap_ftl_data = 1024;
        public static final short fw_info_periph_cap_bitmap_keypad = 8;
        public static final short fw_info_periph_cap_bitmap_mag_card_approved_by_vmc = 512;
        public static final short fw_info_periph_cap_bitmap_magstripe = 1;
        public static final short fw_info_periph_cap_bitmap_mifare_card_approved_by_vmc = 128;
        public static final short fw_info_periph_cap_bitmap_multi_session = 64;
        public static final short fw_info_periph_cap_bitmap_req_price_is_not_final = 256;
        public static final short fw_info_periph_cap_bitmap_touch_screen = 32;
        public String app_sw_ver;
        public String marshall_sdk_version;
        public String model;
        public short periph_cap_bitmap;
        public byte periph_sub_type;
        public byte periph_type;
        public short prot_ver;
        public String serial;
        public String vmc_hw_ver;
        public String vmc_manuf_code;

        public msg_fw_info_t() {
        }

        public msg_fw_info_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            Log.d(marshall_t.TAG, "received fw_info");
            this.prot_ver = ByteArrayUtils.byteArrToShort(bArr, i);
            int i3 = i + 2;
            this.periph_type = bArr[i3];
            int i4 = i3 + 1;
            this.periph_sub_type = bArr[i4];
            int i5 = i4 + 1;
            this.periph_cap_bitmap = ByteArrayUtils.byteArrToShort(bArr, i5);
            int i6 = i5 + 2;
            String byteArrToString = ByteArrayUtils.byteArrToString(bArr, i6, 20);
            this.model = byteArrToString;
            int length = i6 + byteArrToString.length();
            String byteArrToString2 = ByteArrayUtils.byteArrToString(bArr, length, 20);
            this.serial = byteArrToString2;
            int length2 = length + byteArrToString2.length();
            String byteArrToString3 = ByteArrayUtils.byteArrToString(bArr, length2, 20);
            this.app_sw_ver = byteArrToString3;
            int length3 = length2 + byteArrToString3.length();
            String byteArrToString4 = ByteArrayUtils.byteArrToString(bArr, length3, 20);
            this.vmc_hw_ver = byteArrToString4;
            int length4 = length3 + byteArrToString4.length();
            String byteArrToString5 = ByteArrayUtils.byteArrToString(bArr, length4, 20);
            this.vmc_manuf_code = byteArrToString5;
            int length5 = length4 + byteArrToString5.length();
            String byteArrToString6 = ByteArrayUtils.byteArrToString(bArr, length5, 20);
            this.marshall_sdk_version = byteArrToString6;
            return (length5 + byteArrToString6.length()) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int shortToByteArray = ByteArrayUtils.shortToByteArray(bArr, i, this.prot_ver) + i;
            int i3 = shortToByteArray + 1;
            bArr[shortToByteArray] = this.periph_type;
            int i4 = i3 + 1;
            bArr[i3] = this.periph_sub_type;
            int shortToByteArray2 = i4 + ByteArrayUtils.shortToByteArray(bArr, i4, this.periph_cap_bitmap);
            int stringToByteArray = shortToByteArray2 + ByteArrayUtils.stringToByteArray(bArr, shortToByteArray2, this.model, 19);
            int i5 = stringToByteArray + 1;
            bArr[stringToByteArray] = 0;
            int stringToByteArray2 = i5 + ByteArrayUtils.stringToByteArray(bArr, i5, this.serial, 19);
            int i6 = stringToByteArray2 + 1;
            bArr[stringToByteArray2] = 0;
            int stringToByteArray3 = i6 + ByteArrayUtils.stringToByteArray(bArr, i6, this.app_sw_ver, 19);
            int i7 = stringToByteArray3 + 1;
            bArr[stringToByteArray3] = 0;
            int stringToByteArray4 = i7 + ByteArrayUtils.stringToByteArray(bArr, i7, this.vmc_manuf_code, 19);
            int i8 = stringToByteArray4 + 1;
            bArr[stringToByteArray4] = 0;
            int stringToByteArray5 = i8 + ByteArrayUtils.stringToByteArray(bArr, i8, this.vmc_hw_ver, 19);
            bArr[stringToByteArray5] = 0;
            return (stringToByteArray5 + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_config_t extends msg_i {
        public byte[] acquirer_terminal_id;
        public byte[] country_code;
        public byte[] currency_code;
        public byte decimal_place;
        public byte dev_src_id;
        public int keepalive_interval_ms;
        public byte language;
        public byte[] machine_location;
        public short max_msg_len;
        public byte[] merchant_id;
        public byte prot_ver_major;
        public byte prot_ver_minor;
        public byte[] vpos_serial;

        public msg_config_t() {
            this.prot_ver_major = (byte) 0;
            this.prot_ver_minor = (byte) 0;
            this.dev_src_id = (byte) 0;
            this.keepalive_interval_ms = 1000;
            this.language = (byte) 0;
            this.country_code = new byte[3];
            this.currency_code = new byte[3];
            this.decimal_place = (byte) 0;
            this.vpos_serial = new byte[16];
            this.max_msg_len = (short) 512;
        }

        public msg_config_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            Log.d(marshall_t.TAG, "received config");
            int i3 = i + 1;
            this.prot_ver_major = bArr[i];
            int i4 = i3 + 1;
            this.prot_ver_minor = bArr[i3];
            int i5 = i4 + 1;
            this.dev_src_id = bArr[i4];
            this.keepalive_interval_ms = ByteArrayUtils.byteArrToInteger(bArr, i5);
            int i6 = i5 + 4;
            int i7 = i6 + 1;
            this.language = bArr[i6];
            ByteArrayUtils.byteArrToSubByteArray(bArr, i7, this.country_code);
            int length = i7 + this.country_code.length;
            ByteArrayUtils.byteArrToSubByteArray(bArr, length, this.currency_code);
            int length2 = length + this.currency_code.length;
            int i8 = length2 + 1;
            this.decimal_place = bArr[length2];
            ByteArrayUtils.byteArrToSubByteArray(bArr, i8, this.vpos_serial);
            int length3 = i8 + this.vpos_serial.length;
            this.max_msg_len = ByteArrayUtils.byteArrToShort(bArr, length3);
            int i9 = length3 + 2;
            if (this.prot_ver_major >= 1) {
                byte[] bArr2 = new byte[16];
                this.merchant_id = bArr2;
                ByteArrayUtils.byteArrToSubByteArray(bArr, i9, bArr2);
                int length4 = i9 + this.merchant_id.length;
                byte[] bArr3 = new byte[16];
                this.acquirer_terminal_id = bArr3;
                ByteArrayUtils.byteArrToSubByteArray(bArr, length4, bArr3);
                int length5 = length4 + this.acquirer_terminal_id.length;
                byte[] bArr4 = new byte[100];
                this.machine_location = bArr4;
                ByteArrayUtils.byteArrToSubByteArray(bArr, length5, bArr4);
                i9 = length5 + this.machine_location.length;
            }
            return i9 - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.prot_ver_major;
            int i4 = i3 + 1;
            bArr[i3] = this.prot_ver_minor;
            int i5 = i4 + 1;
            bArr[i4] = this.dev_src_id;
            int intToByteArray = i5 + ByteArrayUtils.intToByteArray(bArr, i5, this.keepalive_interval_ms);
            int i6 = intToByteArray + 1;
            bArr[intToByteArray] = this.language;
            int subByteArrToByteArray = i6 + ByteArrayUtils.subByteArrToByteArray(bArr, i6, this.country_code);
            int subByteArrToByteArray2 = subByteArrToByteArray + ByteArrayUtils.subByteArrToByteArray(bArr, subByteArrToByteArray, this.currency_code);
            int i7 = subByteArrToByteArray2 + 1;
            bArr[subByteArrToByteArray2] = this.decimal_place;
            int subByteArrToByteArray3 = i7 + ByteArrayUtils.subByteArrToByteArray(bArr, i7, this.vpos_serial);
            return (subByteArrToByteArray3 + ByteArrayUtils.shortToByteArray(bArr, subByteArrToByteArray3, this.max_msg_len)) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_keepalive_t extends msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_keepalive_t() {
        }

        public msg_keepalive_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_display_msg_t extends msg_i {
        public short audio_file_id;
        public byte[] message;
        public short picture_file_id;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_display_msg_t() {
        }

        public msg_display_msg_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        public msg_display_msg_t(String str, short s, short s2) {
            this.message = str.getBytes();
            this.picture_file_id = s;
            this.audio_file_id = s2;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_display_status_t extends msg_i {
        public byte status;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_display_status_t() {
        }

        public msg_display_status_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            this.status = bArr[i];
            return (i + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_transfer_data_t extends msg_i {
        public static final byte transfer_data_auth_status_approved_e = 0;
        public static final byte transfer_data_auth_status_decline_e = 1;
        public static final int transfer_data_encode_card_bin = 16;
        public static final int transfer_data_encode_card_bin_hash = 32;
        public static final int transfer_data_encode_card_entry_mode = 8;
        public static final int transfer_data_encode_card_type = 4;
        public static final int transfer_data_encode_cc_last_4_digits = 1024;
        public static final int transfer_data_encode_choose_product_timeout = 2;
        public static final int transfer_data_encode_com_status = 256;
        public static final int transfer_data_encode_excel_type = 512;
        public static final int transfer_data_encode_product_code = 2048;
        public static final int transfer_data_encode_product_price = 4096;
        public static final int transfer_data_encode_prop_card_uid = 64;
        public static final int transfer_data_encode_transaction_id = 1;
        public static final int transfer_data_encode_vmc_auth_status = 128;
        public byte[] card_bin;
        public byte[] card_bin_hash;
        public byte card_entry_mode;
        public byte card_type;
        public byte[] cc_last_4_digits;
        public short choose_product_timeout;
        public byte com_status;
        public int encode_bitmap;
        public byte[] excel_data;
        public short product_code;
        public int product_price;
        public byte[] prop_card_uid;
        public byte[] transaction_id;
        public byte vmc_auth_status;

        public msg_transfer_data_t() {
            init();
        }

        public msg_transfer_data_t(byte[] bArr, int i, int i2) {
            init();
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            int byteArrToSubByteArray;
            Log.d(marshall_t.TAG, "received transfer_data");
            int i3 = i;
            while (i3 < i + i2) {
                int i4 = i3 + 1;
                byte b = bArr[i3];
                int i5 = i4 + 1;
                int i6 = bArr[i4] & 255;
                switch (b) {
                    case 1:
                        this.encode_bitmap |= 1;
                        byte[] bArr2 = new byte[i6];
                        this.transaction_id = bArr2;
                        byteArrToSubByteArray = i5 + ByteArrayUtils.byteArrToSubByteArray(bArr, i5, bArr2);
                        Log.d(marshall_t.TAG, String.format("marshall, transaction_id: %d", Integer.valueOf(i6)));
                        break;
                    case 2:
                        this.encode_bitmap |= 2;
                        this.choose_product_timeout = ByteArrayUtils.byteArrToShort(bArr, i5);
                        byteArrToSubByteArray = i5 + i6;
                        Log.d(marshall_t.TAG, String.format("marshall, choose_product_timeout: %d", Short.valueOf(this.choose_product_timeout)));
                        break;
                    case 3:
                        this.encode_bitmap |= 4;
                        this.card_type = bArr[i5];
                        byteArrToSubByteArray = i5 + i6;
                        Log.d(marshall_t.TAG, String.format("marshall, card_type: %d", Byte.valueOf(this.card_type)));
                        break;
                    case 4:
                        this.encode_bitmap |= 8;
                        this.card_entry_mode = bArr[i5];
                        byteArrToSubByteArray = i5 + i6;
                        Log.d(marshall_t.TAG, String.format("marshall, card_entry_mode: %d", Byte.valueOf(this.card_entry_mode)));
                        break;
                    case 5:
                        this.encode_bitmap |= 16;
                        byte[] bArr3 = new byte[i6];
                        this.card_bin = bArr3;
                        byteArrToSubByteArray = i5 + ByteArrayUtils.byteArrToSubByteArray(bArr, i5, bArr3);
                        Log.d(marshall_t.TAG, String.format("marshall, card_bin: %d bytes", Integer.valueOf(i6)));
                        break;
                    case 6:
                        this.encode_bitmap |= 32;
                        byte[] bArr4 = new byte[i6];
                        this.card_bin_hash = bArr4;
                        byteArrToSubByteArray = i5 + ByteArrayUtils.byteArrToSubByteArray(bArr, i5, bArr4);
                        Log.d(marshall_t.TAG, String.format("marshall, card_bin_hash: %d bytes", Integer.valueOf(i6)));
                        break;
                    case 7:
                        this.encode_bitmap |= 64;
                        byte[] bArr5 = new byte[i6];
                        this.prop_card_uid = bArr5;
                        byteArrToSubByteArray = i5 + ByteArrayUtils.byteArrToSubByteArray(bArr, i5, bArr5);
                        Log.d(marshall_t.TAG, String.format("marshall, prop_card_uid: %d bytes", Integer.valueOf(i6)));
                        break;
                    case 8:
                        this.encode_bitmap |= 128;
                        this.vmc_auth_status = bArr[i5];
                        byteArrToSubByteArray = i5 + i6;
                        Log.d(marshall_t.TAG, String.format("marshall, vmc auth: %d", Byte.valueOf(this.vmc_auth_status)));
                        break;
                    case 9:
                        this.encode_bitmap |= 256;
                        this.com_status = bArr[i5];
                        byteArrToSubByteArray = i5 + i6;
                        Log.d(marshall_t.TAG, String.format("marshall, com status: %d", Byte.valueOf(this.com_status)));
                        break;
                    case 10:
                        this.encode_bitmap |= 512;
                        byte[] bArr6 = new byte[i6];
                        this.excel_data = bArr6;
                        byteArrToSubByteArray = i5 + ByteArrayUtils.byteArrToSubByteArray(bArr, i5, bArr6);
                        Log.d(marshall_t.TAG, String.format("marshall, excel data: %d bytes", Integer.valueOf(i6)));
                        break;
                    case 11:
                    case 12:
                    default:
                        byteArrToSubByteArray = i5 + i6;
                        Log.d(marshall_t.TAG, String.format("skipping unknown tag: %d", Byte.valueOf(b)));
                        break;
                    case 13:
                        this.encode_bitmap |= 1024;
                        byte[] bArr7 = new byte[i6];
                        this.cc_last_4_digits = bArr7;
                        byteArrToSubByteArray = i5 + ByteArrayUtils.byteArrToSubByteArray(bArr, i5, bArr7);
                        Log.d(marshall_t.TAG, String.format("marshall, last cc digits: %d bytes", Integer.valueOf(i6)));
                        break;
                    case 14:
                        this.encode_bitmap |= 2048;
                        this.product_code = ByteArrayUtils.byteArrToShort(bArr, i5);
                        byteArrToSubByteArray = i5 + i6;
                        Log.d(marshall_t.TAG, String.format("marshall, product code: %d", Short.valueOf(this.product_code)));
                        break;
                    case 15:
                        this.encode_bitmap |= 4096;
                        this.product_price = ByteArrayUtils.byteArrToInteger(bArr, i5);
                        byteArrToSubByteArray = i5 + i6;
                        Log.d(marshall_t.TAG, String.format("marshall, product price: %d", Integer.valueOf(this.product_price)));
                        break;
                }
                i3 = byteArrToSubByteArray;
            }
            return i3 - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3;
            if ((this.encode_bitmap & 1) != 0) {
                int i4 = i + 1;
                bArr[i] = 1;
                int i5 = i4 + 1;
                byte[] bArr2 = this.transaction_id;
                bArr[i4] = (byte) bArr2.length;
                i3 = i5 + ByteArrayUtils.subByteArrToByteArray(bArr, i5, bArr2);
            } else {
                i3 = i;
            }
            if ((this.encode_bitmap & 2) != 0) {
                int i6 = i3 + 1;
                bArr[i3] = 2;
                int i7 = i6 + 1;
                bArr[i6] = 2;
                i3 = i7 + ByteArrayUtils.shortToByteArray(bArr, i7, this.choose_product_timeout);
            }
            int i8 = this.encode_bitmap;
            if ((i8 & 4) != 0) {
                int i9 = i3 + 1;
                bArr[i3] = 3;
                int i10 = i9 + 1;
                bArr[i9] = 1;
                bArr[i10] = this.card_type;
                i3 = i10 + 1;
            }
            if ((i8 & 8) != 0) {
                int i11 = i3 + 1;
                bArr[i3] = 4;
                int i12 = i11 + 1;
                bArr[i11] = 1;
                bArr[i12] = this.card_entry_mode;
                i3 = i12 + 1;
            }
            if ((i8 & 16) != 0) {
                int i13 = i3 + 1;
                bArr[i3] = 5;
                int i14 = i13 + 1;
                byte[] bArr3 = this.card_bin;
                bArr[i13] = (byte) bArr3.length;
                i3 = i14 + ByteArrayUtils.subByteArrToByteArray(bArr, i14, bArr3);
            }
            if ((this.encode_bitmap & 32) != 0) {
                int i15 = i3 + 1;
                bArr[i3] = 6;
                int i16 = i15 + 1;
                byte[] bArr4 = this.card_bin_hash;
                bArr[i15] = (byte) bArr4.length;
                i3 = i16 + ByteArrayUtils.subByteArrToByteArray(bArr, i16, bArr4);
            }
            if ((this.encode_bitmap & 64) != 0) {
                int i17 = i3 + 1;
                bArr[i3] = 7;
                int i18 = i17 + 1;
                byte[] bArr5 = this.prop_card_uid;
                bArr[i17] = (byte) bArr5.length;
                i3 = i18 + ByteArrayUtils.subByteArrToByteArray(bArr, i18, bArr5);
            }
            int i19 = this.encode_bitmap;
            if ((i19 & 128) != 0) {
                int i20 = i3 + 1;
                bArr[i3] = 8;
                int i21 = i20 + 1;
                bArr[i20] = 1;
                bArr[i21] = this.vmc_auth_status;
                i3 = i21 + 1;
            }
            if ((i19 & 256) != 0) {
                int i22 = i3 + 1;
                bArr[i3] = 9;
                int i23 = i22 + 1;
                bArr[i22] = 1;
                bArr[i23] = this.com_status;
                i3 = i23 + 1;
            }
            if ((i19 & 512) != 0) {
                int i24 = i3 + 1;
                bArr[i3] = 10;
                int i25 = i24 + 1;
                byte[] bArr6 = this.excel_data;
                bArr[i24] = (byte) bArr6.length;
                i3 = i25 + ByteArrayUtils.subByteArrToByteArray(bArr, i25, bArr6);
            }
            return i3 - i;
        }

        private void init() {
            this.encode_bitmap = 0;
            this.transaction_id = null;
            this.card_bin = null;
            this.card_bin_hash = null;
            this.prop_card_uid = null;
            this.excel_data = null;
            this.cc_last_4_digits = null;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_status_t extends msg_i {
        public byte[] data;
        public short data_len;
        public byte status;

        public msg_status_t() {
        }

        public msg_status_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            Log.d(marshall_t.TAG, "received status");
            int i3 = i + 1;
            this.status = bArr[i];
            short byteArrToShort = ByteArrayUtils.byteArrToShort(bArr, i3);
            this.data_len = byteArrToShort;
            int i4 = i3 + 2;
            if (byteArrToShort > 0) {
                byte[] bArr2 = new byte[byteArrToShort];
                this.data = bArr2;
                ByteArrayUtils.byteArrToSubByteArray(bArr, i4, bArr2);
                i4 += this.data_len;
            }
            return i4 - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.status;
            int shortToByteArray = i3 + ByteArrayUtils.shortToByteArray(bArr, i3, this.data_len);
            return (shortToByteArray + ByteArrayUtils.subByteArrToByteArray(bArr, shortToByteArray, this.data)) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_time_req_t extends msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_time_req_t() {
        }

        public msg_time_req_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_time_rsp_t extends msg_i {
        public byte day;
        public byte hours;
        public byte minutes;
        public byte month;
        public byte seconds;
        public byte year;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_time_rsp_t() {
        }

        public msg_time_rsp_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            this.year = bArr[i];
            int i4 = i3 + 1;
            this.month = bArr[i3];
            int i5 = i4 + 1;
            this.day = bArr[i4];
            int i6 = i5 + 1;
            this.hours = bArr[i5];
            int i7 = i6 + 1;
            this.minutes = bArr[i6];
            this.seconds = bArr[i7];
            return (i7 + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_set_periph_param_t extends msg_i {
        public byte[] data;
        public short dcs_code;

        public msg_set_periph_param_t() {
        }

        public msg_set_periph_param_t(short s, byte[] bArr) {
            this.dcs_code = s;
            this.data = bArr;
        }

        public msg_set_periph_param_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            this.dcs_code = ByteArrayUtils.byteArrToShort(bArr, i);
            int i3 = i + 2;
            int i4 = i2 - 2;
            byte[] bArr2 = new byte[i4];
            this.data = bArr2;
            ByteArrayUtils.byteArrToSubByteArray(bArr, i3, bArr2);
            return (i3 + i4) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int shortToByteArray = ByteArrayUtils.shortToByteArray(bArr, i, this.dcs_code) + i;
            return (shortToByteArray + ByteArrayUtils.subByteArrToByteArray(bArr, shortToByteArray, this.data)) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ereceipt_t extends msg_i {
        public byte[] data;
        public byte qr_type;

        public msg_ereceipt_t() {
        }

        public msg_ereceipt_t(int i, byte[] bArr) {
            this.qr_type = (byte) i;
            this.data = bArr;
        }

        public msg_ereceipt_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            this.qr_type = bArr[i];
            int i4 = i2 - 1;
            byte[] bArr2 = new byte[i4];
            this.data = bArr2;
            ByteArrayUtils.byteArrToSubByteArray(bArr, i3, bArr2);
            return (i3 + i4) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.qr_type;
            return (i3 + ByteArrayUtils.subByteArrToByteArray(bArr, i3, this.data)) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_display_control_t extends msg_i {
        public ArrayList<layout_t> layouts;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        /* loaded from: classes.dex */
        public static class label_t {
            String label;
            byte label_num_in_layout;
            byte object_id;

            public label_t(int i, int i2, String str) {
                this.object_id = (byte) i;
                this.label_num_in_layout = (byte) i2;
                this.label = str;
            }
        }

        /* loaded from: classes.dex */
        public static class layout_t {
            short id;
            public ArrayList<label_t> labels = new ArrayList<>();
            public ArrayList<Byte> buttons = new ArrayList<>();

            public layout_t(int i) {
                this.id = (short) i;
            }

            public void add(label_t label_tVar) {
                this.labels.add(label_tVar);
            }

            public void add(byte b) {
                this.buttons.add(Byte.valueOf(b));
            }
        }

        public msg_display_control_t() {
            this.layouts = new ArrayList<>();
        }

        public msg_display_control_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        public void add(layout_t layout_tVar) {
            this.layouts.add(layout_tVar);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i;
            int i4 = 0;
            do {
                if (i4 < this.layouts.size()) {
                    int i5 = i4 + 1;
                    layout_t layout_tVar = this.layouts.get(i4);
                    int i6 = i3 + 1;
                    bArr[i3] = 1;
                    int i7 = i6 + 1;
                    bArr[i6] = 2;
                    i3 = i7 + ByteArrayUtils.shortToByteArray(bArr, i7, layout_tVar.id);
                    for (int i8 = 0; i8 < layout_tVar.labels.size(); i8++) {
                        label_t label_tVar = layout_tVar.labels.get(i8);
                        int i9 = i3 + 1;
                        bArr[i3] = 2;
                        int i10 = i9 + 1;
                        bArr[i9] = (byte) (label_tVar.label.length() + 2);
                        int i11 = i10 + 1;
                        bArr[i10] = label_tVar.object_id;
                        int i12 = i11 + 1;
                        bArr[i11] = label_tVar.label_num_in_layout;
                        i3 = i12 + ByteArrayUtils.stringToByteArray(bArr, i12, label_tVar.label, label_tVar.label.length());
                    }
                    if (layout_tVar.buttons.size() > 0) {
                        int i13 = i3 + 1;
                        bArr[i3] = 3;
                        i3 = i13 + 1;
                        bArr[i13] = (byte) layout_tVar.buttons.size();
                        int i14 = 0;
                        while (i14 < layout_tVar.buttons.size()) {
                            bArr[i3] = layout_tVar.buttons.get(i14).byteValue();
                            i14++;
                            i3++;
                        }
                    }
                    i4 = i5;
                }
            } while (i4 < this.layouts.size());
            return i3 - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_display_event_t extends msg_i {
        public ArrayList<Byte> buttons;
        private final byte display_status_tag_type_button = 1;
        private final byte display_status_tag_type_text = 2;
        public ArrayList<String> text;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_display_event_t() {
            init();
        }

        public msg_display_event_t(byte[] bArr, int i, int i2) {
            init();
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            int i3 = i;
            while (i3 < i2) {
                int i4 = i3 + 1;
                byte b = bArr[i3];
                int i5 = i4 + 1;
                int i6 = bArr[i4];
                if (b == 1) {
                    this.buttons.add(Byte.valueOf(bArr[i5]));
                } else if (b == 2) {
                    this.text.add(new String(bArr, i5, i6));
                }
                i3 = i5 + i6;
            }
            return i3 - i;
        }

        private void init() {
            this.buttons = new ArrayList<>();
            this.text = new ArrayList<>();
        }
    }

    /* loaded from: classes.dex */
    public static class msg_modem_status_t extends msg_i {
        public static final byte modem_status_busy = 2;
        public static final byte modem_status_fail = 1;
        public static final byte modem_status_in_command_error = 4;
        public static final byte modem_status_in_param_error = 3;
        public static final byte modem_status_no_avail_resource = Byte.MIN_VALUE;
        public static final byte modem_status_ok = 0;
        public static final byte modem_status_socket_is_closed = 5;
        public static final byte modem_status_unsupported_socket_type = -127;
        public byte command;
        public byte extra;
        public byte status;

        public msg_modem_status_t() {
        }

        public msg_modem_status_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            this.command = bArr[i];
            int i4 = i3 + 1;
            this.status = bArr[i3];
            this.extra = bArr[i4];
            return (i4 + 1) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.command;
            int i4 = i3 + 1;
            bArr[i3] = this.status;
            bArr[i4] = this.extra;
            return (i4 + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_open_socket_t extends msg_i {
        public static final byte socket_dest_nayax = 0;
        public static final byte socket_dest_other = 1;
        public static final byte socket_protocol_ftp = 0;
        public static final byte socket_protocol_tcp = 0;
        public static final byte socket_protocol_udp = 0;
        public byte dest;
        public String ip_addr;
        public short port;
        public byte protocol;
        public short socket_mtu;
        public short socket_timeout;

        public msg_open_socket_t() {
            this.socket_mtu = (short) -1;
            this.socket_timeout = (short) -1;
        }

        public msg_open_socket_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            this.dest = bArr[i];
            int i4 = i3 + 1;
            this.protocol = bArr[i3];
            this.port = ByteArrayUtils.byteArrToShort(bArr, i4);
            int i5 = i4 + 2;
            int i6 = i5 - i;
            this.ip_addr = ByteArrayUtils.byteArrToString(bArr, i5, i2 - i6);
            return i6;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.dest;
            int i4 = i3 + 1;
            bArr[i3] = this.protocol;
            int shortToByteArray = i4 + ByteArrayUtils.shortToByteArray(bArr, i4, this.port);
            int stringToByteArray = shortToByteArray + ByteArrayUtils.stringToByteArray(bArr, shortToByteArray, this.ip_addr, i2 - (shortToByteArray - i));
            int i5 = stringToByteArray + 1;
            bArr[stringToByteArray] = 0;
            short s = this.socket_mtu;
            if (s >= 0 && this.socket_timeout >= 0) {
                int shortToByteArray2 = i5 + ByteArrayUtils.shortToByteArray(bArr, i5, s);
                i5 = shortToByteArray2 + ByteArrayUtils.shortToByteArray(bArr, shortToByteArray2, this.socket_timeout);
            }
            return i5 - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_close_socket_t extends msg_i {
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_close_socket_t() {
        }

        public msg_close_socket_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }
    }

    /* loaded from: classes.dex */
    public static class msg_socket_transfer_data_t extends msg_i {
        public byte[] data;

        public msg_socket_transfer_data_t() {
        }

        public msg_socket_transfer_data_t(byte[] bArr) {
            this.data = bArr;
        }

        public msg_socket_transfer_data_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            int byteArrToSubByteArray;
            if (i2 < 0) {
                Log.d(marshall_t.TAG, "negative len");
                byteArrToSubByteArray = i;
            } else {
                byte[] bArr2 = new byte[i2];
                this.data = bArr2;
                byteArrToSubByteArray = ByteArrayUtils.byteArrToSubByteArray(bArr, i, bArr2) + i;
            }
            return byteArrToSubByteArray - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return (ByteArrayUtils.subByteArrToByteArray(bArr, i, this.data) + i) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_modem_rx_control_t extends msg_i {
        public static final byte rx_control_pause = 1;
        public static final byte rx_control_resume = 0;
        public byte control;

        public msg_modem_rx_control_t() {
        }

        public msg_modem_rx_control_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            this.control = bArr[i];
            return (i + 1) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            bArr[i] = this.control;
            return (i + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_trace_t extends msg_i {
        public byte num_of_params;
        public short[] param_values;
        public short trace_number;

        public msg_trace_t() {
        }

        public msg_trace_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            this.trace_number = ByteArrayUtils.byteArrToShort(bArr, i);
            int i3 = i + 2;
            int i4 = i3 + 1;
            int i5 = bArr[i3];
            this.num_of_params = (byte) i5;
            this.param_values = new short[i5];
            for (int i6 = 0; i6 < this.num_of_params; i6++) {
                this.param_values[i6] = ByteArrayUtils.byteArrToShort(bArr, i4);
                i4 += 2;
            }
            return i4 - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int shortToByteArray = ByteArrayUtils.shortToByteArray(bArr, i, this.trace_number) + i;
            int i3 = shortToByteArray + 1;
            bArr[shortToByteArray] = this.num_of_params;
            for (int i4 = 0; i4 < this.num_of_params; i4++) {
                i3 += ByteArrayUtils.shortToByteArray(bArr, i3, this.param_values[i4]);
            }
            return i3 - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_alert_t extends msg_i {
        public String alert;
        public short alert_id;

        public msg_alert_t() {
        }

        public msg_alert_t(short s, String str) {
            this.alert_id = s;
            this.alert = str;
        }

        public msg_alert_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            this.alert_id = ByteArrayUtils.byteArrToShort(bArr, i);
            int i3 = i + 2;
            String byteArrToString = ByteArrayUtils.byteArrToString(bArr, i3, 512);
            this.alert = byteArrToString;
            return (i3 + byteArrToString.length()) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int shortToByteArray = ByteArrayUtils.shortToByteArray(bArr, i, this.alert_id) + i;
            return (shortToByteArray + ByteArrayUtils.stringToByteArray(bArr, shortToByteArray, this.alert, 512)) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_mdb_t extends msg_i {
        public msg_i message;

        public msg_mdb_t() {
        }

        public msg_mdb_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            mdb_rsp_t.msg_mdb_rsp_t msg_mdb_rsp_tVar = new mdb_rsp_t.msg_mdb_rsp_t();
            this.message = msg_mdb_rsp_tVar;
            return (msg_mdb_rsp_tVar.decode(bArr, i, i2) + i) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return (this.message.encode(bArr, i, i2) + i) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_reader_t extends msg_i {
        public byte event_type;
        public byte payment_type_data;
        public byte[] payment_type_data_value;
        public byte payment_type_key_value;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_reader_t() {
        }

        public msg_reader_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.event_type;
            int i4 = i3 + 1;
            bArr[i3] = this.payment_type_key_value;
            int i5 = i4 + 1;
            bArr[i4] = this.payment_type_data;
            return (i5 + ByteArrayUtils.subByteArrToByteArray(bArr, i5, this.payment_type_data_value)) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ext_commodore_tlv_t extends msg_i {
        public int body_len;
        public byte[] data;

        private void init() {
        }

        public msg_ext_commodore_tlv_t() {
            init();
            this.data = new byte[512];
        }

        public msg_ext_commodore_tlv_t(byte[] bArr, int i, int i2) {
            init();
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            Log.d(marshall_t.TAG, "received ext_commodore_tlv message");
            byte[] bArr2 = new byte[i2];
            this.data = bArr2;
            System.arraycopy(bArr, i, bArr2, 0, i2);
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            System.arraycopy(this.data, 0, bArr, i, this.body_len);
            return (this.body_len + i) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ev_ext_boot_notification extends msg_i {
        byte cp_charge_profile;
        String cp_model;
        String cp_serial;
        String cp_vendor;
        evse_rec_t[] evse;
        byte no_of_evse;
        String reserved;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_ev_ext_boot_notification() {
        }

        public msg_ev_ext_boot_notification(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            byte[] bytes = StringUtils.paddingStringRight(this.cp_model, 10).getBytes();
            byte[] bytes2 = StringUtils.paddingStringRight(this.cp_vendor, 10).getBytes();
            byte[] bytes3 = StringUtils.paddingStringRight(this.cp_serial, 20).getBytes();
            int subByteArrToByteArray = ByteArrayUtils.subByteArrToByteArray(bArr, i, bytes) + i;
            int subByteArrToByteArray2 = subByteArrToByteArray + ByteArrayUtils.subByteArrToByteArray(bArr, subByteArrToByteArray, bytes2);
            int subByteArrToByteArray3 = subByteArrToByteArray2 + ByteArrayUtils.subByteArrToByteArray(bArr, subByteArrToByteArray2, bytes3);
            int i3 = subByteArrToByteArray3 + 1;
            bArr[subByteArrToByteArray3] = this.cp_charge_profile;
            int i4 = i3 + 1;
            bArr[i3] = this.no_of_evse;
            int subByteArrToByteArray4 = i4 + ByteArrayUtils.subByteArrToByteArray(bArr, i4, new byte[8]);
            for (int i5 = 0; i5 < this.no_of_evse; i5++) {
                evse_rec_t evse_rec_tVar = this.evse[i5];
                byte[] bytes4 = StringUtils.paddingStringRight(evse_rec_tVar.serial, 20).getBytes();
                byte[] bytes5 = StringUtils.paddingStringRight(evse_rec_tVar.sw_version, 25).getBytes();
                byte[] bytes6 = StringUtils.paddingStringRight(evse_rec_tVar.hw_version, 25).getBytes();
                int i6 = subByteArrToByteArray4 + 1;
                bArr[subByteArrToByteArray4] = evse_rec_tVar.connector_id;
                int subByteArrToByteArray5 = i6 + ByteArrayUtils.subByteArrToByteArray(bArr, i6, bytes4);
                int subByteArrToByteArray6 = subByteArrToByteArray5 + ByteArrayUtils.subByteArrToByteArray(bArr, subByteArrToByteArray5, bytes5);
                int subByteArrToByteArray7 = subByteArrToByteArray6 + ByteArrayUtils.subByteArrToByteArray(bArr, subByteArrToByteArray6, bytes6);
                subByteArrToByteArray4 = ByteArrayUtils.subByteArrToByteArray(bArr, subByteArrToByteArray7, new byte[19]) + subByteArrToByteArray7;
            }
            return subByteArrToByteArray4 - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ev_ext_start_notification extends msg_i {
        byte connector_id;
        int current_session_energy_wh;
        int global_energy_wh;
        byte[] session_id;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_ev_ext_start_notification() {
        }

        public msg_ev_ext_start_notification(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int subByteArrToByteArray = ByteArrayUtils.subByteArrToByteArray(bArr, i, this.session_id) + i;
            int i3 = subByteArrToByteArray + 1;
            bArr[subByteArrToByteArray] = this.connector_id;
            int intToByteArray = i3 + ByteArrayUtils.intToByteArray(bArr, i3, this.current_session_energy_wh);
            return (intToByteArray + ByteArrayUtils.intToByteArray(bArr, intToByteArray, this.global_energy_wh)) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ev_ext_stop_notification extends msg_i {
        byte connector_id;
        int current_session_energy_wh;
        int global_energy_wh;
        byte reason;
        byte[] session_id;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_ev_ext_stop_notification() {
        }

        public msg_ev_ext_stop_notification(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int subByteArrToByteArray = ByteArrayUtils.subByteArrToByteArray(bArr, i, this.session_id) + i;
            int i3 = subByteArrToByteArray + 1;
            bArr[subByteArrToByteArray] = this.connector_id;
            int intToByteArray = i3 + ByteArrayUtils.intToByteArray(bArr, i3, this.current_session_energy_wh);
            int intToByteArray2 = intToByteArray + ByteArrayUtils.intToByteArray(bArr, intToByteArray, this.global_energy_wh);
            bArr[intToByteArray2] = this.reason;
            return (intToByteArray2 + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ev_ext_meter_value extends msg_i {
        byte charging_state;
        byte connector_id;
        int current_session_energy_wh;
        int global_energy_wh;
        short[] measure_current;
        short[] measure_power;
        short[] measure_voltage;
        byte[] session_id;
        byte state;
        byte temperature;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_ev_ext_meter_value() {
        }

        public msg_ev_ext_meter_value(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int subByteArrToByteArray = ByteArrayUtils.subByteArrToByteArray(bArr, i, this.session_id) + i;
            int i3 = subByteArrToByteArray + 1;
            bArr[subByteArrToByteArray] = this.connector_id;
            int intToByteArray = i3 + ByteArrayUtils.intToByteArray(bArr, i3, this.current_session_energy_wh);
            int intToByteArray2 = intToByteArray + ByteArrayUtils.intToByteArray(bArr, intToByteArray, this.global_energy_wh);
            int i4 = 0;
            int i5 = 0;
            while (true) {
                short[] sArr = this.measure_voltage;
                if (i5 >= sArr.length) {
                    break;
                }
                intToByteArray2 += ByteArrayUtils.shortToByteArray(bArr, intToByteArray2, sArr[i5]);
                i5++;
            }
            int i6 = 0;
            while (true) {
                short[] sArr2 = this.measure_current;
                if (i6 >= sArr2.length) {
                    break;
                }
                intToByteArray2 += ByteArrayUtils.shortToByteArray(bArr, intToByteArray2, sArr2[i6]);
                i6++;
            }
            while (true) {
                short[] sArr3 = this.measure_power;
                if (i4 < sArr3.length) {
                    intToByteArray2 += ByteArrayUtils.shortToByteArray(bArr, intToByteArray2, sArr3[i4]);
                    i4++;
                } else {
                    int i7 = intToByteArray2 + 1;
                    bArr[intToByteArray2] = this.temperature;
                    int i8 = i7 + 1;
                    bArr[i7] = this.charging_state;
                    bArr[i8] = this.state;
                    return (i8 + 1) - i;
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ev_ext_status_notification extends msg_i {
        byte connector_id;
        byte status;
        short vendor_error;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_ev_ext_status_notification() {
        }

        public msg_ev_ext_status_notification(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.connector_id;
            int i4 = i3 + 1;
            bArr[i3] = this.status;
            return (i4 + ByteArrayUtils.shortToByteArray(bArr, i4, this.vendor_error)) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ev_ext_diag_response extends msg_i {
        evse_diag_rec_t[] evse;
        byte no_of_evse;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_ev_ext_diag_response() {
        }

        public msg_ev_ext_diag_response(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            byte[] bArr2 = new byte[4];
            int i3 = i + 1;
            bArr[i] = this.no_of_evse;
            int i4 = 0;
            while (true) {
                evse_diag_rec_t[] evse_diag_rec_tVarArr = this.evse;
                if (i4 >= evse_diag_rec_tVarArr.length) {
                    return i3 - i;
                }
                evse_diag_rec_t evse_diag_rec_tVar = evse_diag_rec_tVarArr[i4];
                int i5 = i3 + 1;
                bArr[i3] = evse_diag_rec_tVar.connector_id;
                int i6 = i5 + 1;
                bArr[i5] = evse_diag_rec_tVar.charging_state;
                int i7 = i6 + 1;
                bArr[i6] = evse_diag_rec_tVar.state;
                int i8 = i7 + 1;
                bArr[i7] = evse_diag_rec_tVar.warning;
                int shortToByteArray = i8 + ByteArrayUtils.shortToByteArray(bArr, i8, evse_diag_rec_tVar.error);
                i3 = shortToByteArray + ByteArrayUtils.subByteArrToByteArray(bArr, shortToByteArray, bArr2);
                i4++;
            }
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ev_ext_guid_response extends msg_i {
        public byte[] guid;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_ev_ext_guid_response() {
        }

        public msg_ev_ext_guid_response(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            byte[] bArr2 = new byte[i2];
            this.guid = bArr2;
            return (ByteArrayUtils.byteArrToSubByteArray(bArr, i, bArr2) + i) - i;
        }
    }

    public static msg_marshall_t request(byte b, byte b2, msg_i msg_iVar) {
        msg_marshall_t msg_marshall_tVar = new msg_marshall_t();
        msg_marshall_tVar.packet_len = (short) 0;
        msg_marshall_tVar.id = (byte) 0;
        msg_marshall_tVar.options = b2;
        msg_marshall_tVar.body = msg_iVar;
        msg_marshall_tVar.dest = (byte) 0;
        msg_marshall_tVar.func_code = b;
        return msg_marshall_tVar;
    }

    public static msg_marshall_t response(byte b, byte b2) {
        msg_response_t msg_response_tVar = new msg_response_t();
        msg_response_tVar.value = b2;
        return request((byte) 0, b, msg_response_tVar);
    }

    public static msg_marshall_t firmware_info(byte b, msg_fw_info_t msg_fw_info_tVar) {
        return request((byte) 5, b, msg_fw_info_tVar);
    }

    public static msg_marshall_t config(byte b, byte b2) {
        return request((byte) 6, b, new msg_config_t());
    }

    public static msg_marshall_t keepalive() {
        return request((byte) 7, (byte) 1, new msg_keepalive_t());
    }

    public static msg_marshall_t mdb_req(mdb_req_t.msg_mdb_req_t msg_mdb_req_tVar) {
        return request(Byte.MIN_VALUE, (byte) 1, msg_mdb_req_tVar);
    }

    public static short Calc_CRC_CCITT(byte[] bArr, int i, int i2, short s) {
        for (int i3 = i; i3 < i + i2; i3++) {
            s = CRC_CCITT(s, bArr[i3]);
        }
        return s;
    }
}

package com.bitmick.marshall.vmc;

import com.bitmick.marshall.vmc.marshall_t;
import com.bitmick.utils.ByteArrayUtils;
/* loaded from: classes.dex */
public class mdb_t {
    public static final String TAG = "mdb_t";
    public static final byte mdb_dev_addr_cashless_a = 16;
    public static final byte mdb_dev_addr_cashless_b = 96;
    public static final byte mdb_master_addr = 16;

    /* loaded from: classes.dex */
    public static class msg_ftl_req_to_send_t extends marshall_t.msg_i {
        public byte command;
        public byte control;
        public byte dst;
        public byte file_id;
        public byte len;
        public byte src;

        public msg_ftl_req_to_send_t() {
        }

        public msg_ftl_req_to_send_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            this.command = bArr[i];
            byte b = bArr[i3];
            bArr[i3] = (byte) (b + 1);
            this.dst = b;
            byte b2 = bArr[i3];
            bArr[i3] = (byte) (b2 + 1);
            this.src = b2;
            byte b3 = bArr[i3];
            bArr[i3] = (byte) (b3 + 1);
            this.file_id = b3;
            byte b4 = bArr[i3];
            bArr[i3] = (byte) (b4 + 1);
            this.len = b4;
            byte b5 = bArr[i3];
            bArr[i3] = (byte) (b5 + 1);
            this.control = b5;
            return i3 - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.command;
            int i4 = i3 + 1;
            bArr[i3] = this.dst;
            int i5 = i4 + 1;
            bArr[i4] = this.src;
            int i6 = i5 + 1;
            bArr[i5] = this.file_id;
            int i7 = i6 + 1;
            bArr[i6] = this.len;
            bArr[i7] = this.control;
            return (i7 + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ftl_req_to_receive_t extends marshall_t.msg_i {
        public byte control;
        public byte dst;
        public byte file_id;
        public byte max_len;
        public byte src;

        public msg_ftl_req_to_receive_t() {
        }

        public msg_ftl_req_to_receive_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            byte b = bArr[i];
            bArr[i] = (byte) (b + 1);
            this.dst = b;
            byte b2 = bArr[i];
            bArr[i] = (byte) (b2 + 1);
            this.src = b2;
            byte b3 = bArr[i];
            bArr[i] = (byte) (b3 + 1);
            this.file_id = b3;
            byte b4 = bArr[i];
            bArr[i] = (byte) (b4 + 1);
            this.max_len = b4;
            byte b5 = bArr[i];
            bArr[i] = (byte) (b5 + 1);
            this.control = b5;
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.dst;
            int i4 = i3 + 1;
            bArr[i3] = this.src;
            int i5 = i4 + 1;
            bArr[i4] = this.file_id;
            int i6 = i5 + 1;
            bArr[i5] = this.max_len;
            bArr[i6] = this.control;
            return (i6 + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ftl_ok_to_send_t extends marshall_t.msg_i {
        public byte dst;
        public byte src;

        public msg_ftl_ok_to_send_t() {
        }

        public msg_ftl_ok_to_send_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            byte b = bArr[i];
            bArr[i] = (byte) (b + 1);
            this.dst = b;
            byte b2 = bArr[i];
            bArr[i] = (byte) (b2 + 1);
            this.src = b2;
            return i - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.dst;
            bArr[i3] = this.src;
            return (i3 + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ftl_send_block_t extends marshall_t.msg_i {
        public byte block_num;
        public byte[] data;
        public byte dst;

        public msg_ftl_send_block_t() {
        }

        public msg_ftl_send_block_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            byte b = bArr[i];
            bArr[i] = (byte) (b + 1);
            this.dst = b;
            byte b2 = bArr[i];
            bArr[i] = (byte) (b2 + 1);
            this.block_num = b2;
            byte[] bArr2 = new byte[i2 - i];
            this.data = bArr2;
            return (ByteArrayUtils.byteArrToSubByteArray(bArr, i, bArr2) + i) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.dst;
            int i4 = i3 + 1;
            bArr[i3] = this.block_num;
            return (i4 + ByteArrayUtils.subByteArrToByteArray(bArr, i4, this.data)) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_ftl_retry_deny_t extends marshall_t.msg_i {
        public byte dst;
        public byte retry_delay;
        public byte src;

        public msg_ftl_retry_deny_t() {
        }

        public msg_ftl_retry_deny_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            byte b = bArr[i];
            bArr[i] = (byte) (b + 1);
            this.dst = b;
            byte b2 = bArr[i];
            bArr[i] = (byte) (b2 + 1);
            this.src = b2;
            this.retry_delay = bArr[i];
            return (i + 1) - i;
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            int i3 = i + 1;
            bArr[i] = this.dst;
            int i4 = i3 + 1;
            bArr[i3] = this.src;
            bArr[i4] = this.retry_delay;
            return (i4 + 1) - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_sessions_status_t extends marshall_t.msg_i {
        public byte num_opened_sessions;
        public short[] sessions;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_sessions_status_t() {
            this.num_opened_sessions = (byte) 0;
            this.sessions = null;
        }

        public msg_sessions_status_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            int i3 = bArr[i];
            bArr[i] = (byte) (i3 + 1);
            this.num_opened_sessions = (byte) i3;
            this.sessions = new short[i3];
            int i4 = i;
            for (int i5 = 0; i5 < this.num_opened_sessions; i5++) {
                this.sessions[i5] = ByteArrayUtils.byteArrToShort(bArr, i4);
                i4 += 2;
            }
            return i4 - i;
        }
    }

    /* loaded from: classes.dex */
    public static class msg_reader_state_t extends marshall_t.msg_i {
        public byte state;

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int encode(byte[] bArr, int i, int i2) {
            return i - i;
        }

        public msg_reader_state_t() {
            this.state = (byte) 0;
        }

        public msg_reader_state_t(byte[] bArr, int i, int i2) {
            decode(bArr, i, i2);
        }

        @Override // com.bitmick.marshall.vmc.marshall_t.msg_i
        public int decode(byte[] bArr, int i, int i2) {
            byte b = bArr[i];
            bArr[i] = (byte) (b + 1);
            this.state = b;
            return i - i;
        }
    }
}

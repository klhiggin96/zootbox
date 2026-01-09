package com.bitmick.marshall.models;
/* loaded from: classes.dex */
public class vmc_configuration {
    public static final int debug_level_dump_all = 1;
    public static final int debug_level_dump_moderate = 2;
    public static final int debug_level_dump_none = 0;
    public static final int machine_type_none = 0;
    public static final int machine_type_type_beverage = 5;
    public static final int machine_type_type_card_reader = 2;
    public static final int machine_type_type_default = 4;
    public static final int machine_type_type_ev_charger = 11;
    public static final int machine_type_type_laundry_machine = 9;
    public static final int machine_type_type_locker = 7;
    public static final int machine_type_type_money = 16;
    public static final int machine_type_type_paymarket = 12;
    public static final int machine_type_type_photo_booth = 3;
    public static final int machine_type_type_reserved = 1;
    public static final int machine_type_type_retail = 8;
    public static final int machine_type_type_water_filling = 6;
    public static final int vend_denied_policy_cancel = 0;
    public static final int vend_denied_policy_persist = 1;
    public boolean always_idle;
    public boolean debug;
    public int dump_packets_level;
    public String hw_ver;
    public int machine_type;
    public boolean mag_card_approved_by_vmc_support;
    public String manuf_code;
    public String marshall_sdk_version;
    public boolean mifare_approved_by_vmc_support;
    public String model;
    public boolean multi_session_support;
    public boolean multi_vend_support;
    public Object port_vpos;
    public int port_vpos_baud;
    public boolean price_not_final_support;
    public boolean reader_always_on;
    public String serial;
    public String sw_ver;
    public int vend_denied_policy;

    public final void invalidate() {
        this.serial = "";
        this.model = "";
        this.sw_ver = "";
        this.reader_always_on = false;
        this.multi_session_support = false;
        this.multi_vend_support = false;
        this.always_idle = false;
        this.price_not_final_support = false;
        this.mag_card_approved_by_vmc_support = false;
        this.mifare_approved_by_vmc_support = false;
        this.vend_denied_policy = 0;
        this.debug = false;
        this.dump_packets_level = 0;
    }
}

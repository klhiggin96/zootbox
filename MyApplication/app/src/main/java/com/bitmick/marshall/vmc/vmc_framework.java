package com.bitmick.marshall.vmc;

import com.bitmick.utils.Log;
/* loaded from: classes.dex */
public class vmc_framework {
    private static final String TAG = "vmc_framework";
    private static String VERSION = "0.1.5.9";
    private static vmc_framework m_instance;
    public vmc_general_t general;
    public vmc_link link;
    public vmc_socket_t socket;
    public vmc_vend_t vend;

    private vmc_framework(boolean z) {
        init(z);
    }

    public vmc_framework() {
        init(false);
    }

    private void init(boolean z) {
        Log.initialize(null);
        if (!z) {
            Log.d(TAG, "multi-instance marshall enabled");
        }
        vmc_link vmc_linkVar = new vmc_link();
        this.link = vmc_linkVar;
        this.general = (vmc_general_t) vmc_linkVar.register_vmc_client(new vmc_general_t());
        this.vend = (vmc_vend_t) this.link.register_vmc_client(new vmc_vend_t());
        this.socket = (vmc_socket_t) this.link.register_vmc_client(new vmc_socket_t());
    }

    public static vmc_framework getInstance() {
        if (m_instance == null) {
            m_instance = new vmc_framework(true);
        }
        return m_instance;
    }

    /**
     * Reset the singleton instance to allow fresh initialization.
     * Call this before getInstance() when restarting the payment manager
     * to ensure clean state with updated configuration.
     */
    public static void reset() {
        if (m_instance != null) {
            Log.d(TAG, "Resetting vmc_framework singleton");
            try {
                m_instance.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping framework during reset: " + e.getMessage());
            }
            m_instance = null;
        }
    }

    public final void start() {
        this.link.start();
    }

    public final void stop() {
        this.link.stop();
    }

    public static final String get_version() {
        return VERSION;
    }
}

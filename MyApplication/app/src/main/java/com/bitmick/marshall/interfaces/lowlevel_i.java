package com.bitmick.marshall.interfaces;
/* loaded from: classes.dex */
public interface lowlevel_i {

    /* loaded from: classes.dex */
    public interface link_events_t {
        boolean onReceive(byte[] bArr, int i, int i2);
    }

    void init(Object obj, Object obj2);

    void onLinkTimerTick(long j);

    void register_link_events(link_events_t link_events_tVar);

    void reset();

    void start();

    void stop();

    boolean transmit(byte[] bArr, int i);
}

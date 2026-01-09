package com.bitmick.marshall.vmc;

import com.bitmick.marshall.models.MsgObject;
import com.bitmick.marshall.vmc.marshall_t;
import com.bitmick.utils.Log;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
/* loaded from: classes.dex */
public abstract class vmc_client_t {
    private static final int MAX_QUEUE_SIZE = 10;
    private Thread m_client_thread;
    private volatile boolean m_client_thread_running;
    private String m_name;
    protected vmc_link m_vmc_link;
    private LinkedList<MsgObject> m_delayed_queue = new LinkedList<>();
    private BlockingQueue<MsgObject> m_msg_queue = new ArrayBlockingQueue(10);
    private long m_timer_counter_ms = 0;

    public abstract boolean handleMarshallMessage(marshall_t.msg_marshall_t msg_marshall_tVar);

    public abstract void onCommError();

    public abstract void onReady(vmc_link vmc_linkVar);

    public vmc_client_t(String str) {
        this.m_name = str;
    }

    public void start() {
        if (this.m_client_thread == null) {
            Thread thread = new Thread(new ClientHandlingRunnable());
            this.m_client_thread = thread;
            thread.start();
        }
    }

    public void stop() {
        if (this.m_client_thread != null) {
            this.m_client_thread_running = false;
            notify(65535, null);
            this.m_client_thread = null;
        }
    }

    /* loaded from: classes.dex */
    private class ClientHandlingRunnable implements Runnable {
        private ClientHandlingRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            vmc_client_t.this.m_client_thread_running = true;
            while (vmc_client_t.this.m_client_thread_running) {
                while (vmc_client_t.this.m_client_thread_running) {
                    try {
                        vmc_client_t.this.handleMessage((MsgObject) vmc_client_t.this.m_msg_queue.take());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("", String.format("finished client thread: %s", vmc_client_t.this.m_name));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean handleMessage(MsgObject msgObject) {
        if (msgObject.id != 65535) {
            return true;
        }
        this.m_client_thread_running = false;
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void notify(int i, Object obj) {
        notify(i, obj, 0);
    }

    protected final void notify(int i, Object obj, int i2) {
        MsgObject msgObject = new MsgObject();
        msgObject.id = i;
        msgObject.data = obj;
        msgObject.timeout = i2;
        try {
            this.m_msg_queue.add(msgObject);
        } catch (Exception unused) {
        }
    }

    protected final void notify_delayed(int i, Object obj, int i2) {
        MsgObject msgObject = new MsgObject();
        msgObject.id = i;
        msgObject.data = obj;
        msgObject.timeout = i2;
        this.m_delayed_queue.addLast(msgObject);
    }

    public void onTimerTick(long j) {
        long j2 = j - this.m_timer_counter_ms;
        Iterator<MsgObject> it = this.m_delayed_queue.iterator();
        while (it.hasNext()) {
            MsgObject next = it.next();
            next.timeout -= j2;
            if (next.timeout <= 0) {
                this.m_delayed_queue.remove(next);
                this.m_msg_queue.add(next);
            }
        }
        this.m_timer_counter_ms = j;
    }
}

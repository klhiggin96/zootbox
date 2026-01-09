package com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.driver;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class UsbSerialProber {
    private final ProbeTable mProbeTable;

    public UsbSerialProber(ProbeTable probeTable) {
        this.mProbeTable = probeTable;
    }

    public static UsbSerialProber getDefaultProber() {
        return new UsbSerialProber(getDefaultProbeTable());
    }

    public static ProbeTable getDefaultProbeTable() {
        ProbeTable probeTable = new ProbeTable();
        probeTable.addDriver(CdcAcmSerialDriver.class);
        return probeTable;
    }

    public List<UsbSerialDriver> findAllDrivers(final UsbManager usbManager) {
        ArrayList arrayList = new ArrayList();
        for (UsbDevice usbDevice : usbManager.getDeviceList().values()) {
            UsbSerialDriver probeDevice = probeDevice(usbDevice);
            if (probeDevice != null) {
                arrayList.add(probeDevice);
            }
        }
        return arrayList;
    }

    public UsbSerialDriver probeDevice(final UsbDevice usbDevice) {
        Class<? extends UsbSerialDriver> findDriver = this.mProbeTable.findDriver(usbDevice.getVendorId(), usbDevice.getProductId());
        if (findDriver != null) {
            try {
                return findDriver.getConstructor(UsbDevice.class).newInstance(usbDevice);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e2) {
                throw new RuntimeException(e2);
            } catch (InstantiationException e3) {
                throw new RuntimeException(e3);
            } catch (NoSuchMethodException e4) {
                throw new RuntimeException(e4);
            } catch (InvocationTargetException e5) {
                throw new RuntimeException(e5);
            }
        }
        return null;
    }
}

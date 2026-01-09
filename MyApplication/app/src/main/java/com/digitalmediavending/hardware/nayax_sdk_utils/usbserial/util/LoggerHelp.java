package com.digitalmediavending.hardware.nayax_sdk_utils.usbserial.util;

import android.os.Environment;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
/* loaded from: classes.dex */
public class LoggerHelp {
    public void appendLog(String text) {
        Log.e("appendLog", "appendLog call");
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/logger");
        if (!file.exists()) {
            file.mkdir();
        }
        File file2 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/logger/JSON_RESPONSE.txt");
        if (!file2.exists()) {
            try {
                file2.createNewFile();
            } catch (IOException e) {
                Log.e("appendLog", e.toString());
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file2, true));
            bufferedWriter.append((CharSequence) text);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e2) {
            Log.e("appendLog", e2.toString());
            e2.printStackTrace();
        }
    }
}

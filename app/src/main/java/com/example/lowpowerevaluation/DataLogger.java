package com.example.lowpowerevaluation;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * The class that handles File IO related operations,
 * including logging results.
 */
class DataLogger {
    private static final String TAG = DataLogger.class.getSimpleName();
    private static final String DIR = "KeyboardEvalLog";
    private final Handler mHandler;
    private PrintWriter mLogWriter;

    DataLogger(final Handler loggingThreadHandler,
               final String loggerFileName, final String loggerFormat) {
        mHandler = loggingThreadHandler;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final File dir =
                        new File(Environment.getExternalStorageDirectory()
                                + File.separator + DIR);
                dir.mkdirs();
                final File logFile = new File(dir, loggerFileName);
                Log.d(TAG, "logFile=" + logFile.getPath());
                try {
                    mLogWriter = new PrintWriter(
                            new BufferedWriter(new FileWriter(logFile)));
                    logString(loggerFormat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void logString(final String stringToLog) {
        if (mLogWriter == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mLogWriter.print(stringToLog);
                mLogWriter.flush();
            }
        });
    }

    void closeLogFile() {
        if (mLogWriter == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mLogWriter.flush();
                mLogWriter.close();
            }
        });
    }
}

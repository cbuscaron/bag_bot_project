package com.flomio.smartcartlib.util;

import android.annotation.SuppressLint;
import android.util.Log;
import com.flomio.smartcartlib.Constants;

public class Logging {
    public static String LOG_TAG = Constants.LOG_TAG;
    public static boolean DEBUG = Constants.DEBUG;
    public static void logDisabled(String msg, Object... args) {

    }

    public static void logD(String msg, Object... args) {
        String message = String.format(msg, args);
        if (DEBUG) {

            String klassName = Logging.class.getName();
            String methodName = "logD";
            // traceElement.toString() will be in form that android studio
            // can click upon
            StackTraceElement traceElement = getCallerFor(klassName, methodName);
            String threadDetails = getThreadDetails();
            logRaw(String.format("%s%s: %s", traceElement, threadDetails, message));
        }
        else {
            logRaw(message);
        }
    }

    @SuppressLint("DefaultLocale")
    private static String getThreadDetails() {
        Thread t = Thread.currentThread();
        return String.format("[%s,%d]", t.getName(), t
                .getId());
    }

    private static StackTraceElement getCallerFor(String klassName, String methodName) {
        int level = 3; //
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String className = e.getClassName();
            if (className.equals(klassName) && e.getMethodName().equals
                    (methodName)) {
                level = i + 1;
                break;
            }
        }
        return trace[level];
    }

    private static void logRaw(String msg) {

        try {
            Log.d(LOG_TAG, msg);
        } catch (Exception e) {
            System.out.println(msg);
            //throw new RuntimeException(e);
        }
    }
}

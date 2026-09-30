package com.netk.mvolatrack;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Persists crash details without depending on Logcat or a connected development environment. */
public final class CrashLogger implements Thread.UncaughtExceptionHandler {
    public static final String FILE_NAME = "MVolaCash_crash_log.txt";

    private static final Object FILE_LOCK = new Object();
    private static volatile CrashLogger instance;

    private final Context applicationContext;
    private final Thread.UncaughtExceptionHandler previousHandler;

    private CrashLogger(Context context, Thread.UncaughtExceptionHandler previousHandler) {
        applicationContext = context.getApplicationContext();
        this.previousHandler = previousHandler;
    }

    /** Installs the logger once while retaining Android's normal crash handling. */
    public static void initialize(Context context) {
        if (instance != null) return;
        synchronized (CrashLogger.class) {
            if (instance != null) return;
            Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            CrashLogger logger = new CrashLogger(context, previous);
            instance = logger;
            Thread.setDefaultUncaughtExceptionHandler(logger);
        }
    }

    /** Records a caught failure, for example when launching a screen from a click handler. */
    public static void recordException(Context context, Throwable throwable) {
        if (context == null || throwable == null) return;
        writeReport(context.getApplicationContext(), throwable, Thread.currentThread().getName());
    }

    /** Returns the report in the app-specific external files directory, or null if unavailable. */
    public static File getReportFile(Context context) {
        if (context == null) return null;
        File directory = context.getExternalFilesDir(null);
        return directory == null ? null : new File(directory, FILE_NAME);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        writeReport(applicationContext, throwable, thread == null ? "inconnu" : thread.getName());
        if (previousHandler != null) {
            previousHandler.uncaughtException(thread, throwable);
        } else {
            Process.killProcess(Process.myPid());
            System.exit(10);
        }
    }

    private static void writeReport(Context context, Throwable throwable, String threadName) {
        File report = getReportFile(context);
        if (report == null) return;

        StackTraceElement location = firstApplicationFrame(throwable);
        StringWriter stackTrace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stackTrace));
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.ROOT)
                .format(new Date());

        synchronized (FILE_LOCK) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(report, true))) {
                if (report.length() > 0) writer.println();
                writer.println("================ MVolaCash diagnostic ================");
                writer.println("Date et heure : " + timestamp);
                writer.println("Version application : " + applicationVersion(context));
                writer.println("Thread : " + threadName);
                writer.println("Exception : " + throwable.getClass().getName());
                writer.println("Message : " + String.valueOf(throwable.getMessage()));
                writer.println("Classe : " + (location == null ? "inconnue" : location.getClassName()));
                writer.println("Méthode : " + (location == null ? "inconnue" : location.getMethodName()));
                writer.println("Ligne : " + (location == null ? -1 : location.getLineNumber()));
                writer.println("Stacktrace complète :");
                writer.print(stackTrace);
                writer.println("=======================================================");
            } catch (IOException ignored) {
                // There is deliberately no Logcat fallback: diagnostics must remain on the device.
            }
        }
    }

    private static StackTraceElement firstApplicationFrame(Throwable throwable) {
        for (StackTraceElement frame : throwable.getStackTrace()) {
            if (frame.getClassName().startsWith("com.netk.mvolatrack")) return frame;
        }
        StackTraceElement[] frames = throwable.getStackTrace();
        return frames.length == 0 ? null : frames[0];
    }

    private static String applicationVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return String.valueOf(info.versionName) + " (" + info.versionCode + ")";
        } catch (PackageManager.NameNotFoundException ignored) {
            return "inconnue";
        }
    }
}

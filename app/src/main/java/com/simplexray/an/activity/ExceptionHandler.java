package com.simplexray.an.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.simplexray.an.R;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public final class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Activity activity;

    public ExceptionHandler(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        String report = buildReport(thread, throwable);

        // A separate Looper lets the crash dialog receive button events even when
        // the uncaught exception came from the main thread.
        try {
            Thread dialogThread = new Thread(() -> {
                try {
                    Looper.prepare();
                    showCrashDialog(report);
                    Looper.loop();
                } catch (Throwable dialogError) {
                    android.util.Log.e("ExceptionHandler", "Unable to display crash dialog", dialogError);
                    terminate();
                }
            }, "SimpleXray-CrashDialog");
            dialogThread.start();
            dialogThread.join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            terminate();
        } catch (Throwable handlerError) {
            android.util.Log.e("ExceptionHandler", "Crash handler failed", handlerError);
            terminate();
        }
    }

    private String buildReport(Thread thread, Throwable throwable) {
        Writer writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));

        return "APP ERROR:\n\n" + writer
                + "\n\nTHREAD:\n" + thread.getName()
                + "\n\nDEVICE INFO:\n"
                + "Brand: " + Build.BRAND + "\n"
                + "Device: " + Build.DEVICE + "\n"
                + "Model: " + Build.MODEL + "\n"
                + "Id: " + Build.ID + "\n"
                + "Product: " + Build.PRODUCT + "\n"
                + "\nFIRMWARE:\n"
                + "SDK: " + Build.VERSION.SDK_INT + "\n"
                + "Release: " + Build.VERSION.RELEASE + "\n"
                + "Incremental: " + Build.VERSION.INCREMENTAL + "\n";
    }

    private void showCrashDialog(String report) {
        View view = LayoutInflater.from(activity).inflate(R.layout.abc, null, false);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

        MaterialButton showError = view.findViewById(R.id.btn_show_error);
        MaterialButton restart = view.findViewById(R.id.btn_restart);
        MaterialButton exit = view.findViewById(R.id.btn_exit);

        showError.setOnClickListener(v -> showFullScreenErrorDialog(report));
        restart.setOnClickListener(v -> {
            try {
                Intent launchIntent = activity.getPackageManager()
                        .getLaunchIntentForPackage(activity.getPackageName());
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(launchIntent);
                }
            } catch (Throwable restartError) {
                android.util.Log.e("ExceptionHandler", "Unable to restart app", restartError);
            }
            terminate();
        });
        exit.setOnClickListener(v -> terminate());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showFullScreenErrorDialog(String report) {
        View view = LayoutInflater.from(activity)
                .inflate(R.layout.full_screen_error_dialog, null, false);
        TextView message = view.findViewById(R.id.full_error_message);
        message.setText(report);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

        MaterialButton copy = view.findViewById(R.id.btn_copy_full);
        MaterialButton close = view.findViewById(R.id.btn_close_full);

        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager)
                    activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("SimpleXray crash report", report));
                Toast.makeText(activity, "Error copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
        close.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private static void terminate() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
}

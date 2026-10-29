package com.velqor.app.files;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import rikka.shizuku.Shizuku;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class WirelessDebugManager {

    private static final String PREFS_NAME = "velqor_wireless_debug";
    private static final String KEY_ENABLED = "debug_enabled";
    private static final String KEY_PAIRED = "debug_paired";
    private static final String KEY_PORT = "debug_port";
    private static final String KEY_CODE = "debug_code";
    private static final String KEY_PAIRED_DEVICE = "paired_device_name";
    private static final String DEFAULT_PAIRED_DEVICE = "Shizuku Shell (UID 2000)";

    private WirelessDebugManager() {}

    public static class ShizukuFile extends File {
        private final boolean mIsDir;

        public ShizukuFile(File parent, String name, boolean isDir) {
            super(parent, name);
            this.mIsDir = isDir;
        }

        @Override
        public boolean isDirectory() {
            return mIsDir || super.isDirectory();
        }

        @Override
        public boolean isFile() {
            return !mIsDir || super.isFile();
        }

        @Override
        public boolean exists() {
            return true;
        }
    }

    public static boolean isShizukuRunning() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isShizukuPermissionGranted() {
        try {
            if (!isShizukuRunning()) return false;
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void requestShizukuPermission(int requestCode) {
        try {
            if (isShizukuRunning()) {
                Shizuku.requestPermission(requestCode);
            }
        } catch (Throwable ignored) {}
    }

    public static void addBinderReceivedListener(Shizuku.OnBinderReceivedListener listener) {
        try {
            Shizuku.addBinderReceivedListenerSticky(listener);
        } catch (Throwable ignored) {}
    }

    public static void removeBinderReceivedListener(Shizuku.OnBinderReceivedListener listener) {
        try {
            Shizuku.removeBinderReceivedListener(listener);
        } catch (Throwable ignored) {}
    }

    public static void addRequestPermissionResultListener(Shizuku.OnRequestPermissionResultListener listener) {
        try {
            Shizuku.addRequestPermissionResultListener(listener);
        } catch (Throwable ignored) {}
    }

    public static void removeRequestPermissionResultListener(Shizuku.OnRequestPermissionResultListener listener) {
        try {
            Shizuku.removeRequestPermissionResultListener(listener);
        } catch (Throwable ignored) {}
    }

    public static boolean isShizukuInstalled(Context ctx) {
        if (ctx == null) return false;
        try {
            ctx.getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String getShizukuStatus(Context ctx) {
        if (isShizukuRunning()) {
            if (isShizukuPermissionGranted()) {
                return "Đang chạy (Đã cấp quyền)";
            }
            return "Đang chạy (Chờ cấp quyền)";
        }
        if (isShizukuInstalled(ctx)) {
            return "Đã cài đặt (Chưa chạy)";
        }
        return "Chưa kích hoạt";
    }

    public static boolean isDebugEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, true);
    }

    public static void setDebugEnabled(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
    }

    public static boolean isPaired(Context ctx) {
        if (isShizukuRunning() && isShizukuPermissionGranted()) {
            return true;
        }
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PAIRED, false);
    }

    public static void setPaired(Context ctx, boolean paired, String port, String code) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PAIRED, paired)
                .putString(KEY_PORT, port)
                .putString(KEY_CODE, code)
                .putString(KEY_PAIRED_DEVICE, DEFAULT_PAIRED_DEVICE)
                .apply();
    }

    public static void unpair(Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PAIRED, false)
                .remove(KEY_PORT)
                .remove(KEY_CODE)
                .apply();
    }

    public static String getPairedDeviceName(Context ctx) {
        if (isShizukuRunning() && isShizukuPermissionGranted()) {
            return "Shizuku Service";
        }
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PAIRED_DEVICE, DEFAULT_PAIRED_DEVICE);
    }

    public static String getPairingPort(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PORT, "37051");
    }

    public static String getDeviceName() {
        String model = Build.MODEL;
        if (model != null && (model.contains("2511FPC34G") || model.toLowerCase().contains("poco"))) {
            return "POCO X8 Pro";
        }
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer != null && !manufacturer.isEmpty()) {
            return manufacturer.substring(0, 1).toUpperCase() + manufacturer.substring(1) + " " + model;
        }
        return model != null ? model : "Thiết bị Android";
    }

    public static String getDeviceIp(Context ctx) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface intf = interfaces.nextElement();
                    if (!intf.isUp() || intf.isLoopback()) continue;
                    Enumeration<InetAddress> addrs = intf.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            String host = addr.getHostAddress();
                            if (host != null && !host.startsWith("127.")) {
                                return host;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return "192.168.1.29";
    }

    public static boolean isAndroidDataPath(File f) {
        if (f == null) return false;
        String path = f.getAbsolutePath();
        return path.contains("/Android/data") || path.contains("/Android/obb");
    }

    public static File[] listAndroidDataArray(File dir) {
        if (dir == null) return new File[0];
        List<File> list = new ArrayList<>();
        if (isShizukuRunning() && isShizukuPermissionGranted()) {
            try {
                Method newProcessMethod = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
                newProcessMethod.setAccessible(true);
                String[] cmd = new String[]{"sh", "-c", "ls -1pA \"" + dir.getAbsolutePath() + "\" 2>/dev/null"};
                Process process = (Process) newProcessMethod.invoke(null, cmd, null, null);
                if (process != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        boolean isDir = line.endsWith("/");
                        String name = isDir ? line.substring(0, line.length() - 1) : line;
                        list.add(new ShizukuFile(dir, name, isDir));
                    }
                    reader.close();
                    process.waitFor();
                }
            } catch (Throwable ignored) {}
        }
        if (list.isEmpty()) {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ls -1pA \"" + dir.getAbsolutePath() + "\" 2>/dev/null"});
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    boolean isDir = line.endsWith("/");
                    String name = isDir ? line.substring(0, line.length() - 1) : line;
                    list.add(new ShizukuFile(dir, name, isDir));
                }
                reader.close();
                process.waitFor();
            } catch (Throwable ignored) {}
        }
        return list.toArray(new File[0]);
    }
}

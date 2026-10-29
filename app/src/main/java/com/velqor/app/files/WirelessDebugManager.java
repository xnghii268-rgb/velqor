package com.velqor.app.files;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
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
    private static final String DEFAULT_PAIRED_DEVICE = "DN-Turbo";

    private WirelessDebugManager() {}

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
        } catch (Exception ignored) {}
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
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ls -1 \"" + dir.getAbsolutePath() + "\" 2>/dev/null"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    list.add(new File(dir, line));
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception ignored) {}

        if (list.isEmpty()) {
            String path = dir.getAbsolutePath();
            if (path.endsWith("/Android/data")) {
                list.add(new File(dir, "com.termuxpro.app"));
                list.add(new File(dir, "com.velqor.app"));
                list.add(new File(dir, "com.android.chrome"));
                list.add(new File(dir, "com.google.android.youtube"));
                list.add(new File(dir, "com.google.android.gms"));
                list.add(new File(dir, "com.google.android.apps.photos"));
                list.add(new File(dir, "com.facebook.katana"));
                list.add(new File(dir, "com.zing.zalo"));
                list.add(new File(dir, "com.spotify.music"));
            } else if (path.endsWith("/Android/obb")) {
                list.add(new File(dir, "com.dts.freefireth"));
                list.add(new File(dir, "com.vng.pubgmobile"));
                list.add(new File(dir, "com.tencent.ig"));
            }
        }
        return list.toArray(new File[0]);
    }
}

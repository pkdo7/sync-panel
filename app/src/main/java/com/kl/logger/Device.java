package com.kl.logger;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import org.json.JSONObject;
import java.util.Locale;

public final class Device {
    private Device() {}

    static JSONObject info(Context c, String id) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("model", Build.MANUFACTURER + " " + Build.MODEL);
            o.put("sdk", Build.VERSION.SDK_INT);
            o.put("country", Locale.getDefault().getCountry());
            o.put("flag", flag(Locale.getDefault().getCountry()));
            o.put("ip", Ip.get());

            // Operadora SIM
            try {
                TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
                String op = tm.getNetworkOperatorName();
                o.put("sim", op != null && !op.isEmpty() ? op : "N/A");
            } catch (Exception e) {
                o.put("sim", "N/A");
            }

            // Bateria
            try {
                BatteryManager bm = (BatteryManager) c.getSystemService(Context.BATTERY_SERVICE);
                int lvl = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                o.put("batt", lvl);
            } catch (Exception e) {
                o.put("batt", -1);
            }

        } catch (Exception ignored) {}
        return o;
    }

    static String flag(String cc) {
        if (cc == null || cc.length() != 2) return "🌐";
        try {
            int a = 0x1F1E6 + (Character.toUpperCase(cc.charAt(0)) - 'A');
            int b = 0x1F1E6 + (Character.toUpperCase(cc.charAt(1)) - 'A');
            return new String(Character.toChars(a)) + new String(Character.toChars(b));
        } catch (Exception e) {
            return "🌐";
        }
    }
}

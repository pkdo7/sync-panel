package com.kl.logger;

import java.io.*;
import java.net.*;

public final class Ip {
    private Ip() {}

    static String get() {
        String[] providers = { "https://api.ipify.org", "https://checkip.amazonaws.com" };
        for (String p : providers) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(p).openConnection();
                c.setConnectTimeout(8_000);
                c.setReadTimeout(8_000);
                c.setRequestProperty("User-Agent", "sync/1.0");
                try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                    String ip = r.readLine();
                    if (ip != null) return ip.trim();
                }
            } catch (Exception ignored) {}
        }
        return "";
    }
}

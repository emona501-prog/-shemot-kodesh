package com.chaiproductions.shemotkodesh;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Home-screen widget of the 72 names.
 * Everything works inside the widget: previous / next / random spin / pause.
 * Only the dedicated "open" button launches the app.
 * The layout adapts to the size the user gives the widget (small / medium / large).
 * NOTE: keep this file ASCII-only. All Hebrew text lives in WidgetData or in XML.
 */
public class ShemotWidget extends AppWidgetProvider {

    static final int UPDATE_MINUTES = 5;
    static final int COUNT = 72;

    static final String PKG         = "com.chaiproductions.shemotkodesh";
    static final String ACTION_TICK = PKG + ".WIDGET_UPDATE";
    static final String ACTION_SPIN = PKG + ".WIDGET_SPIN";
    static final String ACTION_NEXT = PKG + ".WIDGET_NEXT";
    static final String ACTION_PREV = PKG + ".WIDGET_PREV";
    static final String ACTION_AUTO = PKG + ".WIDGET_AUTO";

    static final String PREFS     = "ShemotWidgetPrefs";
    static final String PREF_IDX  = "idx";
    static final String PREF_AUTO = "auto";

    static final int KIND_SMALL = 0, KIND_MEDIUM = 1, KIND_LARGE = 2;

    static final int WHEEL_PX = 440;

    // Hebrew month (ICU HebrewCalendar constant, Tishri = 0 ... Elul = 12)
    // -> index in WidgetData.TZ_* (Nisan = 0 ... Adar = 11)
    static final int[] MONTH_MAP = {6, 7, 8, 9, 10, 11, 11, 0, 1, 2, 3, 4, 5};

    // ---------------------------------------------------------------
    // AppWidgetProvider
    // ---------------------------------------------------------------

    @Override
    public void onUpdate(Context ctx, AppWidgetManager awm, int[] ids) {
        for (int id : ids) updateWidget(ctx, awm, id);
        scheduleNext(ctx);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager awm, int id, Bundle opts) {
        // the user resized the widget -> pick the fitting layout
        updateWidget(ctx, awm, id);
    }

    @Override
    public void onEnabled(Context ctx) { scheduleNext(ctx); }

    @Override
    public void onDisabled(Context ctx) { cancelSchedule(ctx); }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String action = intent.getAction();
        if (action == null) return;

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int idx = currentIndex(prefs);

        if (ACTION_TICK.equals(action)) {
            if (prefs.getBoolean(PREF_AUTO, true)) {
                prefs.edit().putInt(PREF_IDX, (idx + 1) % COUNT).apply();
            }
            updateAll(ctx);
            scheduleNext(ctx);
        } else if (ACTION_NEXT.equals(action)) {
            prefs.edit().putInt(PREF_IDX, (idx + 1) % COUNT).apply();
            updateAll(ctx);
        } else if (ACTION_PREV.equals(action)) {
            prefs.edit().putInt(PREF_IDX, (idx + COUNT - 1) % COUNT).apply();
            updateAll(ctx);
        } else if (ACTION_AUTO.equals(action)) {
            prefs.edit().putBoolean(PREF_AUTO, !prefs.getBoolean(PREF_AUTO, true)).apply();
            updateAll(ctx);
        } else if (ACTION_SPIN.equals(action)) {
            int target = new java.util.Random().nextInt(COUNT);
            if (target == idx) target = (target + 1 + new java.util.Random().nextInt(COUNT - 1)) % COUNT;
            prefs.edit().putInt(PREF_IDX, target).apply();
            animateSpin(ctx, idx, target);
        }
    }

    static int currentIndex(SharedPreferences prefs) {
        int idx = prefs.getInt(PREF_IDX, 0);
        if (idx < 0 || idx >= COUNT) idx = 0;
        return idx;
    }

    static int[] allIds(Context ctx, AppWidgetManager awm) {
        return awm.getAppWidgetIds(new ComponentName(ctx, ShemotWidget.class));
    }

    static void updateAll(Context ctx) {
        AppWidgetManager awm = AppWidgetManager.getInstance(ctx);
        for (int id : allIds(ctx, awm)) updateWidget(ctx, awm, id);
    }

    // ---------------------------------------------------------------
    // Spin animation: the highlight races around the wheel and slows
    // down onto the chosen name. Runs fully inside the widget.
    // ---------------------------------------------------------------

    void animateSpin(final Context ctx, final int from, final int target) {
        final AppWidgetManager awm = AppWidgetManager.getInstance(ctx);
        final int[] ids = allIds(ctx, awm);

        boolean anyWheel = false;
        for (int id : ids) if (kindFor(ctx, awm, id) != KIND_SMALL) anyWheel = true;
        if (!anyWheel) { updateAll(ctx); return; }

        final BroadcastReceiver.PendingResult pending = goAsync();
        final Handler handler = new Handler(Looper.getMainLooper());
        final int frames = 12;
        final int distance = COUNT + ((target - from) % COUNT + COUNT) % COUNT; // one full turn + the rest
        long delay = 0;

        for (int k = 1; k <= frames; k++) {
            final boolean last = (k == frames);
            float t = (float) k / frames;
            float eased = 1f - (1f - t) * (1f - t) * (1f - t);
            final int pos = last ? target : (from + Math.round(distance * eased)) % COUNT;
            delay += 45 + k * 9; // slows down toward the end
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    try {
                        if (last) {
                            updateAll(ctx);
                        } else {
                            Bitmap bmp = drawWheel(pos);
                            for (int id : ids) {
                                int kind = kindFor(ctx, awm, id);
                                if (kind == KIND_SMALL) continue;
                                RemoteViews rv = new RemoteViews(ctx.getPackageName(), layoutFor(kind));
                                rv.setImageViewBitmap(R.id.w_wheel, bmp);
                                awm.partiallyUpdateAppWidget(id, rv);
                            }
                        }
                    } catch (Exception e) {
                        // a frame failed - the final full update below still fixes the widget
                    } finally {
                        if (last) pending.finish();
                    }
                }
            }, delay);
        }
    }

    // ---------------------------------------------------------------
    // Size -> layout
    // ---------------------------------------------------------------

    static int[] sizeDp(Context ctx, AppWidgetManager awm, int id) {
        Bundle o = awm.getAppWidgetOptions(id);
        if (o == null) return new int[]{0, 0};
        boolean land = ctx.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        int w = o.getInt(land ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
                              : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int h = o.getInt(land ? AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
                              : AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        return new int[]{w, h};
    }

    static int kindFor(Context ctx, AppWidgetManager awm, int id) {
        int[] s = sizeDp(ctx, awm, id);
        int w = s[0], h = s[1];
        if (w <= 0 || h <= 0) return KIND_MEDIUM;
        if (h < 100) return KIND_SMALL;
        // wide and not very tall -> wheel beside the text; otherwise wheel on top
        if (w >= 230 && h < 230 && w >= h * 1.25f) return KIND_MEDIUM;
        return KIND_LARGE;
    }

    static int layoutFor(int kind) {
        if (kind == KIND_SMALL) return R.layout.widget_small;
        if (kind == KIND_LARGE) return R.layout.widget_large;
        return R.layout.widget_medium;
    }

    // ---------------------------------------------------------------
    // Full update of one widget
    // ---------------------------------------------------------------

    static void updateWidget(Context ctx, AppWidgetManager awm, int id) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int idx = currentIndex(prefs);
        boolean auto = prefs.getBoolean(PREF_AUTO, true);

        int[] size = sizeDp(ctx, awm, id);
        int w = size[0], h = size[1];
        int kind = kindFor(ctx, awm, id);
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), layoutFor(kind));

        int color = WidgetData.COLOR[idx];

        // texts (ids missing from a layout are simply skipped by RemoteViews)
        rv.setTextViewText(R.id.w_name, WidgetData.NIKUD[idx]);
        rv.setTextViewText(R.id.w_head, WidgetData.S_SHEM + " " + (idx + 1) + " " + WidgetData.S_OF
                + "  \u00B7  " + WidgetData.SEFIRA[idx]);
        rv.setTextColor(R.id.w_head, color);
        rv.setTextViewText(R.id.w_segula, WidgetData.SEGULA[idx]);
        rv.setTextViewText(R.id.w_pasuk, WidgetData.PASUK[idx]);
        rv.setTextViewText(R.id.w_src, WidgetData.SOURCE[idx]);
        rv.setTextViewText(R.id.w_tz, monthTziruf());
        rv.setTextViewText(R.id.b_auto, auto ? WidgetData.S_PAUSE : WidgetData.S_PLAY);

        if (kind != KIND_SMALL) {
            rv.setImageViewBitmap(R.id.w_wheel, drawWheel(idx));
        }

        // fit the content to the actual size
        if (kind == KIND_LARGE && w > 0) {
            boolean shortH = h < 230;
            boolean narrow = w < 200;
            rv.setViewVisibility(R.id.w_pasuk, shortH ? View.GONE : View.VISIBLE);
            rv.setViewVisibility(R.id.w_src,   h < 280 ? View.GONE : View.VISIBLE);
            rv.setViewVisibility(R.id.w_tz,    (shortH || narrow) ? View.GONE : View.VISIBLE);
            rv.setViewVisibility(R.id.w_head,  h < 170 ? View.GONE : View.VISIBLE);
            rv.setViewVisibility(R.id.b_auto,  narrow ? View.GONE : View.VISIBLE);
            rv.setViewVisibility(R.id.b_open,  w < 150 ? View.GONE : View.VISIBLE);
        } else if (kind == KIND_MEDIUM) {
            rv.setViewVisibility(R.id.w_src,   h < 150 ? View.GONE : View.VISIBLE);
            rv.setViewVisibility(R.id.b_auto,  w < 280 ? View.GONE : View.VISIBLE);
        } else if (kind == KIND_SMALL && w > 0) {
            rv.setViewVisibility(R.id.w_segula, h < 56 ? View.GONE : View.VISIBLE);
            rv.setViewVisibility(R.id.b_open,   w < 200 ? View.GONE : View.VISIBLE);
        }

        // in-widget actions
        rv.setOnClickPendingIntent(R.id.w_wheel,  broadcast(ctx, ACTION_SPIN, 10));
        rv.setOnClickPendingIntent(R.id.w_tap,    broadcast(ctx, ACTION_SPIN, 10));
        rv.setOnClickPendingIntent(R.id.b_spin,   broadcast(ctx, ACTION_SPIN, 10));
        rv.setOnClickPendingIntent(R.id.b_next,   broadcast(ctx, ACTION_NEXT, 11));
        rv.setOnClickPendingIntent(R.id.b_prev,   broadcast(ctx, ACTION_PREV, 12));
        rv.setOnClickPendingIntent(R.id.b_auto,   broadcast(ctx, ACTION_AUTO, 13));

        // only this button opens the app
        Intent open = new Intent(ctx, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        rv.setOnClickPendingIntent(R.id.b_open, PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        awm.updateAppWidget(id, rv);
    }

    static PendingIntent broadcast(Context ctx, String action, int requestCode) {
        Intent i = new Intent(ctx, ShemotWidget.class);
        i.setAction(action);
        return PendingIntent.getBroadcast(ctx, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** "tziruf <month>: <letters>" by the real Hebrew calendar month. */
    static String monthTziruf() {
        int tz;
        try {
            android.icu.util.HebrewCalendar hc = new android.icu.util.HebrewCalendar();
            int m = hc.get(android.icu.util.Calendar.MONTH);
            tz = (m >= 0 && m < MONTH_MAP.length) ? MONTH_MAP[m] : 0;
        } catch (Throwable t) {
            // fallback: rough mapping from the civil month (March ~ Nisan)
            int g = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH);
            tz = (g + 10) % 12;
        }
        return WidgetData.S_TZIRUF + " " + WidgetData.TZ_MONTH[tz] + ":  " + WidgetData.TZ_NAME[tz];
    }

    // ---------------------------------------------------------------
    // Wheel bitmap: 72 segments coloured by sefira, the current one lit,
    // the name itself (with nikud) in the centre.
    // ---------------------------------------------------------------

    static Bitmap drawWheel(int highlight) {
        final int size = WHEEL_PX;
        final float c = size / 2f;
        final float R = size / 2f - 6f;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int hiColor = WidgetData.COLOR[highlight];

        // backdrop disc
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(c, c, R, new int[]{0xFF1B1630, 0xFF0B0A14},
                new float[]{0.2f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(c, c, R, p);
        p.setShader(null);

        // segments
        float sweep = 360f / COUNT;
        float rIn = R * 0.66f, rOut = R * 0.93f;
        float mid = (rIn + rOut) / 2f;
        RectF ring = new RectF(c - mid, c - mid, c + mid, c + mid);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.BUTT);
        float[] hsv = new float[3];
        for (int i = 0; i < COUNT; i++) {
            if (i == highlight) continue;
            Color.colorToHSV(WidgetData.COLOR[i], hsv);
            hsv[1] = Math.min(1f, hsv[1] * 0.9f);
            hsv[2] = 0.42f;
            p.setColor(Color.HSVToColor(hsv));
            p.setStrokeWidth(rOut - rIn);
            canvas.drawArc(ring, i * sweep - 90f + 0.5f, sweep - 1f, false, p);
        }

        // highlighted segment: longer, bright, glowing
        float hIn = R * 0.60f, hOut = R * 0.99f, hMid = (hIn + hOut) / 2f;
        RectF hRing = new RectF(c - hMid, c - hMid, c + hMid, c + hMid);
        p.setStrokeWidth(hOut - hIn);
        p.setColor(hiColor);
        p.setShadowLayer(14f, 0, 0, hiColor);
        canvas.drawArc(hRing, highlight * sweep - 90f - 0.6f, sweep + 1.2f, false, p);
        p.setColor(0xFFFFF4D0);
        p.setStrokeWidth((hOut - hIn) * 0.35f);
        p.setShadowLayer(8f, 0, 0, 0xFFFFE9A8);
        canvas.drawArc(hRing, highlight * sweep - 90f + 0.6f, sweep - 1.2f, false, p);
        p.clearShadowLayer();

        // gold rims
        p.setStrokeWidth(2.5f);
        p.setColor(0xFFB8892B);
        canvas.drawCircle(c, c, R * 0.955f, p);
        p.setStrokeWidth(1.5f);
        p.setColor(0x99B8892B);
        canvas.drawCircle(c, c, R * 0.635f, p);

        // centre disc
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(c, c, R * 0.6f, new int[]{0xFF241C3A, 0xFF0D0B18},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(c, c, R * 0.585f, p);
        p.setShader(null);

        // sefira (small, top)
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        p.setColor(hiColor);
        p.setTextSize(R * 0.115f);
        canvas.drawText(WidgetData.SEFIRA[highlight], c, c - R * 0.33f, p);

        // the name
        String name = WidgetData.NIKUD[highlight];
        p.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        p.setColor(0xFFF3D78E);
        float ts = R * 0.40f;
        p.setTextSize(ts);
        float maxW = R * 0.98f;
        float tw = p.measureText(name);
        if (tw > maxW) { ts = ts * maxW / tw; p.setTextSize(ts); }
        p.setShadowLayer(10f, 0, 0, 0xAADAA520);
        canvas.drawText(name, c, c + ts * 0.30f, p);
        p.clearShadowLayer();

        // number (small, bottom)
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        p.setColor(0xFF8A7A5A);
        p.setTextSize(R * 0.105f);
        canvas.drawText((highlight + 1) + " / " + COUNT, c, c + R * 0.42f, p);

        return bmp;
    }

    // ---------------------------------------------------------------
    // Automatic advance every UPDATE_MINUTES (does not wake the phone)
    // ---------------------------------------------------------------

    static void scheduleNext(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long interval = UPDATE_MINUTES * 60 * 1000L;
        long next = System.currentTimeMillis() + interval;
        try {
            am.setWindow(AlarmManager.RTC, next, 60 * 1000L, broadcast(ctx, ACTION_TICK, 1));
        } catch (Exception e) {
            am.set(AlarmManager.RTC, next, broadcast(ctx, ACTION_TICK, 1));
        }
    }

    static void cancelSchedule(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(broadcast(ctx, ACTION_TICK, 1));
    }
}

package com.chaiproductions.shemotkodesh;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.RemoteViews;

public class ShemotWidget extends AppWidgetProvider {

    static final int    UPDATE_MINUTES = 5;
    static final String ACTION_UPDATE  = "com.chaiproductions.shemotkodesh.WIDGET_UPDATE";

    // ══════════════════════════════════════════════════════════
    // ע"ב שמות הקודש
    // ══════════════════════════════════════════════════════════
    static final String[] NAMES = {
        "וְהוּ","יְלִי","סִיט","עַלְמ","מַהַשׁ","לַלְהּ","אַכְאּ","כַּהַתּ",
        "הַזִּי","אַלַד","לַאוּ","הַהַעּ","יֶזְלָ","מְבַהּ","הַרִי","הַקַמּ",
        "לַאַוּ","כַּלִי","לֵוּוּ","פַּהַלּ","נְלַכּ","יִיַיּ","מֶלַהּ","חַהַוּ",
        "נְתַהּ","הַאַאּ","יֶרַתּ","שַׁאַהּ","רִיִיּ","אוֹמּ","לֶכַבּ","וַשַׁרּ",
        "יֵחוּ","לֵהַחּ","כְּוַקּ","מֶנַדּ","אַנִיּ","חַעַמּ","רֵהַעּ","יֵיזָ",
        "הַהַהּ","מִיכַּ","וֵוַלּ","יְלַהּ","סַאַלּ","עַרִיּ","עַשַׁלּ","מִיהַ",
        "וֵהוּ","דַּנִי","הַחַשׁ","עוֹמּ","נֵנַאּ","נִיתָ","מְבַהּ","פּוֹיּ",
        "נֶמַמּ","יֵיַלּ","הַרַחּ","מִצְרַ","וַמְבּ","יֶהַהּ","עַנַוּ","מַחִי",
        "דַּמַבּ","מַנַקּ","אַיַעּ","חַבוּ","רַאַהּ","יַבַמּ","הַיַיּ","מוּמּ"
    };

    static final String[] SEGULOT = {
        "הגנה מכל עין הרע","עזרה בשידוכין","הצלה מסכנות","הצלחה בעסקים",
        "ריפוי מחלות","חיזוק הזיכרון","קרבת השכינה","ברכה בפרנסה",
        "שמירה במסעות","ביטול גזרות","ניצחון על אויבים","עזרה בלידה",
        "הצלחה בתפילה","ריפוי עצב ודכאון","חיבה בעיני הבריות","ביטול כישוף",
        "שמירת הנשמה","עזרה בלמידה","חיזוק אמונה","שמירה מרוחות רעות",
        "פרנסה טובה","קרבת השם יתברך","ביטול דין","עזרה בחולאים",
        "הצלחה בכל עסקיו","שמירה בלילה","עזרה במשפט","חן ורצון",
        "קרבה לצדיקים","שמיעת תפילה","ברכה בבית","שמחה בלב",
        "ריפוי עיניים","שמירה מאש","ברכה בילדים","כיבוד אב ואם",
        "שמירה מלשון הרע","חיזוק בתשובה","עזרה בצרה","שמירה מקנאה",
        "ביטול עצב","חיזוק קדושה","ברכה בנסיעות","עזרה בזיכרון",
        "פתיחת לב","ביטול קנאה","שמחה בחיים","ריפוי מחלות נסתרות",
        "שמירה כללית","ביטול גאוה","חן בעיני הבריות","עזרה ביום הדין",
        "שמירה מנגעים","חיזוק תפילה","ברכה במעשה ידיים","עזרת צדיקים",
        "ניצחון ברוחניות","שמחה וחיות","ביטול עין הרע","ברכה בזרע",
        "שמירה מדאגות","עזרה בחולי","שמחת החיים","ריפוי מתאונות",
        "ביטול קללות","עזרה בנישואין","שמירה מסכנות","ברכה כללית",
        "הארת הפנים","ביטול מחלוקת","שמירה כללית","כוונת הלב"
    };

    // צירופי י-ה-ו-ה לפי חודשים (ניסן=0 … אדר=11)
    // ממופה לחודש גרגוריאני בקירוב (מרץ=0 … פברואר=11)
    static final String[] TZIRUFIM = {
        "יהוה","יהוה","יוהה","יוהה","ויהה","ויהה",
        "הויה","הוהי","היוה","היהו","ההיו","ההוי"
    };
    static final String[] MONTH_NAMES_HEB = {
        "ניסן","אייר","סיון","תמוז","אב","אלול",
        "תשרי","חשון","כסלו","טבת","שבט","אדר"
    };

    // ══════════════════════════════════════════════════════════
    // AppWidgetProvider overrides
    // ══════════════════════════════════════════════════════════

    @Override
    public void onUpdate(Context ctx, AppWidgetManager awm, int[] ids) {
        for (int id : ids) updateWidget(ctx, awm, id);
        scheduleNext(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACTION_UPDATE.equals(intent.getAction())) {
            AppWidgetManager awm = AppWidgetManager.getInstance(ctx);
            int[] ids = awm.getAppWidgetIds(new ComponentName(ctx, ShemotWidget.class));
            for (int id : ids) updateWidget(ctx, awm, id);
            scheduleNext(ctx);
        }
    }

    @Override
    public void onEnabled(Context ctx)  { scheduleNext(ctx); }

    @Override
    public void onDisabled(Context ctx) { cancelSchedule(ctx); }

    // ══════════════════════════════════════════════════════════
    // Widget update logic
    // ══════════════════════════════════════════════════════════

    static void updateWidget(Context ctx, AppWidgetManager awm, int widgetId) {
        // אינדקס השם הנוכחי — מחזור כל UPDATE_MINUTES דקות
        long mins    = System.currentTimeMillis() / (1000L * 60);
        int  nameIdx = (int)((mins / UPDATE_MINUTES) % 72);

        // צירוף חודשי — לפי חודש גרגוריאני בקירוב לעברי
        // ניסן מתחיל סביב מרץ: מרץ(2)=חודש 0, אפריל(3)=1 …
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int gregMonth = cal.get(java.util.Calendar.MONTH); // 0 = Jan
        // הזזה: ניסן~מרץ, כלומר gregMonth 2 → tziruf 0
        int tzIdx = (gregMonth + 10) % 12;
        String tziruf    = TZIRUFIM[tzIdx];
        String monthName = MONTH_NAMES_HEB[tzIdx];

        // בנה RemoteViews
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_shemot);

        // גלגל — ציור bitmap
        Bitmap wheel = drawWheel(nameIdx);
        rv.setImageViewBitmap(R.id.widget_wheel, wheel);

        // טקסטים
        rv.setTextViewText(R.id.widget_tziruf, "צירוף " + monthName + ":  " + tziruf);
        rv.setTextViewText(R.id.widget_name,   NAMES[nameIdx]);
        rv.setTextViewText(R.id.widget_segula, SEGULOT[nameIdx]);
        rv.setTextViewText(R.id.widget_num,    "שם " + (nameIdx + 1) + " מתוך ע“ב");

        // לחיצה — פתח אפליקציה
        Intent open = new Intent(ctx, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_root, pi);

        awm.updateAppWidget(widgetId, rv);
    }

    // ══════════════════════════════════════════════════════════
    // ציור גלגל השמות — Bitmap 300×300
    // ══════════════════════════════════════════════════════════

    static Bitmap drawWheel(int highlight) {
        final int SIZE = 300, CX = 150, CY = 150;
        Bitmap bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // רקע עגול כהה
        p.setColor(Color.parseColor("#09090f"));
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(CX, CY, 148, p);

        // 72 קשתות
        float sweep = 360f / 72f;
        RectF ring  = new RectF(12, 12, 288, 288);

        for (int i = 0; i < 72; i++) {
            float startAngle = i * sweep - 90f;
            if (i == highlight) {
                // שם נוכחי — זהב בוהק
                p.setColor(Color.parseColor("#DAA520"));
                p.setShadowLayer(8f, 0, 0, Color.parseColor("#DAA52099"));
            } else {
                // שאר השמות — גוון קשת בענן כהה
                float hue = (float) i / 72f * 360f;
                float[] hsv = {hue, 0.60f, 0.26f};
                p.setColor(Color.HSVToColor(hsv));
                p.clearShadowLayer();
            }
            p.setStyle(Paint.Style.FILL);
            canvas.drawArc(ring, startAngle, sweep - 0.5f, true, p);
        }

        // גבול חיצוני
        p.clearShadowLayer();
        p.setColor(Color.parseColor("#3a2500"));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f);
        canvas.drawCircle(CX, CY, 147, p);

        // עיגול פנימי — מסתיר מרכז הקשתות
        p.setColor(Color.parseColor("#09090f"));
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(CX, CY, 82, p);

        // גבול פנימי
        p.setColor(Color.parseColor("#2a1800"));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        canvas.drawCircle(CX, CY, 82, p);

        // מספר השם במרכז
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.parseColor("#DAA520"));
        p.setTextSize(26f);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(highlight + 1), CX, CY + 10, p);

        // קו קטן מתחת למספר
        p.setColor(Color.parseColor("#3a2500"));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        canvas.drawLine(CX - 20, CY + 18, CX + 20, CY + 18, p);

        return bmp;
    }

    // ══════════════════════════════════════════════════════════
    // AlarmManager — עדכון כל UPDATE_MINUTES דקות
    // ══════════════════════════════════════════════════════════

    static void scheduleNext(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ctx, ShemotWidget.class);
        intent.setAction(ACTION_UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 1,
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long intervalMs = UPDATE_MINUTES * 60 * 1000L;
        long now        = System.currentTimeMillis();
        long next       = ((now / intervalMs) + 1) * intervalMs;

        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
        } catch (SecurityException e) {
            // fallback אם אין הרשאת exact alarm
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
        }
    }

    static void cancelSchedule(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ctx, ShemotWidget.class);
        intent.setAction(ACTION_UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 1,
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }
}

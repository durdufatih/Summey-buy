package com.tenis.koc;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Kod ile arayüz oluşturma yardımcıları. */
public class Ui {
    public static final int GREEN = 0xFF2E7D32, GREEN_DARK = 0xFF1B5E20, GREEN_LIGHT = 0xFFE8F5E9,
        LIME = 0xFFEEF7B5, ORANGE = 0xFFE65100, TEXT = 0xFF1C1B1F, TEXT2 = 0xFF5F6368, CARD = 0xFFF4F7F2;

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static int scoreColor(int score) {
        if (score >= 80) return 0xFF43A047;
        if (score >= 60) return 0xFFFFA000;
        return 0xFFE53935;
    }

    public static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    public static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams matchWrap(Context c, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(c, topDp);
        return p;
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    public static TextView body(Context c, String s) {
        return text(c, s, 14, TEXT, false);
    }

    public static TextView small(Context c, String s) {
        return text(c, s, 12.5f, TEXT2, false);
    }

    /** Başlıklı kart; içerik eklemek için dönen kutuya addView yapılır. */
    public static LinearLayout card(Context c, LinearLayout parent, String title) {
        LinearLayout card = vbox(c);
        int p = dp(c, 16);
        card.setPadding(p, p, p, p);
        card.setBackground(rounded(CARD, dp(c, 18)));
        if (title != null) card.addView(text(c, title, 17, TEXT, true));
        parent.addView(card, matchWrap(c, 14));
        return card;
    }

    public static Button button(Context c, String label, int bg, int fg) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(fg);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setBackground(rounded(bg, dp(c, 14)));
        b.setStateListAnimator(null);
        int p = dp(c, 14);
        b.setPadding(p, p, p, p);
        return b;
    }

    public static TextView badge(Context c, int score, int sizeDp) {
        TextView t = text(c, String.valueOf(score), sizeDp / 2.6f, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(scoreColor(score));
        t.setBackground(g);
        int s = dp(c, sizeDp);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return t;
    }

    public static ProgressBar bar(Context c, float v, int color) {
        ProgressBar pb = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(1000);
        pb.setProgress(Math.round(Math.max(0, Math.min(1, v)) * 1000));
        pb.setProgressTintList(ColorStateList.valueOf(color));
        return pb;
    }

    private static View stat(Context c, String label, String value) {
        LinearLayout b = vbox(c);
        b.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = dp(c, 8);
        b.setPadding(p, p + 2, p, p + 2);
        b.setBackground(rounded(LIME, dp(c, 14)));
        TextView v = text(c, value, 22, 0xFF2B3300, true);
        v.setGravity(Gravity.CENTER);
        TextView l = text(c, label, 12, 0xFF2B3300, false);
        l.setGravity(Gravity.CENTER);
        b.addView(v);
        b.addView(l);
        return b;
    }

    /** Özet içeriğini (istatistikler, metin, türler, hatalar, güçlü yönler, sıradaki odak) ekler. */
    public static void summary(Context c, LinearLayout box, Coaching.Summary s) {
        LinearLayout row = hbox(c);
        LinearLayout.LayoutParams lp = weight(1);
        LinearLayout.LayoutParams lp2 = weight(1);
        lp2.leftMargin = dp(c, 8);
        LinearLayout.LayoutParams lp3 = weight(1);
        lp3.leftMargin = dp(c, 8);
        row.addView(stat(c, "Vuruş", String.valueOf(s.strokeCount)), lp);
        row.addView(stat(c, "Ort. puan", String.valueOf(s.avgScore)), lp2);
        if (s.scoreDelta != null) {
            int d = s.scoreDelta;
            row.addView(stat(c, "Değişim", d > 0 ? "+" + d : String.valueOf(d)), lp3);
        } else {
            row.addView(stat(c, "Süre", s.minutes + " dk"), lp3);
        }
        box.addView(row, matchWrap(c, 10));
        box.addView(body(c, s.text), matchWrap(c, 10));

        if (!s.types.isEmpty()) {
            box.addView(text(c, "Vuruş türleri", 14, TEXT, true), matchWrap(c, 12));
            for (Coaching.TypeStat t : s.types) {
                LinearLayout r = hbox(c);
                r.addView(body(c, t.type.label), weight(1));
                TextView n = small(c, t.count + " vuruş   ");
                r.addView(n);
                r.addView(badge(c, t.avgScore, 34));
                box.addView(r, matchWrap(c, 6));
            }
        }

        if (!s.faults.isEmpty()) {
            box.addView(text(c, "Dikkat etmen gerekenler", 14, TEXT, true), matchWrap(c, 12));
            for (int i = 0; i < Math.min(5, s.faults.size()); i++) {
                Coaching.FaultStat f = s.faults.get(i);
                LinearLayout r = hbox(c);
                r.addView(body(c, f.fault.title), weight(1));
                r.addView(text(c, "%" + Math.round(f.rate * 100), 14, TEXT, true));
                box.addView(r, matchWrap(c, 8));
                box.addView(bar(c, f.rate, ORANGE), matchWrap(c, 2));
            }
        }

        if (!s.strengths.isEmpty()) {
            box.addView(text(c, "Güçlü yönlerin", 14, TEXT, true), matchWrap(c, 12));
            for (String st : s.strengths) box.addView(text(c, "✓ " + st, 14, GREEN, false), matchWrap(c, 4));
        }

        if (s.nextFocus != null) {
            Fault f = s.nextFocus;
            LinearLayout fc = vbox(c);
            int p = dp(c, 12);
            fc.setPadding(p, p, p, p);
            fc.setBackground(rounded(0xFFC8E6C9, dp(c, 14)));
            fc.addView(text(c, "Sıradaki odak: " + f.title, 14.5f, 0xFF0B3D0F, true));
            fc.addView(text(c, f.tip, 14, 0xFF0B3D0F, false), matchWrap(c, 4));
            fc.addView(text(c, "Çalışma: " + f.drill + " — " + f.drillDetail, 12.5f, 0xFF0B3D0F, false), matchWrap(c, 4));
            box.addView(fc, matchWrap(c, 12));
        }
    }
}

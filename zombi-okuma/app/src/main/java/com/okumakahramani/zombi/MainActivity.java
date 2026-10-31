package com.okumakahramani.zombi;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Ana menü: seviye seçimi ve kazanılan yıldızlar. */
public class MainActivity extends Activity {

    static final String PREFS = "progress";

    private LinearLayout levelList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF1B1446, 0xFF3D2A7A, 0xFF6A4BB0}));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = Ui.dp(this, 20);
        root.setPadding(p, p, p, p);
        scroll.addView(root);

        root.addView(Ui.label(this, "🧟  Zombi Okuma Kahramanı  🦸", 30, 0xFFFFFFFF, true));
        TextView sub = Ui.label(this,
                "Panodaki heceyi, kelimeyi ya da cümleyi SESLİ oku. Doğru okursan zombi yok olur!",
                16, 0xFFE6DAFB, false);
        sub.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 14));
        root.addView(sub);

        levelList = new LinearLayout(this);
        levelList.setOrientation(LinearLayout.VERTICAL);
        root.addView(levelList, new LinearLayout.LayoutParams(Ui.dp(this, 460),
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView note = Ui.label(this,
                "Yetişkin için: Amaç hız değil, doğru seslendirme. Çocuk okurken zombi durur. "
                        + "Ses tanıma kelimeyi duyamazsa 👍 düğmesine BASILI tutarak onaylayabilirsiniz.",
                13, 0xFFCDC3EA, false);
        note.setPadding(0, Ui.dp(this, 14), 0, 0);
        root.addView(note, new LinearLayout.LayoutParams(Ui.dp(this, 520),
                LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Ui.immersive(this);
        buildLevels();
    }

    private void buildLevels() {
        levelList.removeAllViews();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        for (int i = 0; i < WordBank.LEVELS.length; i++) {
            WordBank.Level lv = WordBank.LEVELS[i];
            int stars = prefs.getInt("stars_" + i, 0);
            String starText = Ui.stars(stars);
            TextView b = Ui.button(this,
                    lv.emoji + "  " + (i + 1) + ". " + lv.title + "    " + starText + "\n" + lv.subtitle,
                    20, lv.color, Ui.INK);
            final int level = i;
            b.setOnClickListener(v -> {
                Intent it = new Intent(this, GameActivity.class);
                it.putExtra(GameActivity.EXTRA_LEVEL, level);
                startActivity(it);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Ui.dp(this, 12);
            levelList.addView(b, lp);
        }
    }
}

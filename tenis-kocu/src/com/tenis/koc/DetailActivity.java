package com.tenis.koc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.List;

/** Tek bir antrenmanın özeti ve vuruş vuruş listesi. */
public class DetailActivity extends Activity {

    public static void open(Context c, long id) {
        Intent i = new Intent(c, DetailActivity.class);
        i.putExtra("id", id);
        c.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final Store store = Store.get(this);
        final Session s = store.find(getIntent().getLongExtra("id", -1));
        if (s == null) {
            finish();
            return;
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout page = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        page.setPadding(p, p, p, p * 2);
        scroll.addView(page);

        page.addView(Ui.text(this, "Antrenman özeti", 22, Ui.TEXT, true));
        page.addView(Ui.small(this, Store.formatDateTime(s.startedAt) + " · " + s.source + " · " + Store.formatClock(s.durationMs)));

        List<Session> previous = new ArrayList<Session>();
        for (Session o : store.between(s.startedAt - 14L * 86400000L, s.startedAt)) if (o.id != s.id) previous.add(o);
        LinearLayout sc = Ui.card(this, page, "Özet");
        Ui.summary(this, sc, Coaching.summarize(java.util.Collections.singletonList(s), previous, "Bu"));

        LinearLayout lc = Ui.card(this, page, "Vuruş vuruş");
        for (StrokeResult st : s.strokes) {
            LinearLayout row = Ui.hbox(this);
            row.addView(Ui.badge(this, st.score, 38));
            LinearLayout col = Ui.vbox(this);
            col.setPadding(Ui.dp(this, 10), 0, 0, 0);
            col.addView(Ui.text(this, Store.formatClock(st.timeMs) + " · " + st.type.label, 14.5f, Ui.TEXT, true));
            List<String> names = new ArrayList<String>();
            for (Fault f : st.faults) names.add(f.title);
            col.addView(Ui.small(this, names.isEmpty() ? "Temiz vuruş" : TextUtils.join(", ", names)));
            row.addView(col, Ui.weight(1));
            lc.addView(row, Ui.matchWrap(this, 10));
        }

        Button home = Ui.button(this, "Ana sayfaya dön", Ui.GREEN, Color.WHITE);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        page.addView(home, Ui.matchWrap(this, 16));
        Button del = Ui.button(this, "Bu antrenmanı sil", 0xFFFFEBEE, 0xFFC62828);
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(DetailActivity.this)
                    .setTitle("Antrenman silinsin mi?")
                    .setPositiveButton("Sil", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            store.delete(s.id);
                            finish();
                        }
                    })
                    .setNegativeButton("Vazgeç", null)
                    .show();
            }
        });
        page.addView(del, Ui.matchWrap(this, 8));
        setContentView(scroll);
    }
}

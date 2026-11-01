package com.tenis.koc;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 1, REQ_VIDEO = 2;
    private static final int TAB_HOME = 0, TAB_HISTORY = 1, TAB_SETTINGS = 2;

    private Store store;
    private int tab = TAB_HOME;
    private ScrollView scroll;
    private Button[] tabs = new Button[3];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = Store.get(this);
        LinearLayout root = Ui.vbox(this);
        root.setBackgroundColor(Color.WHITE);
        scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bar = Ui.hbox(this);
        bar.setBackgroundColor(0xFFF0F4EE);
        int p = Ui.dp(this, 6);
        bar.setPadding(p, p, p, p);
        String[] names = {"🏠  Bugün", "📋  Geçmiş", "⚙  Ayarlar"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            Button t = Ui.button(this, names[i], Color.TRANSPARENT, Ui.TEXT);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tab = idx;
                    render();
                }
            });
            LinearLayout.LayoutParams lp = Ui.weight(1);
            lp.leftMargin = lp.rightMargin = Ui.dp(this, 3);
            bar.addView(t, lp);
            tabs[i] = t;
        }
        root.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        for (int i = 0; i < 3; i++) {
            boolean sel = i == tab;
            tabs[i].setBackground(Ui.rounded(sel ? 0xFFC8E6C9 : Color.TRANSPARENT, Ui.dp(this, 14)));
            tabs[i].setTextColor(sel ? Ui.GREEN_DARK : Ui.TEXT2);
        }
        LinearLayout page = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        page.setPadding(p, p, p, p * 2);
        if (tab == TAB_HOME) home(page);
        else if (tab == TAB_HISTORY) history(page);
        else settings(page);
        scroll.removeAllViews();
        scroll.addView(page);
        scroll.scrollTo(0, 0);
    }

    // ---------------- Bugün ----------------

    private void home(final LinearLayout page) {
        long today = Store.startOfToday();
        page.addView(Ui.text(this, "🎾 Tenis Koçu", 26, Ui.TEXT, true));
        page.addView(Ui.text(this, new SimpleDateFormat("d MMMM EEEE", Store.TR).format(new Date()), 15, Ui.TEXT2, false));

        Button live = Ui.button(this, "▶  Kamerayla antrenmanı başlat", Ui.GREEN, Color.WHITE);
        live.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLive();
            }
        });
        page.addView(live, Ui.matchWrap(this, 16));
        Button video = Ui.button(this, "Kayıtlı videodan analiz et", 0xFFE8F5E9, Ui.GREEN_DARK);
        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("video/*");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(i, "Video seç"), REQ_VIDEO);
            }
        });
        page.addView(video, Ui.matchWrap(this, 8));

        // Günün planı
        List<Session> history = store.between(today - 14L * 86400000L, today);
        Coaching.DailyPlan plan = Coaching.plan(history);
        final String dk = Store.dayKey(System.currentTimeMillis());
        int done = 0;
        for (Coaching.PlanItem it : plan.items) if (store.isChecked(dk + ":" + it.id)) done++;
        LinearLayout pc = Ui.card(this, page, "Günün planı · " + plan.totalMinutes() + " dk · " + done + "/" + plan.items.size());
        pc.addView(Ui.text(this, plan.title, 15, Ui.GREEN, true), Ui.matchWrap(this, 6));
        pc.addView(Ui.body(this, "🎯 " + plan.goal), Ui.matchWrap(this, 4));
        if (!plan.personalized) {
            pc.addView(Ui.small(this, "Henüz yeterli veri yok; bu genel bir plan. Birkaç antrenmandan sonra plan senin hatalarına göre hazırlanır."),
                Ui.matchWrap(this, 4));
        }
        pc.addView(Ui.bar(this, plan.items.isEmpty() ? 0 : (float) done / plan.items.size(), Ui.GREEN), Ui.matchWrap(this, 8));
        for (final Coaching.PlanItem it : plan.items) {
            LinearLayout row = Ui.hbox(this);
            row.setGravity(Gravity.TOP);
            final CheckBox cb = new CheckBox(this);
            cb.setChecked(store.isChecked(dk + ":" + it.id));
            row.addView(cb);
            LinearLayout col = Ui.vbox(this);
            LinearLayout head = Ui.hbox(this);
            final TextView title = Ui.text(this, it.title, 15, Ui.TEXT, true);
            if (cb.isChecked()) title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            head.addView(title, Ui.weight(1));
            head.addView(Ui.small(this, it.minutes + " dk"));
            col.addView(head);
            col.addView(Ui.small(this, it.detail));
            LinearLayout.LayoutParams lp = Ui.weight(1);
            lp.topMargin = Ui.dp(this, 6);
            row.addView(col, lp);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton b, boolean c) {
                    store.toggle(dk + ":" + it.id);
                    int f = title.getPaintFlags();
                    title.setPaintFlags(c ? f | Paint.STRIKE_THRU_TEXT_FLAG : f & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }
            });
            pc.addView(row, Ui.matchWrap(this, 6));
        }
        if (!plan.cues.isEmpty()) {
            pc.addView(Ui.text(this, "Odak noktaları", 14, Ui.TEXT, true), Ui.matchWrap(this, 10));
            for (String cue : plan.cues) pc.addView(Ui.small(this, "• " + cue), Ui.matchWrap(this, 4));
        }

        // Günün özeti
        LinearLayout sc = Ui.card(this, page, "Günün özeti");
        List<Session> todays = store.between(today, Long.MAX_VALUE);
        if (todays.isEmpty()) {
            sc.addView(Ui.body(this, "Bugün henüz analiz edilmiş antrenman yok. Kamerayı başlat; her vuruşu izleyip hatalarını ve güçlü yönlerini burada özetleyeceğim."),
                Ui.matchWrap(this, 6));
        } else {
            Ui.summary(this, sc, Coaching.summarize(todays, history, "Bugün"));
        }

        LinearLayout tc = Ui.card(this, page, "📱 Kamera nasıl yerleştirilmeli?");
        tc.addView(Ui.body(this,
            "• Telefonu 4–6 m uzağa, bel hizasında tripoda (veya fileye/çantaya) dik yerleştir.\n" +
                "• Baştan ayağa tüm vücudun kadrajda olsun; iskelet çizgileri yeşil görünmeli.\n" +
                "• En doğru sonuç için kamerayı tam önüne ya da arkana koy (omuz dönüşü böyle ölçülür).\n" +
                "• Ayarlardan sağ/sol el seçimini yapmayı unutma."), Ui.matchWrap(this, 6));
    }

    private void startLive() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, LiveActivity.class));
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code == REQ_CAMERA) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(new Intent(this, LiveActivity.class));
            } else {
                Toast.makeText(this, "Analiz için kamera izni gerekli", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == REQ_VIDEO && result == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            Intent i = new Intent(this, VideoActivity.class);
            i.setData(uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        }
    }

    // ---------------- Geçmiş ----------------

    private void history(LinearLayout page) {
        page.addView(Ui.text(this, "Geçmiş antrenmanlar", 22, Ui.TEXT, true));
        List<Session> list = store.sessions();
        if (list.isEmpty()) page.addView(Ui.body(this, "Henüz kayıtlı antrenman yok."), Ui.matchWrap(this, 12));
        for (final Session s : list) {
            LinearLayout row = Ui.hbox(this);
            int p = Ui.dp(this, 14);
            row.setPadding(p, p, p, p);
            row.setBackground(Ui.rounded(Ui.CARD, Ui.dp(this, 16)));
            row.addView(Ui.badge(this, s.avgScore(), 46));
            LinearLayout col = Ui.vbox(this);
            col.setPadding(Ui.dp(this, 12), 0, 0, 0);
            col.addView(Ui.text(this, Store.formatDateTime(s.startedAt), 15, Ui.TEXT, true));
            col.addView(Ui.small(this, s.source + " · " + s.strokes.size() + " vuruş · " + Store.formatClock(s.durationMs)));
            List<Coaching.FaultStat> fs = Coaching.faultStats(s.strokes);
            if (!fs.isEmpty()) col.addView(Ui.text(this, "Ana hata: " + fs.get(0).fault.title, 12.5f, Ui.ORANGE, false));
            row.addView(col, Ui.weight(1));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DetailActivity.open(MainActivity.this, s.id);
                }
            });
            page.addView(row, Ui.matchWrap(this, 10));
        }
    }

    // ---------------- Ayarlar ----------------

    private void settings(LinearLayout page) {
        page.addView(Ui.text(this, "Ayarlar", 22, Ui.TEXT, true));

        LinearLayout hc = Ui.card(this, page, "Vuruş elin");
        LinearLayout hr = Ui.hbox(this);
        final boolean left = store.leftHanded();
        Button rb = Ui.button(this, "Sağ el", left ? Color.WHITE : 0xFFC8E6C9, Ui.TEXT);
        Button lb = Ui.button(this, "Sol el", left ? 0xFFC8E6C9 : Color.WHITE, Ui.TEXT);
        rb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                store.setLeftHanded(false);
                render();
            }
        });
        lb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                store.setLeftHanded(true);
                render();
            }
        });
        LinearLayout.LayoutParams l2 = Ui.weight(1);
        l2.leftMargin = Ui.dp(this, 8);
        hr.addView(rb, Ui.weight(1));
        hr.addView(lb, l2);
        hc.addView(hr, Ui.matchWrap(this, 8));
        hc.addView(Ui.small(this, "Forehand / backhand ayrımı ve kol açıları bu ele göre hesaplanır."), Ui.matchWrap(this, 6));

        LinearLayout vc = Ui.card(this, page, "Sesli koç");
        Switch sw = new Switch(this);
        sw.setText("Her vuruştan sonra sesli ipucu ver  ");
        sw.setTextSize(15);
        sw.setChecked(store.voice());
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean c) {
                store.setVoice(c);
            }
        });
        vc.addView(sw, Ui.matchWrap(this, 8));
        vc.addView(Ui.small(this, "Telefon uzakta dururken ekrana bakmana gerek kalmaz: “Dizlerini bük”, “Vuruşu bitir” gibi kısa komutlar duyarsın."),
            Ui.matchWrap(this, 6));

        LinearLayout sc = Ui.card(this, page, "Vuruş algılama hassasiyeti");
        final TextView desc = Ui.small(this, sensText(store.sensitivity()));
        SeekBar sb = new SeekBar(this);
        sb.setMax(2);
        sb.setProgress(store.sensitivity() - 1);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int v, boolean user) {
                store.setSensitivity(v + 1);
                desc.setText(sensText(v + 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        sc.addView(sb, Ui.matchWrap(this, 10));
        sc.addView(desc, Ui.matchWrap(this, 6));

        LinearLayout ic = Ui.card(this, page, "Nasıl çalışır?");
        ic.addView(Ui.small(this,
            "Uygulama kameradan vücudundaki 33 noktayı (omuz, dirsek, bilek, kalça, diz, ayak bileği…) cihaz üzerinde tespit eder; " +
                "internet gerekmez, görüntü hiçbir yere gönderilmez. Bilek hızından vuruş anını yakalar, vuruş türünü (forehand, backhand, " +
                "servis) belirler ve diz bükme, omuz dönüşü, hazırlık, temas mesafesi, vuruş sonu, duruş ve baş sabitliğini ölçer.\n\n" +
                "Not: Top ve raket takip edilmez; topun dışarı/fileye gitmesi değil, tekniğin değerlendirilir. " +
                "Sonuçlar tahminidir ve bir antrenörün yerini tutmaz."), Ui.matchWrap(this, 6));

        Button clear = Ui.button(this, "Tüm antrenman verilerini sil", 0xFFFFEBEE, 0xFFC62828);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Emin misin?")
                    .setMessage("Tüm geçmiş antrenmanlar silinecek. Bu işlem geri alınamaz.")
                    .setPositiveButton("Sil", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            store.clear();
                            render();
                        }
                    })
                    .setNegativeButton("Vazgeç", null)
                    .show();
            }
        });
        page.addView(clear, Ui.matchWrap(this, 16));
    }

    private static String sensText(int s) {
        if (s == 1) return "Düşük: yalnız hızlı ve tam vuruşlar sayılır (maç temposu).";
        if (s == 3) return "Yüksek: yavaş vuruşlar ve gölge çalışmaları da sayılır.";
        return "Orta: çoğu antrenman için önerilen ayar.";
    }
}

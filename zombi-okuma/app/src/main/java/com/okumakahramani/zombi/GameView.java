package com.okumakahramani.zombi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Oyun sahnesi: üstte hece/kelime panosu, altta kahramana doğru yürüyen zombi.
 * Zombi, çocuk dinlenirken durur (amaç hız değil, doğru okuma).
 */
public class GameView extends View {

    public interface Listener {
        void onZombieReachedHero();
        void onZombieVanished();
    }

    private static final int Z_GONE = 0, Z_WALK = 1, Z_DYING = 2, Z_KNOCK = 3, Z_ENTER = 4;

    private static final float HERO_X = 0.11f;
    private static final float HIT_X = 0.25f;
    private static final float START_X = 0.92f;

    /** Pano için PDF'teki pastel renkler. */
    private static final int[] CHIP_COLORS = {
            0xFFCDE8FB, 0xFFF6C6E0, 0xFFFFEFA8, 0xFFD9F5D0, 0xFFE6DAFB, 0xFFFDD9B5
    };
    private static final int INK = 0xFF26335A;

    private Listener listener;

    private final Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint skyPaint = new Paint();
    private final RectF rect = new RectF();
    private final RectF boardRect = new RectF();

    private final Random random = new Random();
    private final ArrayList<Particle> particles = new ArrayList<>();
    private float[] starXs = new float[0], starYs = new float[0];

    private WordItem word;
    private float speed = 0.03f;
    private int zState = Z_GONE;
    private float zombieX = 1.2f;
    private float zStateTime;

    private boolean listening;
    private boolean paused;
    private float rms;
    private float time;
    private float boardTime;
    private float heroShake;
    private long lastNanos;

    private String message;
    private int messageColor = Color.WHITE;
    private float messageTime = 99f;

    private static final class Particle {
        String glyph;
        float x, y, vx, vy, life, maxLife, size, rot, vrot;
    }

    public GameView(Context context) {
        super(context);
        emojiPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setTextAlign(Paint.Align.CENTER);
        strokePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        setFocusable(true);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    // ------------------------------------------------------------------ kontrol

    /** Yeni zombi + yeni kelime. */
    public void setWord(WordItem w, float speed) {
        this.word = w;
        this.speed = speed;
        zombieX = 1.15f;
        zState = Z_ENTER;
        zStateTime = 0;
        boardTime = 0;
        invalidate();
    }

    public void setListening(boolean l) {
        listening = l;
        if (!l) rms = 0;
    }

    public void setRms(float db) {
        // RMS genelde -2..10 dB arası gelir
        rms = Math.max(0f, Math.min(1f, (db + 2f) / 12f));
    }

    public void setPaused(boolean p) {
        paused = p;
        lastNanos = 0;
        invalidate();
    }

    public void replayBoard() {
        boardTime = 0;
    }

    public boolean isZombieActive() {
        return zState == Z_WALK || zState == Z_ENTER || zState == Z_KNOCK;
    }

    public void killZombie() {
        if (zState == Z_GONE || zState == Z_DYING) return;
        zState = Z_DYING;
        zStateTime = 0;
        float cx = zombieX * getWidth();
        float cy = groundY() - zombieSize() * 0.4f;
        String[] glyphs = {"⭐", "✨", "🌟", "💥", "⭐"};
        for (int i = 0; i < 26; i++) {
            spawn(glyphs[random.nextInt(glyphs.length)], cx, cy, zombieSize() * 0.22f, 1.1f);
        }
    }

    public void showMessage(String text, int color) {
        message = text;
        messageColor = color;
        messageTime = 0;
    }

    // ------------------------------------------------------------------ çizim

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        skyPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF1B1446, 0xFF3D2A7A, 0xFF6A4BB0}, null, Shader.TileMode.CLAMP));
        int n = 40;
        starXs = new float[n];
        starYs = new float[n];
        for (int i = 0; i < n; i++) {
            starXs[i] = random.nextFloat();
            starYs[i] = random.nextFloat() * 0.6f;
        }
    }

    private float groundY() {
        return getHeight() * 0.84f;
    }

    private float zombieSize() {
        return getHeight() * 0.26f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : (now - lastNanos) / 1e9f;
        lastNanos = now;
        dt = Math.min(dt, 0.05f);
        if (!paused) update(dt);

        drawBackground(canvas);
        drawHero(canvas);
        drawZombie(canvas);
        drawParticles(canvas);
        drawBoard(canvas);
        drawMessage(canvas);

        postInvalidateOnAnimation();
    }

    private void update(float dt) {
        time += dt;
        boardTime += dt;
        messageTime += dt;
        heroShake = Math.max(0f, heroShake - dt);

        switch (zState) {
            case Z_ENTER:
                zombieX -= 0.5f * dt;
                if (zombieX <= START_X) {
                    zombieX = START_X;
                    zState = Z_WALK;
                }
                break;
            case Z_WALK:
                if (!listening) zombieX -= speed * dt;
                if (zombieX <= HIT_X) {
                    zState = Z_KNOCK;
                    zStateTime = 0;
                    heroShake = 0.6f;
                    float hx = HIT_X * getWidth();
                    for (int i = 0; i < 10; i++) {
                        spawn("💥", hx, groundY() - zombieSize() * 0.5f, zombieSize() * 0.2f, 0.7f);
                    }
                    if (listener != null) post(new Runnable() { @Override public void run() { if (listener != null) listener.onZombieReachedHero(); } });
                }
                break;
            case Z_KNOCK:
                zStateTime += dt;
                zombieX += 1.4f * dt;
                if (zombieX >= START_X) {
                    zombieX = START_X;
                    zState = Z_WALK;
                }
                break;
            case Z_DYING:
                zStateTime += dt;
                if (zStateTime > 0.8f) {
                    zState = Z_GONE;
                    if (listener != null) post(new Runnable() { @Override public void run() { if (listener != null) listener.onZombieVanished(); } });
                }
                break;
            default:
                break;
        }

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life += dt;
            if (p.life >= p.maxLife) {
                it.remove();
                continue;
            }
            p.vy += getHeight() * 1.2f * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.rot += p.vrot * dt;
        }
    }

    private void spawn(String glyph, float x, float y, float size, float life) {
        Particle p = new Particle();
        p.glyph = glyph;
        p.x = x;
        p.y = y;
        double a = random.nextDouble() * Math.PI * 2;
        float v = getHeight() * (0.5f + random.nextFloat() * 0.9f);
        p.vx = (float) Math.cos(a) * v;
        p.vy = (float) Math.sin(a) * v - getHeight() * 0.6f;
        p.size = size * (0.6f + random.nextFloat() * 0.8f);
        p.maxLife = life * (0.7f + random.nextFloat() * 0.6f);
        p.vrot = (random.nextFloat() - 0.5f) * 720f;
        particles.add(p);
    }

    private void drawBackground(Canvas c) {
        int w = getWidth(), h = getHeight();
        c.drawRect(0, 0, w, h, skyPaint);

        fillPaint.setColor(Color.WHITE);
        for (int i = 0; i < starXs.length; i++) {
            float tw = 0.5f + 0.5f * (float) Math.sin(time * 2 + i);
            fillPaint.setAlpha((int) (80 + 150 * tw));
            c.drawCircle(starXs[i] * w, starYs[i] * h, h * 0.004f + tw * h * 0.002f, fillPaint);
        }
        fillPaint.setAlpha(255);

        emojiPaint.setAlpha(255);
        emojiPaint.setTextSize(h * 0.13f);
        c.drawText("🌕", w * 0.06f, h * 0.30f, emojiPaint);

        // zemin
        float gy = groundY();
        fillPaint.setColor(0xFF2F7D3A);
        c.drawRect(0, gy, w, h, fillPaint);
        fillPaint.setColor(0xFF49A85A);
        c.drawRect(0, gy, w, gy + h * 0.025f, fillPaint);

        // mezar taşları ve ağaçlar
        emojiPaint.setTextSize(h * 0.09f);
        emojiPaint.setAlpha(170);
        c.drawText("🪦", w * 0.42f, gy + h * 0.01f, emojiPaint);
        c.drawText("🪦", w * 0.67f, gy + h * 0.01f, emojiPaint);
        emojiPaint.setTextSize(h * 0.16f);
        c.drawText("🌳", w * 0.86f, gy + h * 0.02f, emojiPaint);
        emojiPaint.setAlpha(255);
    }

    private void drawHero(Canvas c) {
        int h = getHeight();
        float size = h * 0.24f;
        float x = HERO_X * getWidth();
        if (heroShake > 0) x += (float) Math.sin(heroShake * 60) * h * 0.015f;
        float y = groundY() + size * 0.05f + (float) Math.sin(time * 3) * h * 0.005f;

        // dinlerken ses dalgaları
        if (listening) {
            strokePaint.setColor(0xFFFFE066);
            strokePaint.setStrokeWidth(h * 0.008f);
            for (int i = 0; i < 3; i++) {
                float ph = (time * 1.5f + i / 3f) % 1f;
                float r = size * (0.45f + ph * 0.6f + rms * 0.25f);
                strokePaint.setAlpha((int) (220 * (1f - ph)));
                c.drawCircle(x, y - size * 0.4f, r, strokePaint);
            }
            strokePaint.setAlpha(255);
        }

        emojiPaint.setTextSize(size);
        c.drawText("🦸", x, y, emojiPaint);

        // kalkan
        emojiPaint.setTextSize(size * 0.45f);
        c.drawText(listening ? "🎤" : "🛡️", x + size * 0.45f, y - size * 0.25f, emojiPaint);
    }

    private void drawZombie(Canvas c) {
        if (zState == Z_GONE || word == null) return;
        int h = getHeight();
        float size = zombieSize();
        float x = zombieX * getWidth();
        boolean moving = (zState == Z_WALK && !listening) || zState == Z_ENTER;
        float bob = moving ? (float) Math.abs(Math.sin(time * 5)) * h * 0.015f : 0f;
        float y = groundY() + size * 0.06f - bob;

        c.save();
        float scale = 1f;
        int alpha = 255;
        if (zState == Z_DYING) {
            float t = Math.min(1f, zStateTime / 0.8f);
            scale = 1f - t;
            alpha = (int) (255 * (1f - t));
            c.rotate(t * 540f, x, y - size * 0.4f);
        } else {
            float tilt = moving ? (float) Math.sin(time * 5) * 6f : 0f;
            if (listening) tilt = (float) Math.sin(time * 2) * 3f; // dinlerken "donmuş" titreme
            c.rotate(tilt, x, y);
        }
        c.scale(scale, scale, x, y - size * 0.4f);

        emojiPaint.setAlpha(alpha);
        emojiPaint.setTextSize(size);
        c.drawText("🧟", x, y, emojiPaint);

        if (listening && zState == Z_WALK) {
            emojiPaint.setTextSize(size * 0.35f);
            c.drawText("❄️", x + size * 0.35f, y - size * 0.75f, emojiPaint);
        }
        emojiPaint.setAlpha(255);
        c.restore();
    }

    private void drawParticles(Canvas c) {
        for (Particle p : particles) {
            float t = p.life / p.maxLife;
            emojiPaint.setAlpha((int) (255 * (1f - t)));
            emojiPaint.setTextSize(p.size);
            c.save();
            c.rotate(p.rot, p.x, p.y);
            c.drawText(p.glyph, p.x, p.y, emojiPaint);
            c.restore();
        }
        emojiPaint.setAlpha(255);
    }

    // ------------------------------------------------------------------ pano

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static float easeInOut(float t) {
        return t * t * (3 - 2 * t);
    }

    private void drawBoard(Canvas c) {
        if (word == null) return;
        int w = getWidth(), h = getHeight();

        float top = h * 0.17f;
        float boardH = h * 0.28f;
        float maxWidth = w * 0.84f;

        float textSize = h * 0.15f;
        float total;
        while (true) {
            textPaint.setTextSize(textSize);
            total = measureBoard(textSize, 1f);
            if (total <= maxWidth || textSize < 14f) break;
            textSize *= 0.92f;
        }
        textPaint.setTextSize(textSize);

        // animasyon zamanlaması: her hece sırayla zıplar, sonra heceler birleşir
        float hopEach = 0.45f;
        int n = word.syllableCount;
        float mergeStart = n * hopEach + 0.25f;
        float merge = word.isSingleSyllable() ? 1f : easeInOut(clamp01((boardTime - mergeStart) / 0.6f));

        float boardW = Math.max(measureBoard(textSize, 1f - merge), w * 0.3f) + textSize * 0.9f;
        float left = (w - boardW) / 2f;
        boardRect.set(left, top, left + boardW, top + boardH);

        // gölge + pano
        fillPaint.setColor(0x55000000);
        rect.set(boardRect);
        rect.offset(0, h * 0.012f);
        c.drawRoundRect(rect, h * 0.04f, h * 0.04f, fillPaint);
        fillPaint.setColor(Color.WHITE);
        c.drawRoundRect(boardRect, h * 0.04f, h * 0.04f, fillPaint);

        float pad = textSize * 0.16f;
        float chipH = textSize * 1.3f;
        float splitGap = textSize * 0.45f;
        float wordGap = textSize * 0.45f;
        float gap = splitGap * (1f - merge);

        float x = (w - measureBoard(textSize, 1f - merge)) / 2f;
        float cy = top + boardH / 2f;
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = cy - (fm.ascent + fm.descent) / 2f;

        int k = 0;
        for (int wi = 0; wi < word.syllables.length; wi++) {
            String[] syl = word.syllables[wi];
            float wordStart = x;
            for (int si = 0; si < syl.length; si++) {
                float sw = textPaint.measureText(syl[si]) + pad * 2;
                float hopT = clamp01((boardTime - k * hopEach) / 0.4f);
                float hop = (hopT > 0 && hopT < 1) ? -(float) Math.sin(Math.PI * hopT) * textSize * 0.3f : 0f;
                boolean visible = boardTime >= k * hopEach || word.isSingleSyllable();
                float appear = word.isSingleSyllable() ? 1f : clamp01((boardTime - k * hopEach) / 0.2f);

                if (visible) {
                    fillPaint.setColor(CHIP_COLORS[k % CHIP_COLORS.length]);
                    fillPaint.setAlpha((int) (255 * appear));
                    rect.set(x, cy - chipH / 2 + hop, x + sw, cy + chipH / 2 + hop);
                    c.drawRoundRect(rect, textSize * 0.22f, textSize * 0.22f, fillPaint);
                    textPaint.setColor(INK);
                    textPaint.setAlpha((int) (255 * appear));
                    c.drawText(syl[si], x + sw / 2f, baseline + hop, textPaint);
                    textPaint.setAlpha(255);
                }
                x += sw;
                if (si < syl.length - 1) {
                    // heceler arası tire (birleşince kaybolur)
                    if (gap > 2f && visible) {
                        textPaint.setColor(0xFFB0B6C8);
                        textPaint.setAlpha((int) (255 * (1f - merge)));
                        c.drawText("-", x + gap / 2f, baseline, textPaint);
                        textPaint.setAlpha(255);
                    }
                    x += gap;
                }
                k++;
            }
            // birleşince kelimenin etrafına çerçeve
            if (merge > 0.05f && syl.length > 1) {
                strokePaint.setColor(INK);
                strokePaint.setAlpha((int) (200 * merge));
                strokePaint.setStrokeWidth(Math.max(2f, textSize * 0.05f));
                rect.set(wordStart - 3, cy - chipH / 2 - 3, x + 3, cy + chipH / 2 + 3);
                c.drawRoundRect(rect, textSize * 0.25f, textSize * 0.25f, strokePaint);
                strokePaint.setAlpha(255);
            }
            x += wordGap;
        }

        // ipucu
        textPaint.setTextSize(h * 0.035f);
        textPaint.setColor(0xFF8A90A8);
        c.drawText("panoya dokun: heceleri tekrar göster", w / 2f, top + boardH - h * 0.012f, textPaint);
    }

    /** Pano genişliği; splitFactor=1 heceler ayrık, 0 birleşik. */
    private float measureBoard(float textSize, float splitFactor) {
        float pad = textSize * 0.16f;
        float gap = textSize * 0.45f * splitFactor;
        float wordGap = textSize * 0.45f;
        float total = 0;
        for (int wi = 0; wi < word.syllables.length; wi++) {
            String[] syl = word.syllables[wi];
            for (int si = 0; si < syl.length; si++) {
                total += textPaint.measureText(syl[si]) + pad * 2;
                if (si < syl.length - 1) total += gap;
            }
            if (wi < word.syllables.length - 1) total += wordGap;
        }
        return total;
    }

    private void drawMessage(Canvas c) {
        if (message == null || messageTime > 2.2f) return;
        int h = getHeight();
        float alpha = messageTime < 1.7f ? 1f : 1f - (messageTime - 1.7f) / 0.5f;
        float pop = messageTime < 0.2f ? 0.6f + 2f * messageTime : 1f;
        float size = h * 0.075f * pop;
        float y = h * 0.53f;

        strokePaint.setTextSize(size);
        strokePaint.setStrokeWidth(size * 0.16f);
        strokePaint.setColor(0xFF1B1446);
        strokePaint.setAlpha((int) (255 * alpha));
        c.drawText(message, getWidth() / 2f, y, strokePaint);
        strokePaint.setAlpha(255);

        textPaint.setTextSize(size);
        textPaint.setColor(messageColor);
        textPaint.setAlpha((int) (255 * alpha));
        c.drawText(message, getWidth() / 2f, y, textPaint);
        textPaint.setAlpha(255);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN && boardRect.contains(e.getX(), e.getY())) {
            replayBoard();
            performClick();
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}

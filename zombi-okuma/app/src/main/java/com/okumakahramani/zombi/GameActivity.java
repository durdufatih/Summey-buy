package com.okumakahramani.zombi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Oyun ekranı: konuşma tanıma, seslendirme ve oyun akışı. */
public class GameActivity extends Activity implements GameView.Listener, RecognitionListener {

    public static final String EXTRA_LEVEL = "level";
    private static final int REQ_MIC = 7;
    private static final int MAX_HEARTS = 3;
    private static final String[] PRAISE = {"Aferin!", "Harika!", "Süpersin!", "Bravo!", "Çok güzel okudun!"};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private GameView gameView;
    private TextView heartsView, scoreView, progressView, micButton;
    private LinearLayout endPanel;
    private TextView endTitle, endText, endNext;

    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ttsReady;

    private int levelIndex;
    private WordBank.Level level;
    private List<WordItem> words = new ArrayList<>();
    private int index;
    private int hearts;
    private int score;
    private boolean listening;
    private boolean answered;
    private boolean gameOver;
    private boolean hintUsed;

    // ------------------------------------------------------------------ yaşam döngüsü

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        levelIndex = Math.max(0, Math.min(WordBank.LEVELS.length - 1,
                getIntent().getIntExtra(EXTRA_LEVEL, 0)));
        level = WordBank.LEVELS[levelIndex];

        buildUi();

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    int r = tts.setLanguage(new Locale("tr", "TR"));
                    ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
                    tts.setSpeechRate(0.8f);
                }
            }
        });

        startRound();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Ui.immersive(this);
        if (!gameOver) gameView.setPaused(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelListening();
        gameView.setPaused(true);
        if (tts != null) tts.stop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    // ------------------------------------------------------------------ arayüz

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1B1446);

        // Oyun alanı + üst bilgi çubuğu
        FrameLayout stage = new FrameLayout(this);
        gameView = new GameView(this);
        gameView.setListener(this);
        stage.addView(gameView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(this, 8);
        top.setPadding(p, p, p, 0);

        TextView back = Ui.button(this, "⬅", 18, 0xCCFFFFFF, Ui.INK);
        back.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });
        top.addView(back);

        heartsView = Ui.label(this, "", 22, Color.WHITE, true);
        heartsView.setPadding(Ui.dp(this, 12), 0, 0, 0);
        top.addView(heartsView);

        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView title = Ui.label(this, level.emoji + " " + level.title, 18, 0xFFE6DAFB, true);
        top.addView(title);

        View spacer2 = new View(this);
        top.addView(spacer2, new LinearLayout.LayoutParams(0, 1, 1f));

        progressView = Ui.label(this, "", 18, Color.WHITE, true);
        progressView.setPadding(0, 0, Ui.dp(this, 14), 0);
        top.addView(progressView);
        scoreView = Ui.label(this, "", 20, 0xFFFFE066, true);
        top.addView(scoreView);

        stage.addView(top, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        // Oyun sonu paneli
        endPanel = new LinearLayout(this);
        endPanel.setOrientation(LinearLayout.VERTICAL);
        endPanel.setGravity(Gravity.CENTER);
        endPanel.setBackground(Ui.rounded(0xF2FFFFFF, Ui.dp(this, 24)));
        int ep = Ui.dp(this, 20);
        endPanel.setPadding(ep, ep, ep, ep);
        endTitle = Ui.label(this, "", 30, Ui.INK, true);
        endPanel.addView(endTitle);
        endText = Ui.label(this, "", 18, Ui.INK, false);
        endText.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 14));
        endPanel.addView(endText);
        LinearLayout endButtons = new LinearLayout(this);
        endButtons.setOrientation(LinearLayout.HORIZONTAL);
        endButtons.setGravity(Gravity.CENTER);
        TextView again = Ui.button(this, "🔁 Tekrar Oyna", 18, 0xFFD9F5D0, Ui.INK);
        again.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { startRound(); } });
        TextView menu = Ui.button(this, "🏠 Menü", 18, 0xFFCDE8FB, Ui.INK);
        menu.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });
        endNext = Ui.button(this, "➡ Sonraki Görev", 18, 0xFFFFEFA8, Ui.INK);
        endNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(GameActivity.this, GameActivity.class);
                it.putExtra(EXTRA_LEVEL, levelIndex + 1);
                startActivity(it);
                finish();
            }
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-2, -2);
        blp.setMargins(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        endButtons.addView(again, blp);
        endButtons.addView(menu, blp);
        endButtons.addView(endNext, blp);
        endPanel.addView(endButtons);
        endPanel.setVisibility(View.GONE);
        stage.addView(endPanel, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

        root.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Alt kontrol çubuğu
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setBackgroundColor(0xFF140F33);
        int cp = Ui.dp(this, 6);
        controls.setPadding(cp, cp, cp, cp);

        TextView listen = Ui.button(this, "🔊 Dinle", 18, 0xFFCDE8FB, Ui.INK);
        listen.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { speakWord(); } });

        micButton = Ui.label(this, "", 20, Color.WHITE, true);
        micButton.setClickable(true);
        micButton.setElevation(Ui.dp(this, 6));
        micButton.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { onMicClicked(); } });

        TextView approve = Ui.button(this, "👍", 20, 0xFFD9F5D0, Ui.INK);
        approve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(GameActivity.this, "Yetişkin onayı için basılı tutun", Toast.LENGTH_SHORT).show();
            }
        });
        approve.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!answered && !gameOver && gameView.isZombieActive()) {
                    cancelListening();
                    onCorrect();
                }
                return true;
            }
        });

        LinearLayout.LayoutParams side = new LinearLayout.LayoutParams(-2, -2);
        side.setMargins(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        controls.addView(listen, side);
        controls.addView(micButton, new LinearLayout.LayoutParams(Ui.dp(this, 260), Ui.dp(this, 64)));
        controls.addView(approve, side);

        root.addView(controls, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
        updateMic();
    }

    private void updateHud() {
        StringBuilder h = new StringBuilder();
        for (int i = 0; i < MAX_HEARTS; i++) h.append(i < hearts ? "❤️" : "🤍");
        heartsView.setText(h.toString());
        scoreView.setText("⭐ " + score);
        progressView.setText(Math.min(index + 1, words.size()) + " / " + words.size());
    }

    private void updateMic() {
        if (listening) {
            micButton.setText("👂 Dinliyorum… oku!");
            micButton.setBackground(Ui.rounded(0xFFE5484D, Ui.dp(this, 32)));
        } else {
            micButton.setText("🎤  SESLİ OKU");
            micButton.setBackground(Ui.rounded(0xFF22A45D, Ui.dp(this, 32)));
            micButton.setScaleX(1f);
            micButton.setScaleY(1f);
        }
    }

    // ------------------------------------------------------------------ oyun akışı

    private void startRound() {
        handler.removeCallbacksAndMessages(null);
        cancelListening();
        words = level.buildRound(random);
        index = 0;
        hearts = MAX_HEARTS;
        score = 0;
        gameOver = false;
        endPanel.setVisibility(View.GONE);
        gameView.setPaused(false);
        showWord();
        gameView.showMessage("Zombiler geliyor! Panodakini oku!", 0xFFFFE066);
    }

    private void showWord() {
        answered = false;
        hintUsed = false;
        gameView.setWord(words.get(index), level.speed);
        updateHud();
    }

    private WordItem current() {
        return words.get(index);
    }

    private void onCorrect() {
        if (answered || gameOver) return;
        answered = true;
        score += hintUsed ? 5 : 10;
        gameView.killZombie();
        String praise = PRAISE[random.nextInt(PRAISE.length)];
        gameView.showMessage("🎉 " + praise, 0xFF8CF0A0);
        speak(praise);
        updateHud();
    }

    private void onWrong(String heard) {
        String msg = heard == null || heard.trim().isEmpty()
                ? "Seni duyamadım 🙂 Tekrar dene!"
                : "Duyduğum: \"" + heard + "\" — tekrar dene!";
        gameView.showMessage(msg, 0xFFFFC2C2);
    }

    @Override
    public void onZombieVanished() {
        if (gameOver) return;
        index++;
        if (index >= words.size()) {
            win();
        } else {
            showWord();
        }
    }

    @Override
    public void onZombieReachedHero() {
        if (answered || gameOver) return;
        hearts--;
        updateHud();
        if (hearts <= 0) {
            lose();
        } else {
            gameView.showMessage("Ah! Bir can gitti. Yavaş yavaş oku!", 0xFFFFC2C2);
        }
    }

    private void win() {
        gameOver = true;
        cancelListening();
        int stars = hearts >= 3 ? 3 : (hearts == 2 ? 2 : 1);
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        String key = "stars_" + levelIndex;
        if (stars > prefs.getInt(key, 0)) prefs.edit().putInt(key, stars).apply();

        endTitle.setText("🏆 Tebrikler, Okuma Kahramanı!");
        endText.setText(Ui.stars(stars)
                + "\nTüm zombileri yendin! Puanın: " + score);
        endNext.setVisibility(levelIndex + 1 < WordBank.LEVELS.length ? View.VISIBLE : View.GONE);
        endPanel.setVisibility(View.VISIBLE);
        speak("Tebrikler! Okuma kahramanı oldun!");
    }

    private void lose() {
        gameOver = true;
        cancelListening();
        gameView.setPaused(true);
        endTitle.setText("🧟 Zombiler bu sefer kazandı");
        endText.setText("Hata yapabilirim. Tekrar denerim.\nHer gün biraz daha öğrenirim! (Puan: " + score + ")");
        endNext.setVisibility(View.GONE);
        endPanel.setVisibility(View.VISIBLE);
    }

    // ------------------------------------------------------------------ seslendirme

    private void speak(String text) {
        if (tts != null && ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "msg");
        }
    }

    private void speakWord() {
        if (gameOver || words.isEmpty()) return;
        if (!ttsReady) {
            gameView.showMessage("Türkçe seslendirme bulunamadı", 0xFFFFC2C2);
            return;
        }
        if (listening) cancelListening();
        hintUsed = true;
        gameView.replayBoard();
        speak(current().text);
    }

    // ------------------------------------------------------------------ konuşma tanıma

    private void onMicClicked() {
        if (gameOver || answered) return;
        if (listening) {
            if (recognizer != null) recognizer.stopListening();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        startListening();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                gameView.showMessage("Mikrofon izni gerekli 🎤", 0xFFFFC2C2);
            }
        }
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            gameView.showMessage("Ses tanıma yok. 👍 ile onaylayın.", 0xFFFFC2C2);
            return;
        }
        if (tts != null) tts.stop();
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(this);
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);

        listening = true;
        gameView.setListening(true);
        updateMic();
        try {
            recognizer.startListening(intent);
        } catch (RuntimeException e) {
            resetRecognizer();
            listening = false;
            gameView.setListening(false);
            updateMic();
            gameView.showMessage("Tekrar dene", 0xFFFFC2C2);
        }
    }

    private void cancelListening() {
        if (recognizer != null && listening) recognizer.cancel();
        listening = false;
        if (gameView != null) gameView.setListening(false);
        if (micButton != null) updateMic();
    }

    private void resetRecognizer() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }

    private void endListeningUi() {
        listening = false;
        gameView.setListening(false);
        updateMic();
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        gameView.showMessage("Şimdi oku! 📖", 0xFFFFE066);
    }

    @Override
    public void onBeginningOfSpeech() {}

    @Override
    public void onRmsChanged(float rmsdB) {
        gameView.setRms(rmsdB);
        if (listening) {
            float s = 1f + Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f)) * 0.08f;
            micButton.setScaleX(s);
            micButton.setScaleY(s);
        }
    }

    @Override
    public void onBufferReceived(byte[] buffer) {}

    @Override
    public void onEndOfSpeech() {}

    @Override
    public void onError(int error) {
        boolean wasListening = listening;
        endListeningUi();
        if (!wasListening || answered || gameOver) return;
        switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                onWrong(null);
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                gameView.showMessage("Mikrofon izni gerekli 🎤", 0xFFFFC2C2);
                break;
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            case SpeechRecognizer.ERROR_SERVER:
                gameView.showMessage("İnternet bağlantısını kontrol et", 0xFFFFC2C2);
                break;
            default:
                resetRecognizer();
                gameView.showMessage("Tekrar dene 🙂", 0xFFFFC2C2);
                break;
        }
    }

    @Override
    public void onResults(Bundle results) {
        boolean wasListening = listening;
        endListeningUi();
        if (!wasListening || answered || gameOver) return;
        ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (SpeechMatcher.findMatch(current().text, list) != null) {
            onCorrect();
        } else {
            onWrong(list != null && !list.isEmpty() ? list.get(0) : null);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        if (!listening || answered || gameOver) return;
        ArrayList<String> list = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (SpeechMatcher.findMatch(current().text, list) != null) {
            cancelListening();
            onCorrect();
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {}
}

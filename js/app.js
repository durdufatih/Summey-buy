document.addEventListener("DOMContentLoaded", () => {
  /* ================= Sekmeler ================= */
  const tabButtons = document.querySelectorAll(".tab-btn");
  tabButtons.forEach((btn) => {
    btn.addEventListener("click", () => {
      tabButtons.forEach((b) => b.classList.toggle("active", b === btn));
      document.querySelectorAll(".tab-panel").forEach((panel) => {
        panel.classList.toggle("active", panel.id === "tab-" + btn.dataset.tab);
      });
    });
  });

  /* ================= Sesli çeviri ================= */
  const micBtn = document.getElementById("mic-btn");
  const micLabel = document.getElementById("mic-label");
  const voiceStatus = document.getElementById("voice-status");
  const voiceSource = document.getElementById("voice-source");
  const voiceTarget = document.getElementById("voice-target");
  const speakToggle = document.getElementById("speak-toggle");
  const clearVoiceBtn = document.getElementById("clear-voice-btn");

  let interimEl = null;

  function clearPlaceholder(el) {
    el.querySelector(".placeholder")?.remove();
  }

  function appendLine(el, text, className) {
    clearPlaceholder(el);
    const p = document.createElement("p");
    if (className) p.className = className;
    p.textContent = text;
    el.appendChild(p);
    el.scrollTop = el.scrollHeight;
    return p;
  }

  const recognizer = Speech.createRecognizer({
    onInterim(text) {
      if (!interimEl && text) interimEl = appendLine(voiceSource, "", "interim");
      if (interimEl) {
        interimEl.textContent = text;
        if (!text) {
          interimEl.remove();
          interimEl = null;
        }
      }
    },
    async onFinal(text) {
      if (interimEl) {
        interimEl.remove();
        interimEl = null;
      }
      appendLine(voiceSource, text);
      const targetLine = appendLine(voiceTarget, "…", "interim");
      try {
        const translated = await Translator.translate(text);
        targetLine.textContent = translated;
        targetLine.className = "";
        if (speakToggle.checked) Speech.speak(translated, "en-US");
      } catch {
        targetLine.textContent = "⚠️ Çeviri alınamadı (bağlantıyı kontrol edin).";
      }
    },
    onStateChange(listening) {
      micBtn.classList.toggle("listening", listening);
      voiceStatus.classList.toggle("listening", listening);
      micLabel.textContent = listening ? "Dinlemeyi Durdur" : "Dinlemeyi Başlat";
      voiceStatus.textContent = listening
        ? "🔴 Dinleniyor… Türkçe konuşun."
        : "Mikrofon kapalı. Başlatmak için butona basın.";
    },
    onError(err) {
      voiceStatus.textContent =
        err === "not-allowed"
          ? "⚠️ Mikrofon izni verilmedi. Tarayıcı ayarlarından izin verin."
          : "⚠️ Ses tanıma hatası: " + err;
    }
  });

  if (!recognizer) {
    document.getElementById("speech-support-warning").classList.remove("hidden");
    micBtn.disabled = true;
  } else {
    micBtn.addEventListener("click", () => {
      recognizer.listening ? recognizer.stop() : recognizer.start();
    });
  }

  clearVoiceBtn.addEventListener("click", () => {
    voiceSource.innerHTML = '<span class="placeholder">Konuştuklarınız burada görünecek…</span>';
    voiceTarget.innerHTML = '<span class="placeholder">Çeviri burada görünecek…</span>';
    interimEl = null;
  });

  /* ================= Yazılı çeviri ================= */
  const textSource = document.getElementById("text-source");
  const textTarget = document.getElementById("text-target");
  const translateBtn = document.getElementById("translate-btn");
  const speakTextBtn = document.getElementById("speak-text-btn");
  const copyBtn = document.getElementById("copy-btn");
  const textStatus = document.getElementById("text-status");

  let debounceTimer = null;
  let lastTranslation = "";

  async function doTranslateText() {
    const text = textSource.value.trim();
    if (!text) {
      textTarget.innerHTML = '<span class="placeholder">Çeviri burada görünecek…</span>';
      lastTranslation = "";
      return;
    }
    textStatus.textContent = "Çevriliyor…";
    try {
      lastTranslation = await Translator.translate(text);
      textTarget.textContent = lastTranslation;
      textStatus.textContent = "";
    } catch {
      textStatus.textContent = "⚠️ Çeviri alınamadı, tekrar deneyin.";
    }
  }

  translateBtn.addEventListener("click", doTranslateText);
  textSource.addEventListener("input", () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(doTranslateText, 700);
  });
  speakTextBtn.addEventListener("click", () => Speech.speak(lastTranslation, "en-US"));
  copyBtn.addEventListener("click", async () => {
    if (!lastTranslation) return;
    await navigator.clipboard.writeText(lastTranslation);
    textStatus.textContent = "Kopyalandı ✓";
    setTimeout(() => (textStatus.textContent = ""), 1500);
  });

  /* ================= Seviyeli pratik ================= */
  const levelSelect = document.getElementById("level-select");
  const paragraphList = document.getElementById("paragraph-list");
  const practiceArea = document.getElementById("practice-area");
  const practiceTitle = document.getElementById("practice-title");
  const practiceLevelBadge = document.getElementById("practice-level-badge");
  const sentenceList = document.getElementById("sentence-list");
  const backToList = document.getElementById("back-to-list");
  const supportMode = document.getElementById("support-mode");

  let activeLevel = PARAGRAPH_LEVELS[0];

  function renderLevels() {
    levelSelect.innerHTML = "";
    PARAGRAPH_LEVELS.forEach((level) => {
      const chip = document.createElement("button");
      chip.className = "level-chip" + (level === activeLevel ? " active" : "");
      chip.textContent = level.name;
      chip.addEventListener("click", () => {
        activeLevel = level;
        renderLevels();
        renderParagraphs();
        showList();
      });
      levelSelect.appendChild(chip);
    });
  }

  function renderParagraphs() {
    paragraphList.innerHTML = "";
    activeLevel.paragraphs.forEach((para) => {
      const card = document.createElement("div");
      card.className = "paragraph-card";
      card.innerHTML =
        '<span class="level-badge">' + activeLevel.name + "</span>" +
        "<h3>" + para.title + "</h3>" +
        "<p>" + para.sentences.length + " cümle · " + para.sentences[0].tr + "…</p>";
      card.addEventListener("click", () => openParagraph(para));
      paragraphList.appendChild(card);
    });
  }

  function showList() {
    practiceArea.classList.add("hidden");
    paragraphList.classList.remove("hidden");
  }

  function makeRevealRow({ flag, text, lang, masked, revealLabel }) {
    const row = document.createElement("div");
    row.className = "sentence-row";

    const tag = document.createElement("span");
    tag.className = "lang-tag";
    tag.textContent = flag;

    const span = document.createElement("span");
    span.className = "sentence-text" + (masked ? " masked" : "");
    span.textContent = text;

    row.append(tag, span);

    const actions = [];

    if (masked) {
      const revealBtn = document.createElement("button");
      revealBtn.className = "ghost-btn";
      revealBtn.textContent = revealLabel;
      revealBtn.addEventListener("click", () => {
        span.classList.remove("masked");
        revealBtn.remove();
      });
      actions.push(revealBtn);
    }

    const listenBtn = document.createElement("button");
    listenBtn.className = "ghost-btn";
    listenBtn.textContent = flag === "🇹🇷" ? "🔊 Dinle" : "🔊 İngilizce Dinle";
    listenBtn.addEventListener("click", () => Speech.speak(text, lang));
    actions.push(listenBtn);

    return { row, actions };
  }

  function openParagraph(para) {
    paragraphList.classList.add("hidden");
    practiceArea.classList.remove("hidden");
    practiceTitle.textContent = para.title;
    practiceLevelBadge.textContent = activeLevel.name;
    sentenceList.innerHTML = "";

    const supported = supportMode.checked;

    para.sentences.forEach((sentence, index) => {
      const card = document.createElement("div");
      card.className = "sentence-card";

      const num = document.createElement("div");
      num.className = "sentence-num";
      num.textContent = "Cümle " + (index + 1) + " / " + para.sentences.length;
      card.appendChild(num);

      const trRow = makeRevealRow({
        flag: "🇹🇷",
        text: sentence.tr,
        lang: "tr-TR",
        masked: supported,
        revealLabel: "👁 Cümleyi Göster"
      });
      const enRow = makeRevealRow({
        flag: "🇬🇧",
        text: sentence.en,
        lang: "en-US",
        masked: supported,
        revealLabel: "💡 Çeviriyi Göster"
      });

      card.append(trRow.row, enRow.row);

      const actionsWrap = document.createElement("div");
      actionsWrap.className = "sentence-actions";
      actionsWrap.append(...trRow.actions, ...enRow.actions);
      card.appendChild(actionsWrap);

      sentenceList.appendChild(card);
    });
  }

  backToList.addEventListener("click", showList);
  supportMode.addEventListener("change", showList);

  renderLevels();
  renderParagraphs();
});

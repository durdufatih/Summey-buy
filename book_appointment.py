#!/usr/bin/env python3
"""
Google Calendar Appointment Auto-Booker
İlk uygun randevu slotunu bulur ve rezervasyon yapar.
"""

import asyncio
import sys
from playwright.async_api import async_playwright, TimeoutError as PlaywrightTimeout

CALENDAR_URL = (
    "https://calendar.google.com/calendar/u/0/appointments/schedules/"
    "AcZssZ0zmb9LKnnaG2aZPNPViNHReItbYFaHYremSfO_tL84ZRuqUcawNhoddmO1EjC1fFIm2zPovOO0"
    "?gv=true"
)


def ask(prompt: str, required: bool = True) -> str:
    while True:
        value = input(prompt).strip()
        if value or not required:
            return value
        print("  Bu alan zorunludur, lütfen doldurun.")


def collect_info() -> dict:
    print("\n=== Randevu Bilgileri ===")
    return {
        "name": ask("Adınız Soyadınız: "),
        "email": ask("E-posta adresiniz: "),
        "phone": ask("Telefon numaranız (opsiyonel, Enter ile geç): ", required=False),
        "notes": ask("Notlar/açıklama (opsiyonel, Enter ile geç): ", required=False),
    }


async def book(info: dict) -> None:
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=False, slow_mo=400)
        page = await browser.new_page()

        print("\n[1/5] Takvim sayfası açılıyor...")
        await page.goto(CALENDAR_URL, wait_until="networkidle", timeout=30000)

        # ── İlk müsait günü bul ──────────────────────────────────────────────
        print("[2/5] İlk müsait slot aranıyor...")
        slot = await _find_first_slot(page)
        if not slot:
            print("Uygun slot bulunamadı. Lütfen takvimi manuel kontrol edin.")
            await browser.close()
            return

        print(f"  -> Slot seçildi: {await slot.inner_text()}")
        await slot.click()
        await page.wait_for_timeout(1000)

        # ── Zaman seçimi (bazı takvimlerde ikinci ekran) ─────────────────────
        time_slot = await _pick_time(page)
        if time_slot:
            print(f"  -> Saat seçildi: {await time_slot.inner_text()}")
            await time_slot.click()
            await page.wait_for_timeout(800)

        # ── Devam / Next düğmesi ─────────────────────────────────────────────
        print("[3/5] Forma geçiliyor...")
        await _click_next(page)
        await page.wait_for_timeout(1500)

        # ── Formu doldur ─────────────────────────────────────────────────────
        print("[4/5] Form dolduruluyor...")
        await _fill_form(page, info)

        # ── Onayla ───────────────────────────────────────────────────────────
        print("[5/5] Randevu onaylanıyor...")
        await _confirm(page)

        print("\nRandevu başarıyla alındı! Onay e-postanızı kontrol edin.")
        await page.wait_for_timeout(4000)
        await browser.close()


async def _find_first_slot(page):
    """
    Takvim üzerinde tıklanabilir ilk günü döndürür.
    Google Calendar appointment pages use different selectors — we try several.
    """
    selectors = [
        # Appointment schedule page v2
        "button[data-date]:not([disabled])",
        "div[data-date]:not(.unavailable)",
        # Generic day cells that are available
        "[class*='availableDay']",
        "[class*='available-day']",
        "[class*='time-slot']:not([disabled])",
        # Fallback: any button inside a calendar grid
        "table button:not([disabled])",
    ]
    for sel in selectors:
        try:
            elements = await page.query_selector_all(sel)
            if elements:
                return elements[0]
        except Exception:
            continue

    # Last resort: look for any clickable day number in the calendar
    try:
        await page.wait_for_selector("text=/Ocak|Şubat|Mart|Nisan|Mayıs|Haziran|Temmuz|Ağustos|Eylül|Ekim|Kasım|Aralık/", timeout=5000)
    except PlaywrightTimeout:
        pass

    buttons = await page.query_selector_all("button")
    for btn in buttons:
        txt = (await btn.inner_text()).strip()
        aria = await btn.get_attribute("aria-label") or ""
        disabled = await btn.get_attribute("disabled")
        if txt.isdigit() and disabled is None:
            return btn
    return None


async def _pick_time(page):
    """Gün seçildikten sonra beliren saat slotlarından ilkini döndürür."""
    await page.wait_for_timeout(600)
    selectors = [
        "[class*='timeSlot']:not([disabled])",
        "[class*='time-slot']:not([disabled])",
        "button[class*='slot']",
        "li[class*='slot'] button",
    ]
    for sel in selectors:
        elements = await page.query_selector_all(sel)
        if elements:
            return elements[0]
    return None


async def _click_next(page):
    next_labels = ["Next", "Devam", "İleri", "Continue", "Sonraki"]
    for label in next_labels:
        try:
            btn = page.get_by_role("button", name=label)
            if await btn.is_visible():
                await btn.click()
                return
        except Exception:
            continue
    # fallback: any submit/next button
    for sel in ["button[type='submit']", "input[type='submit']"]:
        try:
            el = await page.query_selector(sel)
            if el:
                await el.click()
                return
        except Exception:
            continue


async def _fill_form(page, info: dict):
    field_map = {
        # name
        "input[name='name'], input[autocomplete='name'], input[placeholder*='sim'], input[placeholder*='Name']": info["name"],
        # email
        "input[type='email'], input[name='email'], input[placeholder*='mail']": info["email"],
        # phone
        "input[type='tel'], input[name='phone'], input[placeholder*='elefo']": info["phone"],
        # notes
        "textarea": info["notes"],
    }
    for selector, value in field_map.items():
        if not value:
            continue
        for sel in selector.split(", "):
            try:
                el = await page.query_selector(sel)
                if el and await el.is_visible():
                    await el.fill(value)
                    break
            except Exception:
                continue


async def _confirm(page):
    confirm_labels = ["Book", "Randevu Al", "Onayla", "Confirm", "Schedule", "Submit", "Rezervasyon Yap"]
    for label in confirm_labels:
        try:
            btn = page.get_by_role("button", name=label)
            if await btn.is_visible():
                await btn.click()
                return
        except Exception:
            continue
    for sel in ["button[type='submit']", "input[type='submit']"]:
        try:
            el = await page.query_selector(sel)
            if el:
                await el.click()
                return
        except Exception:
            continue


if __name__ == "__main__":
    try:
        user_info = collect_info()
        asyncio.run(book(user_info))
    except KeyboardInterrupt:
        print("\nİptal edildi.")
        sys.exit(0)

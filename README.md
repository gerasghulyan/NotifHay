# NotifHay

**NotifHay** transliterates Armenian notifications into Latin characters so they can be read on your Garmin watch.

Garmin watches cannot display Armenian script — they show `???` instead. NotifHay fixes this by posting a silent Latin copy of every Armenian notification directly to your watch.

---

## How it works

1. A notification arrives on your phone (Telegram, WhatsApp, Viber, etc.)
2. **Macrodroid** (a free automation app) reads the notification and forwards it to NotifHay
3. NotifHay transliterates any Armenian text to Latin and posts it to your Garmin watch via Garmin Connect

Your original notifications are untouched. All processing happens locally on your device — nothing is sent anywhere.

---

## Requirements

- Android phone
- Garmin watch with Garmin Connect installed
- <a href="https://play.google.com/store/apps/details?id=com.arlosoft.macrodroid" target="_blank">Macrodroid</a> (free, from Google Play)

---

## Installation

### Step 1 — Install NotifHay

<a href="https://github.com/gerasghulyan/NotifHay/raw/main/NotifHay.apk" target="_blank">⬇️ Download NotifHay.apk</a>

> You may need to allow installation from unknown sources in your Android settings.

### Step 2 — Grant Notification Access to NotifHay

1. Open NotifHay
2. Tap **Grant Permission** under "Notification Access"
3. Find **NotifHay** in the list and enable it

### Step 3 — Install Macrodroid

Install <a href="https://play.google.com/store/apps/details?id=com.arlosoft.macrodroid" target="_blank">Macrodroid</a> from Google Play (free).

Grant Macrodroid notification access when asked.

### Step 4 — Set up Macrodroid macro

1. Open Macrodroid → tap **+** to create a new macro
2. Name it: `NotifHay`

**Add Trigger:**
- Tap **Add Trigger** → **Notifications** → **Notification Received**
- Tap **Select Applications** and choose the apps you want (e.g. Telegram, WhatsApp, Viber, Gmail, Messages)
- Tap **OK**

**Add Action:**
- Tap **Add Action** → **Connectivity** → **Send Intent**
- Fill in:
  - **Target:** Broadcast
  - **Action:** `com.armin.garmin.NOTIFICATION_RECEIVED`
  - **Package:** `com.armin.garmin`
  - **Class name:** *(leave empty)*
- Scroll to **Extras** and add 3 entries:

| Name    | Value                    |
|---------|--------------------------|
| `pkg`   | `{not_app_package}`      |
| `title` | `{not_title}`            |
| `text`  | `{notification}`         |

> Tap the `{}` button next to each value field to select the variable from the list.

- Tap **OK** → save the macro → make sure the toggle is **ON**

### Step 5 — Set up Garmin Connect

1. Open **Garmin Connect**
2. Go to **Menu → Garmin Devices → your device → Device Settings → Notifications**
3. Find **NotifHay** in the app list and enable it
4. *(Optional but recommended)* Disable the original apps (Telegram, WhatsApp, etc.) from the Garmin Connect notification list — otherwise you will get two notifications on your watch: the original `???` one and the transliterated one from NotifHay

### Step 6 — Disable Battery Optimization

1. Open NotifHay → tap **Disable Battery Optimization**
2. Select **Unrestricted**

This prevents Android from killing NotifHay in the background.

---

## Example

| Original | Transliterated |
|----------|---------------|
| Բարև, ինչպե՞ս ես | Barev, inchpes es |
| Շնորհակալություն | Shnorhagalutyun |
| Ուզում եմ հանդիպել | Uzum em handipel |

---

## Privacy

NotifHay does **not** connect to the internet. All transliteration is done on-device. No notification content is stored or transmitted anywhere.

---

## Troubleshooting

**Not getting notifications on the watch:**
- Make sure the Macrodroid macro toggle is ON
- Make sure Macrodroid has notification access (Android Settings → Apps → Macrodroid → Notifications)
- Make sure "NotifHay" is enabled in Garmin Connect notifications

**Getting two notifications on the watch:**
- Disable the original apps (Telegram, WhatsApp, etc.) from Garmin Connect notifications, keep only NotifHay enabled

**NotifHay stops working after a while:**
- Disable battery optimization for both NotifHay and Macrodroid (Settings → Battery → App power management)

**Banking apps blocked after enabling accessibility:**
- NotifHay does **not** require Accessibility Service. The Macrodroid approach works without it. You can safely ignore the "Grant Accessibility" button in the app.

---

## Supported apps

Any app whose notifications Macrodroid can read. Tested with:
- Telegram
- WhatsApp
- Viber
- Gmail
- SMS / Messages

---

## License

MIT

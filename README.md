# FareGo 🚌🗺️
**Smart Travel Assistant for Ghana — powered by Google Maps & real-time fare estimation**

FareGo is an intelligent mobile application designed to simplify and modernize daily commuting in Ghana. The platform leverages real-time mapping, route optimization, and dynamic fare estimation to help users navigate unfamiliar routes with confidence, accuracy, and convenience.

<img width="498" height="1080" alt="image" src="https://github.com/user-attachments/assets/d2383c4b-553b-4a0d-8b9b-904269fd7e11" />
<img width="498" height="1080" alt="image" src="https://github.com/user-attachments/assets/1d11db20-3177-4056-8d2c-a8d11ddb6afa" />
<img width="498" height="1080" alt="WhatsApp Image 2026-04-22 at 5 29 01 PM" src="https://github.com/user-attachments/assets/b62651b4-6d32-41e1-88ec-4f6f151d41d7" />




---

## ✨ Features

| Feature | Details |
|---------|---------|
| 🗺️ Live Map Navigation | Google Maps SDK with dark theme, polyline routing |
| 🚦 Traffic-aware Routes | Color-coded: 🟢 Low / 🟡 Moderate / 🔴 Heavy |
| 💰 Fare Calculator | TroTro · Taxi · Uber with peak-hour & traffic multipliers |
| 📍 Places Autocomplete | Google Places API filtered to Ghana (GH) |
| 🔔 Trip Notifications | CountDownTimer + foreground service: 10min, 5min, arriving |
| 🔊 Voice Navigation | Text-to-Speech turn-by-turn directions |
| 📜 Route History | Room/SQLite — stored locally, reusable with one tap |
| ⭐ Favourites | Star any past trip for quick access |
| 👤 Profile | Circular avatar, Home/Work saved locations, stats |
| 🔐 Auth | Register/Login with SHA-256 hashed passwords |
| 📴 Offline Fares | Haversine + seeded DB rates when no internet |

---

## 🛠 Tech Stack

- **Java** (Android SDK 34, minSdk 24)
- **Google Maps SDK** + **Directions API** + **Places API**
- **FusedLocationProviderClient** for GPS
- **Room (SQLite)** — local storage for users, routes, fares
- **Retrofit + OkHttp** — Directions API calls
- **Glide** — profile image loading
- **CircleImageView** — circular avatar
- **Material Components** — dark theme UI
- **CountDownTimer + NotificationManager** — trip countdown

---

## 🚀 Setup Instructions

### 1. Clone / Open in Android Studio
```
File → Open → select the FareGo/ folder
```

### 2. Add your API key to `local.properties`
```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY_HERE
ORS_API_KEY=YOUR_ORS_KEY_HERE          # optional fallback
```

Get a Google Maps API key at: https://console.cloud.google.com/  
Enable these APIs on your key:
- **Maps SDK for Android**
- **Directions API**
- **Places API**

### 3. Add Poppins fonts *(optional — app works without them)*
Download from https://fonts.google.com/specimen/Poppins and place:
```
app/src/main/res/font/poppins_regular.ttf
app/src/main/res/font/poppins_bold.ttf
```

### 4. Build & Run
```
Build → Make Project  (Ctrl+F9)
Run → Run 'app'       (Shift+F10)
```

---

## 📁 Project Structure

```
app/src/main/java/com/farego/app/
├── FareGoApp.java              ← Application, channels, Places init
├── activities/
│   ├── SplashActivity.java     ← Launch screen + session check
│   ├── OnboardingActivity.java ← 3-page intro
│   ├── AuthActivity.java       ← Login / Register
│   ├── MainActivity.java       ← Map, GPS, routing, fares, TTS
│   ├── ProfileActivity.java    ← Avatar, saved locations, stats
│   ├── HistoryActivity.java    ← Trip history RecyclerView
│   └── TripDetailActivity.java ← Full trip details + reuse
├── adapters/
│   ├── HistoryAdapter.java
│   └── OnboardingAdapter.java
├── db/
│   ├── AppDatabase.java        ← Room DB with seeded fare rates
│   ├── dao/
│   │   ├── UserDao.java
│   │   ├── RouteHistoryDao.java
│   │   └── FareRateDao.java
│   └── entity/
│       ├── User.java
│       ├── RouteHistory.java
│       └── FareRate.java
├── model/
│   ├── FareResult.java
│   └── RouteInfo.java
├── network/
│   ├── RetrofitClient.java
│   ├── api/DirectionsApi.java
│   └── model/DirectionsResponse.java
├── service/
│   ├── TripNavigationService.java  ← Foreground service + countdown
│   └── NotificationReceiver.java
├── ui/bottomsheet/
│   └── TransportBottomSheet.java   ← Uber-style transport picker
└── utils/
    ├── FareCalculator.java         ← Core Ghana fare engine
    ├── PolylineDecoder.java
    ├── SessionManager.java
    └── HashUtils.java
```

---

## 💰 Fare Rates (Default — Ghana Cedis)

| Transport | Base | Per km | Min | Peak mult |
|-----------|------|--------|-----|-----------|
| TroTro    | GH₵ 2 | GH₵ 0.80 | GH₵ 2 | ×1.10 |
| Taxi      | GH₵ 8 | GH₵ 2.50 | GH₵ 10 | ×1.25 |
| Uber      | GH₵ 12 | GH₵ 3.80 | GH₵ 15 | ×1.50 |

**Peak hours:** 06:30–09:00 and 16:00–19:30  
**Traffic multipliers:** Low ×1.0 · Moderate ×1.2 · Heavy ×1.5

---

## 🔔 Notifications

The foreground `TripNavigationService` fires alerts at:
- **10 min** remaining
- **5 min** remaining  
- **2 min** remaining
- **Arrival**

---

## 📄 License
MIT — build freely, give credit where due.

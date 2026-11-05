# Astronomical Precision, Android Lifecycle & Dynamic Astrological Engine Plan

Comprehensive architecture and implementation plan to resolve astronomical ephemeris inaccuracies, Android process lifecycle state loss, notification permissions, city search normalization, and replace static boilerplate feeds with dynamic transit and synastry calculations.

### User Review & Critical Decisions

> [!IMPORTANT]
> **Summary of User Confirmed Specifications**:
> - **Timezone & DST Handling**: Automatically calculate timezone and DST from birth coordinates and geographical location, converting local birth time to exact UTC before computing celestial longitudes.
> - **100-Year Planetary Calculation**: Full ephemeris calculation for all 101 years, computing actual transiting planetary positions and exact orbital aspect crosses (returns, oppositions, squares) relative to natal placements.
> - **Dynamic Readings & Synastry**: Compute real-time planetary transits using `PlanetCalculator` geometry to replace static boilerplate strings with dynamic daily horoscopes and cross-chart synastry aspects.

- **Confirmed Decision 1**: Time conversion translates local calendar dates and times to UTC epoch/decimal hours using astronomical longitude offsets and timezone rules.
- **Confirmed Decision 2**: Document export state survives Android process recreation using `rememberSaveable` and ViewModel state.
- **Confirmed Decision 3**: Android 13+ (API 33) runtime notification permission (`POST_NOTIFICATIONS`) declared in `AndroidManifest.xml` and requested before activating daily reminder workers.

---

### 1. Overview & Core Concept

- **What It Does**: Upgrades AstroLog's ephemeris engine to provide high-precision astrological math, robust Android system integration, and dynamic readings:
  1. *Astronomical Accuracy*: Accurate Ascendant, House Cusps, and planetary degrees by properly accounting for local-to-UTC time offsets and flexible 12h/24h time formats.
  2. *Centennial Ephemeris Calculation*: Computes true annual solar return charts and transiting positions across all 101 years to find the exact years for Saturn Returns (~29.45y), Jupiter Returns (~11.86y), Uranus Oppositions, and Chiron Returns based on real natal degrees.
  3. *Android System Integrity*: Prevents export failure upon returning from the system file picker, requests notification permissions properly on modern Android versions, and expands city searching to support common abbreviations (`St.`, `Ft.`, hyphens, apostrophes).
  4. *Dynamic Interpretations*: Real daily transits and two-chart synastry aspect evaluations instead of static mock texts.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. *Birth Time & Location Setup*:
     - In Onboarding and Settings, users can type `"2:30 PM"`, `"14:30"`, or `"09:15 AM"` seamlessly without parsing failures.
     - Searching for cities with punctuation or prefixes (e.g., `"St. Louis"`, `"Saint Louis"`, `"Coeur d'Alene"`, `"Fort Worth"`) resolves instantly.
     - Accurate rising signs and house cusps match professional astrological software.
  2. *Daily Transit Horoscopes*:
     - Daily feed cards reflect the day's actual transits (e.g., *Transiting Sun in Scorpio opposite Natal Moon in Taurus*), updating dynamically each day.
  3. *Synastry Compatibility Screen*:
     - Comparing charts calculates real geometric aspects (conjunctions, trines, squares, oppositions) between the two individuals' planets with specific harmony scores and tailored synastry commentary.
  4. *Reliable 100-Year Life Export*:
     - Tapping "Export .md" safely preserves the generated document across system app pauses when the document picker opens, writing the complete century dossier reliably.
  5. *Daily Notification Permission Flow*:
     - Toggling daily reminders on Android 13+ prompts the OS notification permission dialog gracefully if not yet granted.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Automatic Timezone Calculation from Geographic Coordinates**
  - *Chosen Approach*: Derive the base timezone offset ($15^\circ = 1\text{ hour}$) and apply geographic region heuristics to resolve the proper UTC offset without burdening the user with cumbersome manual UTC dropdowns.
  - *Why*: Seamless user experience while eliminating the $75^\circ–90^\circ$ Ascendant calculation shift caused by treating local wall-clock time as UTC.

- **Decision 2: 101-Year Solar Return Ephemeris Loop vs. Hardcoded Constants**
  - *Chosen Approach*: Calculate transiting planetary positions for each year of the 100-year lifespan and detect when the angular difference between a transiting planet and its natal position crosses $0^\circ$ (conjunction/return), $90^\circ$ (square), or $180^\circ$ (opposition).
  - *Why*: Planetary orbits (especially Pluto $e=0.249$, Chiron $e=0.379$, and Jupiter $11.86\text{y}$) deviate significantly from static integer multiples over a full human century.

- **Decision 3: Real-Time Geometric Synastry & Daily Transits**
  - *Chosen Approach*: Compare actual transiting planet positions with natal positions using standard Ptolemaic orbs ($6^\circ–8^\circ$) to synthesize real astronomical conditions for daily feeds and partner compatibility.
  - *Why*: Delivers genuine astrology grounded in planetary physics rather than static strings.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌────────────────────────────────────────────────────────────────────────┐
│                              UI Layer                                  │
│  ┌─────────────────────────┐  ┌─────────────────────────────────────┐  │
│  │ SettingsScreen          │  │ CompatibilityScreen & HomeScreen    │  │
│  │ • rememberSaveable SAF  │  │ • Dynamic Daily Transit Feed        │  │
│  │ • POST_NOTIFICATIONS    │  │ • Geometric Synastry Matrix         │  │
│  └────────────┬────────────┘  └──────────────────┬──────────────────┘  │
└───────────────┼──────────────────────────────────┼─────────────────────┘
                ▼                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        MainViewModel & Data Layer                      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Timezone & Time Parser Pipeline                                  │  │
│  │ • 12h/24h AM/PM Regex Formatter                                  │  │
│  │ • Coordinate-to-UTC Offset Resolver                              │  │
│  │ • CitySearchService Token Normalizer ("st." -> "saint", etc.)    │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │                                  │
│                                     ▼                                  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Ephemeris & Aspect Calculation Engine                            │  │
│  │ • PlanetCalculator: Real-time Transit & Synastry Aspect Analysis │  │
│  │ • AstrologyTimelineGenerator: 101-Year Ephemeris Orbit Crossings │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

- **Target Files & Changes**:
  1. `app/src/main/AndroidManifest.xml`: Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`.
  2. `app/src/main/java/com/example/ui/SettingsScreen.kt`:
     - Use `rememberSaveable` for `generatedReportMarkdown`.
     - Add `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` for notification permission handling.
  3. `app/src/main/java/com/example/ui/AstrologyTimelineGenerator.kt`:
     - Flexible AM/PM and 24h time parsing.
     - Coordinate-based UTC offset alignment.
     - 101-year ephemeris loop detecting exact planetary returns (Saturn, Jupiter, Uranus, Chiron, Nodes) based on actual degree delta crosses.
  4. `app/src/main/java/com/example/data/CityLocation.kt`:
     - Normalization helper for abbreviations (`st.`, `ft.`, `saint`, `fort`) and punctuation stripping.
  5. `app/src/main/java/com/example/ui/MainViewModel.kt`:
     - Dynamic `DAILY_FEED` generation linking transits to the user's natal placements.
     - Dynamic synastry compatibility calculation between profiles.

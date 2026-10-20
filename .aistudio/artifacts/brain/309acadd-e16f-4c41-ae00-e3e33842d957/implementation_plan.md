# Comprehensive 50-State US Cities Database & Smart Search Service

Expand AstroLog's location database to cover all incorporated cities and towns across all 50 US states with high-precision coordinates, two-way state name/abbreviation resolution, sub-millisecond search performance, and automatic online geocoding fallback for rural or unincorporated hamlets.

---

## 1. Context & User Objectives

- **Primary Goal:** Provide comprehensive coverage for all incorporated US cities and towns across all 50 states, ensuring US users find their exact birthplace instantly offline.
- **Clarification Insights:**
  1. **Dataset Scope:** Full coverage of incorporated cities, towns, boroughs, and municipalities across all 50 US states.
  2. **State Name & Abbreviation Handling:** Full bi-directional search support (e.g., searching "Dallas, TX" or "Dallas, Texas" or just "Dallas" immediately resolves and displays both the state abbreviation and full name).
  3. **Unincorporated & Rural Areas:** Transparent fallback to online geocoding (`Geocoder` / Nominatim) when a query is an unincorporated hamlet or rural locality not in the bundled database.

---

## 2. Technical Architecture & Data Strategy

### A. Data Asset & Storage Strategy
Storing thousands of cities directly in Kotlin source code would bloat the bytecode and slow down Gradle compilation. Instead, we will:
1. **Compact Compressed Asset / Resource (`assets/us_cities.csv` or `assets/us_cities.json.gz`):**
   - Store all incorporated cities and towns across all 50 US states plus DC and Puerto Rico with fields: `City Name`, `State Code` (e.g., `CA`), `State Full Name` (e.g., `California`), `Latitude`, `Longitude`, and `Population/Rank` for sorting relevance.
   - Format: Highly compact CSV / delimiter-separated file (e.g., `name|state_code|lat|lon|rank`) bundled directly into `app/src/main/assets/us_cities.tsv`.
2. **Fast In-Memory Prefix & Token Index (`UsCityRepository` / `CitySearchService`):**
   - Streamed and indexed in background on first access or app startup using Kotlin Coroutines (`Dispatchers.IO`).
   - Tokenized lookup trie / index supporting:
     - Exact prefix matching (`"San..."` -> `San Francisco`, `San Antonio`, `San Diego`, `San Jose`, etc.)
     - State code and full name indexing (`"Austin, TX"` or `"Austin, Texas"` matches `Austin, Texas, United States`)
     - Population/relevance weighting so major metropolitan hubs appear before smaller towns sharing the same name (e.g., `Springfield, MO` and `Springfield, IL` before tiny rural Springfields).

### B. Bi-Directional State Mapping
- A complete immutable map of all 50 US states + DC + US territories:
  - `"AL" -> "Alabama"`, `"AK" -> "Alaska"`, ..., `"WY" -> "Wyoming"`.
- When users enter `"Dallas, TX"`, the query parser identifies `"TX"` as Texas and filters results accordingly.
- When users enter `"Dallas, Texas"`, the query parser also matches `"Texas"`.

### C. Live Search Flow with Online Fallback
1. **User types query:** Debounced by 200ms.
2. **Offline Match First:** Search offline repository. If matches are found, return up to 10 top-ranked results instantly (< 5ms response time, completely offline).
3. **Hybrid Fallback:** If the offline database returns fewer than 3 matches or zero matches, or if the user is typing an unincorporated village or county name, seamlessly trigger the online geocoding lookup (`android.location.Geocoder` / Nominatim HTTP fallback with proper network handling).
4. **Display & Feedback:**
   - Display results in the frosted-glass autocomplete menu with city name, state, country, and precise latitude/longitude coordinates.
   - Show a subtle `Online Result` or `Offline Verified` indicator badge.
   - Instant coordinate resolution upon selection, ensuring zero `(0.0, 0.0)` calculation errors.

---

## 3. Step-by-Step Implementation Plan

### Step 1: Asset Creation (`app/src/main/assets/us_cities.tsv`)
- Generate and bundle a comprehensive, curated dataset of all incorporated cities and towns across all 50 US states, with latitude, longitude, state code, and population weighting.
- Include major international capitals and hubs to preserve global coverage while drastically deepening US density.

### Step 2: Comprehensive State Mapping & Query Parser (`com.example.data.UsStateHelper`)
- Add state lookup table (`stateCodeToName`, `stateNameToCode`).
- Clean and normalize query inputs to recognize comma-separated patterns:
  - `[City, StateAbbr]` (e.g. `Naperville, IL`)
  - `[City, StateName]` (e.g. `Naperville, Illinois`)
  - `[City]` alone (e.g. `Naperville`)

### Step 3: High-Performance Search Engine (`com.example.data.CitySearchService`)
- Upgrade `CitySearchService` to load from the asset asynchronously.
- Implement token-based scoring:
  - City prefix match score
  - State match boost
  - Population rank tie-breaker
- Incorporate coroutine-based async online geocoding for unrecognized queries.

### Step 4: UI & Autocomplete Integration Verification
- Verify integration in `OnboardingScreen.kt` and `SettingsScreen.kt`.
- Ensure touch target sizes remain >= 48dp, keyboard action navigation is smooth, and selection instantly sets latitude, longitude, and formatted birth city text.

### Step 5: Unit Testing & Verification
- Add Robolectric / JVM unit tests in `ExampleUnitTest.kt`:
  - Test searching by abbreviation (`"Miami, FL"` -> Miami, Florida)
  - Test searching by full state name (`"Seattle, Washington"` -> Seattle, Washington)
  - Test searching multiple cities with identical names across states (`"Springfield"`)
  - Test rare/smaller incorporated towns in diverse states (e.g. Alaska, Wyoming, Vermont, Hawaii).
- Run compilation and tests to ensure 100% build health.

---

## 4. User Decisions & Trade-Offs

- **Asset Compression vs Load Time:** Using a clean delimiter-separated asset (`us_cities.tsv`) avoids large class bytecode sizes while keeping startup parsing under 35 milliseconds on Android.
- **Offline Reliability:** Users can input any incorporated US municipality without requiring an internet connection or cellular signal, while online geocoding provides safety for rural unincorporated areas.

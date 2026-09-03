# Check-in History Screen — How It Works

## One-line summary
The History screen is an **archive of finished 26-dot "boards"** — each board is a completed
check-in cycle rendered with the exact same calendar grid as Home, so History looks like a
stack of snapshots of past Home screens.

---

## 1. The product concept

Home uses a **26-dot check-in board** (a rolling 26-day cycle shown bottom-up: 1 + 7 + 7 + 7 + 4
dots). When a cycle fills, the board resets and a new cycle begins.

History leverages that existing concept instead of imposing calendar months:

- **One section in History = one finished 26-dot board**, not one calendar month.
- Boards are **consecutive 26-day windows** anchored to the user's first-ever check-in date.
- The header shows **dates as metadata** (`Board 3 · Aug 2 – Aug 29 · 26 check-ins · 0 missed`).
- Months no longer matter — a user checking in 31 days straight just fills one board and spills
  into the next; February and August behave identically. No padding, no truncation, no "why does
  February look weird".

This is also **more identical to Home** than a month grid: the exact same component renders both.

---

## 2. Where the data comes from

**No new network calls.** The screen reads the same shared, already-cached data Home uses:

| Data | Source table | What it provides |
|---|---|---|
| Completed check-ins | `check_ins` | `contact_id` + `checked_in_at` timestamp |
| Missed occurrences | `missed_check_ins` | `contact_id` + `scheduled_date` |
| Contacts | `contacts` | name + `avatar_color` → avatar drawable |

`InMemoryHomeRepository.load()` already fetches all check-in history (from "1970-01-01" to today)
and all missed check-ins into a `HomeSnapshot`. The History **ViewModel observes that shared
repository's state** and re-derives its UI models on every snapshot emission.

Because Home and History read the same snapshot, both screens are always consistent, and History
works offline from the cached snapshot.

---

## 3. Architecture / data flow

```
HomeRepository.state  (shared snapshot: checkInHistory, missedCheckIns, contacts)
        │  collect
        ▼
CheckInHistoryViewModel
        │  applySnapshot()
        ├─ buildHistoryLookupMaps()          → Map<LocalDate, contacts> for completed + missed
        ├─ resolveInitialCountdownStartDate() → board anchor (first-ever activity, same as Home)
        ├─ buildBoardUiModels()              → List<HistoryBoardUiModel> (newest first)
        └─ currentBoardProgress()            → dots filled in the live board (or null if never used)
        ▼
CheckInHistoryState(isLoading, boards, currentBoardProgress)
        │  collectAsStateWithLifecycle
        ▼
CheckInHistoryScreen → BoardSection → CheckInTimelineGrid   (the exact Home grid)
                          └─ dot tap → ModalBottomSheet (DotDetailsSheet)
```

### 3a. Lookup maps — built once, no per-day scans
`buildHistoryLookupMaps()` reduces the raw lists into two `Map<LocalDate, Set<contactId>>`
(completed, missed) in a single pass per snapshot. Two important properties:

- **The Instant → LocalDate conversion happens exactly once, here**, through the existing
  `CheckIn.localDate(timeZone)` helper (device timezone). Everything downstream works purely in
  `LocalDate`, which structurally prevents off-by-one-day bugs from timezone mismatch.
- **A late check-in beats a miss marker**: if a contact missed a scheduled day but later checked
  in that same day, they count as completed, not missed.

### 3b. The board anchor (alignment with Home)
Both Home and History anchor their 26-day cycles at the same date:
`resolveInitialCountdownStartDate()` returns the user's **earliest-ever activity** (first check-in,
missed occurrence, or first scheduled date). Home's rolling window computes
`cycleOffset = (today − anchor) % 26`; History slices the same axis into fixed 26-day boards. So a
board archived in History **always lines up** with the dots the user saw on Home.

### 3c. Board segmentation
`buildBoardUiModels()`:

- `daysElapsed = today − anchor`; `currentBoardIndex = daysElapsed / 26 + 1`.
- Renders `currentBoardIndex − 1` down to `1` (**newest board first**).
- **Current in-progress board is excluded** — it's live on Home, so History keeps only finished boards.
- **Abandoned boards are excluded** — a fully-elapsed 26-day window with zero check-ins means the
  user was away; nothing to archive.
- Per board it computes `completedCount` / `missedCount` (map lookups, O(26) per board) and a
  sparse event list (only days with activity), with per-contact avatar drawables.
### 3d. The in-progress board progress
`currentBoardProgress()` counts dots already filled (a dot is filled when ≥1 contact checked in
that day) inside the live board window. `null` when the user has never checked in. This lets the
empty state tell the truth:

| Situation | Empty-state message |
|---|---|
| Never checked in | "No check-ins yet" |
| First board still in progress (e.g. 6 of 26) | "Your first board isn't finished yet — 6 of 26 dots filled. Finish it on Home and it will appear here." |

---

## 4. The screen, top to bottom

1. **Top bar**: back button + start-aligned "History" title. (No year dropdown — boards span
   years freely, so a year filter would be meaningless; the board date ranges carry the dates.)
2. **Stat row**: `N boards filled · ★ M perfect` — a collectible-feel summary from the palette.
3. **Board sections (newest first)**:
   - Header: `Board 3` + date range (`Aug 2 – Aug 29`) + a **★ Perfect** green pill when the
     board has zero misses.
   - Stats line: `26 check-ins · 0 missed`.
   - The grid: **`CheckInTimelineGrid` — the exact same component as Home** (bottom-up
     1+7+7+7+4 layout, rounded cells, avatars/stacked avatars, green check fill, full-size empty
     gaps for missed days, small inactive dots for days with nothing). `animateBubble = false`
     because history is past-oriented, and dots are tappable.
4. **Dot tap → detail sheet** (`ModalBottomSheet`, same pattern as the app's other sheets):
   - `Board 3 · Dot 5 of 26`
   - Exact date, e.g. `Aug 5, 2026`
   - One row per check-in that day: **contact name + local time (HH:mm)**
   - Rows for missed occurrences (red "Missed" label)
   - "No check-ins this day" when the dot is empty.

---

## 5. Edge cases & product decisions

- **Boards with gaps** show real history — green filled cells where checked in, blank gap cells
  where missed.
- **Multiple contacts on the same day** → one dot per contact (Home's avatar stacking renders
  them together on that dot).
- **Board indexing is global** — Board numbers come from the user's first-ever activity, so
  numbers never reset and stay referable ("I'm on Board 7").
- **Check-ins for deleted/unknown contacts are excluded** (only current contacts' avatars/names
  are rendered).

---

## 6. Performance

- One-time O(check-ins + misses) map build **per repository snapshot**.
- Board derivation is pure `Map` lookups — no days × contacts × check-ins re-scans.
- State is recomputed only when the repository snapshot actually changes; dot-tap details are
  derived lazily on tap from the cached maps — no recompute, no extra I/O.
- Data volume is bounded by human behavior (~365 check-ins/year/contact) — trivial for the
  existing index (`idx_check_ins_contact`) and the repository's 30s staleness cache.

---

## 7. Testing

17 unit tests (`androidApp`):
- **Derivation (11)**: timezone boundary conversion, late-check-in-beats-miss, 26-day window math,
  current/abandoned board exclusion, gap counting, Perfect detection, avatar mapping,
  multi-contact days, dot-detail position/time/misses, current-board progress counting.
- **ViewModel (6)**: loading state, finished-board derivation, Perfect stat counting, dot
  select/dismiss, empty history, in-progress-board empty-state messaging.
---

## 8. File map

| File | Role |
|---|---|
| `home/.../presentation/history/CheckInHistoryState.kt` | Models + pure derivation (lookup maps, board builder, dot details, current progress) |
| `home/.../presentation/history/CheckInHistoryViewModel.kt` | Observes shared HomeRepository, derives state, serves dot taps |
| `home/.../presentation/history/CheckInHistoryScreen.kt` | Screen: stat row, board sections, dot-detail bottom sheet |
| `home/.../presentation/history/HistoryTopBar.kt` | `← History` top bar |
| `home/.../presentation/components/CheckinGrid.kt` | Shared 26-dot `CheckInTimelineGrid` (unchanged — reused as-is) |
| `shared/.../navigation/Screen.kt` / `NavigationTransitionPolicy.kt` | `Screen.CheckInHistory` route + horizontal transition |
| `home/.../HomeScreen.kt` + `onboarding/.../OnboardingApp.kt` | Status card tap → navigate to History |

---

## 9. How to try it

- Real usage: finish a 26-dot board on Home; the next board starts and the finished one appears
  in History (tap the status card).
- For a demo: `onboarding/sql/seed_history_boards.sql` back-fills 3 finished boards (Board 3
  ★ Perfect) + 7 dots of the in-progress board into the database.
- **`isPerfect`** = zero missed occurrences and at least one check-in in the window (⭐ marker).
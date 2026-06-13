# Scaccomatto Chess Application

A feature-rich Java chess application built with Swing, developed over two months. It includes standard chess rules, six gameplay variants, a full Spell Chess system with animated spell cards, Stockfish engine integration, move review, online play, puzzles, and a polished animated menu.

---

## Build and Run

**Compile**
```bash
cd Scaccomatto_final/Scaccomatto
javac src/*.java -d bin
```

**Run**
```bash
cd Scaccomatto_final/Scaccomatto
java -cp bin Main
```

Requires Java 8 or newer. Stockfish must be present in `engines/` for bot play and analysis features.

---

## Project Structure

```
src/          Java source files (all game logic, UI, networking)
bin/          Compiled .class output
assets/
  pieces/     Piece images (wp, wk, wr, wb, wn, wq, bp, bk, br, bb, bn, bq, duck)
  sounds/     Audio clips (move, capture, check, castle, spell effects)
  spells/     Spell card artwork (_inside variants for card interiors)
  avatars/    Bot portrait images (bobby, Carl, Tom, Sarah, levy)
  multiplayer/ Variant selection icons (classic, 960, kinghill, 3checks, atomic, etc.)
  bg.jpg      Main menu background image
  logo.png    Application icon
engines/      Stockfish binary
```

---

## Source Files

| File | Role |
|---|---|
| `Main.java` | Entry point; installs F11 fullscreen shortcut, launches `MainMenu` |
| `MainMenu.java` | Animated main menu with CardLayout navigation, dark/light mode, all screen routing |
| `ChessGame.java` | Core game window: board layout, clocks, spell UI, responsive scaling, hotkeys |
| `Board.java` | Board rendering, mouse input, piece movement, all variant and spell logic |
| `Piece.java` | Abstract piece base; spell-chess state fields (frozen, shielded, bombRook, zoglin) |
| `Pawn.java` | Pawn movement, en passant, double-push tracking |
| `King.java` | King movement, castling path validation, `moved` flag |
| `Rook.java` | Rook movement, `moved` flag for castling rights |
| `Knight.java` | L-shaped movement |
| `Bishop.java` | Diagonal sliding movement |
| `Queen.java` | Combined rook + bishop movement |
| `Duck.java` | Duck Chess variant piece; blocks all standard attacks |
| `Zoglin.java` | Spell Chess piece; moves as queen + knight combined |
| `IsLegal.java` | Legal move generation (simulates each candidate move, filters king-in-check) |
| `Castling.java` | Castling availability check and rook/king repositioning |
| `Promotion.java` | Pawn promotion dialog and piece replacement |
| `MoveNotation.java` | Algebraic notation generation (Nf3, O-O, exd5 e.p., disambiguation, check/mate symbols) |
| `SpellManager.java` | Spell registry, cast validation, king-safety check on spell application, elixir deduction |
| `Spell.java` | Interface: `getId`, `getName`, `getCost`, `canCast`, `apply` |
| `SpellTarget.java` | Target data carrier for spell effects (source square, destination square, etc.) |
| `PlayerState.java` | Per-player state: elixir amount, captured piece list, spell-related flags |
| `CapturedPieceRecord.java` | Piece type and color record for the captured material strip |
| `VariantMode.java` | Enum: `NORMAL`, `SPELL_CHESS`, `ATOMIC`, `THREE_CHECK`, `KING_OF_THE_HILL`, `DUCK_CHESS` |
| `ChessBot.java` | Five bot profiles with ELO, Stockfish skill level, search depth, and personality descriptions |
| `StockfishEngine.java` | UCI process wrapper: position setup, `go depth`, best-move and multi-PV parsing |
| `EngineAnalysis.java` | Result container: best move, centipawn evaluation, mate detection |
| `PositionEvaluator.java` | Continuous position evaluation for the live evaluation bar |
| `UCIConverter.java` | Converts board coordinates ↔ UCI move strings (e.g., `e2e4`, `e7e8q`) |
| `OpeningDetector.java` | FEN-based opening lookup; 100+ openings embedded including Sicilian, Ruy Lopez, QGD |
| `OpeningBookData.java` | Extended opening book with ECO codes and variation names |
| `OpeningNamePanel.java` | Animated overlay that displays the detected opening name during play |
| `Review.java` | Full game review engine: Brilliant, Great, Best, Excellent, Good, Inaccuracy, Mistake, Blunder, Miss classifications via Stockfish MultiPV |
| `MoveHistoryPanel.java` | Scrollable move history table with navigation, opening title, review star icons |
| `EvaluationBar.java` | Animated vertical evaluation bar (±10 pawns full scale, mate overlay) |
| `AnalysisGame.java` | Standalone analysis board with live Stockfish lines, best-move display, PGN input |
| `AnalysisPanel.java` | Analysis menu card shown in the main menu |
| `BotPanel.java` | Bot selection screen with profile cards, ELO stars, and Spectate option |
| `OnlinePanel.java` | Online lobby: host/join by IP and port, variant selector, time control picker |
| `NetworkManager.java` | TCP socket layer; sends/receives moves, spell casts, spell phases, draw offers, resignations |
| `SettingsPanel.java` | In-game settings dropdown: toggles, dropdowns, and theme selection |
| `TimeControlPanel.java` | Time control picker (Bullet / Blitz / Rapid / Custom / Timeless) |
| `CreditsPanel.java` | Credits screen with background image and back navigation |
| `PlaceholderScreen.java` | Stub screen for features not yet released (Lessons) |
| `FullscreenToggle.java` | F11 fullscreen toggle with smooth animated transition |
| `puzzles.java` | Fantasy map puzzle hub with 100 levels, drag-to-pan, sparkle particles, and level locking |

---

## Features

### Chess Rules
- Full legal move validation with per-piece path-clear checks
- En passant capture with `justMovedTwo` pawn flag
- Castling (kingside and queenside) with rook/king `moved` flag tracking and path-safety validation
- Pawn promotion with piece-picker dialog
- Check and checkmate detection via full board simulation
- Stalemate detection
- Draw by insufficient material
- Fifty-move rule draw
- Board flip / auto-flip after each move
- FEN import and export for any position
- PGN support in analysis mode

---

### Game Modes

**Local Play (Play with Friend)**
Launches the variant selection screen. Two players share one keyboard and mouse.

**Bot Play (Play with Bot)**
Full bot selection screen with five distinct profiles:

| Bot | ELO | Stockfish Skill | Search Depth |
|---|---|---|---|
| Bobby Beginner | 200 | 0 | 1 |
| Casual Carl | 700 | 5 | 3 |
| Tactical Tom | 1,400 | 10 | 6 |
| Strategic Sarah | 2,300 | 15 | 10 |
| Grandmother Levy | 6,700 | 20 | 18 |

Each bot has a handwritten backstory and portrait. The bot thinks for a minimum of 1 second per move for a natural feel. Bots run on a background thread and communicate results back to the EDT via `SwingUtilities.invokeLater`.

**Bot Spectate**
Pick separate White and Black bots and watch them play each other. The active bot is determined per-turn from `spectateWhiteBot` / `spectateBlackBot`.

**Online Play**
Peer-to-peer over TCP sockets. One player hosts on a chosen port; the other joins by IP. The host plays White, the guest plays Black. Supports:
- Move synchronisation
- Spell cast and spell phase synchronisation in Spell Chess
- Draw offers and responses
- Resignation

**Analysis**
A separate `AnalysisGame` window with a free-placement board, live Stockfish evaluation, top engine lines displayed, and a best-move overlay. Accepts FEN and PGN input. The evaluation bar updates after each position change.

---

### Chess Variants

All six variants are selectable from the variant grid. Each has a custom icon and description card.

| Variant | Win Condition |
|---|---|
| Classic | Standard checkmate |
| Chess960 | Randomised back rank; standard checkmate |
| King of the Hill | Checkmate **or** move your king to e4, e5, d4, or d5 |
| Three-check | Checkmate **or** give three checks |
| Atomic | Captures explode the captured piece and all adjacent pieces (except pawns); king explosion = loss |
| Spell Chess | Standard checkmate; both players cast spells using an elixir economy |
| Fog of War | Each player only sees squares their pieces can reach; opponents are hidden elsewhere |
| Duck Chess | After every move the active player must also relocate the Duck to any empty square |

---

### Spell Chess

A custom variant unique to Scaccomatto. Both players build elixir each turn and cast spells from a hand of cards.

**Elixir System**
- Each player has an elixir bar with 10 segments
- Elixir regenerates passively each turn
- Spells have individual mana costs
- The elixir bar has a shimmer animation that scrolls across filled segments at 40 ms per frame

**Spell Hand**
- Each player holds 3 playable cards plus 1 face-down "NEXT" preview card
- Opening hand is always: Enderman, Fireball, Uriel — with Fog as the first next card
- After a spell is used, cards slide left with a cubic ease-out animation over 220 ms
- The NEXT card scales down and snaps into the last playable slot
- A new NEXT card is drawn from the remaining pool using a deterministic hash to avoid repeats

**The 8 Spells**

| Spell | Cost | Effect |
|---|---|---|
| Fireball | varies | Deals explosive damage to a target square; triggers a full animated plane sequence |
| Freeze | varies | Freezes a target piece for a set number of turns; the frozen piece cannot move |
| Shield | varies | Shields a friendly piece for a set number of turns; the piece cannot be captured |
| Enderman | varies | Teleports a friendly piece to any empty square on the board |
| Uriel | varies | Promotes a pawn to any piece immediately, regardless of rank |
| Fog | varies | Blankets the board in Fog of War for a set number of turns |
| Bomber | varies | Converts a friendly Rook into a Bomb Rook that explodes on capture |
| Zoglin | varies | Summons a Zoglin — a hybrid piece that moves as Queen + Knight combined |

**Spell Card Visuals**
Each card has a unique colour theme:

| Spell | Card gradient | Border glow |
|---|---|---|
| Fireball | Molten red-orange | Orange |
| Freeze | Electric ice blue | Blue |
| Shield | Emerald green | Green |
| Enderman | Void purple | Purple |
| Uriel | Radiant gold | Gold |
| Fog | Storm teal | Teal |
| Bomber | Creeper green | Green |
| Zoglin | Blood red | Red |

Cards pulse with a floating sine-wave lift when selected (±8px vertical, 2.1 s period). A glaze shine sweeps diagonally across the selected card every 10 seconds. Disabled cards are desaturated and darken their artwork.

---

### Spell Card Travel Animation

When a spell is cast, the used card flies from the spell panel across the screen to the board in a multi-phase animated sequence rendered on `JLayeredPane.DRAG_LAYER` at 16 ms per frame (≈60 fps):

| Phase | Duration | Motion |
|---|---|---|
| In-flight | 750 ms | Quadratic Bézier curve; sinusoidal lateral wobble; card scales from 1× to 2×; horizontal flip spins twice (cosine) |
| Hold at centre | 1,000 ms (2,400 ms for Freeze; 2,000 ms for Fog) | Gentle bob (±4 px); spell-specific effects fire here |
| Out-flight | 800 ms | Second Bézier arc toward board corner; card shrinks from 2× to 0.16×; horizontal flip spins twice again; alpha fades from t=0.72 |

**Spell-specific travel behaviours:**
- **Freeze** — card stays fixed at the board centre and pulses in scale 5 times as if "firing" ice bolts; hold phase extends to 2,400 ms
- **Fog** — during the hold phase the card grows and brightens with a white charge overlay; at the end of hold a shrink + alpha fade plays over 320 ms instead of the normal out-flight
- **Enderman** — after the hold phase the card travels on a curved arc from centre to the exact target square, then shrinks to nothing over 850 ms; a pixel-burst absorption ring expands at impact with purple particles
- **Fireball** — the entire spell resolution lock is held for 6,000 ms + travel time to allow the fireball plane animation on the board to complete

**Clock overlay:** Both clock labels fade to a translucent grey pause overlay (cubic ease, 220 ms) when a spell is resolving, and fade back once the lock releases.

---

### Clocks and Time Controls

- Per-player countdown clocks displayed in rounded pill labels above and below the board
- Clock ticks every 1,000 ms via `javax.swing.Timer`
- Time turns red when under 30 seconds
- Increment is added to the mover's clock immediately after each move
- On timeout, the game checks opponent material sufficiency before declaring a winner or draw
- **Time categories and presets:**

| Category | Presets |
|---|---|
| 🚀 Bullet | 1 min · 1\|1 · 2\|1 |
| ⚡ Blitz | 3 min · 3\|2 · 5 min |
| ⏱ Rapid | 10 min · 15\|10 · 30 min |
| ⚙ Custom | User-defined initial time and increment |
| ♾ Timeless | No limit |

The time selector is a collapsible dropdown within the game window. Clicking it expands or collapses the preset grid with an animated height transition. Keyboard hotkeys 1–9 select presets directly; arrow keys navigate the grid; Enter starts the game.

**UI theme colours for the Start Game button** (8 options driven by `TimeUiTheme`): Blue, Green, Gray, Purple, Red, Orange, Yellow, Pink.

---

### Responsive Layout

The game window is divided by two resizable `JSplitPane` dividers:

- **Left panel** — board + player bars + evaluation bar column. Constrained to a fixed aspect ratio; the board, player bars, fonts, clock labels, avatar cards, and captured material strips all scale proportionally as the divider moves.
- **Centre panel** — time controls (pre-game) or move history (in-game), plus spell panels for Spell Chess.
- **Right panel** — settings dropdown and action buttons.

A `ComponentListener` on the viewport drives `applyFitToScreenScale` which recalculates every child's preferred / minimum / maximum size on each resize event.

---

### Move History Panel

- Two-column table (White / Black) with move numbers
- Scrolls automatically to keep the latest move visible
- Clicking any row navigates to that board position
- Keyboard left/right arrows step through positions
- Opening name displayed as a styled title above the table; updates live after each move by walking the position history backward until a known opening is found
- Variant name shown as the title when no opening is detected
- Review star icons can be overlaid on moves after game review

---

### Opening Detection

- 100+ named openings keyed by FEN position string (pieces + active colour only, ignoring clocks)
- Coverage: King's Pawn, Sicilian, French, Ruy Lopez, Italian, Two Knights, Queen's Gambit, King's Indian, English, and many sub-variations with ECO codes
- `OpeningDetector` walks backward through `getMoveHistoryFEN()` to always show the most recently matched opening

---

### Game Review (`Review.java`)

Classifies every move in a completed game using Stockfish depth 20 with MultiPV 3:

| Symbol | Label | Trigger |
|---|---|---|
| ◆◆ | Brilliant | Played move wins; top alternative loses ≥10% win probability more |
| ◆ | Great | As above but gap is 5–10% |
| ✓ | Best | The engine's top choice |
| ★ | Excellent | Win% loss < threshold for Good |
| ● | Good | Small imprecision |
| ▲ | Inaccuracy | Noticeable slip |
| ✗ | Mistake | Significant error |
| ✗✗ | Blunder | Major error |
| ◉ | Miss | A forced mate was available and was not played |

Win probability is computed from centipawns using a sigmoid curve. A `GameSummary` object aggregates counts and accuracy percentage for the whole game.

---

### Evaluation Bar

- Vertical bar rendered in `EvaluationBar.java` in the left panel gutter
- White advantage fills from the bottom; black advantage from the top
- Full scale = ±1,000 centipawns (±10 pawns)
- Animated fill: target percentage approached at 14% of the remaining gap per 16 ms frame
- Mate overlay: bar snaps to 95% or 5% and shows "M*n*" label
- Live updates driven by `PositionEvaluator` running Stockfish in the background

---

### Settings Panel

Collapsible animated card (cubic ease, height-animated) in the right panel. Contains:

**Toggles**
- Show legal moves (dots on valid target squares)
- Show coordinates (a–h / 1–8 rank/file labels on board edges)
- Premove (allow queuing a move before the opponent plays)
- Auto-flip (rotate board after each move)
- Auto-queen (auto-promote pawns to queen without dialog)
- Sounds
- Low-time alerts (audio + visual warning under 30 s)
- Evaluation bar visibility
- Best-move arrow overlay
- Opening names display

**Dropdowns**
- Animations: Off / Minimal / Normal / Full
- Themes: 8 colour options (Blue / Green / Gray / Purple / Red / Orange / Yellow / Pink) — changes Start Game button gradient in real time
- Piece style selector

---

### Captured Material Strip

Displayed under each player name. Shows overlapping Unicode piece symbols with a spacing overlap of ~⅓ glyph width:

♟ pawns · ♞ knights · ♝ bishops · ♜ rooks · ♛ queens · ✹ Zoglins

A material advantage counter (+*n*) appears to the right when one side leads. The strip scales with the left panel, recalculating font size and preferred dimensions via `setVisualScale`.

---

### Player Info Bar

Each player bar (top = Black, bottom = White, swapped on board flip) contains:
- A grey silhouette avatar card (custom-drawn, scales with panel)
- Player name label
- Captured material strip
- A rounded pill clock label with a dark gradient background and subtle inner border glow

---

### Animated Popups

All in-game dialogs are undecorated `JDialog` windows with custom-painted rounded-rectangle panels (`RoundRectangle2D` via `setShape`):

- **Info popup** — white title, message, green OK button
- **Result popup** (checkmate, stalemate, Three-check, King of the Hill, King Captured) — badge label + large title + message + Continue button with hover state
- **Confirm popup** (resign, exit, draw offer) — gradient background, green Yes and red No buttons, both with hover glow
- All popups close on Escape or Enter

---

### Main Menu

A `JFrame` with `CardLayout` hosting six screens:

| Card | Contents |
|---|---|
| MENU | Title + logo, six menu items with icons, Credits link, Quit pill button, theme toggle |
| LOCAL_VARIANT | 4×2 grid of variant cards with icons and descriptions |
| BOT | Bot selection screen with info panel and Spectate option |
| ONLINE | Host/Join lobby with variant and time control pickers |
| ANALYSIS | Analysis mode entry card |
| CREDITS | Credits screen with background image |

**Dark/Light mode transition:** Clicking the sun/moon toggle in the bottom-right corner triggers a circular reveal animation expanding from the toggle's centre. The circle is drawn on a `JLayeredPane.DRAG_LAYER` overlay using an `Ellipse2D` clip. The reveal phase lasts 580 ms with a cubic ease-out; the overlay then fades out over 140 ms. The toggle button spins 360° with a cubic ease-out over 300 ms.

**Menu item hover:** Each item has a rounded card background that appears on mouse-enter with a drop shadow offset of 1 px.

**Keyboard shortcuts in menu:** 1–8 navigate items; 7 = Credits; 8 = Quit; Space toggles dark mode; Escape returns to the previous screen.

---

### Bot Selection Screen

Separate `JFrame` (1440 × 810) with:
- Left card: bot portrait, name, ELO/depth/skill line, multi-paragraph backstory text area with styled scrollbar
- Right card: `BotPanel` with one row per bot; selected row highlighted in blue; Start Game and Spectate action buttons
- "BOT ARENA" badge in the header
- Back to Menu button

---

### Bot Spectate Screen

Separate `JFrame` with side-by-side White and Black bot selector cards. Each card has:
- Heading (WHITE SIDE / BLACK SIDE) with accent colour badge
- Styled `JComboBox` with custom `BasicComboBoxUI` (dark dropdown, rounded corners)
- Portrait, name, difficulty stars, ELO, depth/skill line, description — all updated live on combo selection
- "VS" disc between the two cards
- Start Spectating and Back buttons

---

### Online Panel

Full-width online lobby card with three sections:
- Variant grid (Classic, Chess960, Three-check, Atomic, Spell Chess, Fog of War, Duck Chess — King of the Hill shows "coming soon")
- Time control button grid (4 presets per row)
- Connection row: IP text field, port field, Host and Join buttons, status label

---

### Puzzles

`puzzles.java` — a standalone `JFrame` that renders a **fantasy map** with 100 puzzle levels:
- Custom `MapCanvas` (a `JPanel` subclass) draws a winding path through a parchment-textured background
- Each level is a circular node on the path, colour-coded: gold = completed (10), green = unlocked (levels 11–16), grey = locked (17–100)
- Sparkle particle effects animate on hovered / nearby nodes using a `Timer` at 30 ms per frame
- The map pans via drag-to-pan (mouse drag translates a viewport offset) and scroll-wheel pan
- On open, the viewport scrolls to show the lowest unlocked level
- Level nodes show a lock icon when locked, a star when completed

---

### Fullscreen

`FullscreenToggle.java`:
- F11 toggles fullscreen globally via a `KeyboardFocusManager` dispatcher
- `FullscreenToggle.enter(frame)` is called automatically when a game window opens
- Previous window bounds and extended state are saved as client properties and restored on exit
- Transition uses a `Timer` at 8 ms per frame for smooth animation

---

### Sound

`SoundManager.java` plays audio clips for:
- Piece move
- Piece capture
- Check
- Castling
- Spell cast effects (per-spell audio cues)
- Low-time alert

Sounds are loaded from `assets/sounds/` and can be toggled in Settings.

---

### Keyboard Hotkeys (In-Game)

| Shortcut | Action |
|---|---|
| 1 / 2 / 3 | Select spell card 1/2/3 (Spell Chess) or time preset 1/2/3 (pre-game) |
| 4–9 | Select time preset 4–9 (pre-game) |
| Arrow keys | Navigate time preset grid (pre-game) |
| Enter | Start game (pre-game) / confirm selected spell (Spell Chess) |
| Escape | Cancel spell selection or pending target / exit prompt |
| Shift + Escape | Exit to Menu button |
| Ctrl + R | Reset Board |
| Ctrl + F | Flip board |
| Ctrl + C | Toggle coordinates |
| Ctrl + Shift + A | Toggle animations |
| Ctrl + Shift + F | Toggle auto-flip |
| Shift + Down | Expand settings dropdown |
| Shift + Up | Collapse settings dropdown |
| F11 | Toggle fullscreen |
| Alt + F4 | Exit confirmation |

---

## Credits

- **Lead Programmer:** [see in-app Credits screen]
- **Lead Graphics:** [see in-app Credits screen]

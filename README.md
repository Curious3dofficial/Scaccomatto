# Scaccomatto Chess Application

## Latest Updates

- Added chess engine integration and analysis support through `StockfishEngine.java`.
- Added opening detection and opening book reference using `OpeningDetector.java` and `OpeningBookData.java`.
- Added online play support with `NetworkManager.java` and `OnlinePanel.java`.
- Added expanded gameplay modes including variants and spell chess features.
- Added time control and match setup support via `TimeControlPanel.java`.
- Added move history, move notation, and captured piece tracking.
- Added UI improvements including fullscreen support, settings, and a polished menu.

## Overview

Scaccomatto is a Java-based chess application built with a Swing user interface. It includes standard chess rules, piece movement, move history, engine support, and a polished desktop experience.

The project is structured for local use and development, with source files in `src/` and compiled output in `bin/`.

## Build and Run

### Compile

```bash
cd Scaccomatto_final/Scaccomatto
javac src/*.java -d bin
```

### Run

```bash
cd Scaccomatto_final/Scaccomatto
java -cp bin Main
```

> Requires Java 8 or newer.

## Project Structure

- `src/` — Java source code
- `bin/` — compiled classes
- `assets/` — images, sounds, and UI resources
- `engines/` — chess engine integration files

## What It Contains

- `Main.java` — application entry point
- `MainMenu.java` — game menu and navigation
- `ChessGame.java` — core game loop and state
- `Board.java` — chessboard rendering and interaction
- piece classes (`Pawn.java`, `Knight.java`, `Bishop.java`, `Rook.java`, `Queen.java`, `King.java`)
- `MoveHistoryPanel.java` — move notation and history display
- `EngineAnalysis.java` and `StockfishEngine.java` — engine-related components
- `OpeningBookData.java` — opening reference support
- `SoundManager.java` — audio playback control

## Gameplay

Players can move pieces on the board using standard chess rules. The application handles:

- legal piece movement
- castling
- pawn promotion
- move validation
- capturing pieces
- tracking move history

## Notes

This repository is intended as a complete standalone desktop chess application. Adjustments and enhancements can be made by modifying the source files in `src/` and recompiling.

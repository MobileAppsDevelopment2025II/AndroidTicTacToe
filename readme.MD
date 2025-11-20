
# Android TicTacToe

Simple Tic-Tac-Toe game for Android written in Kotlin.

The app supports:

- **Single–player vs CPU** (with difficulty levels)
- **Two–player online mode** using **Firebase Authentication (anonymous)** and **Cloud Firestore**
- Custom drawing of the board using a `BoardView`
- Sound effects per player move

This project is based on classic Android views (XML layouts + `AppCompatActivity`), not Jetpack Compose.

---

## Features

### 1. Single–player vs Android

- Human vs CPU on a 3×3 grid.
- Difficulty levels:
  - Easy  
  - Harder  
  - Expert
- Visual feedback of whose turn it is.
- Sound effects:
  - “Human move” sound when the user plays.
  - “CPU move” sound when Android plays.
- Dialogs for:
  - Changing difficulty  
  - New game  
  - About dialog  
  - Quit confirmation  

### 2. Online Multiplayer (Firebase)

- One player creates an **online game** and gets a short **code** (e.g. `A9F3K`).
- The second player joins by entering that code.
- Game state is synchronized in real time via **Cloud Firestore**:
  - Board state (`X` and `O` positions)
  - Current turn
  - Game status and winner
- Both devices see the board update instantly after each move.
- Each device:
  - Plays a “human move” sound for **its own** moves.
  - Plays the “CPU move” sound when the **other player** moves.

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Android Views + XML (`AppCompatActivity`, `MaterialToolbar`, `BoardView`)
- **Min SDK:** 24  
- **Target / Compile SDK:** 36 (at the time of writing)
- **Firebase:**
  - Authentication (anonymous sign-in)
  - Cloud Firestore (real-time game state)
- **Build system:** Gradle Kotlin DSL (`build.gradle.kts`)
- **Package name:** `co.edu.unal.tictactoe`

---

## Project Structure (high level)

```text
AndroidTicTacToe/
 ├─ app/
 │   ├─ src/main/java/co/edu/unal/tictactoe/
 │   │   ├─ MainActivity.kt          # Single-player vs CPU
 │   │   ├─ BoardView.kt             # Custom view that draws the grid
 │   │   ├─ TicTacToeGame.kt         # Game logic (board, moves, winner)
 │   │   └─ online/
 │   │       ├─ OnlineGame.kt        # Firestore document model
 │   │       └─ OnlineGameActivity.kt# Online multiplayer UI + logic
 │   ├─ src/main/res/layout/
 │   │   ├─ activity_main.xml        # Single-player UI
 │   │   └─ activity_online.xml      # Online mode UI
 │   ├─ src/main/res/raw/
 │   │   ├─ move_human.mp3           # Sound when the user plays
 │   │   └─ move_cpu.mp3             # Sound when the other side plays
 │   ├─ google-services.json         # Firebase configuration (NOT in git)
 │   └─ build.gradle.kts
 ├─ build.gradle.kts                 # Root Gradle config
 └─ settings.gradle.kts
````



## Game Logic Overview

`TicTacToeGame` encapsulates the core rules:

* 1D board of size 9 (`0..8`), mapped to 3×3 grid.
* Players:

  * `HUMAN_PLAYER` → `'X'`
  * `COMPUTER_PLAYER` → `'O'`
* Core methods:

  * `clearBoard()`
  * `setMove(player: Char, location: Int): Boolean`
  * `checkForWinner(): Int` (0 = no winner, 1 = tie, 2 = human, 3 = computer)
  * `getComputerMove(): Int` (AI move based on difficulty)

In **online mode**, the AI is not used; the board is updated from Firestore snapshots, and moves are validated inside a Firestore transaction.

---

## Online Mode Architecture (Firebase)

Each online game is stored as a single Firestore document in the `games` collection:

```kotlin
data class OnlineGame(
    val board: List<String> = List(9) { "" }, // "", "X", "O"
    val hostId: String = "",
    val guestId: String = "",
    val currentTurn: String = "",
    val status: String = "waiting",           // "waiting" | "playing" | "finished"
    val winner: String = "",                  // "", hostId, guestId, or "draw"
    val code: String = ""                     // short shareable code, e.g. "A9F3K"
)
```

* **Host player**

  * Signs in anonymously.
  * Taps **Create game**.
  * A random `code` is generated and stored in the document.
  * The app starts listening to that document.
* **Guest player**

  * Signs in anonymously.
  * Enters the `code` and taps **Join**.
  * The app searches `games` where `code == inputCode` and `status == "waiting"`.
  * Sets `guestId` and switches `status` to `"playing"`.

### Moves and Transactions

When the user taps a cell in `OnlineGameActivity`:

1. The app calculates the index (`0..8`) based on the touch coordinates.
2. Runs a Firestore transaction:

   * Reads the current `OnlineGame` document.
   * Validates:

     * `status == "playing"`
     * Cell is empty.
     * `currentTurn` is the current user’s `uid`.
   * Writes:

     * Updated `board` with `"X"` or `"O"` (host is `"X"`, guest is `"O"`).
     * Updated `currentTurn` (other player’s `uid` or empty if finished).
     * `status = "finished"` and `winner` if there is a win or draw.
3. Because both devices are listening to that document, both UIs update in real-time.

---

## Firebase Setup

To use the online mode, you must configure your own Firebase project.

### 1. Create a Firebase project

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new project (or use an existing one).

### 2. Register the Android app

1. In your Firebase project, add a new **Android app**.
2. Set the **Android package name** to match the app:

   * `co.edu.unal.tictactoe`
3. (Optional) Set app nickname and SHA-1.
4. Download the generated `google-services.json`.

### 3. Put `google-services.json` in the correct place

Place the file in the **module `app` directory**:

```text
AndroidTicTacToe/
  app/
    google-services.json    <-- here
    build.gradle.kts
    src/
```

> Do not put this file inside any nested `AndroidTicTacToe/app` folder from the original repo structure.
> The module that builds the app is `<root>/app`.

### 4. Enable Anonymous Authentication

In Firebase Console:

1. Go to **Authentication → Sign-in method**.
2. Enable **Anonymous** sign-in.

The online mode uses anonymous users for simplicity; each device gets a unique `uid` automatically.

### 5. Create a Firestore database

1. Go to **Cloud Firestore**.
2. Click **Create database**.
3. For development, you can start in test mode (allow read/write) and later tighten security rules.

---

## Ignoring `google-services.json` in Git

`google-services.json` contains project–specific config and is **not** meant to be committed to version control.

In the root `.gitignore`, add:

```gitignore
# Ignore Firebase config files
**/google-services.json
```

If you already committed it previously:

```bash
git rm --cached app/google-services.json
git commit -m "Remove google-services.json from repo and ignore it"
git push
```

Each developer should download their own `google-services.json` from Firebase and place it under `app/`.

You may optionally provide a template file, e.g. `app/google-services.example.json`, with a comment indicating where to put the real file.

---

## Building & Running

### Requirements

* Android Studio (Giraffe or newer is recommended)
* Android SDK 24+
* A configured Firebase project (for online mode)

### Steps

1. Clone the repository:

   ```bash
   git clone <repo-url>
   cd AndroidTicTacToe
   ```

2. Open the project in Android Studio.

3. Make sure you have:

   * Correct `google-services.json` under `app/`.
   * Internet permission in `AndroidManifest.xml`:

     ```xml
     <uses-permission android:name="android.permission.INTERNET" />
     ```

4. Build and run on a device or emulator.

To test **online multiplayer**, run the app on **two physical devices or emulators** and connect them to the same Firebase project.

---

## How to Play

### Single–player vs Android

1. Launch the app.
2. You start as **X** (human).
3. Tap a cell to place your mark.
4. Android responds after a short delay.
5. Use the top menu:

   * **New Game** to reset.
   * **Difficulty** to choose between Easy / Harder / Expert.
   * **About** to see app info.
   * **Quit** to close the game.

### Online Match

1. In the top-right menu, select **Online match**.
2. On the first device (Host):

   * Tap **Create game**.
   * A code will be generated and displayed, e.g. `Code: A9F3K`.
   * Share this code with your friend.
3. On the second device (Guest):

   * Enter the code in the text field.
   * Tap **Join**.
4. Once both are connected:

   * The host plays as **X**, the guest plays as **O**.
   * The info label shows whose turn it is.
   * Both boards update in real time.

---

## Future Improvements (Ideas)

* Display “You are X / You are O” explicitly on the online screen.
* Add rematch / “Play again” button at the end of a match.
* Add user-friendly errors when Firestore or network is unavailable.
* Move Firebase code into a clean repository layer (e.g. `OnlineGameRepository`).
* Add E2E tests for game logic and winner detection.

---




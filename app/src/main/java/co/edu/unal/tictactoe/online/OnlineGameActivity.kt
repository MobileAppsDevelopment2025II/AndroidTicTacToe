package co.edu.unal.tictactoe.online

import android.media.MediaPlayer
import android.os.Bundle
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import co.edu.unal.tictactoe.BoardView
import co.edu.unal.tictactoe.R
import co.edu.unal.tictactoe.TicTacToeGame
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.FirebaseApp


class OnlineGameActivity : AppCompatActivity() {

    private lateinit var mGame: TicTacToeGame
    private lateinit var mBoardView: BoardView
    private lateinit var mInfoTextView: TextView
    private lateinit var mCodeTextView: TextView
    private lateinit var mCreateButton: Button
    private lateinit var mJoinButton: Button
    private lateinit var mJoinCodeEditText: EditText

    // NUEVO
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val games get() = db.collection("games")


    private var currentGameId: String? = null
    private var currentGame: OnlineGame? = null
    private var registration: ListenerRegistration? = null

    private var mHumanMediaPlayer: MediaPlayer? = null
    private var mComputerMediaPlayer: MediaPlayer? = null

    // Para detectar qué casilla cambió entre un snapshot y otro
    private var lastBoard: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online)

        // 🔥 Inicializar Firebase en esta app/proceso
        FirebaseApp.initializeApp(this)

        // Ahora sí podemos obtener las instancias
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.online_title)
        toolbar.setNavigationOnClickListener { finish() }

        mInfoTextView = findViewById(R.id.information)
        mBoardView = findViewById(R.id.board)
        mCodeTextView = findViewById(R.id.text_game_code)
        mCreateButton = findViewById(R.id.button_create)
        mJoinButton = findViewById(R.id.button_join)
        mJoinCodeEditText = findViewById(R.id.edit_join_code)

        mGame = TicTacToeGame()
        mBoardView.setGame(mGame)

        signInAnonymousIfNeeded()

        mCreateButton.setOnClickListener { createGame() }
        mJoinButton.setOnClickListener {
            val code = mJoinCodeEditText.text.toString()
            joinGameByCode(code)
        }

        mBoardView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                handleBoardTouch(event.x, event.y)
            }
            true
        }
    }


    private fun signInAnonymousIfNeeded() {
        if (auth.currentUser != null) {
            mInfoTextView.text = getString(R.string.online_choose_action)
            return
        }
        mInfoTextView.text = getString(R.string.online_signing_in)

        auth.signInAnonymously()
            .addOnSuccessListener {
                mInfoTextView.text = getString(R.string.online_choose_action)
            }
            .addOnFailureListener { e ->
                mInfoTextView.text = getString(
                    R.string.online_signin_error,
                    e.message ?: "unknown"
                )
            }
    }

    private fun createGame() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Signing in, try again in a moment", Toast.LENGTH_SHORT).show()
            signInAnonymousIfNeeded()
            return
        }

        val code = generateCode()

        // 👀 Debug: ver que el código se genera
        Toast.makeText(this, "Generated code: $code", Toast.LENGTH_SHORT).show()

        val game = OnlineGame(
            board = List(9) { "" },
            hostId = user.uid,
            guestId = "",
            currentTurn = user.uid,
            status = "waiting",
            winner = "",
            code = code              // <-- importantísimo
        )

        mInfoTextView.text = getString(R.string.online_creating)

        games.add(game)
            .addOnSuccessListener { ref ->
                Toast.makeText(this, "Game created: ${ref.id}", Toast.LENGTH_SHORT).show()
                listenToGame(ref.id)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error creating game: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun joinGameByCode(codeInput: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Signing in, try again in a moment", Toast.LENGTH_SHORT).show()
            signInAnonymousIfNeeded()
            return
        }

        val code = codeInput.trim().uppercase()
        if (code.isEmpty()) {
            Toast.makeText(this, "Enter a code", Toast.LENGTH_SHORT).show()
            return
        }

        mInfoTextView.text = getString(R.string.online_searching)

        games.whereEqualTo("code", code)
            .whereEqualTo("status", "waiting")
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()
                if (doc == null) {
                    mInfoTextView.text = getString(R.string.online_not_found)
                    return@addOnSuccessListener
                }

                val gameId = doc.id
                games.document(gameId)
                    .update(
                        mapOf(
                            "guestId" to user.uid,
                            "status" to "playing",
                            "currentTurn" to (doc.getString("hostId") ?: user.uid)
                        )
                    )
                    .addOnSuccessListener {
                        listenToGame(gameId)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error joining game: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                mInfoTextView.text = getString(
                    R.string.online_search_error,
                    e.message ?: "unknown"
                )
            }
    }

    private fun listenToGame(gameId: String) {
        registration?.remove()
        currentGameId = gameId

        registration = games.document(gameId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    mInfoTextView.text = "Listener error: ${error.message}"
                    return@addSnapshotListener
                }
                if (snap != null && snap.exists()) {
                    val game = snap.toObject(OnlineGame::class.java) ?: return@addSnapshotListener
                    currentGame = game
                    updateUiFromGame(game)
                }
            }
    }

    private fun updateUiFromGame(game: OnlineGame) {
        // 0. Guardar el tablero anterior y actualizar lastBoard
        val previousBoard = lastBoard
        lastBoard = game.board.toList()

        // 1. Actualizar el modelo gráfico para que BoardView pinte bien
        mGame.clearBoard()

        val xChar = TicTacToeGame.HUMAN_PLAYER
        val oChar = TicTacToeGame.COMPUTER_PLAYER

        for (i in game.board.indices) {
            when (game.board[i]) {
                "X" -> mGame.setMove(xChar, i)
                "O" -> mGame.setMove(oChar, i)
            }
        }
        mBoardView.invalidate()

        // 2. Identificar quién soy yo (host = X, guest = O)
        val uid = auth.currentUser?.uid
        val isHost = uid != null && uid == game.hostId
        val mySymbol = if (isHost) "X" else "O"

        // 3. Detectar qué casilla cambió y reproducir sonido
        if (previousBoard != null && previousBoard.size == game.board.size) {
            var changedIndex = -1
            for (i in game.board.indices) {
                if (previousBoard[i] != game.board[i]) {
                    changedIndex = i
                    break
                }
            }

            if (changedIndex != -1) {
                val newSymbol = game.board[changedIndex]
                if (newSymbol == mySymbol) {
                    mHumanMediaPlayer?.start()
                } else if (newSymbol.isNotEmpty()) {
                    mComputerMediaPlayer?.start()
                }
            }
        }

        // 4. Mostrar el código de la partida
        if (game.code.isNotEmpty()) {
            mCodeTextView.text = "Code: ${game.code}"
        } else {
            mCodeTextView.text = ""
        }

        // 5. Mensaje de estado
        val statusText = when {
            game.status == "waiting" ->
                "Share this code and wait for the other player."

            game.status == "playing" && game.currentTurn == uid ->
                "Your turn ($mySymbol)"

            game.status == "playing" ->
                "Opponent's turn"

            game.status == "finished" && game.winner == "draw" ->
                "Draw!"

            game.status == "finished" && game.winner == uid ->
                "You win!"

            game.status == "finished" && game.winner.isNotEmpty() ->
                "You lose!"

            else -> ""
        }

        mInfoTextView.text = statusText
    }



    private fun handleBoardTouch(x: Float, y: Float) {
        val game = currentGame ?: return
        if (game.status != "playing") return

        val cellW = mBoardView.getBoardCellWidth()
        val cellH = mBoardView.getBoardCellHeight()
        if (cellW <= 0 || cellH <= 0) return

        val col = (x.toInt() / cellW)
        val row = (y.toInt() / cellH)
        val pos = row * 3 + col

        if (pos < 0 || pos >= TicTacToeGame.BOARD_SIZE) return

        makeMove(pos)
    }

    private fun makeMove(position: Int) {
        val uid = auth.currentUser?.uid ?: return
        val gameId = currentGameId ?: return
        val gameRef = games.document(gameId)

        db.runTransaction { tx ->
            val snap = tx.get(gameRef)
            if (!snap.exists()) return@runTransaction null

            val game = snap.toObject(OnlineGame::class.java) ?: return@runTransaction null

            if (game.status != "playing") return@runTransaction null
            if (position !in 0..8) return@runTransaction null
            if (game.board[position].isNotEmpty()) return@runTransaction null
            if (game.currentTurn != uid) return@runTransaction null

            val isHost = uid == game.hostId
            val symbol = if (isHost) "X" else "O"

            val newBoard = game.board.toMutableList()
            newBoard[position] = symbol

            var newStatus = game.status
            var newWinner = game.winner
            var nextTurn = if (uid == game.hostId) game.guestId else game.hostId

            val winnerSymbol = checkWinnerSymbol(newBoard)
            if (winnerSymbol != null) {
                newStatus = "finished"
                newWinner = if (winnerSymbol == "X") game.hostId else game.guestId
                nextTurn = ""
            } else if (newBoard.all { it.isNotEmpty() }) {
                newStatus = "finished"
                newWinner = "draw"
                nextTurn = ""
            }

            val updated = game.copy(
                board = newBoard,
                status = newStatus,
                winner = newWinner,
                currentTurn = nextTurn
            )

            tx.set(gameRef, updated)
            null
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Move error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkWinnerSymbol(board: List<String>): String? {
        val lines = listOf(
            listOf(0, 1, 2),
            listOf(3, 4, 5),
            listOf(6, 7, 8),
            listOf(0, 3, 6),
            listOf(1, 4, 7),
            listOf(2, 5, 8),
            listOf(0, 4, 8),
            listOf(2, 4, 6)
        )

        for ((a, b, c) in lines) {
            if (board[a].isNotEmpty() &&
                board[a] == board[b] &&
                board[a] == board[c]
            ) {
                return board[a]
            }
        }
        return null
    }

    private fun generateCode(length: Int = 5): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val sb = StringBuilder()
        val random = java.util.Random()
        repeat(length) {
            val idx = random.nextInt(chars.length)
            sb.append(chars[idx])
        }
        return sb.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        registration?.remove()
    }


    override fun onResume() {
        super.onResume()
        // Usa los mismos audios de la versión vs máquina
        mHumanMediaPlayer = MediaPlayer.create(applicationContext, R.raw.move_human)
        mComputerMediaPlayer = MediaPlayer.create(applicationContext, R.raw.move_cpu)
    }

    override fun onPause() {
        super.onPause()
        try { mHumanMediaPlayer?.release() } catch (_: Exception) {}
        try { mComputerMediaPlayer?.release() } catch (_: Exception) {}
        mHumanMediaPlayer = null
        mComputerMediaPlayer = null
    }

}

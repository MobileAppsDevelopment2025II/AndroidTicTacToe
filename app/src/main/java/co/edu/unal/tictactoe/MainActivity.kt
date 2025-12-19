package co.edu.unal.tictactoe

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import co.edu.unal.tictactoe.online.OnlineGameActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mGame: TicTacToeGame
    private lateinit var mBoardView: BoardView
    private lateinit var mInfoTextView: TextView

    private var mGameOver = false

    // Sonidos
    private var mHumanMediaPlayer: MediaPlayer? = null
    private var mComputerMediaPlayer: MediaPlayer? = null

    // Retardo para el turno de la computadora (para que se lea "Android turn")
    private val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Toolbar como ActionBar (asegúrate de tenerla en activity_main.xml con id "toolbar")
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Si quieres centrar/ocultar el título, configúralo en el XML o aquí con supportActionBar

        mInfoTextView = findViewById(R.id.information)
        mBoardView = findViewById(R.id.board)

        mGame = TicTacToeGame()
        mBoardView.setGame(mGame)

        if (savedInstanceState == null) {
            startNewGame()
        } else {
            restoreFromState(savedInstanceState)
        }

        // Listener de toques sobre el tablero
        mBoardView.setOnTouchListener { _: View, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_DOWN && !mGameOver) {
                val cellW = mBoardView.getBoardCellWidth()
                val cellH = mBoardView.getBoardCellHeight()
                if (cellW > 0 && cellH > 0) {
                    val col = event.x.toInt() / cellW
                    val row = event.y.toInt() / cellH
                    val pos = row * 3 + col

                    if (pos >= 0 && pos < TicTacToeGame.BOARD_SIZE) {
                        if (setMove(TicTacToeGame.HUMAN_PLAYER, pos)) {
                            mHumanMediaPlayer?.start()
                            var winner = mGame.checkForWinner()

                            if (winner == 0) {
                                mInfoTextView.setText(R.string.turn_computer)
                                // Juega la computadora tras 1s
                                uiHandler.postDelayed({
                                    val move = mGame.getComputerMove()
                                    if (move != -1) {
                                        setMove(TicTacToeGame.COMPUTER_PLAYER, move)
                                        mComputerMediaPlayer?.start()
                                    }
                                    winner = mGame.checkForWinner()
                                    when (winner) {
                                        0 -> mInfoTextView.setText(R.string.turn_human)
                                        1 -> endGame(R.string.result_tie)
                                        2 -> endGame(R.string.result_human_wins)
                                        3 -> endGame(R.string.result_computer_wins)
                                    }
                                }, 1000L)
                            } else {
                                when (winner) {
                                    1 -> endGame(R.string.result_tie)
                                    2 -> endGame(R.string.result_human_wins)
                                    3 -> endGame(R.string.result_computer_wins)
                                }
                            }
                        }
                    }
                }
            }
            false
        }
    }

    private fun startNewGame() {
        mGame.clearBoard()
        mGameOver = false
        mBoardView.invalidate()
        mInfoTextView.setText(R.string.first_human)
    }

    private fun endGame(messageRes: Int) {
        mGameOver = true
        mInfoTextView.setText(messageRes)
    }

    private fun setMove(player: Char, location: Int): Boolean {
        if (mGame.setMove(player, location)) {
            mBoardView.invalidate()
            return true
        }
        return false
    }

    private fun restoreFromState(state: Bundle) {
        val board: CharArray? = state.getCharArray("board")
        mGameOver = state.getBoolean("gameOver", false)
        mInfoTextView.text = state.getCharSequence("info") ?: getString(R.string.first_human)

        // Restaurar dificultad
        val diffOrdinal = state.getInt("difficulty", TicTacToeGame.DifficultyLevel.Expert.ordinal)
        val levels = TicTacToeGame.DifficultyLevel.values()
        val safeIdx = if (diffOrdinal >= 0 && diffOrdinal < levels.size) diffOrdinal
        else TicTacToeGame.DifficultyLevel.Expert.ordinal
        mGame.setDifficultyLevel(levels[safeIdx])

        // Reconstruir el tablero en el modelo
        mGame.clearBoard()
        if (board != null) {
            var i = 0
            while (i < board.size && i < TicTacToeGame.BOARD_SIZE) {
                val ch = board[i]
                if (ch == TicTacToeGame.HUMAN_PLAYER || ch == TicTacToeGame.COMPUTER_PLAYER) {
                    mGame.setMove(ch, i)
                }
                i++
            }
        }
        mBoardView.invalidate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharArray("board", mGame.getBoardSnapshot())
        outState.putBoolean("gameOver", mGameOver)
        outState.putCharSequence("info", mInfoTextView.text)
        outState.putInt("difficulty", mGame.getDifficultyLevel().ordinal)
    }

    // ===== Menú =====
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_game -> { startNewGame(); true }
            R.id.action_difficulty -> { showDifficultyDialog(); true }
            R.id.action_quit -> { confirmQuitDialog(); true }
            R.id.action_about -> { showAboutDialog(); true }
            R.id.action_online -> {
                startActivity(Intent(this, OnlineGameActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDifficultyDialog() {
        val levelsText = arrayOf(
            getString(R.string.difficulty_easy),
            getString(R.string.difficulty_harder),
            getString(R.string.difficulty_expert)
        )
        val current = when (mGame.getDifficultyLevel()) {
            TicTacToeGame.DifficultyLevel.Easy -> 0
            TicTacToeGame.DifficultyLevel.Harder -> 1
            TicTacToeGame.DifficultyLevel.Expert -> 2
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.difficulty_choose)
            .setSingleChoiceItems(levelsText, current) { dialog, which ->
                val chosen = when (which) {
                    0 -> TicTacToeGame.DifficultyLevel.Easy
                    1 -> TicTacToeGame.DifficultyLevel.Harder
                    else -> TicTacToeGame.DifficultyLevel.Expert
                }
                mGame.setDifficultyLevel(chosen)
                Toast.makeText(applicationContext, levelsText[which], Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun confirmQuitDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.quit_question)
            .setPositiveButton(R.string.yes) { _, _ -> finish() }
            .setNegativeButton(R.string.no, null)
            .setCancelable(false)
            .show()
    }

    private fun showAboutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.about_dialog, null)
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
    }

    // ===== Sonidos =====
    override fun onResume() {
        super.onResume()
        // Asegúrate de tener res/raw/move_human.mp3 y res/raw/move_cpu.mp3
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

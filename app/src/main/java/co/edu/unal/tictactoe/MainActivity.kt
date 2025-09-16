package co.edu.unal.tictactoe

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mGame: TicTacToeGame
    private lateinit var mInfoTextView: TextView
    private lateinit var mBoardButtons: Array<Button>
    private var mGameOver = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // NUEVO: configura la toolbar para el menú
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mBoardButtons = arrayOf(
            findViewById<Button>(R.id.one),
            findViewById<Button>(R.id.two),
            findViewById<Button>(R.id.three),
            findViewById<Button>(R.id.four),
            findViewById<Button>(R.id.five),
            findViewById<Button>(R.id.six),
            findViewById<Button>(R.id.seven),
            findViewById<Button>(R.id.eight),
            findViewById<Button>(R.id.nine)
        )
        mInfoTextView = findViewById(R.id.information)
        mGame = TicTacToeGame()

        if (savedInstanceState == null) {
            startNewGame()
        } else {
            restoreFromState(savedInstanceState)
        }
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

        mGame.clearBoard()
        var idx = 0
        while (idx < mBoardButtons.size) {
            val btn = mBoardButtons[idx]
            val ch: Char = if (board != null && idx >= 0 && idx < board.size) board[idx]
            else TicTacToeGame.OPEN_SPOT

            if (ch == TicTacToeGame.OPEN_SPOT) {
                btn.text = ""
                btn.isEnabled = !mGameOver
            } else {
                mGame.setMove(ch, idx)
                paintButton(btn, ch)
            }
            idx++
        }
        attachClickListeners()
    }

    private fun startNewGame() {
        mGame.clearBoard()
        mGameOver = false

        var i = 0
        while (i < mBoardButtons.size) {
            val btn = mBoardButtons[i]
            btn.text = ""
            btn.isEnabled = true
            i++
        }
        attachClickListeners()
        mInfoTextView.setText(R.string.first_human)
    }

    private fun attachClickListeners() {
        var index = 0
        while (index < mBoardButtons.size) {
            val button = mBoardButtons[index]
            val pos = index
            button.setOnClickListener {
                if (!mGameOver && button.isEnabled) {
                    if (mGame.setMove(TicTacToeGame.HUMAN_PLAYER, pos)) {
                        paintButton(button, TicTacToeGame.HUMAN_PLAYER)
                        var winner = mGame.checkForWinner()

                        if (winner == 0) {
                            mInfoTextView.setText(R.string.turn_computer)
                            val move = mGame.getComputerMove()
                            if (move != -1) {
                                mGame.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
                                paintButton(mBoardButtons[move], TicTacToeGame.COMPUTER_PLAYER)
                            }
                            winner = mGame.checkForWinner()
                        }

                        when (winner) {
                            0 -> mInfoTextView.setText(R.string.turn_human)
                            1 -> endGame(R.string.result_tie)
                            2 -> endGame(R.string.result_human_wins)
                            3 -> endGame(R.string.result_computer_wins)
                        }
                    }
                }
            }
            index++
        }
    }

    private fun paintButton(btn: Button, player: Char) {
        btn.isEnabled = false
        btn.text = player.toString()
        btn.setTextColor(
            if (player == TicTacToeGame.HUMAN_PLAYER) Color.rgb(0, 200, 0) else Color.rgb(200, 0, 0)
        )
    }

    private fun endGame(messageRes: Int) {
        mGameOver = true
        var i = 0
        while (i < mBoardButtons.size) {
            mBoardButtons[i].isEnabled = false
            i++
        }
        mInfoTextView.setText(messageRes)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharArray("board", mGame.getBoardSnapshot())
        outState.putBoolean("gameOver", mGameOver)
        outState.putCharSequence("info", mInfoTextView.text)
        outState.putInt("difficulty", mGame.getDifficultyLevel().ordinal)
    }

    // ====== Menú ======
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ====== Diálogos ======
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
        val dialog = builder.create()
        dialog.show()
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

}

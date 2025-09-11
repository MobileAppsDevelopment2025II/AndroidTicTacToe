package co.edu.unal.tictactoe

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mGame: TicTacToeGame
    private lateinit var mInfoTextView: TextView
    private lateinit var mBoardButtons: Array<Button>
    private var mGameOver = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Tipamos explícitamente cada findViewById para que sean Buttons
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

        mGame.clearBoard()

        // Re-pintar y restaurar sin usar getOrNull (no existe para CharArray)
        for (idx in 0..(mBoardButtons.size - 1)) {
            val btn = mBoardButtons[idx]
            val ch: Char = if (board != null && idx >= 0 && idx < board.size) {
                board[idx]
            } else {
                TicTacToeGame.OPEN_SPOT
            }
            if (ch == TicTacToeGame.OPEN_SPOT) {
                btn.text = ""
                btn.isEnabled = !mGameOver
            } else {
                mGame.setMove(ch, idx)
                paintButton(btn, ch)
            }
        }

        attachClickListeners()
    }

    private fun startNewGame() {
        mGame.clearBoard()
        mGameOver = false

        for (btn in mBoardButtons) {
            btn.text = ""
            btn.isEnabled = true
        }
        attachClickListeners()
        mInfoTextView.setText(R.string.first_human)
    }

    private fun attachClickListeners() {
        for (index in 0..(mBoardButtons.size - 1)) {
            val button = mBoardButtons[index]
            button.setOnClickListener {
                if (!mGameOver && button.isEnabled) {
                    if (mGame.setMove(TicTacToeGame.HUMAN_PLAYER, index)) {
                        paintButton(button, TicTacToeGame.HUMAN_PLAYER)
                        var winner = mGame.checkForWinner()

                        if (winner == 0) {
                            mInfoTextView.setText(R.string.turn_computer)
                            val move = mGame.getComputerMove()
                            mGame.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
                            paintButton(mBoardButtons[move], TicTacToeGame.COMPUTER_PLAYER)
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
        for (btn in mBoardButtons) {
            btn.isEnabled = false
        }
        mInfoTextView.setText(messageRes)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharArray("board", mGame.getBoardSnapshot())
        outState.putBoolean("gameOver", mGameOver)
        outState.putCharSequence("info", mInfoTextView.text)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_new_game) {
            startNewGame()
            true
        } else super.onOptionsItemSelected(item)
    }
}

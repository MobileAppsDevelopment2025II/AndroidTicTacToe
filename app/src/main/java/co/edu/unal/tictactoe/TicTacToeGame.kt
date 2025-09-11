package co.edu.unal.tictactoe

import kotlin.random.Random

class TicTacToeGame(private val rng: Random = Random.Default) {

    companion object {
        const val BOARD_SIZE = 9
        const val OPEN_SPOT = ' '
        const val HUMAN_PLAYER = 'X'
        const val COMPUTER_PLAYER = 'O'
    }

    private val board = CharArray(BOARD_SIZE) { OPEN_SPOT }

    fun clearBoard() {
        var i = 0
        while (i < BOARD_SIZE) {
            board[i] = OPEN_SPOT
            i++
        }
    }

    fun setMove(player: Char, location: Int): Boolean {
        if (location < 0 || location >= BOARD_SIZE) return false
        if (board[location] != OPEN_SPOT) return false
        board[location] = player
        return true
    }

    fun getComputerMove(): Int {
        // 1) Ganar si puede
        var i = 0
        while (i < BOARD_SIZE) {
            if (board[i] == OPEN_SPOT) {
                board[i] = COMPUTER_PLAYER
                val w = checkForWinner()
                board[i] = OPEN_SPOT
                if (w == 3) return i
            }
            i++
        }

        // 2) Bloquear al humano
        i = 0
        while (i < BOARD_SIZE) {
            if (board[i] == OPEN_SPOT) {
                board[i] = HUMAN_PLAYER
                val w = checkForWinner()
                board[i] = OPEN_SPOT
                if (w == 2) return i
            }
            i++
        }

        // 3) Centro, esquinas, lados
        val preferred = intArrayOf(4, 0, 2, 6, 8, 1, 3, 5, 7)
        var j = 0
        while (j < preferred.size) {
            val idx = preferred[j]
            if (board[idx] == OPEN_SPOT) return idx
            j++
        }

        // 4) Primer espacio libre
        i = 0
        while (i < BOARD_SIZE) {
            if (board[i] == OPEN_SPOT) return i
            i++
        }
        return 0
    }

    /**
     * 0 = nadie aún, 1 = empate, 2 = gana X (humano), 3 = gana O (Android)
     */
    fun checkForWinner(): Int {
        val lines = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8), // filas
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8), // columnas
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)                        // diagonales
        )

        var k = 0
        while (k < lines.size) {
            val line = lines[k]
            val a = line[0]
            val b = line[1]
            val c = line[2]

            if (board[a] != OPEN_SPOT && board[a] == board[b] && board[b] == board[c]) {
                return if (board[a] == HUMAN_PLAYER) 2 else 3
            }
            k++
        }

        // ¿Tablero lleno?
        var filled = true
        var i = 0
        while (i < BOARD_SIZE) {
            if (board[i] == OPEN_SPOT) {
                filled = false
                break
            }
            i++
        }
        return if (filled) 1 else 0
    }

    /** Snapshot sin copyOf (compatibilidad total) */
    fun getBoardSnapshot(): CharArray {
        val snap = CharArray(BOARD_SIZE)
        var i = 0
        while (i < BOARD_SIZE) {
            snap[i] = board[i]
            i++
        }
        return snap
    }
}
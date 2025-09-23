package co.edu.unal.tictactoe

import kotlin.random.Random

class TicTacToeGame(private val rng: Random = Random.Default) {

    companion object {
        const val BOARD_SIZE = 9
        const val OPEN_SPOT = ' '
        const val HUMAN_PLAYER = 'X'
        const val COMPUTER_PLAYER = 'O'
    }

    // ===== Dificultad =====
    enum class DifficultyLevel { Easy, Harder, Expert }

    private var mDifficultyLevel: DifficultyLevel = DifficultyLevel.Expert
    fun getDifficultyLevel(): DifficultyLevel = mDifficultyLevel
    fun setDifficultyLevel(level: DifficultyLevel) { mDifficultyLevel = level }

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

    // === Selección de jugada según dificultad ===
    fun getComputerMove(): Int {
        return when (mDifficultyLevel) {
            DifficultyLevel.Easy -> getRandomMove()
            DifficultyLevel.Harder -> {
                val win = getWinningMove()
                if (win != -1) win else getRandomMove()
            }
            DifficultyLevel.Expert -> {
                var move = getWinningMove()
                if (move == -1) move = getBlockingMove()
                if (move == -1) move = getRandomMove()
                move
            }
        }
    }

    // Movimiento aleatorio válido
    private fun getRandomMove(): Int {
        // Si no hay huecos, -1
        var freeCount = 0
        var i = 0
        while (i < BOARD_SIZE) {
            if (board[i] == OPEN_SPOT) freeCount++
            i++
        }
        if (freeCount == 0) return -1

        // Elegir al azar uno de los libres
        val target = rng.nextInt(freeCount)
        var seen = 0
        i = 0
        while (i < BOARD_SIZE) {
            if (board[i] == OPEN_SPOT) {
                if (seen == target) return i
                seen++
            }
            i++
        }
        return -1
    }

    // Ganar si es posible (devuelve índice o -1)
    private fun getWinningMove(): Int {
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
        return -1
    }

    // Bloquear al humano si va a ganar (devuelve índice o -1)
    private fun getBlockingMove(): Int {
        var i = 0
        while (i < BOARD_SIZE) {
            if (board[i] == OPEN_SPOT) {
                board[i] = HUMAN_PLAYER
                val w = checkForWinner()
                board[i] = OPEN_SPOT
                if (w == 2) return i
            }
            i++
        }
        return -1
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
            val a = lines[k][0]
            val b = lines[k][1]
            val c = lines[k][2]
            if (board[a] != OPEN_SPOT && board[a] == board[b] && board[b] == board[c]) {
                return if (board[a] == HUMAN_PLAYER) 2 else 3
            }
            k++
        }
        var filled = true
        var i = 0
        while (i < BOARD_SIZE) {
            if (board[i] == OPEN_SPOT) { filled = false; break }
            i++
        }
        return if (filled) 1 else 0
    }

    fun getBoardSnapshot(): CharArray {
        val snap = CharArray(BOARD_SIZE)
        var i = 0
        while (i < BOARD_SIZE) { snap[i] = board[i]; i++ }
        return snap
    }

    fun getBoardOccupant(index: Int): Char {
        return if (index >= 0 && index < BOARD_SIZE) {
            // Devuelve 'X', 'O' o ' ' (OPEN_SPOT)
            val snap = getBoardSnapshot()
            snap[index]
        } else {
            OPEN_SPOT
        }
    }

}

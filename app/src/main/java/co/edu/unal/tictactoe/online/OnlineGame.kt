package co.edu.unal.tictactoe.online

data class OnlineGame(
    val board: List<String> = List(9) { "" }, // "", "X", "O"
    val hostId: String = "",
    val guestId: String = "",
    val currentTurn: String = "",
    val status: String = "waiting", // waiting, playing, finished
    val winner: String = "",
    val code: String = ""
)

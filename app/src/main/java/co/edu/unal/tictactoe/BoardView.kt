package co.edu.unal.tictactoe  // ⬅️ ajusta si tu paquete es distinto

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BoardView : View {

    companion object {
        const val GRID_WIDTH = 6
    }

    private var mHumanBitmap: Bitmap? = null
    private var mComputerBitmap: Bitmap? = null
    private var mPaint: Paint? = null

    // Referencia al juego para consultar el estado del tablero
    private var mGame: TicTacToeGame? = null

    constructor(context: Context) : super(context) { initialize() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { initialize() }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initialize() }

    private fun initialize() {
        // Carga de bitmaps (pon tus nombres reales en drawable)
        mHumanBitmap = BitmapFactory.decodeResource(resources, R.drawable.x_img)
        mComputerBitmap = BitmapFactory.decodeResource(resources, R.drawable.o_img)

        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint!!.color = Color.LTGRAY
        mPaint!!.strokeWidth = GRID_WIDTH.toFloat()
    }

    fun setGame(game: TicTacToeGame) {
        mGame = game
        invalidate()
    }

    fun getBoardCellWidth(): Int {
        val w = width
        return if (w <= 0) 0 else w / 3
    }

    fun getBoardCellHeight(): Int {
        val h = height
        return if (h <= 0) 0 else h / 3
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val boardWidth = width
        val boardHeight = height
        val paint = mPaint ?: return

        // === Líneas de la grilla ===
        val cellWidth = if (boardWidth > 0) boardWidth / 3 else 0
        val cellHeight = if (boardHeight > 0) boardHeight / 3 else 0

        // Verticales
        canvas.drawLine(cellWidth.toFloat(), 0f, cellWidth.toFloat(), boardHeight.toFloat(), paint)
        canvas.drawLine((cellWidth * 2).toFloat(), 0f, (cellWidth * 2).toFloat(), boardHeight.toFloat(), paint)

        // Horizontales
        canvas.drawLine(0f, cellHeight.toFloat(), boardWidth.toFloat(), cellHeight.toFloat(), paint)
        canvas.drawLine(0f, (cellHeight * 2).toFloat(), boardWidth.toFloat(), (cellHeight * 2).toFloat(), paint)

        // === X y O en sus celdas ===
        val game = mGame ?: return
        var i = 0
        while (i < TicTacToeGame.BOARD_SIZE) {
            val col = i % 3
            val row = i / 3

            // Rect destino sin tapar las líneas (dejamos un margen del ancho de la línea)
            val left = col * cellWidth + GRID_WIDTH
            val top = row * cellHeight + GRID_WIDTH
            val right = (col + 1) * cellWidth - GRID_WIDTH
            val bottom = (row + 1) * cellHeight - GRID_WIDTH

            val occupant = game.getBoardOccupant(i)
            if (occupant == TicTacToeGame.HUMAN_PLAYER) {
                val bmp = mHumanBitmap
                if (bmp != null) {
                    canvas.drawBitmap(bmp, null, Rect(left, top, right, bottom), null)
                }
            } else if (occupant == TicTacToeGame.COMPUTER_PLAYER) {
                val bmp = mComputerBitmap
                if (bmp != null) {
                    canvas.drawBitmap(bmp, null, Rect(left, top, right, bottom), null)
                }
            }
            i++
        }
    }
}

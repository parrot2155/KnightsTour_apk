package com.knightstravel

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.knightstravel.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var boardSize = 8
    private lateinit var boardState: Array<IntArray>
    private var knightPos = Pair(-1, -1)
    private var moveCount = 0
    private var gameActive = true
    private var isGameStarted = false

    // 색상 정의
    private val colorLightSquare by lazy { ContextCompat.getColor(this, R.color.gray_700) }
    private val colorDarkSquare by lazy { ContextCompat.getColor(this, R.color.gray_800) }
    private val colorVisited by lazy { ContextCompat.getColor(this, R.color.indigo_700) }
    private val colorStatusInfo by lazy { ContextCompat.getColor(this, R.color.blue_400) }
    private val colorStatusSuccess by lazy { ContextCompat.getColor(this, R.color.yellow_400) }
    private val colorStatusInProgress by lazy { ContextCompat.getColor(this, R.color.green_400) }
    private val colorStatusFailure by lazy { ContextCompat.getColor(this, R.color.red_500) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupControls()
        initGame()
    }

    private fun setupControls() {
        // 보드 크기 스피너 설정
        val sizes = arrayOf("5x5", "6x6", "7x7", "8x8", "9x9")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sizes)
        binding.boardSizeSpinner.adapter = adapter
        binding.boardSizeSpinner.setSelection(3) // Default 8x8

        binding.boardSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                initGame()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 리셋 버튼
        binding.resetButton.setOnClickListener {
            initGame()
        }
    }

    private fun initGame() {
        boardSize = binding.boardSizeSpinner.selectedItem.toString().substring(0, 1).toInt()
        boardState = Array(boardSize) { IntArray(boardSize) { 0 } }
        moveCount = 0
        knightPos = Pair(-1, -1)
        gameActive = true
        isGameStarted = false

        updateInfo()
        setStatus("시작할 칸을 선택하세요.", colorStatusInfo)
        renderBoard()
    }

    private fun renderBoard() {
        binding.chessboardGrid.removeAllViews()
        binding.chessboardGrid.columnCount = boardSize
        binding.chessboardGrid.rowCount = boardSize

        val screenWidth = resources.displayMetrics.widthPixels
        val gridSize = screenWidth.coerceAtMost((400 * resources.displayMetrics.density).toInt())
        val cellSize = gridSize / boardSize

        for (y in 0 until boardSize) {
            for (x in 0 until boardSize) {
                val cell = FrameLayout(this)
                val params = FrameLayout.LayoutParams(cellSize, cellSize)
                cell.layoutParams = params

                // 배경색 설정
                val isLight = (x + y) % 2 == 0
                cell.setBackgroundColor(if (isLight) colorLightSquare else colorDarkSquare)

                if (boardState[y][x] > 0) {
                    cell.setBackgroundColor(colorVisited)
                    val stepNumber = TextView(this).apply {
                        text = boardState[y][x].toString()
                        setTextColor(Color.WHITE)
                        textSize = 16f
                        gravity = Gravity.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    cell.addView(stepNumber)
                }

                if (isGameStarted && knightPos.first == x && knightPos.second == y) {
                    val knight = TextView(this).apply {
                        text = "♞"
                        setTextColor(ContextCompat.getColor(this.context, R.color.yellow_100))
                        textSize = 36f
                        gravity = Gravity.CENTER
                    }
                    cell.removeAllViews() // 이전 step number 제거
                    cell.addView(knight)
                }

                cell.setOnClickListener { handleCellClick(x, y) }
                binding.chessboardGrid.addView(cell)
            }
        }

        if (gameActive && isGameStarted) {
            highlightPossibleMoves()
        }
    }

    private fun highlightPossibleMoves() {
        val validMoves = getValidMoves(knightPos.first, knightPos.second)
        validMoves.forEach { move ->
            val index = move.second * boardSize + move.first
            val cell = binding.chessboardGrid.getChildAt(index) as? FrameLayout
            cell?.let {
                val dot = View(this).apply {
                    setBackgroundResource(R.drawable.possible_move_dot)
                    val dotSize = (cell.width * 0.25).toInt()
                    layoutParams = FrameLayout.LayoutParams(dotSize, dotSize, Gravity.CENTER)
                }
                it.addView(dot)
            }
        }
    }


    private fun handleCellClick(x: Int, y: Int) {
        if (!gameActive) return

        if (!isGameStarted) {
            isGameStarted = true
            moveCount = 1
            knightPos = Pair(x, y)
            boardState[y][x] = moveCount

            setStatus("행운을 빌어요!", colorStatusInProgress)
            updateInfo()
            renderBoard()
            checkGameState()
        } else {
            val isValidMove = getValidMoves(knightPos.first, knightPos.second).any { it.first == x && it.second == y }
            if (isValidMove) {
                moveCount++
                knightPos = Pair(x, y)
                boardState[y][x] = moveCount
                updateInfo()
                renderBoard()
                checkGameState()
            }
        }
    }

    private fun getValidMoves(x: Int, y: Int): List<Pair<Int, Int>> {
        val moves = listOf(
            Pair(x + 1, y + 2), Pair(x + 1, y - 2),
            Pair(x - 1, y + 2), Pair(x - 1, y - 2),
            Pair(x + 2, y + 1), Pair(x + 2, y - 1),
            Pair(x - 2, y + 1), Pair(x - 2, y - 1)
        )
        return moves.filter { (mx, my) ->
            mx in 0 until boardSize && my in 0 until boardSize && boardState[my][mx] == 0
        }
    }

    private fun updateInfo() {
        binding.moveCountTextView.text = moveCount.toString()
        val remaining = (boardSize * boardSize) - moveCount
        binding.remainingCountTextView.text = remaining.toString()
    }

    private fun checkGameState() {
        if (moveCount == boardSize * boardSize) {
            setStatus("성공! 모든 칸을 방문했습니다!", colorStatusSuccess)
            gameActive = false
            renderBoard() // 마지막 나이트 위치를 숫자로 변경하기 위해 재렌더링
            return
        }

        if (isGameStarted && getValidMoves(knightPos.first, knightPos.second).isEmpty()) {
            setStatus("게임 오버! 더 이상 움직일 수 없습니다.", colorStatusFailure)
            gameActive = false
        }
    }

    private fun setStatus(message: String, color: Int) {
        binding.statusMessageTextView.text = message
        binding.statusMessageTextView.setTextColor(color)
    }
}
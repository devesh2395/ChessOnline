package com.example.chessonline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class ChessBoardView extends View {

    private static final int COLS = 8;
    private float squareSize;
    private String[][] board = new String[8][8];
    private int[] selectedSquare = null;
    private OnMoveListener moveListener;
    private boolean flipped = false;  // ← added

    private final Paint lightSquarePaint = new Paint();
    private final Paint darkSquarePaint  = new Paint();
    private final Paint selectedPaint    = new Paint();
    private final Paint piecePaint       = new Paint();

    private static final Map<String, String> PIECE_UNICODE = new HashMap<>();
    static {
        PIECE_UNICODE.put("wK", "♔"); PIECE_UNICODE.put("wQ", "♕");
        PIECE_UNICODE.put("wR", "♖"); PIECE_UNICODE.put("wB", "♗");
        PIECE_UNICODE.put("wN", "♘"); PIECE_UNICODE.put("wP", "♙");
        PIECE_UNICODE.put("bK", "♚"); PIECE_UNICODE.put("bQ", "♛");
        PIECE_UNICODE.put("bR", "♜"); PIECE_UNICODE.put("bB", "♝");
        PIECE_UNICODE.put("bN", "♞"); PIECE_UNICODE.put("bP", "♟");
    }

    public interface OnMoveListener {
        void onMove(int fromRow, int fromCol, int toRow, int toCol);
    }

    public ChessBoardView(Context context) {
        super(context);
        init();
    }

    public ChessBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        lightSquarePaint.setColor(0xFFF0D9B5);
        darkSquarePaint.setColor(0xFFB58863);
        selectedPaint.setColor(0xFFAACC44);

        piecePaint.setAntiAlias(true);
        piecePaint.setTextAlign(Paint.Align.CENTER);

        board = fenToArray("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec)
        );
        setMeasuredDimension(size, size);
        squareSize = size / (float) COLS;
        piecePaint.setTextSize(squareSize * 0.75f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                // Account for board flip
                int drawCol = flipped ? (7 - col) : col;  // ← changed
                int drawRow = flipped ? (7 - row) : row;  // ← changed
                float left   = drawCol * squareSize;       // ← changed
                float top    = drawRow * squareSize;       // ← changed
                float right  = left + squareSize;
                float bottom = top  + squareSize;

                // Pick square colour
                Paint squarePaint;
                if (selectedSquare != null
                        && selectedSquare[0] == row
                        && selectedSquare[1] == col) {
                    squarePaint = selectedPaint;
                } else {
                    squarePaint = (row + col) % 2 == 0
                            ? lightSquarePaint : darkSquarePaint;
                }

                canvas.drawRect(left, top, right, bottom, squarePaint);

                // Draw the piece
                if (board[row][col] != null) {
                    String symbol = PIECE_UNICODE.get(board[row][col]);
                    if (symbol != null) {
                        float x = left + squareSize / 2f;
                        float y = top  + squareSize / 2f
                                - (piecePaint.descent() + piecePaint.ascent()) / 2f;
                        canvas.drawText(symbol, x, y, piecePaint);
                    }
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            int col = (int) (event.getX() / squareSize);
            int row = (int) (event.getY() / squareSize);

            // Clamp to board bounds
            col = Math.max(0, Math.min(7, col));
            row = Math.max(0, Math.min(7, row));

            // Convert touch back to logical coordinates if flipped
            if (flipped) {
                col = 7 - col;
                row = 7 - row;
            }

            if (selectedSquare == null) {
                if (board[row][col] != null) {
                    selectedSquare = new int[]{row, col};
                }
            } else {
                if (moveListener != null) {
                    moveListener.onMove(
                            selectedSquare[0], selectedSquare[1], row, col);
                }
                selectedSquare = null;
            }
            invalidate();
        }
        return true;
    }

    public void setBoardState(String[][] state) {
        this.board = state;
        invalidate();
    }

    public void setOnMoveListener(OnMoveListener listener) {
        this.moveListener = listener;
    }

    // ← added
    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        invalidate();
    }

    public static String[][] fenToArray(String fen) {
        String[][] arr = new String[8][8];
        String[] rows = fen.split(" ")[0].split("/");
        for (int r = 0; r < 8; r++) {
            int c = 0;
            for (char ch : rows[r].toCharArray()) {
                if (Character.isDigit(ch)) {
                    c += (ch - '0');
                } else {
                    boolean isWhite = Character.isUpperCase(ch);
                    arr[r][c] = (isWhite ? "w" : "b")
                            + Character.toUpperCase(ch);
                    c++;
                }
            }
        }
        return arr;
    }
}
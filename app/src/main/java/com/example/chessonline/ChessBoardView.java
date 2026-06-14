package com.example.chessonline;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChessBoardView extends View {

    // ── preference keys & defaults ──────────────────────────────────────
    public static final String PREFS_NAME    = "chess_colors";
    public static final String KEY_LIGHT     = "light_square";
    public static final String KEY_DARK      = "dark_square";
    public static final String KEY_SELECTED  = "selected_square";
    public static final String KEY_WHITE_PC  = "white_piece";
    public static final String KEY_BLACK_PC  = "black_piece";

    public static final int DEFAULT_LIGHT    = 0xFFF0D9B5;
    public static final int DEFAULT_DARK     = 0xFFB58863;
    public static final int DEFAULT_SELECTED = 0xFF7FC97F;
    public static final int DEFAULT_WHITE_PC = 0xFFFFF8E1;
    public static final int DEFAULT_BLACK_PC = 0xFF212121;

    private static final int COLS = 8;

    // ── state ────────────────────────────────────────────────────────────
    private float squareSize;
    private String[][] board   = new String[8][8];
    private int[]  selectedSq  = null;
    private List<int[]> legalDests = new ArrayList<>();
    private boolean flipped    = false;
    private boolean interactive = true;
    private char playerColorChar = 0; // 'w' or 'b', 0 = unrestricted

    // ── runtime colors ───────────────────────────────────────────────────
    private int lightColor    = DEFAULT_LIGHT;
    private int darkColor     = DEFAULT_DARK;
    private int selectedColor = DEFAULT_SELECTED;
    private int whitePcColor  = DEFAULT_WHITE_PC;
    private int blackPcColor  = DEFAULT_BLACK_PC;

    // ── paints ───────────────────────────────────────────────────────────
    private final Paint lightPaint     = new Paint();
    private final Paint darkPaint      = new Paint();
    private final Paint selectedPaint  = new Paint();
    private final Paint legalDotPaint  = new Paint();
    private final Paint legalRingPaint = new Paint();
    private final Paint whitePcPaint   = new Paint();
    private final Paint blackPcPaint   = new Paint();
    private final Paint shadowPaint    = new Paint();

    // ── unicode glyphs ───────────────────────────────────────────────────
    private static final Map<String, String> UNICODE = new HashMap<>();
    static {
        UNICODE.put("wK", "♔"); UNICODE.put("wQ", "♕");
        UNICODE.put("wR", "♖"); UNICODE.put("wB", "♗");
        UNICODE.put("wN", "♘"); UNICODE.put("wP", "♙");
        UNICODE.put("bK", "♚"); UNICODE.put("bQ", "♛");
        UNICODE.put("bR", "♜"); UNICODE.put("bB", "♝");
        UNICODE.put("bN", "♞"); UNICODE.put("bP", "♟");
    }

    // ── listeners ────────────────────────────────────────────────────────
    public interface OnMoveListener {
        void onMove(int fromRow, int fromCol, int toRow, int toCol);
    }
    public interface OnSelectionChangedListener {
        void onPieceSelected(int row, int col);
        void onSelectionCleared();
    }

    private OnMoveListener moveListener;
    private OnSelectionChangedListener selectionListener;

    // ── constructors ─────────────────────────────────────────────────────
    public ChessBoardView(Context c) { super(c); init(c); }
    public ChessBoardView(Context c, AttributeSet a) { super(c, a); init(c); }

    private void init(Context c) {
        loadColorsFromPrefs(c);
        board = fenToArray("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    // ── color management ─────────────────────────────────────────────────
    public void loadColorsFromPrefs(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        lightColor    = sp.getInt(KEY_LIGHT,    DEFAULT_LIGHT);
        darkColor     = sp.getInt(KEY_DARK,     DEFAULT_DARK);
        selectedColor = sp.getInt(KEY_SELECTED, DEFAULT_SELECTED);
        whitePcColor  = sp.getInt(KEY_WHITE_PC, DEFAULT_WHITE_PC);
        blackPcColor  = sp.getInt(KEY_BLACK_PC, DEFAULT_BLACK_PC);
        applyColors();
    }

    private void applyColors() {
        lightPaint.setColor(lightColor);
        darkPaint.setColor(darkColor);
        selectedPaint.setColor(selectedColor);

        // Legal move dot — translucent version of selectedColor
        int legalColor = (selectedColor & 0x00FFFFFF) | 0x99000000;
        legalDotPaint.setColor(legalColor);
        legalDotPaint.setStyle(Paint.Style.FILL);
        legalDotPaint.setAntiAlias(true);

        legalRingPaint.setColor(legalColor);
        legalRingPaint.setStyle(Paint.Style.STROKE);
        legalRingPaint.setAntiAlias(true);

        whitePcPaint.setColor(whitePcColor);
        whitePcPaint.setAntiAlias(true);
        whitePcPaint.setTextAlign(Paint.Align.CENTER);

        blackPcPaint.setColor(blackPcColor);
        blackPcPaint.setAntiAlias(true);
        blackPcPaint.setTextAlign(Paint.Align.CENTER);

        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setAntiAlias(true);
        shadowPaint.setTextAlign(Paint.Align.CENTER);

        invalidate();
    }

    public void setLightSquareColor(int c) { lightColor    = c; applyColors(); }
    public void setDarkSquareColor(int c)  { darkColor     = c; applyColors(); }
    public void setSelectedColor(int c)    { selectedColor = c; applyColors(); }
    public void setWhitePieceColor(int c)  { whitePcColor  = c; applyColors(); }
    public void setBlackPieceColor(int c)  { blackPcColor  = c; applyColors(); }

    // ── measure ──────────────────────────────────────────────────────────
    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int size = Math.min(MeasureSpec.getSize(wSpec), MeasureSpec.getSize(hSpec));
        setMeasuredDimension(size, size);
        squareSize = size / (float) COLS;
        float ts = squareSize * 0.70f;
        whitePcPaint.setTextSize(ts);
        blackPcPaint.setTextSize(ts);
        shadowPaint.setTextSize(ts);
        shadowPaint.setStrokeWidth(squareSize * 0.05f);
        legalRingPaint.setStrokeWidth(squareSize * 0.07f);
    }

    // ── draw ─────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        if (squareSize == 0) return;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int dr = flipped ? (7 - row) : row;
                int dc = flipped ? (7 - col) : col;
                float l = dc * squareSize;
                float t = dr * squareSize;
                float cx = l + squareSize / 2f;
                float cy = t + squareSize / 2f;

                // ── square background ─────────────────────────────────
                boolean isSel = selectedSq != null
                        && selectedSq[0] == row && selectedSq[1] == col;
                Paint sqPaint = isSel ? selectedPaint
                        : ((row + col) % 2 == 0 ? lightPaint : darkPaint);
                canvas.drawRect(l, t, l + squareSize, t + squareSize, sqPaint);

                // ── legal move indicators ─────────────────────────────
                boolean isLegal = false;
                for (int[] d : legalDests) {
                    if (d[0] == row && d[1] == col) { isLegal = true; break; }
                }
                if (isLegal) {
                    if (board[row][col] == null) {
                        // small dot for empty squares
                        canvas.drawCircle(cx, cy, squareSize * 0.16f, legalDotPaint);
                    } else {
                        // ring around occupied squares (capture)
                        canvas.drawCircle(cx, cy, squareSize * 0.42f, legalRingPaint);
                    }
                }

                // ── piece ─────────────────────────────────────────────
                String piece = board[row][col];
                if (piece != null) {
                    String sym = UNICODE.get(piece);
                    if (sym != null) {
                        boolean isWhite = piece.charAt(0) == 'w';
                        Paint pcPaint = isWhite ? whitePcPaint : blackPcPaint;
                        float ty = cy - (pcPaint.descent() + pcPaint.ascent()) / 2f;

                        // contrasting shadow/outline for visibility
                        shadowPaint.setColor(isWhite ? 0xBB000000 : 0xBBFFFFFF);
                        canvas.drawText(sym, cx, ty, shadowPaint);
                        canvas.drawText(sym, cx, ty, pcPaint);
                    }
                }
            }
        }
    }

    // ── touch ────────────────────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!interactive || e.getAction() != MotionEvent.ACTION_UP) return true;

        int col = Math.max(0, Math.min(7, (int)(e.getX() / squareSize)));
        int row = Math.max(0, Math.min(7, (int)(e.getY() / squareSize)));
        if (flipped) { col = 7 - col; row = 7 - row; }

        if (selectedSq == null) {
            // First tap: only select own pieces
            String piece = board[row][col];
            boolean ownPiece = piece != null
                    && (playerColorChar == 0 || piece.charAt(0) == playerColorChar);
            if (ownPiece) {
                selectedSq = new int[]{row, col};
                if (selectionListener != null)
                    selectionListener.onPieceSelected(row, col);
            }
        } else {
            // Second tap: re-select if own piece, else fire move
            String piece = board[row][col];
            boolean ownPiece = piece != null
                    && (playerColorChar == 0 || piece.charAt(0) == playerColorChar);
            if (ownPiece && !(selectedSq[0] == row && selectedSq[1] == col)) {
                // Re-select different own piece
                selectedSq = new int[]{row, col};
                legalDests.clear();
                if (selectionListener != null)
                    selectionListener.onPieceSelected(row, col);
            } else {
                // Fire move
                if (moveListener != null)
                    moveListener.onMove(selectedSq[0], selectedSq[1], row, col);
                selectedSq = null;
                legalDests.clear();
                if (selectionListener != null)
                    selectionListener.onSelectionCleared();
            }
        }
        invalidate();
        return true;
    }

    // ── public API ───────────────────────────────────────────────────────
    public void setBoardState(String[][] state)              { board = state; invalidate(); }
    public void setFlipped(boolean f)                         { flipped = f; invalidate(); }
    public void setInteractive(boolean v)                     { interactive = v; }
    public void setPlayerColor(String color) {
        playerColorChar = color.equals("white") ? 'w' : 'b';
    }
    public void setOnMoveListener(OnMoveListener l)           { moveListener = l; }
    public void setOnSelectionChangedListener(OnSelectionChangedListener l) { selectionListener = l; }

    public void setLegalDestinations(List<int[]> dests) {
        legalDests = dests != null ? dests : new ArrayList<>();
        invalidate();
    }

    public void clearSelection() {
        selectedSq = null;
        legalDests.clear();
        invalidate();
    }
//commenting for third commit.
    // ── FEN parser ───────────────────────────────────────────────────────
    public static String[][] fenToArray(String fen) {
        String[][] arr = new String[8][8];
        String[] rows = fen.split(" ")[0].split("/");
        for (int r = 0; r < 8; r++) {
            int c = 0;
            for (char ch : rows[r].toCharArray()) {
                if (Character.isDigit(ch)) c += (ch - '0');
                else {
                    arr[r][c] = (Character.isUpperCase(ch) ? "w" : "b")
                            + Character.toUpperCase(ch);
                    c++;
                }
            }
        }
        return arr;
    }
}
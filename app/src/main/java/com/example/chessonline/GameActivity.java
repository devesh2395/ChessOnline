package com.example.chessonline;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity
        implements ChessBoardView.OnSelectionChangedListener {

    private ChessBoardView boardView;
    private TextView tvStatus, tvRoomCode, tvCapturedTop, tvCapturedBottom;
    private DatabaseReference gameRef;
    private Board chessBoard;
    private String myColor, gameId;
    private String[][] currentBoardState = new String[8][8];

    private static final String DB_URL =
            "https://chessonline-5008e-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String TAG = "Chess";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Intent data first — myColor needed before setFlipped
        gameId  = getIntent().getStringExtra("gameId");
        myColor = getIntent().getStringExtra("color");

        boardView        = findViewById(R.id.chessBoardView);
        tvStatus         = findViewById(R.id.tvStatus);
        tvRoomCode       = findViewById(R.id.tvRoomCode);
        tvCapturedTop    = findViewById(R.id.tvCapturedTop);
        tvCapturedBottom = findViewById(R.id.tvCapturedBottom);
        Button btnSettings = findViewById(R.id.btnSettings);

        tvRoomCode.setText(gameId);
        boardView.setFlipped(myColor.equals("black"));
        boardView.setPlayerColor(myColor);
        boardView.setOnSelectionChangedListener(this);

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        // Chess engine
        chessBoard = new Board();
        chessBoard.loadFromFen(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        // Firebase
        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        gameRef = db.getReference("games").child(gameId);
        gameRef.keepSynced(true);

        gameRef.child("fen").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String fen = snapshot.getValue(String.class);
                Log.d(TAG, "FEN received: " + fen + "  color=" + myColor);
                if (fen != null) {
                    chessBoard.loadFromFen(fen);
                    currentBoardState = ChessBoardView.fenToArray(fen);
                    boardView.setBoardState(currentBoardState);
                    boardView.clearSelection();
                    updateCapturedPieces(fen);
                    updateStatusText();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                tvStatus.setText("Connection error");
            }
        });

        db.getReference(".info/connected").addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {
                        if (!Boolean.TRUE.equals(s.getValue(Boolean.class)))
                            tvStatus.setText("Reconnecting...");
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });

        boardView.setOnMoveListener((fromRow, fromCol, toRow, toCol) -> {
            if (!isMyTurn()) { tvStatus.setText("Not your turn"); return; }
            String from = toSq(fromCol, fromRow);
            String to   = toSq(toCol, toRow);
            try {
                Move move = new Move(
                        Square.fromValue(from.toUpperCase()),
                        Square.fromValue(to.toUpperCase()));
                if (chessBoard.legalMoves().contains(move)) {
                    chessBoard.doMove(move);
                    gameRef.child("fen").setValue(chessBoard.getFen())
                            .addOnSuccessListener(u -> Log.d(TAG, "FEN pushed"))
                            .addOnFailureListener(e -> Log.e(TAG, "Push failed: " + e));
                    updateStatusText();
                } else {
                    tvStatus.setText("Illegal move");
                }
            } catch (Exception e) {
                tvStatus.setText("Invalid move");
            }
        });
    }

    // ── OnSelectionChangedListener ──────────────────────────────────────
    @Override
    public void onPieceSelected(int row, int col) {
        List<int[]> dests = new ArrayList<>();
        try {
            Square fromSq = Square.fromValue(toSq(col, row).toUpperCase());
            for (Move m : chessBoard.legalMoves()) {
                if (m.getFrom() == fromSq) {
                    String toVal = m.getTo().value().toLowerCase();
                    int toCol = toVal.charAt(0) - 'a';
                    int toRow = 8 - (toVal.charAt(1) - '0');
                    dests.add(new int[]{toRow, toCol});
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Legal moves error: " + e.getMessage());
        }
        boardView.setLegalDestinations(dests);
    }

    @Override
    public void onSelectionCleared() {
        boardView.setLegalDestinations(null);
    }

    // ── reload colors when returning from settings ───────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        boardView.loadColorsFromPrefs(this);
    }

    // ── helpers ──────────────────────────────────────────────────────────
    private boolean isMyTurn() {
        boolean wt = chessBoard.getSideToMove() == Side.WHITE;
        return (wt && myColor.equals("white")) || (!wt && myColor.equals("black"));
    }

    private void updateStatusText() {
        if (chessBoard.isMated())
            tvStatus.setText(chessBoard.getSideToMove() == Side.BLACK
                    ? "White wins!" : "Black wins!");
        else if (chessBoard.isDraw()) tvStatus.setText("Draw!");
        else if (isMyTurn()) tvStatus.setText("Your turn (" + myColor + ")");
        else tvStatus.setText("Opponent's turn");
    }

    private void updateCapturedPieces(String fen) {
        // Starting counts: Q, R, B, N, P
        int[] startW = {1, 2, 2, 2, 8};
        int[] startB = {1, 2, 2, 2, 8};
        char[] wPcs  = {'Q', 'R', 'B', 'N', 'P'};
        char[] bPcs  = {'q', 'r', 'b', 'n', 'p'};
        // Symbols to display when these pieces are captured
        String[] wCapSyms = {"♛", "♜", "♝", "♞", "♟"}; // black pieces shown when white captures them
        String[] bCapSyms = {"♕", "♖", "♗", "♘", "♙"}; // white pieces shown when black captures them

        String board = fen.split(" ")[0];
        int[] curW = new int[5];
        int[] curB = new int[5];
        for (char c : board.toCharArray()) {
            for (int i = 0; i < 5; i++) {
                if (c == wPcs[i]) curW[i]++;
                if (c == bPcs[i]) curB[i]++;
            }
        }

        // Pieces white captured = black pieces missing
        StringBuilder capturedByWhite = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < startB[i] - curB[i]; j++)
                capturedByWhite.append(wCapSyms[i]);
        }

        // Pieces black captured = white pieces missing
        StringBuilder capturedByBlack = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < startW[i] - curW[i]; j++)
                capturedByBlack.append(bCapSyms[i]);
        }

        // Top = opponent's captures, Bottom = my captures
        if (myColor.equals("white")) {
            tvCapturedTop.setText(capturedByBlack.toString());
            tvCapturedBottom.setText(capturedByWhite.toString());
        } else {
            tvCapturedTop.setText(capturedByWhite.toString());
            tvCapturedBottom.setText(capturedByBlack.toString());
        }
    }

    private String toSq(int col, int row) {
        return "" + (char)('a' + col) + (8 - row);
    }
}
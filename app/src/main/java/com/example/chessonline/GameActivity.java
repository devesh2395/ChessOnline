package com.example.chessonline;

import android.os.Bundle;
import android.util.Log;
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


public class GameActivity extends AppCompatActivity {

    private ChessBoardView boardView;
    private TextView tvStatus;
    private TextView tvRoomCode;
    private DatabaseReference gameRef;
    private Board chessBoard;
    private String myColor;
    private String gameId;

    private static final String DB_URL =
            "https://chessonline-5008e-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final String TAG = "Chess";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Get intent data first — myColor is needed before setFlipped
        gameId  = getIntent().getStringExtra("gameId");
        myColor = getIntent().getStringExtra("color");

        Log.d(TAG, "GameActivity started — gameId: " + gameId + " color: " + myColor);

        // Get views
        boardView  = findViewById(R.id.chessBoardView);
        tvStatus   = findViewById(R.id.tvStatus);
        tvRoomCode = findViewById(R.id.tvRoomCode);

        tvRoomCode.setText(gameId);
        boardView.setFlipped(myColor.equals("black"));

        // Set up chess engine
        chessBoard = new Board();
        chessBoard.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        // Connect to Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        gameRef = database.getReference("games").child(gameId);

        // Keep this node synced even if connection drops briefly
        gameRef.keepSynced(true);

        // Listen for FEN changes
        gameRef.child("fen").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String fen = snapshot.getValue(String.class);
                Log.d(TAG, "FEN received: " + fen + " | myColor: " + myColor);
                if (fen != null) {
                    chessBoard.loadFromFen(fen);
                    boardView.setBoardState(ChessBoardView.fenToArray(fen));
                    updateStatusText();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Firebase listener cancelled: " + error.getMessage());
                tvStatus.setText("Connection error: " + error.getMessage());
            }
        });

        // Monitor Firebase connection state
        database.getReference(".info/connected").addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        boolean connected = Boolean.TRUE.equals(
                                snapshot.getValue(Boolean.class));
                        Log.d(TAG, "Firebase connected: " + connected);
                        if (!connected) {
                            tvStatus.setText("Reconnecting...");
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });

        // Handle moves
        boardView.setOnMoveListener((fromRow, fromCol, toRow, toCol) -> {
            Log.d(TAG, "Move tapped — myColor: " + myColor
                    + " isMyTurn: " + isMyTurn());

            if (!isMyTurn()) {
                tvStatus.setText("Not your turn");
                return;
            }

            String from = toSquareName(fromCol, fromRow);
            String to   = toSquareName(toCol,   toRow);
            Log.d(TAG, "Attempting: " + from + " → " + to);

            try {
                Square fromSq = Square.fromValue(from.toUpperCase());
                Square toSq   = Square.fromValue(to.toUpperCase());
                Move move = new Move(fromSq, toSq);

                if (chessBoard.legalMoves().contains(move)) {
                    chessBoard.doMove(move);
                    String newFen = chessBoard.getFen();
                    Log.d(TAG, "Pushing FEN: " + newFen);
                    gameRef.child("fen").setValue(newFen)
                            .addOnSuccessListener(u ->
                                    Log.d(TAG, "FEN pushed successfully"))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "FEN push failed: " + e.getMessage()));
                    updateStatusText();
                } else {
                    Log.d(TAG, "Illegal move: " + from + " → " + to);
                    tvStatus.setText("Illegal move");
                }
            } catch (Exception e) {
                Log.e(TAG, "Move exception: " + e.getMessage());
                tvStatus.setText("Invalid move");
            }
        });
    }

    private boolean isMyTurn() {
        boolean whiteTurn = chessBoard.getSideToMove() == Side.WHITE;
        return (whiteTurn  && myColor.equals("white"))
                || (!whiteTurn && myColor.equals("black"));
    }

    private void updateStatusText() {
        if (chessBoard.isMated()) {
            boolean whiteJustMoved = chessBoard.getSideToMove() == Side.BLACK;
            tvStatus.setText(whiteJustMoved ? "White wins!" : "Black wins!");
        } else if (chessBoard.isDraw()) {
            tvStatus.setText("Draw!");
        } else if (isMyTurn()) {
            tvStatus.setText("Your turn (" + myColor + ")");
        } else {
            tvStatus.setText("Opponent's turn");
        }
    }

    private String toSquareName(int col, int row) {
        return "" + (char) ('a' + col) + (8 - row);
    }
}
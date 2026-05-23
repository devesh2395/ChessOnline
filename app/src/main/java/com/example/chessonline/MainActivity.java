package com.example.chessonline;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private EditText etRoomCode;
    private DatabaseReference gamesRef;

    private static final String DB_URL =
            "https://chessonline-5008e-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static final String TAG = "Chess";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnCreate = findViewById(R.id.btnCreate);
        Button btnJoin   = findViewById(R.id.btnJoin);
        tvStatus         = findViewById(R.id.tvStatus);
        etRoomCode       = findViewById(R.id.etRoomCode);

        gamesRef = FirebaseDatabase.getInstance(DB_URL).getReference("games");

        // Disable buttons until signed in
        btnCreate.setEnabled(false);
        btnJoin.setEnabled(false);
        tvStatus.setText("Connecting...");

        // Sign in anonymously first
        FirebaseAuth.getInstance()
                .signInAnonymously()
                .addOnSuccessListener(result -> {
                    tvStatus.setText("");
                    btnCreate.setEnabled(true);
                    btnJoin.setEnabled(true);
                    Log.d(TAG, "Signed in: " + result.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Auth failed: " + e.getMessage());
                    Log.e(TAG, "Auth error: " + e.getMessage());
                });

        btnCreate.setOnClickListener(v -> {
            String code = generateRoomCode();
            tvStatus.setText("Creating room " + code + "...");

            gamesRef.child(code).child("fen").setValue(STARTING_FEN)
                    .addOnSuccessListener(unused -> {
                        tvStatus.setText("Room created!\nShare this code: " + code
                                + "\n\nWaiting for opponent...");
                        launchGame(code, "white");
                    })
                    .addOnFailureListener(e ->
                            tvStatus.setText("Error: " + e.getMessage()));
        });

        btnJoin.setOnClickListener(v -> {
            String code = etRoomCode.getText().toString()
                    .toUpperCase().trim();

            if (code.length() != 6) {
                tvStatus.setText("Enter a 6-letter room code");
                return;
            }

            tvStatus.setText("Joining room " + code + "...");

            gamesRef.child(code).child("fen").get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            launchGame(code, "black");
                        } else {
                            tvStatus.setText("Room \"" + code + "\" not found.\nAsk your opponent for the code.");
                        }
                    })
                    .addOnFailureListener(e ->
                            tvStatus.setText("Error: " + e.getMessage()));
        });
    }

    private void launchGame(String roomCode, String color) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("gameId", roomCode);
        intent.putExtra("color", color);
        startActivity(intent);
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}
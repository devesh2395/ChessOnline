package com.example.chessonline;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private ChessBoardView previewBoard;
    private SharedPreferences prefs;

    private int lightColor, darkColor, selectedColor, whitePcColor, blackPcColor;

    private View swatchLight, swatchDark, swatchSelected, swatchWhite, swatchBlack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(ChessBoardView.PREFS_NAME, MODE_PRIVATE);
        loadColors();

        previewBoard   = findViewById(R.id.previewBoard);
        swatchLight    = findViewById(R.id.swatchLight);
        swatchDark     = findViewById(R.id.swatchDark);
        swatchSelected = findViewById(R.id.swatchSelected);
        swatchWhite    = findViewById(R.id.swatchWhitePiece);
        swatchBlack    = findViewById(R.id.swatchBlackPiece);

        Button btnLight    = findViewById(R.id.btnLightSquare);
        Button btnDark     = findViewById(R.id.btnDarkSquare);
        Button btnSelected = findViewById(R.id.btnSelected);
        Button btnWhite    = findViewById(R.id.btnWhitePiece);
        Button btnBlack    = findViewById(R.id.btnBlackPiece);
        Button btnReset    = findViewById(R.id.btnReset);

        previewBoard.setInteractive(false);
        refreshPreview();
        updateSwatches();

        btnLight.setOnClickListener(v ->
                new ColorPickerDialog(this, lightColor, color -> {
                    lightColor = color;
                    saveAndRefresh();
                }).show());

        btnDark.setOnClickListener(v ->
                new ColorPickerDialog(this, darkColor, color -> {
                    darkColor = color;
                    saveAndRefresh();
                }).show());

        btnSelected.setOnClickListener(v ->
                new ColorPickerDialog(this, selectedColor, color -> {
                    selectedColor = color;
                    saveAndRefresh();
                }).show());

        btnWhite.setOnClickListener(v ->
                new ColorPickerDialog(this, whitePcColor, color -> {
                    whitePcColor = color;
                    saveAndRefresh();
                }).show());

        btnBlack.setOnClickListener(v ->
                new ColorPickerDialog(this, blackPcColor, color -> {
                    blackPcColor = color;
                    saveAndRefresh();
                }).show());

        btnReset.setOnClickListener(v -> {
            lightColor    = ChessBoardView.DEFAULT_LIGHT;
            darkColor     = ChessBoardView.DEFAULT_DARK;
            selectedColor = ChessBoardView.DEFAULT_SELECTED;
            whitePcColor  = ChessBoardView.DEFAULT_WHITE_PC;
            blackPcColor  = ChessBoardView.DEFAULT_BLACK_PC;
            saveAndRefresh();
        });
    }

    private void saveAndRefresh() {
        prefs.edit()
                .putInt(ChessBoardView.KEY_LIGHT,    lightColor)
                .putInt(ChessBoardView.KEY_DARK,     darkColor)
                .putInt(ChessBoardView.KEY_SELECTED, selectedColor)
                .putInt(ChessBoardView.KEY_WHITE_PC, whitePcColor)
                .putInt(ChessBoardView.KEY_BLACK_PC, blackPcColor)
                .apply();
        refreshPreview();
        updateSwatches();
    }

    private void refreshPreview() {
        previewBoard.setLightSquareColor(lightColor);
        previewBoard.setDarkSquareColor(darkColor);
        previewBoard.setSelectedColor(selectedColor);
        previewBoard.setWhitePieceColor(whitePcColor);
        previewBoard.setBlackPieceColor(blackPcColor);
    }

    private void updateSwatches() {
        swatchLight.setBackgroundColor(lightColor);
        swatchDark.setBackgroundColor(darkColor);
        swatchSelected.setBackgroundColor(selectedColor);
        swatchWhite.setBackgroundColor(whitePcColor);
        swatchBlack.setBackgroundColor(blackPcColor);
    }

    private void loadColors() {
        lightColor    = prefs.getInt(ChessBoardView.KEY_LIGHT,    ChessBoardView.DEFAULT_LIGHT);
        darkColor     = prefs.getInt(ChessBoardView.KEY_DARK,     ChessBoardView.DEFAULT_DARK);
        selectedColor = prefs.getInt(ChessBoardView.KEY_SELECTED, ChessBoardView.DEFAULT_SELECTED);
        whitePcColor  = prefs.getInt(ChessBoardView.KEY_WHITE_PC, ChessBoardView.DEFAULT_WHITE_PC);
        blackPcColor  = prefs.getInt(ChessBoardView.KEY_BLACK_PC, ChessBoardView.DEFAULT_BLACK_PC);
    }
}
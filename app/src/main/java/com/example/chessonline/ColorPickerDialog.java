package com.example.chessonline;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class ColorPickerDialog {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private final Context context;
    private final int initialColor;
    private final OnColorSelectedListener listener;

    public ColorPickerDialog(Context ctx, int initialColor,
                             OnColorSelectedListener listener) {
        this.context      = ctx;
        this.initialColor = initialColor;
        this.listener     = listener;
    }

    public void show() {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_color_picker, null);

        View     preview = view.findViewById(R.id.colorPreview);
        SeekBar  sbR     = view.findViewById(R.id.sbRed);
        SeekBar  sbG     = view.findViewById(R.id.sbGreen);
        SeekBar  sbB     = view.findViewById(R.id.sbBlue);
        TextView tvR     = view.findViewById(R.id.tvRed);
        TextView tvG     = view.findViewById(R.id.tvGreen);
        TextView tvB     = view.findViewById(R.id.tvBlue);

        final int[] rgb = {
                Color.red(initialColor),
                Color.green(initialColor),
                Color.blue(initialColor)
        };

        sbR.setMax(255); sbR.setProgress(rgb[0]);
        sbG.setMax(255); sbG.setProgress(rgb[1]);
        sbB.setMax(255); sbB.setProgress(rgb[2]);
        preview.setBackgroundColor(initialColor);
        tvR.setText(String.valueOf(rgb[0]));
        tvG.setText(String.valueOf(rgb[1]));
        tvB.setText(String.valueOf(rgb[2]));

        SeekBar.OnSeekBarChangeListener seekListener =
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar sb, int p, boolean u) {
                        rgb[0] = sbR.getProgress();
                        rgb[1] = sbG.getProgress();
                        rgb[2] = sbB.getProgress();
                        int c = Color.rgb(rgb[0], rgb[1], rgb[2]);
                        preview.setBackgroundColor(c);
                        tvR.setText(String.valueOf(rgb[0]));
                        tvG.setText(String.valueOf(rgb[1]));
                        tvB.setText(String.valueOf(rgb[2]));
                    }
                    @Override public void onStartTrackingTouch(SeekBar sb) {}
                    @Override public void onStopTrackingTouch(SeekBar sb) {}
                };
        sbR.setOnSeekBarChangeListener(seekListener);
        sbG.setOnSeekBarChangeListener(seekListener);
        sbB.setOnSeekBarChangeListener(seekListener);

        // Preset swatches
        int[][] presets = {
                {0xF0, 0xD9, 0xB5}, // classic light tan
                {0xB5, 0x88, 0x63}, // classic dark brown
                {0x86, 0xA6, 0x6A}, // green light
                {0x53, 0x7A, 0x47}, // green dark
                {0x9F, 0xBD, 0xD2}, // blue light
                {0x4D, 0x7C, 0x9E}, // blue dark
                {0xFF, 0xF8, 0xE1}, // ivory (white pieces)
                {0x21, 0x21, 0x21}, // near-black (black pieces)
                {0xFF, 0xFF, 0xFF}, // pure white
                {0x00, 0x00, 0x00}, // pure black
        };
        int[] presetIds = {
                R.id.preset1, R.id.preset2, R.id.preset3, R.id.preset4, R.id.preset5,
                R.id.preset6, R.id.preset7, R.id.preset8, R.id.preset9, R.id.preset10
        };
        for (int i = 0; i < presets.length; i++) {
            int[] p   = presets[i];
            int color = Color.rgb(p[0], p[1], p[2]);
            View sw   = view.findViewById(presetIds[i]);
            sw.setBackgroundColor(color);
            sw.setOnClickListener(v -> {
                sbR.setProgress(Color.red(color));
                sbG.setProgress(Color.green(color));
                sbB.setProgress(Color.blue(color));
            });
        }

        new AlertDialog.Builder(context)
                .setTitle("Pick a Color")
                .setView(view)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (listener != null)
                        listener.onColorSelected(
                                Color.rgb(sbR.getProgress(),
                                        sbG.getProgress(),
                                        sbB.getProgress()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
package com.example.texttospeechapp;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private Button btnSelectFile, btnPlay, btnStop;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI references
        editText = findViewById(R.id.editText);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnPlay = findViewById(R.id.btnPlay);
        btnStop = findViewById(R.id.btnStop);

        // TTS Initialization
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
            }
        });

        // Button actions
        btnSelectFile.setOnClickListener(v -> openFile());
        btnPlay.setOnClickListener(v -> playText());
        btnStop.setOnClickListener(v -> tts.stop());

        // Ask permissions for storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }
    }

    // --- Open file selector for TXT/PDF ---
    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String path = uri.toString().toLowerCase();
                if (path.endsWith(".pdf")) {
                    readPdf(uri);
                } else {
                    readTxt(uri);
                }
            }
        }
    }

    // --- Read TXT file ---
    private void readTxt(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            editText.setText(sb.toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "File read error", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Read PDF file (text-based PDFs only) ---
    private void readPdf(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            PDDocument document = PDDocument.load(inputStream);

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document).trim();

            document.close();

            if (text.isEmpty()) {
                Toast.makeText(this, "PDF contains no extractable text", Toast.LENGTH_LONG).show();
            } else {
                editText.setText(text);
            }

            Log.d("PDFText", "Extracted: " + text); // debug log

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF read error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // --- Speak the text ---
    private void playText() {
        String input = editText.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "No text loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        tts.speak(input, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}

package es.upm.btb.microphone;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    private TextView decibelTextView;
    private ImageButton recordButton;
    private File outputFile;

    private Timer timer;
    private Handler handler;

    private static final int DECIBEL_CALIBRATION_ADJUSTMENT = 63;
    //private static final int MEASUREMENT_PERIOD = 100; // Se lee cada 100 milisegundos. 10 veces por segundo
    private static final int MEASUREMENT_PERIOD = 1000; // Se lee cada 100 milisegundos. 1 vez por segundo
    private static final int REQUEST_MICROPHONE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
        }

        decibelTextView = findViewById(R.id.tvDecibel);
        recordButton = findViewById(R.id.recordButton);

        handler = new Handler(Looper.getMainLooper());

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    stopRecording();
                    recordButton.setImageResource(R.drawable.ic_rec);
                } else {
                    startRecording();
                    recordButton.setImageResource(R.drawable.ic_stop);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
            return;
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            outputFile = new File(getCacheDir(), "temp_audio.3gp");
            mediaRecorder.setOutputFile(outputFile.getAbsolutePath());

            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            startTimer();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al iniciar grabación", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                // Si no se llegó a iniciar correctamente
                e.printStackTrace();
            }
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            stopTimer();

            // Borrar archivo temporal
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
            }
        }
    }


    private void startTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateDecibelLevel();
            }
        }, 0, MEASUREMENT_PERIOD);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void updateDecibelLevel() {
        if (mediaRecorder != null) {
            int amplitude = mediaRecorder.getMaxAmplitude();


            final double decibels;
            if (amplitude > 0) {
                decibels = 20 * Math.log10(amplitude / 2700.0);
                if (!Double.isFinite(decibels)) return;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            String decibelText = String.format(Locale.getDefault(), "%.1f dB", decibels + DECIBEL_CALIBRATION_ADJUSTMENT);

                            decibelTextView.setText(decibelText);
                            int iThreshold = (int) (decibels + DECIBEL_CALIBRATION_ADJUSTMENT);
                            if (iThreshold > 100) {
                                decibelTextView.setTextColor(Color.RED);
                            } else if (iThreshold > 75) {
                                decibelTextView.setTextColor(Color.YELLOW);
                            } else if (iThreshold > 50) {
                                decibelTextView.setTextColor(Color.GREEN);
                            } else {
                                decibelTextView.setTextColor(Color.BLUE);
                            }

                            sendDecibelTelemetry(decibels + DECIBEL_CALIBRATION_ADJUSTMENT);
                        }
                    });
            }
        }
    }

    // Add this method to MainActivity
    private void sendDecibelTelemetry(double decibels) {
        ConfigReader configReader = new ConfigReader(this);
        String url = configReader.getProperty("url");
        Log.e("telemetry", "Enviando: " + decibels);
        Log.e("telemetry", "URL: " + url);

        if (url != null) {
            try {
                JSONObject json = new JSONObject();
                json.put("decibels", decibels);
                Log.e("telemetry", "Payload: " + json.toString());
                // Send in a background thread
                new Thread(() -> NetworkUtils.sendTelemetryData(json, url)).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
    }
}

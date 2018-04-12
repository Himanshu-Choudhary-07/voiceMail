package com.voicemail_project.android.voicemail;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;

import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import io.netopen.hotbitmapgg.library.view.RingProgressBar;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer = new MediaPlayer();

    private FirebaseAuth mAuth;

    private Toolbar mToolbar;

    private ImageView mImageView;

    private RingProgressBar mRingProgressBar;
    Timer timer = new Timer();

    private MediaRecorder mRecorder;

    private String mFileName;

    private static final String LOG_TAG = "Record_Log";
    private int MAX_DURATION = 60000;
    private boolean stopped;

    private TextView textLabel;

    private String tyu = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRingProgressBar = (RingProgressBar) findViewById(R.id.progressBar);
        textLabel = (TextView) findViewById(R.id.mTextView);

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/recorded_audio.mp3";

        mAuth = FirebaseAuth.getInstance();

        mRingProgressBar.setMax(MAX_DURATION);
        mRingProgressBar.setProgress(0);
        startProgress();

        mImageView = (ImageView) findViewById(R.id.record_image);
        mImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        playFunction();
                        break;

                    case MotionEvent.ACTION_UP:
                        nextFunction();

                        break;

                    default:
                        break;
                }
                return true;
            }
        });

        mToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("VoiceMail");
    }

    private void playFunction(){
        final MediaPlayer sound_one = MediaPlayer.create(this, R.raw.open_sound);

        startRecording();
        textLabel.setText("Recording....");
        stopped = false;
        mRingProgressBar.setVisibility(View.VISIBLE);


        Vibrator vb = (Vibrator)   getSystemService(Context.VIBRATOR_SERVICE);
        vb.vibrate(200);
        sound_one.start();
    }

    private void nextFunction(){
        final MediaPlayer sound_two = MediaPlayer.create(this, R.raw.close_sound);

        stopRecording();
        textLabel.setText("Recording Stopped");
        mRingProgressBar.setProgress(0);
        mRingProgressBar.setVisibility(View.INVISIBLE);
        stopped = true;


        Vibrator vb2 = (Vibrator)   getSystemService(Context.VIBRATOR_SERVICE);
        vb2.vibrate(100);
        sound_two.start();
        tyu = convert();
        Intent next_intent = new Intent(MainActivity.this, LastActivity.class);
        next_intent.putExtra("recVoice",tyu);
        startActivity(next_intent);
    }

    private void startProgress() {
        timer.schedule(new TimerTask() {

            @Override
            public void run() {

                if(!stopped)  // call ui only when  the progress is not stopped
                {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            try
                            {

                                mRingProgressBar.setProgress(mRingProgressBar.getProgress()+1000);

                            } catch (Exception e) {}



                        }
                    });
                }
            }



        }, 1, 1000);
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setMaxDuration(MAX_DURATION);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            Intent mAuthIntent = new Intent(MainActivity.this, start_Activity.class);
            startActivity(mAuthIntent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.sign_out:
                mAuth.signOut();
                sendToStart();
                break;

            case R.id.inbox:
                Intent i = new Intent(MainActivity.this,InboxActivity.class);
                startActivity(i);
        }
        return true;
    }

    private void sendToStart() {
        Intent signOut = new Intent(MainActivity.this,start_Activity.class);
        startActivity(signOut);
        finish();
    }

    public String convert() {

        //------This save the mp3 audio to the internal memory---------

        String outputFile =
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/recorded_audio.mp3";

        byte[] soundBytes = new byte[0];
        try {
            InputStream inputStream =
                    getContentResolver().openInputStream(Uri.fromFile(new File(outputFile)));

            soundBytes = new byte[inputStream.available()];
            soundBytes = toByteArray(inputStream);

            Toast.makeText(this, "Recordin Finished" + " " + soundBytes, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String mainString = bytesToHex(soundBytes);

        return mainString;
    }

    //------This converts the audio to the byte array------------

    public byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read = 0;
        byte[] buffer = new byte[1024];
        while (read != -1) {
            read = in.read(buffer);
            if (read != -1)
                out.write(buffer,0,read);
        }
        out.close();
        return out.toByteArray();
    }

    //-----------This converts the byte array to a string----------

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

}

package com.voicemail_project.android.voicemail;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class LastActivity extends AppCompatActivity {

    private EditText mEditText;
    private Button mPlay;

    private android.support.v7.widget.Toolbar mToolbar;

    private String mRecord;
    private String mEditted;

    private MediaPlayer mediaPlayer = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_last);

        mRecord = getIntent().getExtras().getString("recVoice");
        mEditted = "#VoiceMail " + mRecord;

        mEditText = (EditText) findViewById(R.id.resultText);
        mEditText.setText(mEditted);

        mPlay = (Button) findViewById(R.id.playButton);

        mToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.contact_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte [] music = hexStringToByteArray(mRecord);

                playMp3(music);
            }
        });
    }


    //-------TO PLAY THE RECORED AUDIO USE THIS FUNCTION-------

    private void playMp3(byte[] mp3SoundByteArray) {
        try {
            // create temp file that will hold byte array
            File tempMp3 = File.createTempFile("kurchina", "mp3", getCacheDir());
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(mp3SoundByteArray);
            fos.close();

            // resetting mediaplayer instance to evade problems
            mediaPlayer.reset();

            // In case you run into issues with threading consider new instance like:
            // MediaPlayer mediaPlayer = new MediaPlayer();

            // Tried passing path directly, but kept getting
            // "Prepare failed.: status=0x1"
            // so using file descriptor instead
            FileInputStream fis = new FileInputStream(tempMp3);
            mediaPlayer.setDataSource(fis.getFD());

            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();
        }
    }

    //-------THIS CONVERTS THE HEXSTRING TO BYTE ARRAY

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    //--------TO SEND SMS USE THIS FUNCTION----------

    private void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();

        ArrayList<String> messageParts = sms.divideMessage(message);

        sms.sendMultipartTextMessage(phoneNumber, null, messageParts, null, null);

        Toast.makeText(this,mEditted,Toast.LENGTH_LONG).show();

    }

}

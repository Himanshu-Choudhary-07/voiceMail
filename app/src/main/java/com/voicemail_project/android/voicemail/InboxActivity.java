package com.voicemail_project.android.voicemail;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class InboxActivity extends AppCompatActivity {

    private static InboxActivity inst;
    private ArrayList<String> mSmsList = new ArrayList<String>();
    private ListView mListView;
    public ArrayAdapter mAdapter;
    final int REQUEST_CODE_FOR_PERMISSION = 123;

    private Toolbar mToolbar;

    public static InboxActivity instance() {
        return inst;
    }

    @Override
    public void onStart() {
        super.onStart();
        inst = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        mToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.inboxHeader);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("SMS INBOX");

        mListView = (ListView) findViewById(R.id.message_list);

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSmsList);

        mListView.setAdapter(mAdapter);

        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") ==
                PackageManager.PERMISSION_GRANTED) {
            refreshInbox();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_FOR_PERMISSION);
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    String[] smsMessages = mSmsList.get(i).split("\n");
                    String address = smsMessages[0];
                    String smsMessage = "";
                    for (int j = 1; j < smsMessages.length; ++j) {
                        smsMessage += smsMessages[j];
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void refreshInbox() {
        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null);
        int indexBody = smsInboxCursor.getColumnIndex("body");
        int indexAddress = smsInboxCursor.getColumnIndex("address");
        if (indexBody < 0 || !smsInboxCursor.moveToFirst()) return;
        mAdapter.clear();
        do {

            String str = smsInboxCursor.getString(indexAddress) +
                    "\n" + smsInboxCursor.getString(indexBody) + "\n";

            if (str.contains("#VoiceMail")) {
                String originalString = str;
                originalString = originalString.replaceFirst("#VoiceMail ", "");
                mAdapter.add(originalString);
            }

        } while (smsInboxCursor.moveToNext());
    }

    public void updateList(final String smsMessage) {
        mAdapter.insert(smsMessage, 0);
        mAdapter.notifyDataSetChanged();
    }
}
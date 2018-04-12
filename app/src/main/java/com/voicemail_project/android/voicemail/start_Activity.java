package com.voicemail_project.android.voicemail;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class start_Activity extends AppCompatActivity {
    private EditText mPhoneText;
    private EditText mCodeText;

    private ProgressBar mPhoneBar;
    private ProgressBar mCodeBar;

    private LinearLayout mPhoneLayout;
    private LinearLayout mCodeLayout;

    private TextView mErrorTextView;
    private TextView mUsageTextView;

    private Button mSendButton;
    private int btntype = 0;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBack;

    private FirebaseAuth mAuth;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    private String mPhoneNumberTransfer;

    private DatabaseReference mDatabaseReference;
    private FirebaseUser mCurrentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_);

        mAuth = FirebaseAuth.getInstance();

        mErrorTextView = (TextView) findViewById(R.id.inCompleteTextView);
        mUsageTextView = (TextView) findViewById(R.id.usage_textView);

        mPhoneText = (EditText) findViewById(R.id.phone_editText);
        mCodeText = (EditText) findViewById(R.id.verification_code_editText);

        mPhoneLayout = (LinearLayout) findViewById(R.id.phoneLayout);
        mCodeLayout = (LinearLayout) findViewById(R.id.verification_layout);

        mPhoneBar = (ProgressBar) findViewById(R.id.phone_progress);
        mCodeBar = (ProgressBar) findViewById(R.id.verification_progress);

        mSendButton = (Button) findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(btntype == 0) {

                    mPhoneBar.setVisibility(View.VISIBLE);
                    mPhoneText.setEnabled(false);
                    mSendButton.setEnabled(false);

                    String mPhoneNumber = mPhoneText.getText().toString();

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            mPhoneNumber,
                            120,
                            TimeUnit.SECONDS,
                            start_Activity.this,
                            mCallBack

                    );
                }else{

                    mSendButton.setEnabled(false);
                    mCodeBar.setVisibility(View.VISIBLE);

                    String VerificationCode = mCodeText.getText().toString();

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, VerificationCode);
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });

        mCallBack = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {

                mErrorTextView.setText(R.string.error);
                mErrorTextView.setVisibility(View.VISIBLE);
                mUsageTextView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                mPhoneBar.setVisibility(View.INVISIBLE);
                mCodeLayout.setVisibility(View.VISIBLE);

                mSendButton.setText("VERIFY CODE");

                btntype = 1;

                mSendButton.setEnabled(true);

            }
        };

    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information

                            FirebaseUser user = task.getResult().getUser();
                            contact_function();

                        } else {
                            // Sign in failed, display a message and update the UI
                            mErrorTextView.setText(R.string.error);
                            mErrorTextView.setVisibility(View.VISIBLE);
                            mUsageTextView.setVisibility(View.INVISIBLE);

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                mErrorTextView.setText(R.string.code_error);
                            }
                        }
                    }
                });
    }

    private void contact_function() {

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        String current_id = mCurrentUser.getUid();
        mPhoneNumberTransfer = mCurrentUser.getPhoneNumber();

        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(current_id);

        HashMap<String,String> hashMap = new HashMap<>();
        hashMap.put("number",mPhoneNumberTransfer);
        hashMap.put("name", "Display_Name");
        hashMap.put("image", "No_Image");

        mDatabaseReference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {

                    Intent verificationComplete = new Intent(start_Activity.this, MainActivity.class);
                    startActivity(verificationComplete);
                    finish();

                    Toast.makeText(start_Activity.this, "Successfully Verified.", Toast.LENGTH_LONG).show();

                } else {

                    Toast.makeText(start_Activity.this, "Error Ocurred: Please try again.", Toast.LENGTH_LONG).show();

                }
            }
        });
    }
}

package com.example.android.meditationhub.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.android.meditationhub.BuildConfig;
import com.example.android.meditationhub.R;
import com.example.android.meditationhub.databinding.ActivitySignInBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import timber.log.Timber;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding signInBinding;

    private boolean isLogin;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signInBinding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        mAuth = FirebaseAuth.getInstance();

        setupLogRegButton();

        signInBinding.regLogSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupLogRegButton();
            }
        });

        signInBinding.logRegBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEmpty()) return;
                inProgress(true);

                if (isLogin) {
                    mAuth.signInWithEmailAndPassword(signInBinding.emailEditText.getText().toString(),
                            signInBinding.passwordEditText.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            Toast.makeText(SignInActivity.this, "You have been signed in", Toast.LENGTH_SHORT).show();
                            ;
                            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                            return;
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            inProgress(false);
                            Toast.makeText(SignInActivity.this, "Sign in failed: " + e, Toast.LENGTH_SHORT).show();
                            Timber.e(e);
                        }
                    });
                } else {
                    if (TextUtils.isEmpty(signInBinding.activationEt.getText().toString())) {
                        signInBinding.activationEt.setError("REQUIRED");
                        return;
                    }
                    if (signInBinding.activationEt.getText().toString() != BuildConfig.ACTIVATION_KEY) {
                        mAuth.createUserWithEmailAndPassword(signInBinding.emailEditText.getText().toString(),
                                signInBinding.passwordEditText.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                Toast.makeText(SignInActivity.this, "You account has been created", Toast.LENGTH_SHORT).show();
                                ;
                                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                inProgress(false);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                inProgress(false);
                                Toast.makeText(SignInActivity.this, "Registration has failed: " + e, Toast.LENGTH_SHORT).show();
                                Timber.e(e);
                            }
                        });
                    } else {
                        Toast.makeText(SignInActivity.this, "Your activation code is incorrect. Access to the meditation audio files is restricted to those who have received the code.", Toast.LENGTH_LONG).show();
                    }

                }

            }
        });

        signInBinding.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });
    }

    private void setupLogRegButton() {
        if (signInBinding.regLogSwitch.isChecked()) {
            isLogin = true;
            signInBinding.activationEt.setVisibility(View.GONE);
        } else {
            isLogin = false;
            signInBinding.activationEt.setVisibility(View.VISIBLE);
        }
    }

    private void inProgress(boolean x) {
        if (x) {
            signInBinding.loginProgressBar.setVisibility(View.VISIBLE);
            signInBinding.logRegBt.setEnabled(false);
            signInBinding.regLogSwitch.setEnabled(false);
            signInBinding.backButton.setEnabled(false);
        } else {
            signInBinding.loginProgressBar.setVisibility(View.GONE);
            signInBinding.logRegBt.setEnabled(true);
            signInBinding.regLogSwitch.setEnabled(true);
            signInBinding.backButton.setEnabled(true);
        }
    }

    private boolean isEmpty() {
        if (TextUtils.isEmpty(signInBinding.emailEditText.getText().toString())) {
            signInBinding.emailEditText.setError("REQUI RED");
            return true;
        }
        if (TextUtils.isEmpty(signInBinding.passwordEditText.getText().toString())) {
            signInBinding.passwordEditText.setError("REQUIRED");
            return true;
        }
        return false;
    }
}

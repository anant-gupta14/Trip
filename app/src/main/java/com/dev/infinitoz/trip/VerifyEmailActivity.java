package com.dev.infinitoz.trip;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.trip.util.Messages;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private TextView emailVerifyMessage, email;
    private Button verifyEmailBT;
    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);
        linearLayout = findViewById(R.id.verifyLayout);
        emailVerifyMessage = findViewById(R.id.emailVerifyMessage);
        email = findViewById(R.id.email);
        verifyEmailBT = findViewById(R.id.verifyEmailBT);

        emailVerifyMessage.setText(Messages.EMAIL_VERIFY_MESSAGE);
        FirebaseUser user = (FirebaseUser) TripContext.getValue(Constants.USER);
        if (user != null) {
            email.setText(user.getEmail());
        }
        verifyEmailBT.setOnClickListener(v -> {
            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Snackbar.make(linearLayout, Messages.VERIFICATION_EMAIL_SENT, Snackbar.LENGTH_LONG).show();
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(VerifyEmailActivity.this, MainActivity.class);
                        startActivity(intent);
                    } else {
                        Snackbar.make(linearLayout, Messages.SOMETHING_WENT_WRONG, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
        });
    }
}

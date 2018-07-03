package com.dev.infinitoz;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.dev.infinitoz.trip.Constants;
import com.dev.infinitoz.trip.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText newPass, confirmNewPass, oldPass;
    private Button changePassBT;
    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        oldPass = findViewById(R.id.oldPass);
        newPass = findViewById(R.id.newPass);
        confirmNewPass = findViewById(R.id.confirmChangePass);
        changePassBT = findViewById(R.id.changePassBT);
        linearLayout = findViewById(R.id.changePassLayout);

        changePassBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ("".equals(oldPass.getText().toString().trim()) || "".equals(newPass.getText().toString().trim()) || "".equals(confirmNewPass.getText().toString().trim())) {
                    Snackbar.make(linearLayout, "password cant be empty", Snackbar.LENGTH_LONG).show();
                }
                FirebaseUser user = (FirebaseUser) TripContext.getValue(Constants.USER);
                String email = user.getEmail();
                AuthCredential credential = EmailAuthProvider.getCredential(email, oldPass.getText().toString());
                user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            user.updatePassword(newPass.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Snackbar.make(linearLayout, "password updated", Snackbar.LENGTH_LONG).show();
                                    } else {
                                        Snackbar.make(linearLayout, "Something wrong", Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            });
                        } else {
                            Snackbar.make(linearLayout, "Auth failed", Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }
}

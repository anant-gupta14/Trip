package com.dev.infinitoz.trip;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.infinitoz.TripContext;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private TextView userEmailTextView;
    private EditText userName, userPhone;
    private RadioGroup vehicleGroup;
    private Button updateButton;

    private FirebaseUser user;
    private DatabaseReference userDBRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        user = (FirebaseUser) TripContext.getValue(Constants.USER);
        initalize();
        getUserInfo(user.getUid().toString());
        updateButton.setOnClickListener(v -> {
            saveUserProfie();
        });


    }

    private void saveUserProfie() {
        Map<String, Object> saveMap = new HashMap<>();
        if (userName.getText().toString() != null) {
            saveMap.put(Constants.NAME, userName.getText().toString());
        }

        if (userPhone.getText().toString() != null) {
            saveMap.put(Constants.PHONE, userPhone.getText().toString());
        }
        int radioButtonId = vehicleGroup.getCheckedRadioButtonId();
        final RadioButton radioButton = findViewById(radioButtonId);
        if (radioButton.getText() == null) {
            Toast.makeText(UserProfileActivity.this, "Please select Convaynace mode", Toast.LENGTH_SHORT).show();
            return;
        }
        saveMap.put(Constants.VEHICLE_TYPE, radioButton.getText().toString());
        userDBRef.updateChildren(saveMap);

    }

    private void getUserInfo(String userId) {
        userDBRef = FirebaseDatabase.getInstance().getReference().child(Constants.USERS).child(userId);
        userDBRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> dataMap = (Map<String, Object>) dataSnapshot.getValue();
                    if (dataMap.get(Constants.NAME) != null) {
                        userName.setText(dataMap.get(Constants.NAME).toString());
                    }
                    if (dataMap.get(Constants.PHONE) != null) {
                        userPhone.setText(dataMap.get(Constants.PHONE).toString());
                    }
                    if (dataMap.get(Constants.VEHICLE_TYPE) != null) {
                        String vType = dataMap.get(Constants.VEHICLE_TYPE).toString();
                        switch (vType) {
                            case Constants.CAR:
                                vehicleGroup.check(R.id.userCar);
                                break;
                            case Constants.BIKE:
                                vehicleGroup.check(R.id.userBike);
                                break;
                            case Constants.HUMAN:
                                vehicleGroup.check(R.id.human);
                                break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private void initalize() {
        userEmailTextView = findViewById(R.id.userEmail);
        userName = findViewById(R.id.userName);
        userPhone = findViewById(R.id.userPhone);
        vehicleGroup = findViewById(R.id.vehicleGroup);
        updateButton = findViewById(R.id.updateButton);
    }
}

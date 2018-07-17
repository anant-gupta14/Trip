package com.dev.infinitoz.trip;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.model.User;
import com.dev.infinitoz.trip.util.Utility;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminManagementActivity extends AppCompatActivity {
    String tripId;
    //FirebaseRecyclerAdapter<User, UserViewHolder> adapter;
    UserViewAdapter adapter;

    private RecyclerView recyclerView;
    private DatabaseReference tripUserDBReference, userDBRef;
    private boolean isUserView;
    private List<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_management);
        recyclerView = findViewById(R.id.userList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        if (TripContext.getValue(Constants.IS_USER_VIEW) != null) {
            isUserView = true;
        }
        populateUsers();
        populateAdmin();
    }

    private void populateUsers() {
        tripId = (String) TripContext.getValue(Constants.TRIP_ID);
        tripUserDBReference = FirebaseDatabase.getInstance().getReference().child(Constants.TRIP).child(tripId).child(Constants.USERS);

        tripUserDBReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    users = new ArrayList<>();
                    Map<String, Object> usersMap = (Map<String, Object>) dataSnapshot.getValue();
                    final Integer[] count = new Integer[1];
                    count[0] = 0;
                    for (String userID : usersMap.keySet()) {
                        count[0] = new Integer(count[0].intValue() + 1);

                        Map<String, Object> userData = (Map<String, Object>) usersMap.get(userID);
                        if (userData.get(Constants.IS_REMOVED) == null || !(boolean) userData.get(Constants.IS_REMOVED)) {
                            User user = checkUserInMap(userID);
                            if (user != null) {
                                users.add(user);
                            } else {
                                FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                            User user = dataSnapshot.getValue(User.class);
                                            user.setuId(userID);
                                            Log.d("User", user.toString());
                                            ((Map<String, User>) TripContext.getValue(Constants.USER_DATA_MAP)).put(userID, user);
                                            users.add(user);

                                        }
                                        if (count[0].equals(usersMap.size())) {
                                            adapter = new UserViewAdapter(users);
                                            recyclerView.setAdapter(adapter);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }
                            if (count[0].equals(usersMap.size())) {
                                adapter = new UserViewAdapter(users);
                                recyclerView.setAdapter(adapter);
                            }
                        }
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void populateAdmin() {
        String adminId = (String) TripContext.getValue(Constants.ADMIN);
        if (adminId != null) {
            User user = checkUserInMap(adminId);
            if (user != null) {
                users.add(user);
                adapter.notifyDataSetChanged();
            }
        }

    }


    private User checkUserInMap(String userID) {
        Map<String, User> userMap = (Map<String, User>) TripContext.getValue(Constants.USER_DATA_MAP);
        if (userMap != null) {
            return userMap.get(userID);
        }
        return null;
    }

    public void onRemove(View view) {
        Integer position = (Integer) view.getTag();
        User user = users.remove(position.intValue());
        adapter.notifyItemRemoved(position.intValue());
        removeUserFromTrip(user.getuId());
    }

    private void removeUserFromTrip(String userId) {
        Utility.removeUserFromTrip(true, userId, tripId);
        //Utility.updateUserToTrip(false, userId);
    }

    public void restore(User user, int position) {
        users.add(position, user);
        adapter.notifyItemInserted(position);
    }

    public class UserViewAdapter extends RecyclerView.Adapter<UserViewHolder> {
        List<User> users;

        public UserViewAdapter() {
        }

        public UserViewAdapter(List<User> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.field, parent, false);

            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            holder.userName.setText(users.get(position).getName());
            holder.userPhone.setText(users.get(position).getPhone());
            holder.deleteBtn.setTag(position);
            Log.d("position::", +position + "");
            if (TripContext.getValue(Constants.IS_USER_VIEW) != null) {
                holder.deleteBtn.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return users.size();
        }


    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userPhone;
        Button deleteBtn;

        public UserViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userPhone = itemView.findViewById(R.id.userPhone);
            deleteBtn = itemView.findViewById(R.id.delete_button);
        }
    }

}
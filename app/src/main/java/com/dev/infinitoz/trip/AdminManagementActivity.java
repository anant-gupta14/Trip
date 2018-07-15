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
    List<User> users = new ArrayList<>();
    private RecyclerView recyclerView;
    private DatabaseReference tripUserDBReference, userDBRef;
    private boolean isUserView;

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
    }

    private void populateUsers() {
        tripId = (String) TripContext.getValue(Constants.TRIP_ID);
        tripUserDBReference = FirebaseDatabase.getInstance().getReference().child(Constants.TRIP).child(tripId).child(Constants.USERS);

        tripUserDBReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> usersMap = (Map<String, Object>) dataSnapshot.getValue();
                    final Integer[] count = new Integer[1];
                    count[0] = 0;
                    for (String userID : usersMap.keySet()) {
                        count[0] = new Integer(count[0].intValue() + 1);
                        Map<String, Object> userData = (Map<String, Object>) usersMap.get(userID);
                        if (userData.get(Constants.IS_REMOVED) == null || !(boolean) userData.get(Constants.IS_REMOVED)) {
                            FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                        User user = dataSnapshot.getValue(User.class);
                                        user.setuId(userID);
                                        Log.d("User", user.toString());
                                        users.add(user);

                                    }
                                    if (count[0].equals(usersMap.size())) {
                                        if (isUserView) {
                                            adapter = new UserViewOnlyAdapter(users);
                                            recyclerView.setAdapter(adapter);
                                        } else {
                                            adapter = new UserViewAdapter(users);
                                            recyclerView.setAdapter(adapter);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



        /*
        userDBRef = FirebaseDatabase.getInstance().getReference(Constants.USERS);
        FirebaseRecyclerOptions<User> options = new FirebaseRecyclerOptions.Builder<User>()
                .setIndexedQuery( qry,userDBRef, User.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<User, UserViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull User user) {
                holder.userName.setText(user.getName());
            }

            @NonNull
            @Override
            public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.field, parent, false);

                return new UserViewHolder(view);
            }
        };

    }

    public void onDelete(View v) {
       // parentLinearLayout.removeView((View) v.getParent());
    }

    public void onAddField(String userName) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View rowView = inflater.inflate(R.layout.field, null);
        TextView userTV = rowView.findViewById(R.id.userName);
        userTV.setText(userName);
        // Add the new row before the add field button.
        //parentLinearLayout.addView(rowView, parentLinearLayout.getChildCount() - 1);
    }



    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }*/
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
            holder.deleteBtn.setTag(position);
            Log.d("position::", +position + "");
        }

        @Override
        public int getItemCount() {
            return users.size();
        }


    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        Button deleteBtn;

        public UserViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            deleteBtn = itemView.findViewById(R.id.delete_button);
        }
    }

    public class UserViewOnlyAdapter extends UserViewAdapter {
        List<User> users;


        public UserViewOnlyAdapter(List<User> users) {
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
        }

        @Override
        public int getItemCount() {
            return users.size();
        }


    }

    public class UserViewOnlyHolder extends RecyclerView.ViewHolder {
        TextView userName;
        Button deleteBtn;

        public UserViewOnlyHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            deleteBtn = itemView.findViewById(R.id.delete_button);
            deleteBtn.setVisibility(View.GONE);
        }
    }
}
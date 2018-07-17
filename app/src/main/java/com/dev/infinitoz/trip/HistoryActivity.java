package com.dev.infinitoz.trip;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dev.infinitoz.model.History;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<History> histories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.historyList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        populateHistories();

    }

    private void populateHistories() {
        String userId = getIntent().getExtras().getString(Constants.USER_ID);
        FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userId).child(Constants.HISTORY).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot history : dataSnapshot.getChildren()) {
                        fetchHistoryInfo(history.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void fetchHistoryInfo(String historyId) {

        FirebaseDatabase.getInstance().getReference(Constants.HISTORY).child(historyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                histories = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    History history = new History();
                    if (dataSnapshot.child(Constants.START_POINT) != null) {
                        history.setStartPoint(dataSnapshot.child(Constants.START_POINT).getValue().toString());
                    }
                    if (dataSnapshot.child(Constants.END_POINT) != null) {
                        history.setEndPoint(dataSnapshot.child(Constants.END_POINT).getValue().toString());
                    }
                    if (dataSnapshot.child(Constants.CREATED_TIME) != null) {
                        history.setTimeStamp(dataSnapshot.child(Constants.CREATED_TIME).getValue().toString());
                    }
                    histories.add(history);
                    adapter = new HistoryAdapter(histories);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder>

    {
        List<History> histories;

        HistoryAdapter(List<History> histories) {
            this.histories = histories;
        }

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.history_field, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            holder.startPoint.setText(Constants.START_POINT.toUpperCase() + ":" + histories.get(position).getStartPoint());
            holder.endPoint.setText(Constants.END_POINT.toUpperCase() + ":" + histories.get(position).getEndPoint());
            holder.timeStamp.setText(histories.get(position).getTimeStamp());

        }

        @Override
        public int getItemCount() {
            return histories.size();
        }
    }

    public class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView startPoint, endPoint, timeStamp;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            startPoint = itemView.findViewById(R.id.startPoint);
            endPoint = itemView.findViewById(R.id.endPoint);
            timeStamp = itemView.findViewById(R.id.timeStamp);
        }
    }
}

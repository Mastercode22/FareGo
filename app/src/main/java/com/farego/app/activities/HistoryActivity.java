package com.farego.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farego.app.R;
import com.farego.app.adapters.HistoryAdapter;
import com.farego.app.db.AppDatabase;
import com.farego.app.utils.SessionManager;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private HistoryAdapter adapter;
    private AppDatabase db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db      = AppDatabase.getInstance(this);
        session = new SessionManager(this);

        recyclerView = findViewById(R.id.rv_history);
        tvEmpty      = findViewById(R.id.tv_history_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new HistoryAdapter(
                item -> {
                    // Reuse route: pass id to MainActivity
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("reuse_route_id", item.id);
                    startActivity(intent);
                    finish();
                },
                item -> {
                    // Toggle favourite on background thread
                    AppDatabase.DB_EXECUTOR.execute(() ->
                            db.routeHistoryDao().setFavourite(item.id, !item.isFavourite));
                }
        );

        recyclerView.setAdapter(adapter);
        loadHistory();

        findViewById(R.id.btn_history_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_clear_history).setOnClickListener(v -> clearHistory());
    }

    private void loadHistory() {
        db.routeHistoryDao().getByUser(session.getUserId())
                .observe(this, list -> {
                    if (list == null || list.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                        adapter.submitList(list);
                    }
                });
    }

    private void clearHistory() {
        AppDatabase.DB_EXECUTOR.execute(() ->
                db.routeHistoryDao().clearHistory(session.getUserId()));
    }
}
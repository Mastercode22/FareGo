package com.farego.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.farego.app.R;
import com.farego.app.db.AppDatabase;
import com.farego.app.db.entity.RouteHistory;
import com.farego.app.utils.FareCalculator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TripDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ROUTE_ID = "route_id";

    private TextView tvFrom, tvTo, tvDate, tvDistance,
            tvDuration, tvFare, tvTransport, tvTraffic;

    private int routeId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        tvFrom      = findViewById(R.id.tv_detail_from);
        tvTo        = findViewById(R.id.tv_detail_to);
        tvDate      = findViewById(R.id.tv_detail_date);
        tvDistance  = findViewById(R.id.tv_detail_distance);
        tvDuration  = findViewById(R.id.tv_detail_duration);
        tvFare      = findViewById(R.id.tv_detail_fare);
        tvTransport = findViewById(R.id.tv_detail_transport);
        tvTraffic   = findViewById(R.id.tv_detail_traffic);

        routeId = getIntent().getIntExtra(EXTRA_ROUTE_ID, -1);
        if (routeId != -1) loadRoute(routeId);

        Button btnReuseRoute = findViewById(R.id.btn_reuse_route);
        btnReuseRoute.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("reuse_route_id", routeId);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btn_detail_back).setOnClickListener(v -> finish());
    }

    private void loadRoute(int id) {
        AppDatabase.DB_EXECUTOR.execute(() -> {
            RouteHistory route = AppDatabase.getInstance(this).routeHistoryDao().getById(id);
            if (route == null) return;

            runOnUiThread(() -> {
                tvFrom.setText(route.originLabel);
                tvTo.setText(route.destinationLabel);
                tvDistance.setText(String.format("%.1f km", route.distanceKm));
                tvDuration.setText(route.durationMinutes + " min");
                tvFare.setText(FareCalculator.format(route.estimatedFare));
                tvTransport.setText(route.transportType);
                tvTraffic.setText(route.trafficCondition != null ? route.trafficCondition : "—");

                String date = new SimpleDateFormat("EEE dd MMM yyyy, HH:mm", Locale.getDefault())
                        .format(new Date(route.timestamp));
                tvDate.setText(date);
            });
        });
    }
}
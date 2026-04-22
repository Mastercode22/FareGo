package com.farego.app.ui.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.farego.app.R;
import com.farego.app.db.AppDatabase;
import com.farego.app.db.entity.FareRate;
import com.farego.app.model.FareResult;
import com.farego.app.utils.FareCalculator;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class TransportBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_DISTANCE = "distance_km";
    private static final String ARG_TRAFFIC  = "traffic";

    public interface OnTransportSelectedListener {
        void onSelected(FareCalculator.TransportType type);
    }

    private OnTransportSelectedListener listener;
    private double distanceKm;
    private FareCalculator.TrafficCondition trafficCondition;

    // Cards
    private CardView cardTrotro, cardTaxi, cardUber;
    private TextView tvTrotroFare, tvTaxiFare, tvUberFare;
    private TextView tvTrotroBadge, tvTaxiBadge, tvUberBadge;

    private FareCalculator.TransportType selected = FareCalculator.TransportType.TROTRO;

    public static TransportBottomSheet newInstance(double distanceKm,
            FareCalculator.TrafficCondition traffic) {
        TransportBottomSheet sheet = new TransportBottomSheet();
        Bundle args = new Bundle();
        args.putDouble(ARG_DISTANCE, distanceKm);
        args.putString(ARG_TRAFFIC, traffic.name());
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnTransportSelectedListener(OnTransportSelectedListener l) {
        this.listener = l;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetStyle);
        if (getArguments() != null) {
            distanceKm       = getArguments().getDouble(ARG_DISTANCE, 5.0);
            trafficCondition = FareCalculator.TrafficCondition.valueOf(
                    getArguments().getString(ARG_TRAFFIC, "LOW"));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_transport, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardTrotro    = view.findViewById(R.id.card_trotro);
        cardTaxi      = view.findViewById(R.id.card_taxi);
        cardUber      = view.findViewById(R.id.card_uber);
        tvTrotroFare  = view.findViewById(R.id.tv_trotro_fare);
        tvTaxiFare    = view.findViewById(R.id.tv_taxi_fare);
        tvUberFare    = view.findViewById(R.id.tv_uber_fare);
        tvTrotroBadge = view.findViewById(R.id.tv_trotro_badge);
        tvTaxiBadge   = view.findViewById(R.id.tv_taxi_badge);
        tvUberBadge   = view.findViewById(R.id.tv_uber_badge);

        loadFares();

        cardTrotro.setOnClickListener(v -> selectTransport(FareCalculator.TransportType.TROTRO));
        cardTaxi.setOnClickListener(v   -> selectTransport(FareCalculator.TransportType.TAXI));
        cardUber.setOnClickListener(v   -> selectTransport(FareCalculator.TransportType.UBER));

        view.findViewById(R.id.btn_confirm_transport).setOnClickListener(v -> {
            if (listener != null) listener.onSelected(selected);
            dismiss();
        });

        highlightSelected();
    }

    private void loadFares() {
        AppDatabase.DB_EXECUTOR.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

            FareRate trotroRate = db.fareRateDao().getByType("TROTRO");
            FareRate taxiRate   = db.fareRateDao().getByType("TAXI");
            FareRate uberRate   = db.fareRateDao().getByType("UBER");

            FareResult trotro = trotroRate != null
                    ? FareCalculator.calculate(distanceKm, trotroRate, trafficCondition)
                    : FareCalculator.estimateOffline(distanceKm, FareCalculator.TransportType.TROTRO, trafficCondition);

            FareResult taxi = taxiRate != null
                    ? FareCalculator.calculate(distanceKm, taxiRate, trafficCondition)
                    : FareCalculator.estimateOffline(distanceKm, FareCalculator.TransportType.TAXI, trafficCondition);

            FareResult uber = uberRate != null
                    ? FareCalculator.calculate(distanceKm, uberRate, trafficCondition)
                    : FareCalculator.estimateOffline(distanceKm, FareCalculator.TransportType.UBER, trafficCondition);

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                tvTrotroFare.setText(trotro.getFormattedRange());
                tvTaxiFare.setText(taxi.getFormattedRange());
                tvUberFare.setText(uber.getFormattedRange());

                if (trotro.isPeakHour) tvTrotroBadge.setVisibility(View.VISIBLE);
                if (taxi.isPeakHour)   tvTaxiBadge.setVisibility(View.VISIBLE);
                if (uber.isPeakHour)   tvUberBadge.setVisibility(View.VISIBLE);
            });
        });
    }

    private void selectTransport(FareCalculator.TransportType type) {
        selected = type;
        highlightSelected();
    }

    private void highlightSelected() {
        int activeColor   = requireContext().getResources().getColor(R.color.yellow_accent, null);
        int inactiveColor = requireContext().getResources().getColor(R.color.surface_card, null);
        int activeStroke  = requireContext().getResources().getColor(R.color.yellow_accent, null);

        resetCard(cardTrotro);
        resetCard(cardTaxi);
        resetCard(cardUber);

        CardView active = selected == FareCalculator.TransportType.TROTRO ? cardTrotro
                        : selected == FareCalculator.TransportType.TAXI   ? cardTaxi
                        : cardUber;

        active.setCardBackgroundColor(0xFF1A1A2E);
        active.setCardElevation(12f);
        // Glow via elevation + tint — stroke requires MaterialCardView; using elevation here
    }

    private void resetCard(CardView card) {
        card.setCardBackgroundColor(
                requireContext().getResources().getColor(R.color.surface_card, null));
        card.setCardElevation(4f);
    }
}

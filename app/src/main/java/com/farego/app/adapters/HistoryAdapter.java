package com.farego.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.farego.app.R;
import com.farego.app.utils.FareCalculator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.farego.app.db.entity.RouteHistory;

public class HistoryAdapter extends ListAdapter<RouteHistory, HistoryAdapter.ViewHolder> {

    public interface OnItemClickListener  { void onClick(RouteHistory item); }
    public interface OnFavClickListener   { void onFav(RouteHistory item); }

    private final OnItemClickListener clickListener;
    private final OnFavClickListener  favListener;

    private static final DiffUtil.ItemCallback<RouteHistory> DIFF =
            new DiffUtil.ItemCallback<RouteHistory>() {
        @Override public boolean areItemsTheSame(@NonNull RouteHistory a, @NonNull RouteHistory b) {
            return a.id == b.id;
        }
        @Override public boolean areContentsTheSame(@NonNull RouteHistory a, @NonNull RouteHistory b) {
            return a.timestamp == b.timestamp && a.isFavourite == b.isFavourite;
        }
    };

    public HistoryAdapter(OnItemClickListener click, OnFavClickListener fav) {
        super(DIFF);
        this.clickListener = click;
        this.favListener   = fav;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RouteHistory item = getItem(position);
        holder.bind(item, clickListener, favListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDestination, tvDate, tvFare, tvDistance, tvTransport;
        ImageButton btnFav;

        ViewHolder(View v) {
            super(v);
            tvDestination = v.findViewById(R.id.tv_history_destination);
            tvDate        = v.findViewById(R.id.tv_history_date);
            tvFare        = v.findViewById(R.id.tv_history_fare);
            tvDistance    = v.findViewById(R.id.tv_history_distance);
            tvTransport   = v.findViewById(R.id.tv_history_transport);
            btnFav        = v.findViewById(R.id.btn_history_fav);
        }

        void bind(RouteHistory item,
                  OnItemClickListener click,
                  OnFavClickListener  fav) {
            tvDestination.setText(item.destinationLabel);
            tvFare.setText(FareCalculator.format(item.estimatedFare));
            tvDistance.setText(String.format("%.1f km", item.distanceKm));
            tvTransport.setText(item.transportType);

            String date = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                    .format(new Date(item.timestamp));
            tvDate.setText(date);

            btnFav.setImageResource(item.isFavourite
                    ? R.drawable.ic_star_filled
                    : R.drawable.ic_star_outline);

            itemView.setOnClickListener(v -> click.onClick(item));
            btnFav.setOnClickListener(v -> fav.onFav(item));
        }
    }
}

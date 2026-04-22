package com.farego.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.farego.app.R;

/** Simple 3-page onboarding ViewPager2 adapter using RecyclerView pages. */
public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageHolder> {

    private static final int[] TITLES = {
            R.string.onboard_title_1,
            R.string.onboard_title_2,
            R.string.onboard_title_3
    };
    private static final int[] DESCS = {
            R.string.onboard_desc_1,
            R.string.onboard_desc_2,
            R.string.onboard_desc_3
    };
    private static final int[] IMAGES = {
            R.drawable.ic_onboard_map,
            R.drawable.ic_onboard_fare,
            R.drawable.ic_onboard_go
    };

    private final LayoutInflater inflater;

    public OnboardingAdapter(FragmentActivity activity) {
        this.inflater = LayoutInflater.from(activity);
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_onboarding_page, parent, false);
        return new PageHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        holder.image.setImageResource(IMAGES[position]);
        holder.title.setText(TITLES[position]);
        holder.desc.setText(DESCS[position]);
    }

    @Override
    public int getItemCount() { return 3; }

    static class PageHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, desc;

        PageHolder(View v) {
            super(v);
            image = v.findViewById(R.id.iv_onboard_image);
            title = v.findViewById(R.id.tv_onboard_title);
            desc  = v.findViewById(R.id.tv_onboard_desc);
        }
    }
}

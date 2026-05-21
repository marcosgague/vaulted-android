package com.example.vaulted;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private List<AchievementEntity> list = new ArrayList<>();

    public void setList(List<AchievementEntity> items) {
        this.list = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvDescription;
        private final ImageView ivStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAchievementName);
            tvDescription = itemView.findViewById(R.id.tvAchievementDescription);
            ivStatus = itemView.findViewById(R.id.ivAchievementStatus);
        }

        public void bind(AchievementEntity a) {
            tvName.setText(a.name);
            tvDescription.setText(a.description.isEmpty() ? "Sin descripciÃ³n" : a.description);

            Log.d("AchievementAdapter", "iconUrl: " + a.iconUrl);

            if (a.iconUrl != null && !a.iconUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(a.iconUrl)
                        .placeholder(R.drawable.ic_achievement_locked)
                        .error(R.drawable.ic_achievement_locked)
                        .into(ivStatus);
            } else {
                ivStatus.setImageResource(a.unlocked
                        ? R.drawable.ic_achievement_unlocked
                        : R.drawable.ic_achievement_locked);
            }
        }
    }}

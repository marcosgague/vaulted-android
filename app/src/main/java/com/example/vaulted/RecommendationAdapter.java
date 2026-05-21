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

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {

    public interface OnRecommendationClickListener {
        void onRecommendationClick(GameRecommendation recommendation);
    }

    private List<GameRecommendation> recommendations = new ArrayList<>();
    private final OnRecommendationClickListener listener;

    public RecommendationAdapter(OnRecommendationClickListener listener) {
        this.listener = listener;
    }

    public void setRecommendations(List<GameRecommendation> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(recommendations.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivCover;
        private final TextView tvTitle, tvReason, tvBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivRecommendationCover);
            tvTitle = itemView.findViewById(R.id.tvRecommendationTitle);
            tvReason = itemView.findViewById(R.id.tvRecommendationReason);
            tvBadge = itemView.findViewById(R.id.tvRecommendationBadge);
        }

        void bind(GameRecommendation recommendation, OnRecommendationClickListener listener) {
            GameEntity game = recommendation.game;
            tvTitle.setText(game.title);
            tvReason.setText(recommendation.reason);
            tvBadge.setText("Recomendado");

            Glide.with(itemView.getContext())
                    .load(game.coverUrl)
                    .placeholder(R.drawable.ic_game_placeholder)
                    .error(R.drawable.ic_game_placeholder)
                    .into(ivCover);

            itemView.setOnClickListener(v -> listener.onRecommendationClick(recommendation));
        }
    }
}

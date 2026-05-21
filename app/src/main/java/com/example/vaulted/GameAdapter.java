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
import java.util.Collections;
import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    public interface OnGameClickListener {
        void onGameClick(GameEntity game);
    }

    private List<GameEntity> fullList = new ArrayList<>();
    private List<GameEntity> filteredList = new ArrayList<>();
    private final OnGameClickListener listener;

    public GameAdapter(OnGameClickListener listener) {
        this.listener = listener;
    }

    public void setFullList(List<GameEntity> games) {
        fullList = new ArrayList<>(games);
        filteredList = new ArrayList<>(games);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            String lower = query.toLowerCase();
            for (GameEntity g : fullList) {
                if (g.title != null && g.title.toLowerCase().contains(lower)) {
                    filteredList.add(g);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void sortByHours(boolean descending) {
        Collections.sort(fullList, (a, b) ->
                descending ? Integer.compare(b.hoursPlayed, a.hoursPlayed)
                        : Integer.compare(a.hoursPlayed, b.hoursPlayed));
        Collections.sort(filteredList, (a, b) ->
                descending ? Integer.compare(b.hoursPlayed, a.hoursPlayed)
                        : Integer.compare(a.hoursPlayed, b.hoursPlayed));
        notifyDataSetChanged();
    }

    public void sortByName() {
        Collections.sort(fullList, this::compareByTitle);
        Collections.sort(filteredList, this::compareByTitle);
        notifyDataSetChanged();
    }

    public void sortByLastPlayed() {
        Collections.sort(fullList, (a, b) -> Integer.compare(b.lastPlayed, a.lastPlayed));
        Collections.sort(filteredList, (a, b) -> Integer.compare(b.lastPlayed, a.lastPlayed));
        notifyDataSetChanged();
    }

    private int compareByTitle(GameEntity a, GameEntity b) {
        if (a.title == null) return 1;
        if (b.title == null) return -1;
        return a.title.compareToIgnoreCase(b.title);
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        holder.bind(filteredList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivCover;
        private final TextView tvTitle, tvHours, tvPlatform;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvHours = itemView.findViewById(R.id.tvHours);
            tvPlatform = itemView.findViewById(R.id.tvPlatform);
        }

        public void bind(GameEntity game, OnGameClickListener listener) {
            tvTitle.setText(game.title != null ? game.title : "Sin titulo");
            tvHours.setText(game.hoursPlayed + "h jugadas");
            tvPlatform.setText(game.platform != null ? game.platform.toUpperCase() : "");

            Glide.with(itemView.getContext())
                    .load(game.coverUrl)
                    .placeholder(R.drawable.ic_game_placeholder)
                    .error(R.drawable.ic_game_placeholder)
                    .into(ivCover);

            itemView.setOnClickListener(v -> listener.onGameClick(game));
        }
    }
}

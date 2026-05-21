package com.example.vaulted;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private List<GameReview> reviews = new ArrayList<>();

    public void setReviews(List<GameReview> reviews) {
        this.reviews = reviews != null ? reviews : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reviews.get(position));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvGameTitle, tvRating, tvText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGameTitle = itemView.findViewById(R.id.tvReviewGameTitle);
            tvRating = itemView.findViewById(R.id.tvReviewRating);
            tvText = itemView.findViewById(R.id.tvReviewText);
        }

        void bind(GameReview review) {
            tvGameTitle.setText(review.gameTitle != null && !review.gameTitle.isEmpty()
                    ? review.gameTitle
                    : "Juego");
            tvRating.setText(estrellas(review.rating));
            tvText.setText(review.text != null && !review.text.isEmpty()
                    ? review.text
                    : "Sin texto");
        }

        private String estrellas(long rating) {
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i <= 5; i++) {
                builder.append(i <= rating ? "*" : "-");
            }
            return builder.toString();
        }
    }
}

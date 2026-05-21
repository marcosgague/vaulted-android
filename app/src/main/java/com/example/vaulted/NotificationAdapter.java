package com.example.vaulted;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<NotificationItem> items = new ArrayList<>();

    public void setItems(List<NotificationItem> notifications) {
        items.clear();
        items.addAll(notifications);
        // aquí no se complica la vida con diffs porque el volumen es pequeño y el refresco completo sale más limpio
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvBody;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvBody = itemView.findViewById(R.id.tvNotificationBody);
        }

        void bind(NotificationItem item) {
            tvTitle.setText(item.title);
            tvBody.setText(item.body);
        }
    }
}

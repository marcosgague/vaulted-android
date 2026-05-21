package com.example.vaulted;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final String currentUid;
    private final List<ChatMessage> messages = new ArrayList<>();

    public MessageAdapter(String currentUid) {
        this.currentUid = currentUid;
    }

    public void setMessages(List<ChatMessage> items) {
        messages.clear();
        messages.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position), currentUid);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageText);
        }

        void bind(ChatMessage message, String currentUid) {
            boolean mine = currentUid.equals(message.from);
            tvMessage.setText(message.text);
            tvMessage.setBackgroundResource(mine ? R.drawable.bg_message_me : R.drawable.bg_message_other);
            tvMessage.setTextColor(itemView.getResources().getColor(
                    mine ? R.color.background : R.color.text_primary,
                    itemView.getContext().getTheme()
            ));

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) tvMessage.getLayoutParams();
            params.gravity = mine ? Gravity.END : Gravity.START;
            tvMessage.setLayoutParams(params);
        }
    }
}

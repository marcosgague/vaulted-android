package com.example.vaulted;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Typeface;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(ChatConversation conversation);
    }

    private final List<ChatConversation> conversations = new ArrayList<>();
    private final OnConversationClickListener listener;

    public ChatAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setConversations(List<ChatConversation> items) {
        conversations.clear();
        conversations.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_conversation, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(conversations.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvName, tvUsername, tvLastMessage, tvUnreadCount;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivChatAvatar);
            tvName = itemView.findViewById(R.id.tvChatName);
            tvUsername = itemView.findViewById(R.id.tvChatUsername);
            tvLastMessage = itemView.findViewById(R.id.tvChatLastMessage);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }

        void bind(ChatConversation conversation, OnConversationClickListener listener) {
            tvName.setText(conversation.name);
            tvUsername.setText(conversation.username);
            // aquí se aprieta un poco el diseño para que un chat no leído destaque incluso antes de abrirlo
            tvLastMessage.setText(conversation.unread
                    ? "Mensaje nuevo: " + conversation.lastMessage
                    : conversation.lastMessage);
            tvLastMessage.setTypeface(null, conversation.unread ? Typeface.BOLD : Typeface.NORMAL);
            tvName.setTypeface(null, conversation.unread ? Typeface.BOLD : Typeface.NORMAL);
            tvLastMessage.setTextColor(itemView.getResources().getColor(
                    conversation.unread ? R.color.text_primary : R.color.text_secondary,
                    itemView.getContext().getTheme()
            ));
            if (conversation.unreadCount > 0) {
                tvUnreadCount.setVisibility(View.VISIBLE);
                // si la cuenta se dispara, se recorta para que el badge no rompa el layout de la fila
                tvUnreadCount.setText(conversation.unreadCount > 99 ? "99+" : String.valueOf(conversation.unreadCount));
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }

            if (conversation.avatarUrl != null && !conversation.avatarUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(conversation.avatarUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_profile);
            }

            itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
        }
    }
}

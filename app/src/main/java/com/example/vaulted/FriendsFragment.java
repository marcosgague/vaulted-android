package com.example.vaulted;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsFragment extends Fragment {

    public static final String ARG_TARGET_USER_ID = "targetUserId";
    public static final String ARG_TARGET_NAME = "targetName";
    public static final String ARG_TARGET_USERNAME = "targetUsername";

    private ChatAdapter chatAdapter;
    private MessageAdapter messageAdapter;
    private TextView tvEmpty, tvChatTarget, tvChatTargetUsername;
    private View layoutChatList, layoutChatThread;
    private EditText etChatMessage;
    private RecyclerView rvMessages;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration conversationsListener;
    private ListenerRegistration messagesListener;
    private String currentUid;
    private String selectedChatId;
    private String selectedUserId;
    private String selectedName;
    private String selectedUsername;

    public FriendsFragment() {
        super(R.layout.fragment_friends);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;
        currentUid = mAuth.getCurrentUser().getUid();

        tvEmpty = view.findViewById(R.id.tvChatEmpty);
        tvChatTarget = view.findViewById(R.id.tvChatTarget);
        tvChatTargetUsername = view.findViewById(R.id.tvChatTargetUsername);
        layoutChatList = view.findViewById(R.id.layoutChatList);
        layoutChatThread = view.findViewById(R.id.layoutChatThread);
        etChatMessage = view.findViewById(R.id.etChatMessage);

        RecyclerView rvChat = view.findViewById(R.id.rvChat);
        chatAdapter = new ChatAdapter(this::abrirConversacion);
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(chatAdapter);

        rvMessages = view.findViewById(R.id.rvMessages);
        messageAdapter = new MessageAdapter(currentUid);
        LinearLayoutManager messagesLayout = new LinearLayoutManager(getContext());
        messagesLayout.setStackFromEnd(true);
        rvMessages.setLayoutManager(messagesLayout);
        rvMessages.setAdapter(messageAdapter);

        view.findViewById(R.id.btnBackToChats).setOnClickListener(v -> mostrarLista());
        view.findViewById(R.id.btnSendMessage).setOnClickListener(v -> enviarMensaje());

        escucharConversaciones();
        configurarChatEntrante();
    }

    @Override
    public void onDestroyView() {
        if (conversationsListener != null) conversationsListener.remove();
        if (messagesListener != null) messagesListener.remove();
        super.onDestroyView();
    }

    private void configurarChatEntrante() {
        Bundle args = getArguments();
        if (args == null) return;

        String userId = args.getString(ARG_TARGET_USER_ID);
        String name = args.getString(ARG_TARGET_NAME, "usuario");
        String username = args.getString(ARG_TARGET_USERNAME, "");
        if (userId != null) {
            abrirConversacion(new ChatConversation(
                    getChatId(currentUid, userId),
                    userId,
                    name,
                    username,
                    null,
                    ""
            ));
        }
    }

    private void escucharConversaciones() {
        conversationsListener = db.collection("chats")
                .whereArrayContains("participants", currentUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;
                    if (error != null || snapshot == null) {
                        tvEmpty.setText("No se pudieron cargar conversaciones");
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    List<ChatConversation> conversations = new ArrayList<>();
                    List<DocumentSnapshot> chatDocs = snapshot.getDocuments();
                    if (chatDocs.isEmpty()) {
                        chatAdapter.setConversations(conversations);
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (DocumentSnapshot chatDoc : snapshot.getDocuments()) {
                        List<String> participants = (List<String>) chatDoc.get("participants");
                        if (participants == null) continue;

                        String otherUid = null;
                        for (String uid : participants) {
                            if (!currentUid.equals(uid)) {
                                otherUid = uid;
                                break;
                            }
                        }
                        if (otherUid == null) continue;

                        String lastMessage = chatDoc.getString("lastMessage");
                        String lastMessageFrom = chatDoc.getString("lastMessageFrom");
                        Timestamp updatedAt = chatDoc.getTimestamp("updatedAt");
                        Timestamp lastRead = chatDoc.getTimestamp("lastRead." + currentUid);
                        // aquí se decide si la conversación merece marcarse como no leída antes de contar los mensajes uno por uno
                        boolean unread = lastMessageFrom != null
                                && !lastMessageFrom.equals(currentUid)
                                && updatedAt != null
                                && (lastRead == null || updatedAt.compareTo(lastRead) > 0);
                        agregarConversacionConPerfil(
                                conversations,
                                chatDocs.size(),
                                chatDoc.getId(),
                                otherUid,
                                lastMessage,
                                unread,
                                lastRead
                        );
                    }
                });
    }

    private void agregarConversacionConPerfil(List<ChatConversation> conversations, int totalChats,
                                              String chatId, String otherUid, String lastMessage,
                                              boolean unread, @Nullable Timestamp lastRead) {
        db.collection("users").document(otherUid).get()
                .addOnSuccessListener(userDoc -> {
                    String name = userDoc.getString("name");
                    String username = userDoc.getString("username");
                    String avatarUrl = userDoc.getString("steamAvatarUrl");

                    agregarConversacionConContador(conversations, totalChats, chatId, otherUid,
                            name != null && !name.isEmpty() ? name : "Usuario",
                            username != null && !username.isEmpty() ? username : "@usuario",
                            avatarUrl,
                            lastMessage,
                            unread,
                            lastRead);
                })
                .addOnFailureListener(e -> {
                    agregarConversacionConContador(conversations, totalChats, chatId, otherUid,
                            "Usuario", "@usuario", null, lastMessage, unread, lastRead);
                });
    }

    private void agregarConversacionConContador(List<ChatConversation> conversations, int totalChats,
                                                String chatId, String otherUid, String name,
                                                String username, @Nullable String avatarUrl,
                                                @Nullable String lastMessage, boolean unread,
                                                @Nullable Timestamp lastRead) {
        // el contador real sale de recorrer los mensajes pendientes, no solo de mirar el último texto del chat
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int unreadCount = 0;
                    for (DocumentSnapshot messageDoc : snapshot.getDocuments()) {
                        String to = messageDoc.getString("to");
                        Boolean alreadyRead = messageDoc.getBoolean("readBy." + currentUid);
                        if (currentUid.equals(to) && (alreadyRead == null || !alreadyRead)) {
                            unreadCount++;
                        }
                    }

                    agregarConversacionFinal(conversations, totalChats, chatId, otherUid, name,
                            username, avatarUrl, lastMessage, unreadCount);
                })
                .addOnFailureListener(e -> agregarConversacionFinal(conversations, totalChats,
                        chatId, otherUid, name, username, avatarUrl, lastMessage, unread ? 1 : 0));
    }

    private void agregarConversacionFinal(List<ChatConversation> conversations, int totalChats,
                                          String chatId, String otherUid, String name,
                                          String username, @Nullable String avatarUrl,
                                          @Nullable String lastMessage, int unreadCount) {
        conversations.add(new ChatConversation(
                chatId,
                otherUid,
                name,
                username,
                avatarUrl,
                lastMessage != null && !lastMessage.isEmpty()
                        ? lastMessage
                        : "Sin mensajes todavia",
                unreadCount > 0,
                unreadCount
        ));

        if (conversations.size() == totalChats) {
            chatAdapter.setConversations(conversations);
            tvEmpty.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void abrirConversacion(ChatConversation conversation) {
        selectedChatId = conversation.chatId != null
                ? conversation.chatId
                : getChatId(currentUid, conversation.userId);
        selectedUserId = conversation.userId;
        selectedName = conversation.name;
        selectedUsername = conversation.username;
        actualizarEncabezadoChat();
        layoutChatList.setVisibility(View.GONE);
        layoutChatThread.setVisibility(View.VISIBLE);
        cargarUsuarioSeleccionado();
        marcarConversacionComoLeida();
        escucharMensajes();
    }

    private void marcarConversacionComoLeida() {
        Timestamp readAt = Timestamp.now();
        Map<String, Object> readMap = new HashMap<>();
        readMap.put(currentUid, readAt);
        Map<String, Object> data = new HashMap<>();
        data.put("lastRead", readMap);

        // aquí se toca el resumen del chat y también cada mensaje pendiente para que la vista previa y el hilo digan lo mismo
        db.collection("chats")
                .document(selectedChatId)
                .set(data, com.google.firebase.firestore.SetOptions.merge());

        db.collection("chats")
                .document(selectedChatId)
                .collection("messages")
                .whereEqualTo("to", currentUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        escucharConversaciones();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.update(doc.getReference(), "readBy." + currentUid, true);
                    }
                    batch.commit().addOnCompleteListener(task -> escucharConversaciones());
                })
                .addOnFailureListener(e -> escucharConversaciones());
    }

    private void cargarUsuarioSeleccionado() {
        db.collection("users").document(selectedUserId).get()
                .addOnSuccessListener(doc -> {
                    String realName = doc.getString("name");
                    String realUsername = doc.getString("username");

                    if (realName != null && !realName.isEmpty()) {
                        selectedName = realName;
                    }
                    if (realUsername != null && !realUsername.isEmpty()) {
                        selectedUsername = realUsername;
                    }
                    actualizarEncabezadoChat();
                });
    }

    private void actualizarEncabezadoChat() {
        tvChatTarget.setText(selectedName != null && !selectedName.isEmpty()
                ? selectedName
                : "Usuario");
        tvChatTargetUsername.setText(selectedUsername != null && !selectedUsername.isEmpty()
                ? selectedUsername
                : "@usuario");
    }

    private void escucharMensajes() {
        if (messagesListener != null) messagesListener.remove();

        messagesListener = db.collection("chats")
                .document(selectedChatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;
                    if (error != null || snapshot == null) return;

                    List<ChatMessage> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        messages.add(new ChatMessage(
                                doc.getString("from"),
                                doc.getString("text") != null ? doc.getString("text") : "",
                                doc.getTimestamp("createdAt") != null
                                        ? doc.getTimestamp("createdAt")
                                        : Timestamp.now()
                        ));
                    }
                    messageAdapter.setMessages(messages);
                    if (!messages.isEmpty()) {
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void mostrarLista() {
        layoutChatThread.setVisibility(View.GONE);
        layoutChatList.setVisibility(View.VISIBLE);
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    private void enviarMensaje() {
        if (selectedUserId == null || selectedChatId == null) return;

        String text = etChatMessage.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(getContext(), "Escribe un mensaje", Toast.LENGTH_SHORT).show();
            return;
        }

        cargarPerfilActualYEnviar(text);
    }

    private void cargarPerfilActualYEnviar(String text) {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(currentUser -> enviarMensajeConPerfil(text, currentUser))
                .addOnFailureListener(e -> enviarMensajeConPerfil(text, null));
    }

    private void enviarMensajeConPerfil(String text, @Nullable DocumentSnapshot currentUser) {
        Map<String, Object> chatData = new HashMap<>();
        // se refrescan estos metadatos en cada envío para que la lista de chats no arrastre nombres o avatares viejos
        chatData.put("participants", Arrays.asList(currentUid, selectedUserId));
        chatData.put("lastMessage", text);
        chatData.put("lastMessageFrom", currentUid);
        chatData.put("updatedAt", Timestamp.now());
        chatData.put("participantNames." + currentUid, getStringOrDefault(currentUser, "name", "Yo"));
        chatData.put("participantUsernames." + currentUid, getStringOrDefault(currentUser, "username", ""));
        chatData.put("participantAvatars." + currentUid, getStringOrDefault(currentUser, "steamAvatarUrl", ""));
        chatData.put("participantNames." + selectedUserId, selectedName);
        chatData.put("participantUsernames." + selectedUserId, selectedUsername);

        Map<String, Object> message = new HashMap<>();
        message.put("from", currentUid);
        message.put("to", selectedUserId);
        message.put("text", text);
        message.put("createdAt", Timestamp.now());

        db.collection("chats")
                .document(selectedChatId)
                .set(chatData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> db.collection("chats")
                        .document(selectedChatId)
                        .collection("messages")
                        .add(message)
                        .addOnSuccessListener(doc -> {
                            crearNotificacionMensaje(text);
                            etChatMessage.setText("");
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(),
                                "No se pudo enviar el mensaje", Toast.LENGTH_SHORT).show()));
    }

    private String getStringOrDefault(@Nullable DocumentSnapshot doc, String field, String fallback) {
        if (doc == null) return fallback;
        String value = doc.getString(field);
        return value != null ? value : fallback;
    }

    private void crearNotificacionMensaje(String text) {
        Map<String, Object> notification = new HashMap<>();
        // este aviso es el pequeño empujón que hace que el otro usuario vea movimiento aunque no tenga abierto el chat
        notification.put("type", "message");
        notification.put("fromUid", currentUid);
        notification.put("title", "Nuevo mensaje");
        notification.put("body", text);
        notification.put("createdAt", Timestamp.now());
        notification.put("read", false);

        db.collection("users")
                .document(selectedUserId)
                .collection("notifications")
                .add(notification);
    }

    private String getChatId(String uidA, String uidB) {
        return uidA.compareTo(uidB) < 0 ? uidA + "_" + uidB : uidB + "_" + uidA;
    }
}

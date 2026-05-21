package com.example.vaulted;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private NotificationAdapter adapter;
    private TextView tvEmpty;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public NotificationsFragment() {
        super(R.layout.fragment_notifications);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        tvEmpty = view.findViewById(R.id.tvNotificationsEmpty);

        RecyclerView rv = view.findViewById(R.id.rvNotifications);
        adapter = new NotificationAdapter();
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        cargarNotificaciones();
    }

    private void cargarNotificaciones() {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;
                    if (error != null || snapshot == null) {
                        tvEmpty.setText("No se pudieron cargar notificaciones");
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    // se escuchan en tiempo real para que el panel se sienta vivo y no dependa de refrescos manuales
                    List<NotificationItem> items = new ArrayList<>();
                    snapshot.forEach(doc -> items.add(new NotificationItem(
                            doc.getString("type"),
                            doc.getString("title") != null ? doc.getString("title") : "Notificacion",
                            doc.getString("body") != null ? doc.getString("body") : "",
                            doc.getTimestamp("createdAt") != null
                                    ? doc.getTimestamp("createdAt")
                                    : Timestamp.now()
                    )));

                    adapter.setItems(items);
                    tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }
}

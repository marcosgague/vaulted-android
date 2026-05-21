package com.example.vaulted;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchFragment extends Fragment {

    private ChatAdapter peopleAdapter;
    private TextView tvEmpty, tvPeopleHeader;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<ChatConversation> allPeople = new ArrayList<>();

    public SearchFragment() {
        super(R.layout.fragment_search);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        tvEmpty = view.findViewById(R.id.tvSearchEmpty);
        tvPeopleHeader = view.findViewById(R.id.tvPeopleHeader);

        RecyclerView rvPeople = view.findViewById(R.id.rvPeopleSearch);
        peopleAdapter = new ChatAdapter(this::abrirPerfil);
        rvPeople.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPeople.setAdapter(peopleAdapter);

        configurarBuscador(view);
        cargarUsuarios();
    }

    private void configurarBuscador(View view) {
        EditText searchInput = view.findViewById(R.id.searchSocial);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrar(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void cargarUsuarios() {
        if (mAuth.getCurrentUser() == null) return;

        String currentUid = mAuth.getCurrentUser().getUid();
        db.collection("users").get()
                .addOnSuccessListener(snapshot -> {
                    allPeople.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (doc.getId().equals(currentUid)) continue;

                        String name = doc.getString("name");
                        String username = doc.getString("username");
                        String avatarUrl = doc.getString("steamAvatarUrl");
                        asegurarUsernameLower(doc.getId(), username);

                        allPeople.add(new ChatConversation(
                                doc.getId(),
                                name != null && !name.isEmpty() ? name : "Jugador",
                                username != null && !username.isEmpty() ? username : "@usuario",
                                avatarUrl,
                                "Toca para ver perfil"
                        ));
                    }

                    peopleAdapter.setConversations(allPeople);
                    tvEmpty.setVisibility(allPeople.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    tvEmpty.setText("No se pudieron cargar usuarios");
                    tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void filtrar(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        if (mAuth.getCurrentUser() == null) return;

        // si el texto empieza por @ tiene más sentido buscar directamente sobre el nombre normalizado de firestore
        if (query.startsWith("@")) {
            tvPeopleHeader.setVisibility(View.VISIBLE);
            buscarUsuariosPorUsername(query);
        } else {
            List<ChatConversation> filteredPeople = filtrarPersonasLocalmente(query);
            peopleAdapter.setConversations(query.isEmpty() ? allPeople : filteredPeople);
            tvPeopleHeader.setVisibility(View.VISIBLE);
            boolean empty = query.isEmpty() ? allPeople.isEmpty() : filteredPeople.isEmpty();
            tvEmpty.setText(query.isEmpty()
                    ? "Empieza escribiendo un @usuario o nombre"
                    : "No se encontraron usuarios");
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    private List<ChatConversation> filtrarPersonasLocalmente(String query) {
        List<ChatConversation> filtered = new ArrayList<>();
        if (query == null || query.isEmpty()) return filtered;

        for (ChatConversation person : allPeople) {
            String username = person.username != null ? person.username.toLowerCase(Locale.ROOT) : "";
            String name = person.name != null ? person.name.toLowerCase(Locale.ROOT) : "";
            if (username.contains(query) || name.contains(query)) {
                filtered.add(person);
            }
        }
        return filtered;
    }

    private void asegurarUsernameLower(String userId, String username) {
        if (username == null || username.isEmpty()) return;

        Map<String, Object> data = new HashMap<>();
        data.put("usernameLower", username.toLowerCase(Locale.ROOT));
        // esto corrige usuarios antiguos en segundo plano para que entren también en el buscador actual
        db.collection("users").document(userId)
                .set(data, com.google.firebase.firestore.SetOptions.merge());
    }

    private void buscarUsuariosPorUsername(String query) {
        db.collection("users")
                .whereGreaterThanOrEqualTo("usernameLower", query)
                .whereLessThanOrEqualTo("usernameLower", query + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ChatConversation> filtered = new ArrayList<>();
                    String currentUid = mAuth.getCurrentUser() != null
                            ? mAuth.getCurrentUser().getUid()
                            : "";
                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (doc.getId().equals(currentUid)) continue;

                        String name = doc.getString("name");
                        String username = doc.getString("username");
                        String avatarUrl = doc.getString("steamAvatarUrl");

                        filtered.add(new ChatConversation(
                                doc.getId(),
                                name != null && !name.isEmpty() ? name : "Jugador",
                                username != null && !username.isEmpty() ? username : "@usuario",
                                avatarUrl,
                                "Toca para ver perfil"
                        ));
                    }

                    // si firestore no devuelve nada, aún queda esta red de seguridad con la lista ya cargada en memoria
                    if (filtered.isEmpty()) {
                        for (ChatConversation person : allPeople) {
                            if (person.username.toLowerCase(Locale.ROOT).contains(query)) {
                                filtered.add(person);
                            }
                        }
                    }

                    peopleAdapter.setConversations(filtered);
                    tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    peopleAdapter.setConversations(new ArrayList<>());
                    tvEmpty.setText("No se pudieron buscar usuarios");
                    tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void abrirPerfil(ChatConversation conversation) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).abrirPerfilUsuario(conversation);
        }
    }
}

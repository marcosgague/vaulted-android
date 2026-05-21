package com.example.vaulted;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LibraryFragment extends Fragment {

    private boolean horasDesc = true;
    private SortMode sortMode = SortMode.RECENT;

    private GameAdapter adapter;
    private GameRepository repo;
    private String uid;

    public LibraryFragment() {
        super(R.layout.fragment_library);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        repo = new GameRepository(requireContext());

        RecyclerView rv = view.findViewById(R.id.rvLibraryGames);
        adapter = new GameAdapter(this::abrirDetalle);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        repo.getGamesByUser(uid).observe(getViewLifecycleOwner(), games -> {
            adapter.setFullList(games);
            aplicarOrdenActual();
        });

        EditText searchView = view.findViewById(R.id.searchView);
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // primero se filtra y luego se reaplica el orden para que la lista no cambie de criterio mientras escribes
                adapter.filter(s != null ? s.toString() : "");
                aplicarOrdenActual();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        Chip chipHoras = view.findViewById(R.id.chipHoras);
        Chip chipNombre = view.findViewById(R.id.chipNombre);
        Chip chipRecientes = view.findViewById(R.id.chipRecientes);

        chipHoras.setOnClickListener(v -> {
            horasDesc = !horasDesc;
            sortMode = SortMode.HOURS;
            // este mismo chip hace dos cosas: activa el criterio por horas y además invierte el sentido del orden
            chipHoras.setText(horasDesc ? "Mas jugados" : "Menos jugados");
            aplicarOrdenActual();
        });

        chipNombre.setOnClickListener(v -> {
            sortMode = SortMode.NAME;
            aplicarOrdenActual();
        });
        chipRecientes.setOnClickListener(v -> {
            sortMode = SortMode.RECENT;
            aplicarOrdenActual();
        });

        view.findViewById(R.id.btnSync).setOnClickListener(v -> sincronizar());
    }

    private void sincronizar() {
        FirebaseFirestore.getInstance()
                .collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String steamId = doc.getString("steamId");
                    if (steamId == null || steamId.isEmpty()) {
                        Toast.makeText(getContext(), "No tienes Steam vinculado", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (BuildConfig.STEAM_API_KEY.isEmpty()) {
                        Toast.makeText(getContext(), "Falta configurar la clave de Steam", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repo.syncSteamGames(steamId, BuildConfig.STEAM_API_KEY, uid);
                    Toast.makeText(getContext(), "Sincronizando...", Toast.LENGTH_SHORT).show();
                });
    }

    private void abrirDetalle(GameEntity game) {
        NavHostFragment.findNavController(this)
                .navigate(R.id.gameDetailFragment, GameDetailFragment.createArguments(game));
    }

    private void aplicarOrdenActual() {
        // el orden se centraliza aquí para que búsqueda, chips y recargas de datos no compitan entre sí
        if (sortMode == SortMode.HOURS) {
            adapter.sortByHours(horasDesc);
        } else if (sortMode == SortMode.NAME) {
            adapter.sortByName();
        } else {
            adapter.sortByLastPlayed();
        }
    }

    private enum SortMode {
        HOURS,
        NAME,
        RECENT
    }
}

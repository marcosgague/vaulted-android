package com.example.vaulted;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private TextView           tvWelcome;
    private TextView           tvTotalHours;
    private TextView           tvTotalGames;
    private TextView           tvNoRecommendations;
    private RecyclerView       rvRecentGames;
    private RecyclerView       rvRecommendations;
    private GameAdapter        adapter;
    private RecommendationAdapter recommendationsAdapter;
    private List<GameEntity>   ownGames = new ArrayList<>();
    private SteamAppSearchRepository steamAppSearchRepository;

    public DashboardFragment() {
        super(R.layout.fragment_dashboard);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvWelcome     = view.findViewById(R.id.tvWelcome);
        tvTotalHours  = view.findViewById(R.id.tvTotalHours);
        tvTotalGames  = view.findViewById(R.id.tvTotalGames);
        tvNoRecommendations = view.findViewById(R.id.tvNoRecommendations);
        rvRecentGames = view.findViewById(R.id.rvRecentGames);
        rvRecommendations = view.findViewById(R.id.rvRecommendations);
        cargarSaludo();
        steamAppSearchRepository = new SteamAppSearchRepository();

        adapter = new GameAdapter(game -> NavHostFragment.findNavController(this)
                .navigate(R.id.gameDetailFragment, GameDetailFragment.createArguments(game)));
        rvRecentGames.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentGames.setAdapter(adapter);

        recommendationsAdapter = new RecommendationAdapter(recommendation ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.gameDetailFragment,
                                GameDetailFragment.createRecommendationArguments(
                                        recommendation.game,
                                        recommendation.reason)));
        rvRecommendations.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecommendations.setAdapter(recommendationsAdapter);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        viewModel.totalHours.observe(getViewLifecycleOwner(), hours -> {
            tvTotalHours.setText(hours != null ? hours + " horas totales" : "0 horas totales");
        });

        viewModel.totalGames.observe(getViewLifecycleOwner(), count -> {
            tvTotalGames.setText(count != null ? count + " juegos" : "0 juegos");
        });
        viewModel.recentGames.observe(getViewLifecycleOwner(), games -> {
            if (games != null) {
                ownGames = games;
                adapter.setFullList(games);
                cargarRecomendaciones();
            }
        });
    }

    private void cargarSaludo() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    tvWelcome.setText("Bienvenido de nuevo, "
                            + (name != null && !name.isEmpty() ? name : "jugador"));
                });
    }

    private void cargarRecomendaciones() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Set<String> ownedIds = new HashSet<>();
        for (GameEntity game : ownGames) {
            ownedIds.add(game.externalId);
        }

        FirebaseFirestore.getInstance().collection("users").get()
                .addOnSuccessListener(snapshot -> {
                    // primero se juntan los favoritos de otros usuarios y se filtran los juegos que el usuario ya tiene
                    Map<String, GameEntity> recommendations = new HashMap<>();
                    Map<String, Integer> counts = new HashMap<>();
                    List<String> unresolvedIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (doc.getId().equals(currentUid)) continue;

                        List<Map<String, Object>> favoriteGames = (List<Map<String, Object>>) doc.get("favoriteGames");
                        if (favoriteGames != null && !favoriteGames.isEmpty()) {
                            addFavoriteGameRecommendations(favoriteGames, recommendations, counts, ownedIds);
                        } else {
                            List<String> favoriteIds = (List<String>) doc.get("favoriteGameIds");
                            addUnresolvedFavoriteIds(favoriteIds, unresolvedIds, recommendations, counts, ownedIds);
                        }
                    }

                    if (unresolvedIds.isEmpty()) {
                        pintarRecomendaciones(recommendations, counts);
                    } else {
                        steamAppSearchRepository.getAppsByIds(unresolvedIds, games -> {
                            for (GameEntity game : games) {
                                if (!recommendations.containsKey(game.externalId)) {
                                    recommendations.put(game.externalId, game);
                                }
                            }
                            pintarRecomendaciones(recommendations, counts);
                        });
                    }
                })
                .addOnFailureListener(e -> tvNoRecommendations.setVisibility(View.VISIBLE));
    }

    private void pintarRecomendaciones(Map<String, GameEntity> recommendations, Map<String, Integer> counts) {
        if (!isAdded()) return;
        List<GameRecommendation> items = new ArrayList<>();
        for (GameEntity game : recommendations.values()) {
            normalizarTituloRecomendado(game);
            int count = counts.containsKey(game.externalId) ? counts.get(game.externalId) : 1;
            String reason = count > 1
                    ? "Varios usuarios con gustos parecidos lo tienen entre sus favoritos."
                    : "Un usuario compatible contigo lo tiene entre sus favoritos.";
            items.add(new GameRecommendation(game, reason));
        }
        // se da prioridad a los juegos que aparecen repetidos en varios perfiles para evitar malas sugerncias
        items.sort((a, b) -> Integer.compare(
                counts.containsKey(b.game.externalId) ? counts.get(b.game.externalId) : 1,
                counts.containsKey(a.game.externalId) ? counts.get(a.game.externalId) : 1
        ));
        if (items.size() > 5) {
            items = items.subList(0, 5);
        }
        recommendationsAdapter.setRecommendations(items);
        tvNoRecommendations.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addUnresolvedFavoriteIds(List<String> favoriteIds,
                                          List<String> unresolvedIds,
                                          Map<String, GameEntity> recommendations,
                                          Map<String, Integer> counts,
                                          Set<String> ownedIds) {
        if (favoriteIds == null) return;

        // si solo existe el id, se deja pendiente para resolverlo después contra el catálogo global de steam
        for (String favoriteId : favoriteIds) {
            if (favoriteId == null || ownedIds.contains(favoriteId)) {
                continue;
            }
            counts.put(favoriteId, counts.containsKey(favoriteId) ? counts.get(favoriteId) + 1 : 1);
            if (!unresolvedIds.contains(favoriteId)) {
                unresolvedIds.add(favoriteId);
            }
        }
    }

    private void addFavoriteGameRecommendations(List<Map<String, Object>> favoriteGames,
                                                Map<String, GameEntity> recommendations,
                                                Map<String, Integer> counts,
                                                Set<String> ownedIds) {
        for (Map<String, Object> favorite : favoriteGames) {
            String gameId = valorMapa(favorite, "gameId");
            if (gameId.isEmpty() || ownedIds.contains(gameId)) {
                continue;
            }
            counts.put(gameId, counts.containsKey(gameId) ? counts.get(gameId) + 1 : 1);
            if (recommendations.containsKey(gameId)) {
                continue;
            }

            recommendations.put(gameId, new GameEntity(
                    gameId,
                    valorOPlaceholder(valorMapa(favorite, "title"), "Juego recomendado"),
                    valorOPlaceholder(valorMapa(favorite, "platform"), "steam"),
                    valorMapa(favorite, "coverUrl"),
                    0,
                    "",
                    0,
                    0,
                    valorMapa(favorite, "headerUrl")
            ));
        }
    }

    private String valorMapa(Map<String, Object> item, String key) {
        Object value = item.get(key);
        return value != null ? value.toString() : "";
    }

    private String valorOPlaceholder(String value, String placeholder) {
        return value != null && !value.isEmpty() ? value : placeholder;
    }

    private boolean tieneNombreReal(GameEntity game) {
        return game.title != null
                && !game.title.trim().isEmpty()
                && !"Juego recomendado".equalsIgnoreCase(game.title.trim())
                && !game.title.trim().toLowerCase(java.util.Locale.ROOT).startsWith("juego de steam #");
    }

    private void normalizarTituloRecomendado(GameEntity game) {
        if (!tieneNombreReal(game)) {
            game.title = "Juego recomendado";
        }
    }
}


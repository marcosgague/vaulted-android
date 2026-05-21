package com.example.vaulted;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserProfileFragment extends Fragment {

    public static final String ARG_USER_ID = "userId";
    public static final String ARG_NAME = "name";
    public static final String ARG_USERNAME = "username";
    public static final String ARG_AVATAR_URL = "avatarUrl";

    private String userId;
    private String name;
    private String username;
    private GameRepository gameRepository;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GameAdapter topGamesAdapter;
    private GameAdapter favoriteGamesAdapter;
    private AchievementAdapter featuredAchievementsAdapter;
    private ReviewAdapter reviewAdapter;
    private TextView tvNoGames, tvNoFavorites, tvPublicProfileName, tvPublicProfileUsername;
    private TextView tvNoFeaturedAchievements, tvNoPublicReviews;
    private TextView tvPublicCpu, tvPublicGpu, tvPublicRam;
    private TextView tvCompatibilityScore, tvCompatibilitySummary, tvCompatibilityDetails, tvCommonGames;
    private ImageView ivPublicProfilePhoto;
    private Button btnFollowUser;
    private boolean following;
    private List<GameEntity> currentUserGames = new ArrayList<>();
    private List<GameEntity> targetUserGames = new ArrayList<>();
    private List<String> currentFavoriteIds = new ArrayList<>();
    private List<String> targetFavoriteIds = new ArrayList<>();
    private boolean currentGamesLoaded, targetGamesLoaded, currentFavoritesLoaded, targetFavoritesLoaded;

    public UserProfileFragment() {
        super(R.layout.fragment_user_profile);
    }

    public static Bundle createArguments(ChatConversation conversation) {
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, conversation.userId);
        args.putString(ARG_NAME, conversation.name);
        args.putString(ARG_USERNAME, conversation.username);
        args.putString(ARG_AVATAR_URL, conversation.avatarUrl);
        return args;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        userId = args.getString(ARG_USER_ID);
        name = args.getString(ARG_NAME, "Jugador");
        username = args.getString(ARG_USERNAME, "@usuario");
        String avatarUrl = args.getString(ARG_AVATAR_URL);
        gameRepository = new GameRepository(requireContext());
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvPublicProfileName = view.findViewById(R.id.tvPublicProfileName);
        tvPublicProfileUsername = view.findViewById(R.id.tvPublicProfileUsername);
        tvPublicCpu = view.findViewById(R.id.tvPublicCpu);
        tvPublicGpu = view.findViewById(R.id.tvPublicGpu);
        tvPublicRam = view.findViewById(R.id.tvPublicRam);
        tvCompatibilityScore = view.findViewById(R.id.tvCompatibilityScore);
        tvCompatibilitySummary = view.findViewById(R.id.tvCompatibilitySummary);
        tvCompatibilityDetails = view.findViewById(R.id.tvCompatibilityDetails);
        tvCommonGames = view.findViewById(R.id.tvCommonGames);
        ivPublicProfilePhoto = view.findViewById(R.id.ivPublicProfilePhoto);
        tvPublicProfileName.setText(name);
        tvPublicProfileUsername.setText(username);
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this).load(avatarUrl).circleCrop().into(ivPublicProfilePhoto);
        }

        RecyclerView rvTop = view.findViewById(R.id.rvPublicTopGames);
        RecyclerView rvFavorites = view.findViewById(R.id.rvPublicFavoriteGames);
        RecyclerView rvFeaturedAchievements = view.findViewById(R.id.rvFeaturedAchievements);
        RecyclerView rvReviews = view.findViewById(R.id.rvPublicReviews);
        tvNoGames = view.findViewById(R.id.tvNoPublicGames);
        tvNoFavorites = view.findViewById(R.id.tvNoPublicFavorites);
        tvNoFeaturedAchievements = view.findViewById(R.id.tvNoFeaturedAchievements);
        tvNoPublicReviews = view.findViewById(R.id.tvNoPublicReviews);

        topGamesAdapter = new GameAdapter(this::openGame);
        favoriteGamesAdapter = new GameAdapter(this::openGame);
        featuredAchievementsAdapter = new AchievementAdapter();
        reviewAdapter = new ReviewAdapter();
        rvTop.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTop.setAdapter(topGamesAdapter);
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFavorites.setAdapter(favoriteGamesAdapter);
        rvFeaturedAchievements.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFeaturedAchievements.setAdapter(featuredAchievementsAdapter);
        rvReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        rvReviews.setAdapter(reviewAdapter);

        view.findViewById(R.id.btnMessageUser).setOnClickListener(v -> abrirChat());
        btnFollowUser = view.findViewById(R.id.btnFollowUser);
        btnFollowUser.setOnClickListener(v -> seguirUsuario());
        cargarDatosPerfil();
        cargarEstadoSeguimiento();
        cargarJuegos();
        cargarFavoritos();
        cargarCompatibilidad();
        cargarReviews();
    }

    private void cargarEstadoSeguimiento() {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    List<String> followingIds = (List<String>) doc.get("followingIds");
                    if (followingIds != null && followingIds.contains(userId)) {
                        marcarComoSiguiendo();
                    } else {
                        marcarComoNoSiguiendo();
                    }
                });
    }

    private void cargarDatosPerfil() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    String realName = doc.getString("name");
                    String realUsername = doc.getString("username");
                    String avatarUrl = doc.getString("steamAvatarUrl");
                    String cpu = doc.getString("cpu");
                    String gpu = doc.getString("gpu");
                    String ramSize = doc.getString("ramSize");
                    String ramType = doc.getString("ramType");
                    List<Map<String, Object>> featured = (List<Map<String, Object>>) doc.get("featuredAchievements");

                    if (realName != null && !realName.isEmpty()) {
                        name = realName;
                    }
                    if (realUsername != null && !realUsername.isEmpty()) {
                        username = realUsername;
                    }

                    tvPublicProfileName.setText(name);
                    tvPublicProfileUsername.setText(username);
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this).load(avatarUrl).circleCrop().into(ivPublicProfilePhoto);
                    }
                    tvPublicCpu.setText(valorOPlaceholder(cpu));
                    tvPublicGpu.setText(valorOPlaceholder(gpu));
                    tvPublicRam.setText(valorOPlaceholder(ramSize) + " " + valorOPlaceholder(ramType));
                    mostrarLogrosDestacados(featured);
                });
    }

    private void mostrarLogrosDestacados(List<Map<String, Object>> featured) {
        List<AchievementEntity> achievements = new ArrayList<>();
        if (featured != null) {
            // firestore guarda estos logros como mapas simples, así que aquí se reconstruyen para reutilizar el adaptador normal
            for (Map<String, Object> item : featured) {
                AchievementEntity achievement = new AchievementEntity();
                achievement.gameExternalId = valorMapa(item, "gameId");
                achievement.name = valorMapa(item, "name");
                String gameTitle = valorMapa(item, "gameTitle");
                String description = valorMapa(item, "description");
                achievement.description = gameTitle.isEmpty()
                        ? description
                        : gameTitle + (description.isEmpty() ? "" : " - " + description);
                achievement.iconUrl = valorMapa(item, "iconUrl");
                Object unlocked = item.get("unlocked");
                achievement.unlocked = unlocked instanceof Boolean && (Boolean) unlocked;
                achievements.add(achievement);
            }
        }

        featuredAchievementsAdapter.setList(achievements);
        tvNoFeaturedAchievements.setVisibility(achievements.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String valorMapa(Map<String, Object> item, String key) {
        Object value = item.get(key);
        return value != null ? value.toString() : "";
    }

    private String valorOPlaceholder(String value) {
        return value != null && !value.isEmpty() ? value : "Sin especificar";
    }

    private void cargarJuegos() {
        gameRepository.getTopGamesByUser(userId, 5).observe(getViewLifecycleOwner(), games -> {
            topGamesAdapter.setFullList(games);
            tvNoGames.setVisibility(games == null || games.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void cargarFavoritos() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    List<String> favoriteIds = (List<String>) doc.get("favoriteGameIds");
                    gameRepository.getGamesByUser(userId).observe(getViewLifecycleOwner(), games -> {
                        List<GameEntity> favorites = new ArrayList<>();
                        if (favoriteIds != null && games != null) {
                            for (GameEntity game : games) {
                                if (favoriteIds.contains(game.externalId)) {
                                    favorites.add(game);
                                }
                            }
                        }
                        favoriteGamesAdapter.setFullList(favorites);
                        tvNoFavorites.setVisibility(favorites.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                });
    }

    private void cargarReviews() {
        db.collection("users")
                .document(userId)
                .collection("reviews")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<GameReview> reviews = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        GameReview review = doc.toObject(GameReview.class);
                        reviews.add(review);
                    }
                    reviewAdapter.setReviews(reviews);
                    tvNoPublicReviews.setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> tvNoPublicReviews.setVisibility(View.VISIBLE));
    }

    private void cargarCompatibilidad() {
        if (mAuth.getCurrentUser() == null) {
            mostrarCompatibilidadSinDatos();
            return;
        }

        String currentUid = mAuth.getCurrentUser().getUid();

        gameRepository.getGamesByUser(currentUid).observe(getViewLifecycleOwner(), games -> {
            currentUserGames = games != null ? games : new ArrayList<>();
            currentGamesLoaded = true;
            actualizarCompatibilidad();
        });

        gameRepository.getGamesByUser(userId).observe(getViewLifecycleOwner(), games -> {
            targetUserGames = games != null ? games : new ArrayList<>();
            targetGamesLoaded = true;
            actualizarCompatibilidad();
        });

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    currentFavoriteIds = leerFavoritos(doc);
                    currentFavoritesLoaded = true;
                    actualizarCompatibilidad();
                })
                .addOnFailureListener(e -> {
                    currentFavoritesLoaded = true;
                    actualizarCompatibilidad();
                });

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    targetFavoriteIds = leerFavoritos(doc);
                    targetFavoritesLoaded = true;
                    actualizarCompatibilidad();
                })
                .addOnFailureListener(e -> {
                    targetFavoritesLoaded = true;
                    actualizarCompatibilidad();
                });
    }

    private List<String> leerFavoritos(DocumentSnapshot doc) {
        List<String> favorites = (List<String>) doc.get("favoriteGameIds");
        return favorites != null ? new ArrayList<>(favorites) : new ArrayList<>();
    }

    private void actualizarCompatibilidad() {
        if (!currentGamesLoaded || !targetGamesLoaded || !currentFavoritesLoaded || !targetFavoritesLoaded) {
            return;
        }

        if (currentUserGames.isEmpty() || targetUserGames.isEmpty()) {
            mostrarCompatibilidadSinDatos();
            return;
        }

        Map<String, GameEntity> currentById = new HashMap<>();
        for (GameEntity game : currentUserGames) {
            currentById.put(game.externalId, game);
        }

        // aquí se cruzan ambas bibliotecas para localizar juegos comunes y medir hasta qué punto se parecen los hábitos de juego
        List<GameEntity> commonGames = new ArrayList<>();
        int sharedHours = 0;
        int maxSharedHours = 0;
        for (GameEntity targetGame : targetUserGames) {
            GameEntity currentGame = currentById.get(targetGame.externalId);
            if (currentGame != null) {
                commonGames.add(targetGame);
                sharedHours += Math.min(currentGame.hoursPlayed, targetGame.hoursPlayed);
                maxSharedHours += Math.max(currentGame.hoursPlayed, targetGame.hoursPlayed);
            }
        }

        Set<String> targetFavorites = new HashSet<>(targetFavoriteIds);
        int favoriteMatches = 0;
        for (String favoriteId : currentFavoriteIds) {
            if (targetFavorites.contains(favoriteId)) {
                favoriteMatches++;
            }
        }

        // este cálculo tiene bastante miga: mezcla juegos en común, favoritos compartidos y proporción de horas para que el porcentaje no salga inflado por casualidad
        int libraryBase = Math.max(1, Math.min(currentUserGames.size(), targetUserGames.size()));
        float sharedRatio = Math.min(1f, commonGames.size() / (float) Math.min(20, libraryBase));
        int gamesScore = Math.round(sharedRatio * 70f);
        int favoriteScore = favoriteMatches > 0
                ? Math.min(25, 15 + (favoriteMatches * 5))
                : 0;
        int hoursScore = maxSharedHours > 0 ? Math.min(15, Math.round((sharedHours * 15f) / maxSharedHours)) : 0;
        int totalScore = Math.min(100, gamesScore + favoriteScore + hoursScore);
        if (commonGames.size() >= 10 && favoriteMatches > 0) {
            totalScore = Math.max(totalScore, 86);
        } else if (commonGames.size() >= 15) {
            totalScore = Math.max(totalScore, 82);
        }

        tvCompatibilityScore.setText(totalScore + "%");
        tvCompatibilitySummary.setText(resumenCompatibilidad(totalScore));
        tvCompatibilityDetails.setText(commonGames.size() + " juegos en comun · "
                + favoriteMatches + " favoritos en comun");
        tvCommonGames.setText(textoJuegosEnComun(commonGames));
    }

    private String resumenCompatibilidad(int score) {
        if (score >= 75) return "Muy compatible";
        if (score >= 45) return "Buena compatibilidad";
        if (score >= 20) return "Compatibilidad moderada";
        return "Pocos gustos en comun";
    }

    private String textoJuegosEnComun(List<GameEntity> commonGames) {
        if (commonGames.isEmpty()) {
            return "Sin juegos en comun todavia";
        }

        commonGames.sort((a, b) -> Integer.compare(b.hoursPlayed, a.hoursPlayed));
        List<String> titles = new ArrayList<>();
        int limit = Math.min(3, commonGames.size());
        for (int i = 0; i < limit; i++) {
            String title = commonGames.get(i).title;
            if (title != null && !title.isEmpty()) {
                titles.add(title);
            }
        }
        return titles.isEmpty() ? "Juegos compartidos disponibles" : "En comun: " + String.join(", ", titles);
    }

    private void mostrarCompatibilidadSinDatos() {
        tvCompatibilityScore.setText("--%");
        tvCompatibilitySummary.setText("Sin datos suficientes");
        tvCompatibilityDetails.setText("Ambos usuarios deben tener juegos sincronizados");
        tvCommonGames.setText("");
    }

    private void openGame(GameEntity game) {
        NavHostFragment.findNavController(this)
                .navigate(R.id.gameDetailFragment, GameDetailFragment.createArguments(game));
    }

    private void abrirChat() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).abrirChatConUsuario(userId, name, username);
        }
    }

    private void seguirUsuario() {
        if (mAuth.getCurrentUser() == null || userId == null) return;

        String currentUid = mAuth.getCurrentUser().getUid();
        if (currentUid.equals(userId)) return;

        if (following) {
            dejarDeSeguir(currentUid);
            return;
        }

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(currentUser -> {
                    List<String> followingIds = (List<String>) currentUser.get("followingIds");
                    if (followingIds != null && followingIds.contains(userId)) {
                        Toast.makeText(getContext(), "Ya sigues a " + username, Toast.LENGTH_SHORT).show();
                        marcarComoSiguiendo();
                        return;
                    }

                    btnFollowUser.setEnabled(false);
                    guardarSeguimiento(currentUid);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "No se pudo seguir al usuario", Toast.LENGTH_SHORT).show());
    }

    private void guardarSeguimiento(String currentUid) {
        Map<String, Object> currentUserData = new HashMap<>();
        currentUserData.put("following", FieldValue.increment(1));
        currentUserData.put("followingIds", FieldValue.arrayUnion(userId));

        Map<String, Object> targetUserData = new HashMap<>();
        targetUserData.put("followers", FieldValue.increment(1));
        targetUserData.put("followerIds", FieldValue.arrayUnion(currentUid));

        db.collection("users").document(currentUid)
                .set(currentUserData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> db.collection("users").document(userId)
                        .set(targetUserData, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener(done -> {
                            crearNotificacionSeguimiento(currentUid);
                            marcarComoSiguiendo();
                            Toast.makeText(getContext(),
                                    "Ahora sigues a " + username, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            btnFollowUser.setEnabled(true);
                            Toast.makeText(getContext(),
                                    "No se pudo seguir al usuario", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    btnFollowUser.setEnabled(true);
                    Toast.makeText(getContext(),
                            "No se pudo seguir al usuario", Toast.LENGTH_SHORT).show();
                });
    }

    private void crearNotificacionSeguimiento(String currentUid) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "follow");
        notification.put("fromUid", currentUid);
        notification.put("title", "Nuevo seguidor");
        notification.put("body", username + " tiene un nuevo seguidor");
        notification.put("createdAt", Timestamp.now());
        notification.put("read", false);

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .add(notification);
    }

    private void marcarComoSiguiendo() {
        following = true;
        btnFollowUser.setText("Siguiendo");
        btnFollowUser.setEnabled(true);
    }

    private void marcarComoNoSiguiendo() {
        following = false;
        btnFollowUser.setText("Seguir");
        btnFollowUser.setEnabled(true);
    }

    private void dejarDeSeguir(String currentUid) {
        btnFollowUser.setEnabled(false);

        Map<String, Object> currentUserData = new HashMap<>();
        currentUserData.put("following", FieldValue.increment(-1));
        currentUserData.put("followingIds", FieldValue.arrayRemove(userId));

        Map<String, Object> targetUserData = new HashMap<>();
        targetUserData.put("followers", FieldValue.increment(-1));
        targetUserData.put("followerIds", FieldValue.arrayRemove(currentUid));

        db.collection("users").document(currentUid)
                .set(currentUserData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> db.collection("users").document(userId)
                        .set(targetUserData, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener(done -> {
                            marcarComoNoSiguiendo();
                            Toast.makeText(getContext(),
                                    "Has dejado de seguir a " + username, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            btnFollowUser.setEnabled(true);
                            Toast.makeText(getContext(),
                                    "No se pudo dejar de seguir", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    btnFollowUser.setEnabled(true);
                    Toast.makeText(getContext(),
                            "No se pudo dejar de seguir", Toast.LENGTH_SHORT).show();
                });
    }
}

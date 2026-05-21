package com.example.vaulted;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GameDetailFragment extends Fragment {

    private static final String TAG = "GameDetailFragment";
    private static final String ARG_TITLE = "title";
    private static final String ARG_HOURS = "hours";
    private static final String ARG_HOURS_2WEEKS = "hours2weeks";
    private static final String ARG_PLATFORM = "platform";
    private static final String ARG_COVER = "cover";
    private static final String ARG_HEADER = "header";
    private static final String ARG_LAST_PLAYED = "lastPlayed";
    private static final String ARG_EXTERNAL_ID = "externalId";
    private static final String ARG_RECOMMENDED = "recommended";
    private static final String ARG_RECOMMENDATION_REASON = "recommendationReason";

    public GameDetailFragment() {
        super(R.layout.fragment_game_detail);
    }

    public static GameDetailFragment newInstance(GameEntity game) {
        GameDetailFragment f = new GameDetailFragment();
        f.setArguments(createArguments(game));
        return f;
    }

    public static Bundle createArguments(GameEntity game) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, game.title);
        args.putInt(ARG_HOURS, game.hoursPlayed);
        args.putInt(ARG_HOURS_2WEEKS, game.hoursLast2Weeks);
        args.putString(ARG_PLATFORM, game.platform);
        args.putString(ARG_COVER, game.coverUrl);
        args.putString(ARG_HEADER, game.headerUrl);
        args.putInt(ARG_LAST_PLAYED, game.lastPlayed);
        args.putString(ARG_EXTERNAL_ID, game.externalId);
        return args;
    }

    public static Bundle createRecommendationArguments(GameEntity game, String reason) {
        Bundle args = createArguments(game);
        args.putBoolean(ARG_RECOMMENDED, true);
        args.putString(ARG_RECOMMENDATION_REASON, reason);
        return args;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String externalId = args.getString(ARG_EXTERNAL_ID);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String title = args.getString(ARG_TITLE);
        String coverUrl = args.getString(ARG_COVER);
        boolean recommended = args.getBoolean(ARG_RECOMMENDED, false);
        String recommendationReason = args.getString(ARG_RECOMMENDATION_REASON);
        Button btnToggleFavorite = view.findViewById(R.id.btnToggleFavorite);
        btnToggleFavorite.setOnClickListener(v -> toggleFavorite(uid, externalId));
        RatingBar ratingReview = view.findViewById(R.id.ratingReview);
        EditText etReviewText = view.findViewById(R.id.etReviewText);
        Button btnSaveReview = view.findViewById(R.id.btnSaveReview);
        btnSaveReview.setOnClickListener(v -> guardarReview(uid, externalId, title, coverUrl, ratingReview, etReviewText));
        cargarReview(uid, externalId, ratingReview, etReviewText);

        Glide.with(this)
                .load(args.getString(ARG_HEADER))
                .error(R.drawable.ic_game_placeholder)
                .into((ImageView) view.findViewById(R.id.ivDetailCover));

        ((TextView) view.findViewById(R.id.tvDetailTitle)).setText(args.getString(ARG_TITLE));
        ((TextView) view.findViewById(R.id.tvDetailPlatform))
                .setText(args.getString(ARG_PLATFORM).toUpperCase());
        ((TextView) view.findViewById(R.id.tvDetailHours))
                .setText(args.getInt(ARG_HOURS) + " horas totales");
        if (recommended) {
            // cuando el juego llega desde recomendaciones, esta pantalla se comporta como una ficha informativa y no como un juego propio
            mostrarDetalleRecomendado(view, recommendationReason);
            return;
        }

        int h2w = args.getInt(ARG_HOURS_2WEEKS);
        TextView tvHours2Weeks = view.findViewById(R.id.tvDetailHours2Weeks);
        if (h2w > 0) {
            tvHours2Weeks.setText(h2w + "h en las ultimas 2 semanas");
            tvHours2Weeks.setVisibility(View.VISIBLE);
        } else {
            tvHours2Weeks.setVisibility(View.GONE);
        }

        int lastPlayed = args.getInt(ARG_LAST_PLAYED);
        TextView tvLastPlayed = view.findViewById(R.id.tvDetailLastPlayed);
        if (lastPlayed > 0) {
            String fechaTexto = "Ultima vez: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(new Date((long) lastPlayed * 1000));
            tvLastPlayed.setText(fechaTexto);
            tvLastPlayed.setVisibility(View.VISIBLE);
        } else {
            tvLastPlayed.setVisibility(View.GONE);
        }

        ProgressBar progressBar = view.findViewById(R.id.progressAchievements);
        TextView tvCount = view.findViewById(R.id.tvAchievementsCount);
        TextView tvNoAchievements = view.findViewById(R.id.tvNoAchievements);
        RecyclerView rvAchievements = view.findViewById(R.id.rvAchievements);

        AchievementAdapter adapter = new AchievementAdapter();
        rvAchievements.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAchievements.setAdapter(adapter);
        rvAchievements.setNestedScrollingEnabled(false);

        GameDetailViewModel viewModel = new ViewModelProvider(this).get(GameDetailViewModel.class);

        viewModel.getSyncInProgress().observe(getViewLifecycleOwner(), inProgress -> {
            progressBar.setVisibility(inProgress != null && inProgress ? View.VISIBLE : View.GONE);
        });

        viewModel.getAchievements(externalId).observe(getViewLifecycleOwner(), achievements -> {
            Log.d(TAG, "Observer disparado, logros: " + (achievements == null ? "null" : achievements.size()));

            if (achievements == null || achievements.isEmpty()) {
                // si todavía no hay logros guardados en local, se intenta una sincronización puntual solo para este juego
                FirebaseFirestore.getInstance()
                        .collection("users").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            String steamId = doc.getString("steamId");
                            if (steamId != null && !steamId.isEmpty()) {
                                if (BuildConfig.STEAM_API_KEY.isEmpty()) {
                                    Log.w(TAG, "Steam API key no configurada; se omite la sincronizacion puntual");
                                    return;
                                }
                                viewModel.syncIfNeeded(steamId, BuildConfig.STEAM_API_KEY, externalId);
                            } else {
                                progressBar.setVisibility(View.GONE);
                                tvNoAchievements.setVisibility(View.VISIBLE);
                                tvNoAchievements.setText("Cuenta Steam no vinculada");
                            }
                        });
            } else {
                progressBar.setVisibility(View.GONE);
                tvNoAchievements.setVisibility(View.GONE);
                adapter.setList(achievements);

                long unlocked = achievements.stream()
                        .filter(a -> a.unlocked)
                        .count();
                tvCount.setVisibility(View.VISIBLE);
                tvCount.setText(unlocked + " / " + achievements.size() + " logros desbloqueados");
            }
        });
    }

    private void mostrarDetalleRecomendado(View view, String recommendationReason) {
        view.findViewById(R.id.tvDetailHours).setVisibility(View.GONE);
        view.findViewById(R.id.tvDetailHours2Weeks).setVisibility(View.GONE);
        view.findViewById(R.id.tvDetailLastPlayed).setVisibility(View.GONE);
        view.findViewById(R.id.btnToggleFavorite).setVisibility(View.GONE);
        view.findViewById(R.id.layoutReviewSection).setVisibility(View.GONE);
        view.findViewById(R.id.dividerAchievements).setVisibility(View.GONE);
        view.findViewById(R.id.tvAchievementsCount).setVisibility(View.GONE);
        view.findViewById(R.id.progressAchievements).setVisibility(View.GONE);
        view.findViewById(R.id.tvNoAchievements).setVisibility(View.GONE);
        view.findViewById(R.id.rvAchievements).setVisibility(View.GONE);
        TextView tvReason = view.findViewById(R.id.tvRecommendationDetailReason);
        tvReason.setText(recommendationReason != null && !recommendationReason.isEmpty()
                ? recommendationReason
                : "Te lo recomendamos porque encaja con los gustos de usuarios compatibles contigo.");
        tvReason.setVisibility(View.VISIBLE);
    }

    private void toggleFavorite(String uid, String externalId) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("favoriteGameIds", FieldValue.arrayUnion(externalId))
                .addOnSuccessListener(v -> Toast.makeText(getContext(),
                        "Juego anadido a favoritos", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "No se pudo guardar favorito", Toast.LENGTH_SHORT).show());
    }

    private void cargarReview(String uid, String externalId, RatingBar ratingReview, EditText etReviewText) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("reviews")
                .document(externalId)
                .get()
                .addOnSuccessListener(doc -> {
                    Long rating = doc.getLong("rating");
                    String text = doc.getString("text");
                    if (rating != null) {
                        ratingReview.setRating(rating);
                    }
                    if (text != null) {
                        etReviewText.setText(text);
                    }
                });
    }

    private void guardarReview(String uid, String externalId, String title, String coverUrl,
                               RatingBar ratingReview, EditText etReviewText) {
        int rating = Math.round(ratingReview.getRating());
        String text = etReviewText.getText().toString().trim();

        if (rating <= 0) {
            Toast.makeText(getContext(), "Elige una puntuacion", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        // cada review se guarda usando el id del juego para que siempre se edite la misma entrada y no se duplique
        data.put("gameId", externalId);
        data.put("gameTitle", title);
        data.put("coverUrl", coverUrl);
        data.put("rating", rating);
        data.put("text", text);
        data.put("updatedAt", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("reviews")
                .document(externalId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> Toast.makeText(getContext(),
                        "Review guardada", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "No se pudo guardar la review", Toast.LENGTH_SHORT).show());
    }
}

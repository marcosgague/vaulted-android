package com.example.vaulted;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LinkAccountsFragment extends Fragment {

    private EditText etSteamId;
    private TextView tvProfileName, tvProfileUsername, tvProfileEmail, tvFollowers, tvFollowing;
    private ImageView ivProfilePhoto;
    private Spinner spinnerCpu, spinnerGpu, spinnerRamSize, spinnerRamType;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SteamProfileRepository steamProfileRepository;
    private GameRepository gameRepository;
    private List<GameEntity> userGames = new ArrayList<>();
    private List<String> favoriteGameIds = new ArrayList<>();
    private List<AchievementEntity> userAchievements = new ArrayList<>();
    private List<String> featuredAchievementKeys = new ArrayList<>();

    private static final String[] CPU_OPTIONS = {
            "Intel Core i9-14900K", "Intel Core i7-14700K", "Intel Core i5-14600K",
            "Intel Core i9-13900K", "Intel Core i7-13700K", "Intel Core i5-13600K",
            "Intel Core i9-12900K", "Intel Core i7-12700K", "Intel Core i5-12600K",
            "AMD Ryzen 9 7950X3D", "AMD Ryzen 9 7950X", "AMD Ryzen 9 7900X",
            "AMD Ryzen 7 7800X3D", "AMD Ryzen 7 7700X", "AMD Ryzen 5 7600X",
            "AMD Ryzen 9 5950X", "AMD Ryzen 7 5800X3D", "AMD Ryzen 7 5800X",
            "AMD Ryzen 5 5600X", "Intel Core i5-12400F"
    };

    private static final String[] GPU_OPTIONS = {
            "NVIDIA GeForce RTX 4090", "NVIDIA GeForce RTX 4080 SUPER", "NVIDIA GeForce RTX 4080",
            "NVIDIA GeForce RTX 4070 Ti SUPER", "NVIDIA GeForce RTX 4070 Ti", "NVIDIA GeForce RTX 4070 SUPER",
            "NVIDIA GeForce RTX 4070", "NVIDIA GeForce RTX 4060 Ti", "NVIDIA GeForce RTX 4060",
            "NVIDIA GeForce RTX 3090", "NVIDIA GeForce RTX 3080", "NVIDIA GeForce RTX 3070",
            "AMD Radeon RX 7900 XTX", "AMD Radeon RX 7900 XT", "AMD Radeon RX 7800 XT",
            "AMD Radeon RX 7700 XT", "AMD Radeon RX 7600", "AMD Radeon RX 6950 XT",
            "AMD Radeon RX 6800 XT", "Intel Arc A770"
    };

    private static final String[] RAM_TYPES = {"DDR3", "DDR4", "DDR5"};

    public LinkAccountsFragment() {
        super(R.layout.fragment_link_accounts);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        steamProfileRepository = new SteamProfileRepository();
        gameRepository = new GameRepository(requireContext());

        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileUsername = view.findViewById(R.id.tvProfileUsername);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvFollowers = view.findViewById(R.id.tvFollowers);
        tvFollowing = view.findViewById(R.id.tvFollowing);
        etSteamId = view.findViewById(R.id.etSteamId);
        spinnerCpu = view.findViewById(R.id.spinnerCpu);
        spinnerGpu = view.findViewById(R.id.spinnerGpu);
        spinnerRamSize = view.findViewById(R.id.spinnerRamSize);
        spinnerRamType = view.findViewById(R.id.spinnerRamType);

        configurarSpinners();

        view.findViewById(R.id.btnGuardarCuentas).setOnClickListener(v -> guardarCuentas());
        view.findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> cerrarSesion());
        view.findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> confirmarEliminarCuenta());
        view.findViewById(R.id.btnChangeEmail).setOnClickListener(v -> mostrarDialogoEmail());
        view.findViewById(R.id.btnChangePassword).setOnClickListener(v -> mostrarDialogoPassword());
        view.findViewById(R.id.btnSaveSpecs).setOnClickListener(v -> guardarEspecificaciones());
        view.findViewById(R.id.btnEditFavorites).setOnClickListener(v -> mostrarDialogoFavoritos());
        view.findViewById(R.id.btnEditFeaturedAchievements).setOnClickListener(v -> mostrarDialogoLogrosDestacados());
        view.findViewById(R.id.btnSteamIdGuide).setOnClickListener(v -> mostrarGuiaSteamId());

        cargarPerfil();
        cargarJuegosUsuario();
        cargarLogrosUsuario();
    }

    private void configurarSpinners() {
        configurarSpinner(spinnerCpu, CPU_OPTIONS);
        configurarSpinner(spinnerGpu, GPU_OPTIONS);
        configurarSpinner(spinnerRamSize, crearOpcionesRam());
        configurarSpinner(spinnerRamType, RAM_TYPES);
    }

    private void configurarSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_spinner_selected,
                options
        );
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinner.setAdapter(adapter);
    }

    private String[] crearOpcionesRam() {
        List<String> values = new ArrayList<>();
        for (int gb = 4; gb <= 288; gb += 4) {
            values.add(gb + " GB");
        }
        return values.toArray(new String[0]);
    }

    private void cargarPerfil() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "Sin email");

        Uri photoUrl = user.getPhotoUrl();
        if (photoUrl != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivProfilePhoto);
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;

                    String name = doc.getString("name");
                    String username = doc.getString("username");
                    String steamId = doc.getString("steamId");
                    String steamAvatarUrl = doc.getString("steamAvatarUrl");
                    Long followers = doc.getLong("followers");
                    Long following = doc.getLong("following");
                    List<String> favorites = (List<String>) doc.get("favoriteGameIds");
                    List<Map<String, Object>> featured = (List<Map<String, Object>>) doc.get("featuredAchievements");

                    tvProfileName.setText(name != null && !name.isEmpty() ? name : "Jugador");
                    tvProfileUsername.setText(username != null && !username.isEmpty() ? username : "@usuario");
                    tvFollowers.setText(String.valueOf(followers != null ? followers : 0));
                    tvFollowing.setText(String.valueOf(following != null ? following : 0));
                    favoriteGameIds = favorites != null ? new ArrayList<>(favorites) : new ArrayList<>();
                    featuredAchievementKeys = extraerClavesLogros(featured);
                    seleccionarValor(spinnerCpu, doc.getString("cpu"));
                    seleccionarValor(spinnerGpu, doc.getString("gpu"));
                    seleccionarValor(spinnerRamSize, doc.getString("ramSize"));
                    seleccionarValor(spinnerRamType, doc.getString("ramType"));

                    if (steamId != null && !steamId.isEmpty()) {
                        etSteamId.setText(steamId);
                        cargarFotoSteam(steamId);
                    } else if (steamAvatarUrl != null && !steamAvatarUrl.isEmpty()) {
                        mostrarAvatar(steamAvatarUrl);
                    }
                });
    }

    private void seleccionarValor(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equals(spinner.getItemAtPosition(i).toString())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void cargarJuegosUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        gameRepository.getGamesByUser(user.getUid()).observe(getViewLifecycleOwner(), games -> {
            userGames = games != null ? games : new ArrayList<>();
        });
    }

    private void cargarLogrosUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        gameRepository.getAchievementsByUser(user.getUid()).observe(getViewLifecycleOwner(), achievements -> {
            userAchievements = achievements != null ? achievements : new ArrayList<>();
        });
    }

    private void guardarEspecificaciones() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("cpu", spinnerCpu.getSelectedItem().toString());
        data.put("gpu", spinnerGpu.getSelectedItem().toString());
        data.put("ramSize", spinnerRamSize.getSelectedItem().toString());
        data.put("ramType", spinnerRamType.getSelectedItem().toString());

        db.collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> Toast.makeText(getContext(),
                        "Especificaciones guardadas", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "No se pudieron guardar las especificaciones", Toast.LENGTH_SHORT).show());
    }

    private void mostrarDialogoFavoritos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        if (userGames.isEmpty()) {
            Toast.makeText(getContext(), "Sincroniza Steam para elegir favoritos", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] titles = new String[userGames.size()];
        boolean[] checked = new boolean[userGames.size()];
        for (int i = 0; i < userGames.size(); i++) {
            GameEntity game = userGames.get(i);
            titles[i] = game.title != null ? game.title : "Sin titulo";
            checked[i] = favoriteGameIds.contains(game.externalId);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Elige tus favoritos")
                .setMultiChoiceItems(titles, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    List<String> selectedIds = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            selectedIds.add(userGames.get(i).externalId);
                        }
                    }
                    guardarFavoritos(user.getUid(), selectedIds);
                })
                .show();
    }

    private void guardarFavoritos(String uid, List<String> selectedIds) {
        Map<String, Object> data = new HashMap<>();
        data.put("favoriteGameIds", selectedIds);
        data.put("favoriteGames", crearMetadatosFavoritos(selectedIds));

        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    favoriteGameIds = new ArrayList<>(selectedIds);
                    Toast.makeText(getContext(), "Favoritos guardados", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "No se pudieron guardar favoritos", Toast.LENGTH_SHORT).show());
    }

    private List<Map<String, Object>> crearMetadatosFavoritos(List<String> selectedIds) {
        List<Map<String, Object>> favoriteGames = new ArrayList<>();
        // además de guardar los ids, se guardan metadatos básicos para no depender siempre de una consulta extra
        for (String selectedId : selectedIds) {
            for (GameEntity game : userGames) {
                if (game.externalId.equals(selectedId)) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("gameId", game.externalId);
                    data.put("title", game.title);
                    data.put("coverUrl", game.coverUrl);
                    data.put("headerUrl", game.headerUrl);
                    data.put("platform", game.platform);
                    favoriteGames.add(data);
                    break;
                }
            }
        }
        return favoriteGames;
    }

    private void mostrarDialogoLogrosDestacados() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        if (userAchievements.isEmpty()) {
            Toast.makeText(getContext(), "Sincroniza logros desde tus juegos para destacarlos", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] titles = new String[userAchievements.size()];
        boolean[] checked = new boolean[userAchievements.size()];
        for (int i = 0; i < userAchievements.size(); i++) {
            AchievementEntity achievement = userAchievements.get(i);
            titles[i] = nombreJuego(achievement.gameExternalId) + " - "
                    + (achievement.name != null ? achievement.name : "Logro");
            checked[i] = featuredAchievementKeys.contains(claveLogro(achievement));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Elige hasta 6 logros")
                .setMultiChoiceItems(titles, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                    // el límite evita que el perfil se llene demasiado y obliga a destacar solo lo más representativo
                    if (isChecked && contarSeleccionados(checked) > 6) {
                        checked[which] = false;
                        ((AlertDialog) dialog).getListView().setItemChecked(which, false);
                        Toast.makeText(getContext(), "Puedes destacar hasta 6 logros", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    List<Map<String, Object>> selected = new ArrayList<>();
                    List<String> keys = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            AchievementEntity achievement = userAchievements.get(i);
                            selected.add(mapaLogroDestacado(achievement));
                            keys.add(claveLogro(achievement));
                        }
                    }
                    guardarLogrosDestacados(user.getUid(), selected, keys);
                })
                .show();
    }

    private int contarSeleccionados(boolean[] checked) {
        int count = 0;
        for (boolean selected : checked) {
            if (selected) count++;
        }
        return count;
    }

    private Map<String, Object> mapaLogroDestacado(AchievementEntity achievement) {
        Map<String, Object> data = new HashMap<>();
        data.put("gameId", achievement.gameExternalId);
        data.put("gameTitle", nombreJuego(achievement.gameExternalId));
        data.put("name", achievement.name);
        data.put("description", achievement.description);
        data.put("iconUrl", achievement.iconUrl);
        data.put("unlocked", achievement.unlocked);
        return data;
    }

    private void guardarLogrosDestacados(String uid, List<Map<String, Object>> selected, List<String> keys) {
        Map<String, Object> data = new HashMap<>();
        data.put("featuredAchievements", selected);

        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    featuredAchievementKeys = keys;
                    Toast.makeText(getContext(), "Logros destacados guardados", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "No se pudieron guardar los logros", Toast.LENGTH_SHORT).show());
    }

    private List<String> extraerClavesLogros(List<Map<String, Object>> featured) {
        List<String> keys = new ArrayList<>();
        if (featured == null) return keys;
        for (Map<String, Object> achievement : featured) {
            Object gameId = achievement.get("gameId");
            Object name = achievement.get("name");
            if (gameId != null && name != null) {
                keys.add(gameId + "|" + name);
            }
        }
        return keys;
    }

    private String claveLogro(AchievementEntity achievement) {
        return achievement.gameExternalId + "|" + achievement.name;
    }

    private String nombreJuego(String externalId) {
        for (GameEntity game : userGames) {
            if (game.externalId.equals(externalId)) {
                return game.title != null && !game.title.isEmpty() ? game.title : "Juego";
            }
        }
        return "Juego";
    }

    private void cargarFotoSteam(String steamId) {
        if (BuildConfig.STEAM_API_KEY.isEmpty()) {
            Toast.makeText(requireContext(), "Falta configurar la clave de Steam", Toast.LENGTH_SHORT).show();
            return;
        }

        steamProfileRepository.loadProfile(BuildConfig.STEAM_API_KEY, steamId, new SteamProfileRepository.ProfileCallback() {
            @Override
            public void onProfileLoaded(@NonNull SteamProfileResponse.Player player) {
                if (!isAdded() || player.avatarFull == null || player.avatarFull.isEmpty()) return;
                mostrarAvatar(player.avatarFull);

                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) return;

                Map<String, Object> data = new HashMap<>();
                data.put("steamAvatarUrl", player.avatarFull);
                data.put("steamProfileUrl", player.profileUrl);
                data.put("steamPersonaName", player.personaName);
                db.collection("users").document(user.getUid()).set(data, SetOptions.merge());
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private void mostrarAvatar(String avatarUrl) {
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(ivProfilePhoto);
    }

    private void mostrarDialogoEmail() {
        EditText input = crearInputDialogo("Nuevo email", "textEmailAddress");

        new AlertDialog.Builder(requireContext())
                .setTitle("Cambiar email")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (dialog, which) -> cambiarEmail(input.getText().toString().trim()))
                .show();
    }

    private void cambiarEmail(String nuevoEmail) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        if (nuevoEmail.isEmpty()) {
            Toast.makeText(getContext(), "Introduce un email", Toast.LENGTH_SHORT).show();
            return;
        }

        user.updateEmail(nuevoEmail)
                .addOnSuccessListener(v -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("email", nuevoEmail);
                    data.put("emailLower", nuevoEmail.toLowerCase(Locale.ROOT));
                    db.collection("users").document(user.getUid()).set(data, SetOptions.merge());
                    tvProfileEmail.setText(nuevoEmail);
                    Toast.makeText(getContext(), "Email actualizado", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Error cambiando email: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void mostrarDialogoPassword() {
        EditText input = crearInputDialogo("Nueva contraseña", "textPassword");

        new AlertDialog.Builder(requireContext())
                .setTitle("Cambiar contraseña")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (dialog, which) ->
                        cambiarPassword(input.getText().toString().trim()))
                .show();
    }

    private void cambiarPassword(String nuevaPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        if (nuevaPassword.length() < 6) {
            Toast.makeText(getContext(), "La contraseña debe tener al menos 6 caracteres",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        user.updatePassword(nuevaPassword)
                .addOnSuccessListener(v -> Toast.makeText(getContext(),
                        "Contraseña actualizada", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Error cambiando contraseña: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private EditText crearInputDialogo(String hint, String inputType) {
        EditText input = new EditText(requireContext());
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(getResources().getColor(R.color.text_primary, requireContext().getTheme()));
        input.setHintTextColor(getResources().getColor(R.color.text_secondary, requireContext().getTheme()));
        input.setPadding(32, 16, 32, 16);
        if ("textPassword".equals(inputType)) {
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        }
        return input;
    }

    private void cerrarSesion() {
        mAuth.signOut();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void confirmarEliminarCuenta() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar cuenta")
                .setMessage("Esta accion eliminara tu perfil, juegos sincronizados, reviews y acceso a la cuenta. No se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarCuenta())
                .show();
    }

    private void eliminarCuenta() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        db.collection("users").document(uid)
                .delete()
                .addOnSuccessListener(v -> eliminarDatosLocalesYAuth(user, uid))
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "No se pudo eliminar el perfil: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void eliminarDatosLocalesYAuth(FirebaseUser user, String uid) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            // primero se limpian los datos locales y después se intenta borrar la cuenta autenticada para no dejar restos visibles en la app
            VaultedDatabase database = VaultedDatabase.getInstance(requireContext());
            database.achievementDao().deleteByUserGames(uid);
            database.gameDao().deleteAllByUser(uid);

            AppExecutors.getInstance().mainThread().execute(() -> user.delete()
                    .addOnSuccessListener(v -> {
                        Toast.makeText(getContext(), "Cuenta eliminada", Toast.LENGTH_SHORT).show();
                        cerrarSesion();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(),
                            "Inicia sesion de nuevo para eliminar la cuenta: " + e.getMessage(),
                            Toast.LENGTH_LONG).show()));
        });
    }

    private void mostrarGuiaSteamId() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Como encontrar tu Steam ID")
                .setMessage("1. Abre Steam y entra en tu perfil.\n\n" +
                        "2. Pulsa en Editar perfil o mira la URL de tu perfil.\n\n" +
                        "3. Si la direccion termina en un numero largo, ese es tu Steam ID.\n\n" +
                        "4. Si tienes una URL personalizada, abre tu perfil en un navegador y usa una pagina como steamid.io para convertirla.")
                .setNegativeButton("Cerrar", null)
                .setPositiveButton("Abrir SteamID.io", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://steamid.io/"));
                    startActivity(intent);
                })
                .show();
    }

    private void guardarCuentas() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        String steamId = etSteamId.getText().toString().trim();

        if (steamId.isEmpty()) {
            Toast.makeText(getContext(), "Introduce tu Steam ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("steamId", steamId);

        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Steam vinculado con exito", Toast.LENGTH_SHORT).show();

                    cargarFotoSteam(steamId);
                    GameRepository repo = new GameRepository(requireContext());
                    if (BuildConfig.STEAM_API_KEY.isEmpty()) {
                        Toast.makeText(requireContext(), "Falta configurar la clave de Steam", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repo.syncSteamGames(steamId, BuildConfig.STEAM_API_KEY, uid);
                    Toast.makeText(getContext(), "Sincronizando juegos de Steam...", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

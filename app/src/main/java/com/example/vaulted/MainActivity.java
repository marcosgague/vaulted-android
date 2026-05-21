package com.example.vaulted;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        // este borrado inicial evita arrastrar logros viejos cuando la app vuelve a montarse con otra sesión o tras una resincronización
        AppExecutors.getInstance().diskIO().execute(() -> {
            VaultedDatabase.getInstance(this).achievementDao().deleteAll();
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            bottomNav.setOnItemSelectedListener(item -> {
                int destinationId = item.getItemId();
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() == destinationId) {
                    return true;
                }

                // estas opciones ayudan a que cambiar de pestaña no cree una pila rara de pantallas al navegar mucho
                NavOptions navOptions = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                        .build();

                navController.navigate(destinationId, null, navOptions);
                return true;
            });
        }
    }

    public void cerrarSesion() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    public void abrirChatConUsuario(String userId, String name, String username) {
        if (navController == null) return;

        // este atajo permite saltar al chat desde un perfil sin obligar al usuario a reconstruir la conversación a mano
        Bundle args = new Bundle();
        args.putString(FriendsFragment.ARG_TARGET_USER_ID, userId);
        args.putString(FriendsFragment.ARG_TARGET_NAME, name);
        args.putString(FriendsFragment.ARG_TARGET_USERNAME, username);

        navController.navigate(R.id.friendsFragment, args);
    }

    public void abrirPerfilUsuario(ChatConversation conversation) {
        if (navController == null) return;
        navController.navigate(R.id.userProfileFragment, UserProfileFragment.createArguments(conversation));
    }
}


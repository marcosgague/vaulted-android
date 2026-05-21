package com.example.vaulted;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginEmail, etLoginPassword;
    private EditText etRegisterUsername, etRegisterName, etRegisterEmail, etRegisterPassword;
    private Button btnLogin, btnRegister;
    private View layoutLogin, layoutRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        TabLayout tabAuth = findViewById(R.id.tabAuth);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutRegister = findViewById(R.id.layoutRegister);
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterName = findViewById(R.id.etRegisterName);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        tabAuth.addTab(tabAuth.newTab().setText("Iniciar sesion"));
        tabAuth.addTab(tabAuth.newTab().setText("Crear cuenta"));
        tabAuth.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mostrarFormulario(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        btnLogin.setOnClickListener(v -> loginUsuario());
        btnRegister.setOnClickListener(v -> registrarUsuario());
    }

    private void mostrarFormulario(int tabPosition) {
        boolean loginSeleccionado = tabPosition == 0;
        // aquí no hay navegación real; simplemente se cambia qué bloque se ve para mantener todo el acceso en una sola pantalla
        layoutLogin.setVisibility(loginSeleccionado ? View.VISIBLE : View.GONE);
        layoutRegister.setVisibility(loginSeleccionado ? View.GONE : View.VISIBLE);
    }

    private void loginUsuario() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Rellena email y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        goToMain();
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage() : "Error desconocido";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registrarUsuario() {
        String username = normalizarUsername(etRegisterUsername.getText().toString().trim());
        String name = etRegisterName.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String usernameLower = username.toLowerCase(Locale.ROOT);
        String emailLower = email.toLowerCase(Locale.ROOT);

        // toda esta criba se hace antes de tocar auth para no crear cuentas medio válidas que luego toque desmontar
        if (username.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Rellena usuario, nombre, email y contraseña",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!username.matches("@[A-Za-z0-9._]{3,20}")) {
            Toast.makeText(this, "El usuario debe empezar por @ y tener de 3 a 20 caracteres",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        crearUsuarioAuth(username, usernameLower, name, email, emailLower, password);
    }

    private String normalizarUsername(String username) {
        if (username.isEmpty() || username.startsWith("@")) {
            return username;
        }
        return "@" + username;
    }

    private void crearUsuarioAuth(String username, String usernameLower, String name,
                                  String email, String emailLower, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // auth crea primero la cuenta y después se remata el perfil, porque aquí ya disponemos del uid definitivo
                        comprobarUsernameYCrearPerfil(username, usernameLower, name, email, emailLower);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        String error = task.getException() != null
                                ? task.getException().getMessage() : "Error desconocido";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void comprobarUsernameYCrearPerfil(String username, String usernameLower, String name,
                                               String email, String emailLower) {
        db.collection("users")
                .whereEqualTo("usernameLower", usernameLower)
                .limit(1)
                .get()
                .addOnSuccessListener(usernameSnapshot -> {
                    if (!usernameSnapshot.isEmpty()) {
                        // si el @ ya existía, toca dar marcha atrás para no dejar una cuenta colgando solo en authentication
                        eliminarUsuarioActual();
                        Toast.makeText(this, "Ese @usuario ya existe", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    crearDocumentoUsuario(username, usernameLower, name, email, emailLower);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error comprobando usuario: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void eliminarUsuarioActual() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        user.delete().addOnCompleteListener(task -> {
            mAuth.signOut();
            progressBar.setVisibility(View.GONE);
        });
    }

    private void crearDocumentoUsuario(String username, String usernameLower, String name,
                                       String email, String emailLower) {
        String uid = mAuth.getCurrentUser().getUid();

        // este documento es la base del perfil de Vaulted, aunque Steam todavía no esté configurado en este punto
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("usernameLower", usernameLower);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("emailLower", emailLower);
        userData.put("steamId", "");

        db.collection("users")
                .document(uid)
                .set(userData)
                .addOnSuccessListener(v -> {
                    progressBar.setVisibility(View.GONE);
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Cuenta creada, pero no se guardó el perfil",
                            Toast.LENGTH_LONG).show();
                    goToMain();
                });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

package com.example.togames.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, Animation.AnimationListener {

    private LinearLayout loginMenu, registerMenu;
    private ConstraintLayout bottomMenu;
    private EditText editText_login_email, editText_login_password, editText_email, editText_password,
            editText_password2, editText_name, editText_surname, editText_age, editText_weight, editText_height;
    private Animation animLoginFadeIn, animRegisterFadeIn, animLoginFadeOut, animRegisterFadeOut;
    private Button button_login, button_register, button_goToRegister, button_backToLogin;
    private FirebaseAuth auth;
    private User userToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme((AppSettings.getInstance(this).isDarkTheme) ?
                R.style.NoTitleThemeDark : R.style.NoTitleTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        // Check if user is signed in (non-null).
        // If yes, go to MainActivity
        if (auth.getCurrentUser() != null) {
            Intent intent_signedIn = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent_signedIn);
        }

        button_login = findViewById(R.id.button_login);
        button_register = findViewById(R.id.button_register);
        button_goToRegister = findViewById(R.id.button_goToRegister);
        button_backToLogin = findViewById(R.id.button_backToLogin);
        editText_login_email = findViewById(R.id.editText_login_email);
        editText_login_password = findViewById(R.id.editText_login_password);
        editText_email = findViewById(R.id.editText_email);
        editText_password = findViewById(R.id.editText_password);
        editText_password2 = findViewById(R.id.editText_password2);
        editText_name = findViewById(R.id.editText_name);
        editText_surname = findViewById(R.id.editText_surname);
        editText_age = findViewById(R.id.editText_age);
        editText_weight = findViewById(R.id.editText_weight);
        editText_height = findViewById(R.id.editText_height);
        loginMenu = findViewById(R.id.linearLayout_login_menu);
        registerMenu = findViewById(R.id.linearLayout_register_menu);
        bottomMenu = findViewById(R.id.constraintLayout_login_bottom);
        animLoginFadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
        animLoginFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
        animRegisterFadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
        animRegisterFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);

        button_login.setOnClickListener(this);
        button_register.setOnClickListener(this);
        button_goToRegister.setOnClickListener(this);
        button_backToLogin.setOnClickListener(this);
        animLoginFadeOut.setAnimationListener(this);
        animLoginFadeIn.setAnimationListener(this);
        animRegisterFadeOut.setAnimationListener(this);
        animRegisterFadeIn.setAnimationListener(this);

        loginMenu.startAnimation(animLoginFadeIn);
    }

    @Override
    public void onAnimationStart(Animation animation) {
        if (animation == animLoginFadeIn) {
            loginMenu.setVisibility(View.VISIBLE);
        } else if (animation == animRegisterFadeIn) {
            registerMenu.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (animation == animLoginFadeOut) { // Animation(Login menu fade out) is ended
            // Make login menu and bottom menu invisible
            // Start the animation to make register menu fade in
            loginMenu.setVisibility(View.INVISIBLE);
            bottomMenu.setVisibility(View.GONE);
            registerMenu.startAnimation(animRegisterFadeIn);
        } else if (animation == animRegisterFadeOut) { // Animation(Register menu fade out) is ended
            // Start the animation to make register menu fade in
            // and bottom menu slide up
            registerMenu.setVisibility(View.INVISIBLE);
            bottomMenu.animate().translationY(0).setDuration(500);
            bottomMenu.setVisibility(View.VISIBLE);
            loginMenu.startAnimation(animLoginFadeIn);
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // Not needed
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_login) { // Login button is clicked
            // Check if email or password is empty,
            // If yes, do nothing.
            String email = editText_login_email.getText().toString();
            String password = editText_login_password.getText().toString();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, R.string.invalid_input,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Sign in to Firebase Authentication
            button_login.setEnabled(false);
            auth.signInWithEmailAndPassword(email, password).
                    addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            button_login.setEnabled(true);
                            if (task.isSuccessful()) {
                                // If login is successful, go to MainActivity
                                Intent intent_login = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent_login);
                                finish();
                            } else {
                                // If login is not successful, show an error message
                                Toast.makeText(LoginActivity.this, R.string.login_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else if (id == R.id.button_register) { // Register button is clicked
            // Check if all of the required fields are filled,
            // and check if passwords match.
            String email = editText_email.getText().toString();
            String password = editText_password.getText().toString();
            String password2 = editText_password2.getText().toString();
            String name = editText_name.getText().toString();
            String surname = editText_surname.getText().toString();
            String age = editText_age.getText().toString();
            String weight = editText_weight.getText().toString();
            String height = editText_height.getText().toString();
            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || age.isEmpty() ||
                    weight.isEmpty() || height.isEmpty()) {
                Toast.makeText(LoginActivity.this, "All fields must be filled",
                        Toast.LENGTH_SHORT).show();
                return;
            } else if (!password.equals(password2)) {
                Toast.makeText(LoginActivity.this, "Passwords does not match",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Register to Firebase Authentication and save data into database
            button_register.setEnabled(false);
            userToRegister = new User(email, name, surname, age, weight, height,6000, null);
            auth.createUserWithEmailAndPassword(email, password).
                    addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // If register is successful, save user data into
                                // Firebase database and go to MainActivity
                                String userId = auth.getCurrentUser().getUid();
                                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().
                                        child("user_data").child(userId);
                                userRef.setValue(userToRegister);

                                Intent intent_register = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent_register);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Register Failed",
                                        Toast.LENGTH_SHORT).show();
                                button_register.setEnabled(true);
                            }
                        }
                    });
        } else if (id == R.id.button_goToRegister) { // 'Go to register' button is clicked
            // Start the animation to make the login menu fade out
            // and bottom menu slide down
            loginMenu.startAnimation(animLoginFadeOut);
            bottomMenu.animate().translationY(bottomMenu.getHeight()).setDuration(500);
            button_goToRegister.setClickable(false);
            button_backToLogin.setClickable(true);
        } else if (id == R.id.button_backToLogin) { // 'Go to login' button is clicked
            // Start the animation to make register menu fade out
            registerMenu.startAnimation(animRegisterFadeOut);
            button_backToLogin.setClickable(false);
            button_goToRegister.setClickable(true);
        }
    }
}

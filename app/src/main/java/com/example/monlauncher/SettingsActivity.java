package com.example.monlauncher;

import android.app.role.RoleManager; // Ajouté
import android.content.Context;
import android.content.Intent; // Ajouté
import android.content.SharedPreferences;
import android.os.Build; // Ajouté
import android.os.Bundle;
import android.provider.Settings; // Ajouté
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText editFavCount;
    private Spinner spinnerAlign, spinnerLeft, spinnerRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editFavCount = findViewById(R.id.edit_fav_count);
        spinnerAlign = findViewById(R.id.spinner_alignment);
        spinnerLeft = findViewById(R.id.spinner_swipe_left);
        spinnerRight = findViewById(R.id.spinner_swipe_right);

        setupSpinners();
        loadCurrentSettings();

        // Bouton Sauvegarder
        findViewById(R.id.btn_save).setOnClickListener(v -> saveSettings());

        // Bouton pour mettre par défaut (Ajoute cet ID dans ton XML ou supprime cette ligne)
        if (findViewById(R.id.btn_set_default) != null) {
            findViewById(R.id.btn_set_default).setOnClickListener(v -> openDefaultLauncherSettings());
        }
    }

    private void openDefaultLauncherSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);
                startActivityForResult(intent, 123);
            }
        } else {
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            startActivity(intent);
        }
    }

    private void setupSpinners() {
        String[] alignments = {"Gauche", "Centré", "Droite"};
        ArrayAdapter<String> adapterAlign = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, alignments);
        adapterAlign.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlign.setAdapter(adapterAlign);

        String[] actions = {"Aucun", "Appareil Photo", "Téléphone"};
        ArrayAdapter<String> adapterActions = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, actions);
        adapterActions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLeft.setAdapter(adapterActions);
        spinnerRight.setAdapter(adapterActions);
    }

    private void loadCurrentSettings() {
        SharedPreferences pref = getSharedPreferences("LauncherConfig", Context.MODE_PRIVATE);

        int currentCount = pref.getInt("fav_count", 4);
        editFavCount.setText(String.valueOf(currentCount));

        String currentAlign = pref.getString("alignment", "Gauche");
        setSpinnerValue(spinnerAlign, currentAlign);

        String currentLeft = pref.getString("swipe_left_action", "Aucun");
        setSpinnerValue(spinnerLeft, currentLeft);

        String currentRight = pref.getString("swipe_right_action", "Aucun");
        setSpinnerValue(spinnerRight, currentRight);
    }

    // Petite méthode helper pour éviter les erreurs de cast et simplifier
    private void setSpinnerValue(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter != null) {
            int pos = adapter.getPosition(value);
            spinner.setSelection(pos);
        }
    }

    private void saveSettings() {
        SharedPreferences pref = getSharedPreferences("LauncherConfig", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        try {
            int count = Integer.parseInt(editFavCount.getText().toString());
            if (count > 8) count = 8;
            editor.putInt("fav_count", count);
        } catch (NumberFormatException e) {
            editor.putInt("fav_count", 4);
        }

        editor.putString("alignment", spinnerAlign.getSelectedItem().toString());
        editor.putString("swipe_left_action", spinnerLeft.getSelectedItem().toString());
        editor.putString("swipe_right_action", spinnerRight.getSelectedItem().toString());

        editor.apply();
        Toast.makeText(this, "Paramètres enregistrés", Toast.LENGTH_SHORT).show();
        finish();
    }
}
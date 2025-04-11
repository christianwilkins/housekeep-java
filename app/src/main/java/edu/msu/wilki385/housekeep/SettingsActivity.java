package edu.msu.wilki385.housekeep;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MySettings";
    private static final String KEY_EXAMPLE = "example_preference";

    private EditText editTextPreference;
    private Button savePreferenceButton;
    private Button logoutButton;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        editTextPreference = findViewById(R.id.editTextPreference);
        savePreferenceButton = findViewById(R.id.buttonSavePreference);
        logoutButton = findViewById(R.id.buttonLogoutSettings);
        backButton = findViewById(R.id.buttonBackSettings);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentValue = prefs.getString(KEY_EXAMPLE, "");
        editTextPreference.setText(currentValue);

        savePreferenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newValue = editTextPreference.getText().toString().trim();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_EXAMPLE, newValue);
                editor.apply();
                Toast.makeText(SettingsActivity.this, "Preference saved!", Toast.LENGTH_SHORT).show();
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(SettingsActivity.this, "Signed Out!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                finish();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // simply return to the previous activity
            }
        });
    }
}

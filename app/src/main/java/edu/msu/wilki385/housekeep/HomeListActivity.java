package edu.msu.wilki385.housekeep;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_list);
        Button logoutButton = findViewById(R.id.buttonLogout);
        logoutButton.setOnClickListener(v -> logoutUser());
        Button createHomeButton = findViewById(R.id.buttonCreateHome);
        createHomeButton.setOnClickListener(v -> showCreateHomeDialog());
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
    }

    private void showCreateHomeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Home");
        final EditText input = new EditText(this);
        input.setHint("Enter Home Name");
        builder.setView(input);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String homeName = input.getText().toString().trim();
                if (homeName.isEmpty()) {
                    Toast.makeText(HomeListActivity.this, "Home name cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    createHome(homeName);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void createHome(String homeName) {
        if (userId == null) return;
        String homeUuid = UUID.randomUUID().toString();
        Map<String, Object> newHome = new HashMap<>();
        newHome.put("uuid", homeUuid);
        newHome.put("name", homeName);
        db.collection("users").document(userId).collection("homes").document(homeUuid)
                .set(newHome)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Home created! ID: " + homeUuid, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create home: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeHome(String homeUuid) {
        if (userId == null) return;
        db.collection("users").document(userId).collection("homes").document(homeUuid)
                .delete()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Home removed!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove home: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Signed Out!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}

package edu.msu.wilki385.housekeep;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

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

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
    }

    private void createHome(String homeName) {
        if (userId == null) return;
        Map<String, Object> newHome = new HashMap<>();
        newHome.put("name", homeName);
        DocumentReference userDocRef = db.collection("users").document(userId);
        userDocRef.update("homes", FieldValue.arrayUnion(newHome))
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Home created!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create home: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void removeHome(String homeName) {
        if (userId == null) return;
        Map<String, Object> homeToRemove = new HashMap<>();
        homeToRemove.put("name", homeName);
        DocumentReference userDocRef = db.collection("users").document(userId);
        userDocRef.update("homes", FieldValue.arrayRemove(homeToRemove))
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Home removed!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove home: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Signed Out!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}

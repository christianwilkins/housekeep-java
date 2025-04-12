package edu.msu.wilki385.housekeep;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.UUID;
import edu.msu.wilki385.housekeep.collections.House;

public class HomeListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;
    private LinearLayout linearLayoutHomes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_list);

        Button settingsButton = findViewById(R.id.buttonSettings);
        settingsButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        Button createHouseButton = findViewById(R.id.buttonCreateHome);
        createHouseButton.setOnClickListener(v -> showCreateHouseDialog());

        linearLayoutHomes = findViewById(R.id.linearLayoutHomes);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        getHouses();
    }

    private void showCreateHouseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New House");

        final EditText input = new EditText(this);
        // Convert 16dp to pixels
        int paddingPixels = (int) (16 * getResources().getDisplayMetrics().density + 0.5f);
        input.setPadding(paddingPixels, paddingPixels, paddingPixels, paddingPixels);
        input.setHint("Enter House Name");
        builder.setView(input);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String houseName = input.getText().toString().trim();
                if (TextUtils.isEmpty(houseName)) {
                    Toast.makeText(HomeListActivity.this, "House name cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    createHouse(houseName);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                dialog.cancel();
            }
        });
        builder.show();
    }


    private void createHouse(String houseName) {
        if (userId == null) return;
        String houseId = UUID.randomUUID().toString();
        House house = new House(houseId, houseName, userId, null);
        db.collection("users").document(userId).collection("houses").document(houseId)
                .set(house)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "House created! ID: " + houseId, Toast.LENGTH_SHORT).show();
                    getHouses();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create house: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeHouse(String houseId) {
        if (userId == null) return;
        db.collection("users").document(userId).collection("houses").document(houseId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "House removed!", Toast.LENGTH_SHORT).show();
                    getHouses();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove house: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void getHouses() {
        db.collection("users").document(userId).collection("houses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    linearLayoutHomes.removeAllViews();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        House house = doc.toObject(House.class);
                        View card = LayoutInflater.from(this).inflate(R.layout.item_home, linearLayoutHomes, false);
                        TextView textView = card.findViewById(R.id.textViewHouseName);
                        textView.setText(house.getName());
                        Button deleteButton = card.findViewById(R.id.buttonDeleteHouse);
                        deleteButton.setOnClickListener(v -> removeHouse(house.getId()));

                        Button enterButton = card.findViewById(R.id.buttonEnterHouse);
                        enterButton.setOnClickListener(v -> {
                            Intent intent = new Intent(HomeListActivity.this, HomeTaskActivity.class);
                            intent.putExtra("houseId", house.getId());
                            intent.putExtra("HOUSE_NAME", house.getName());
                            startActivity(intent);
                        });

                        linearLayoutHomes.addView(card);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to get houses: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}

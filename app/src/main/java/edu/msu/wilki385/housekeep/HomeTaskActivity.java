package edu.msu.wilki385.housekeep;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.UUID;
import edu.msu.wilki385.housekeep.collections.Task;

public class HomeTaskActivity extends Activity {

    private ListView taskList;
    private ImageButton plusButton;
    private ArrayList<String> currentTasks = new ArrayList<>();
    private ArrayList<Task> taskObjects = new ArrayList<>();
    private ArrayAdapter<String> taskAdapter;

    private FirebaseFirestore db;
    private String userId;
    private String houseId;
    private String houseName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_tasks);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Retrieve using the exact same key "houseId"
        houseId = getIntent().getStringExtra("houseId");
        houseName = getIntent().getStringExtra("HOUSE_NAME");

        db = FirebaseFirestore.getInstance();

        taskList = findViewById(R.id.taskList);
        plusButton = findViewById(R.id.plusButton);

        taskAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, currentTasks);
        taskList.setAdapter(taskAdapter);

        plusButton.setOnClickListener(v -> showAddTaskDialog());

        taskList.setOnItemLongClickListener((parent, view, position, id) -> {
            String taskId = taskObjects.get(position).getId();
            removeTask(taskId);
            return true;
        });

        getTasks();
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Task");
        final EditText input = new EditText(this);
        input.setHint("Enter task name");
        builder.setView(input);
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                String taskName = input.getText().toString().trim();
                if(TextUtils.isEmpty(taskName)){
                    Toast.makeText(HomeTaskActivity.this, "Task name cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    createTask(taskName);
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void createTask(String taskName) {
        String taskId = UUID.randomUUID().toString();
        Task task = new Task(taskId, taskName);
        db.collection("users").document(userId)
                .collection("houses").document(houseId)
                .collection("tasks").document(taskId)
                .set(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show();
                    getTasks();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeTask(String taskId) {
        db.collection("users").document(userId)
                .collection("houses").document(houseId)
                .collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task removed!", Toast.LENGTH_SHORT).show();
                    getTasks();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void getTasks() {
        db.collection("users").document(userId)
                .collection("houses").document(houseId)
                .collection("tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskObjects.clear();
                    currentTasks.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        taskObjects.add(task);
                        currentTasks.add(task.getName());
                    }
                    taskAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to get tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}

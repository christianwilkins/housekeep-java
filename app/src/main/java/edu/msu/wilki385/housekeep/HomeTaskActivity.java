package edu.msu.wilki385.housekeep;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.UUID;

import edu.msu.wilki385.housekeep.collections.Task;

public class HomeTaskActivity extends Activity {
    private ListView taskList;
    private Button backButton;
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
        houseId = getIntent().getStringExtra("houseId");
        houseName = getIntent().getStringExtra("HOUSE_NAME");

        db = FirebaseFirestore.getInstance();

        taskList = findViewById(R.id.taskList);
        plusButton = findViewById(R.id.plusButton);
        backButton = findViewById(R.id.backButton);

        setupCustomTaskAdapter();

        plusButton.setOnClickListener(v -> showAddTaskDialog());

        backButton.setOnClickListener(v -> finish());

        taskList.setOnItemLongClickListener((parent, view, position, id) -> {
            String taskId = taskObjects.get(position).getId();
            removeTask(taskId);
            return true;
        });

        getTasks();
    }

    private void setupCustomTaskAdapter() {
        taskAdapter = new ArrayAdapter<String>(this, R.layout.task_item, currentTasks) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.task_item, parent, false);
                }

                CheckBox checkBox = convertView.findViewById(R.id.taskCheckbox);
                TextView taskText = convertView.findViewById(R.id.taskText);
                LinearLayout taskRow = convertView.findViewById(R.id.taskRow);

                String task = currentTasks.get(position);
                taskText.setText(task.replace(" [Done]", ""));

                boolean isDone = task.contains(" [Done]");
                checkBox.setChecked(isDone);
                taskText.setPaintFlags(isDone
                        ? taskText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                        : taskText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

                checkBox.setOnClickListener(v -> {
                    if (checkBox.isChecked()) {
                        currentTasks.set(position, taskText.getText().toString() + " [Done]");
                    } else {
                        currentTasks.set(position, taskText.getText().toString());
                    }
                    notifyDataSetChanged();
                });

                taskRow.setOnClickListener(v -> showTaskDetailDialog(taskText.getText().toString()));

                return convertView;
            }
        };

        taskList.setAdapter(taskAdapter);
    }

    private void showTaskDetailDialog(String task) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.task_detail_dialog, null);

        TextView titleView = dialogView.findViewById(R.id.taskDetailTitle);
        TextView descriptionView = dialogView.findViewById(R.id.taskDetailDescription);
        ImageView imageView = dialogView.findViewById(R.id.taskDetailImage);
        Button editButton = dialogView.findViewById(R.id.editButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);
        Button closeButton = dialogView.findViewById(R.id.closeButton);

        titleView.setText(task);
        descriptionView.setText("This task needs to be completed soon.");
        imageView.setImageResource(R.drawable.ic_launcher_background);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        editButton.setOnClickListener(v -> {
            showInputDialog("Edit Task", "Update task name:", newText -> {
                if (!newText.trim().isEmpty()) {
                    int index = currentTasks.indexOf(task);
                    if (index != -1) {
                        String taskId = taskObjects.get(index).getId();
                        updateTask(taskId, newText);
                        dialog.dismiss();
                    }
                }
            });
        });

        deleteButton.setOnClickListener(v -> {
            int index = currentTasks.indexOf(task);
            if (index != -1) {
                String taskId = taskObjects.get(index).getId();
                removeTask(taskId);
            }
            dialog.dismiss();
        });

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showAddTaskDialog() {
        showInputDialog("Add Task", "Enter new task:", taskName -> {
            if (TextUtils.isEmpty(taskName)) {
                Toast.makeText(HomeTaskActivity.this, "Task name cannot be empty", Toast.LENGTH_SHORT).show();
            } else {
                createTask(taskName);
            }
        });
    }

    private void showInputDialog(String title, String message, OnInputConfirmed callback) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_input, null);
        final EditText input = dialogView.findViewById(R.id.editTextInput);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    String text = input.getText().toString();
                    callback.onConfirmed(text);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

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

    private void updateTask(String taskId, String newName) {
        db.collection("users").document(userId)
                .collection("houses").document(houseId)
                .collection("tasks").document(taskId)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();
                    getTasks();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    interface OnInputConfirmed {
        void onConfirmed(String text);
    }
}

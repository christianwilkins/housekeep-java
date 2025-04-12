package edu.msu.wilki385.housekeep;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HomeTaskActivity extends Activity {
    private ListView taskList;
    private ImageButton plusButton;

    private ArrayList<String> currentTasks = new ArrayList<>();
    private ArrayAdapter<String> taskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_tasks);
        String houseId = getIntent().getStringExtra("houseId");

        taskList = findViewById(R.id.taskList);
        plusButton = findViewById(R.id.plusButton);

        // Sample task
        currentTasks.add("Clean refrigerator coils");

        setupCustomTaskAdapter();

        plusButton.setOnClickListener(view -> {
            showInputDialog("Add Task", "Enter new task:", task -> {
                if (!task.trim().isEmpty()) {
                    currentTasks.add(task);
                    taskAdapter.notifyDataSetChanged();
                }
            });
        });
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

                // Checkbox click (mark done)
                checkBox.setOnClickListener(v -> {
                    if (checkBox.isChecked()) {
                        currentTasks.set(position, taskText.getText().toString() + " [Done]");
                    } else {
                        currentTasks.set(position, taskText.getText().toString());
                    }
                    notifyDataSetChanged();
                });

                // Row click (go to task details)
                taskRow.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), TaskDetailActivity.class);
                    intent.putExtra("task", taskText.getText().toString());
                    getContext().startActivity(intent);
                });

                return convertView;
            }
        };

        taskList.setAdapter(taskAdapter);
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

    interface OnInputConfirmed {
        void onConfirmed(String text);
    }
}

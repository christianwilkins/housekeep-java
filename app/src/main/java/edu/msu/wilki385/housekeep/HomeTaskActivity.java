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
    private Spinner areaSpinner;
    private ListView taskList;
    private ImageButton plusButton;

    private ArrayList<String> areaList = new ArrayList<>();
    private ArrayAdapter<String> areaAdapter;

    private ArrayList<String> currentTasks = new ArrayList<>();
    private ArrayAdapter<String> taskAdapter;

    private HashMap<String, ArrayList<String>> areaTasks = new HashMap<>();

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_tasks);

        areaSpinner = findViewById(R.id.areaSpinner);
        taskList = findViewById(R.id.taskList);
        plusButton = findViewById(R.id.plusButton);

        // Initial sample category and task
        areaList.add("Kitchen");
        areaTasks.put("Kitchen", new ArrayList<>());
        areaTasks.get("Kitchen").add("Clean refrigerator coils");

        areaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, areaList);
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        areaSpinner.setAdapter(areaAdapter);

        taskAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, currentTasks);
        setupCustomTaskAdapter();

        areaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTaskList(areaList.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        plusButton.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(HomeTaskActivity.this, plusButton);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.add_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                String selectedArea = (String) areaSpinner.getSelectedItem();
                if (item.getItemId() == R.id.add_category) {
                    showInputDialog("Add Category", "Enter category name:", name -> {
                        if (!name.trim().isEmpty() && !areaTasks.containsKey(name)) {
                            areaList.add(name);
                            areaTasks.put(name, new ArrayList<>());
                            areaAdapter.notifyDataSetChanged();
                            areaSpinner.setSelection(areaList.size() - 1);
                        }
                    });
                    return true;
                } else if (item.getItemId() == R.id.add_task) {
                    if (selectedArea != null) {
                        showInputDialog("Add Task", "Enter task for " + selectedArea + ":", task -> {
                            if (!task.trim().isEmpty()) {
                                areaTasks.get(selectedArea).add(task);
                                updateTaskList(selectedArea);
                            }
                        });
                    }
                    return true;
                } else {
                    return false;
                }
            });

            popup.show();
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
                    Toast.makeText(getContext(), "Opening details for: " + taskText.getText(), Toast.LENGTH_SHORT).show();
                    // You can use an Intent to navigate to a detail view activity here
                    Intent intent = new Intent(getContext(), TaskDetailActivity.class);
                    intent.putExtra("task", taskText.getText().toString());
                    getContext().startActivity(intent);
                });

                return convertView;
            }
        };

        taskList.setAdapter(taskAdapter);
    }


    private void updateTaskList(String area) {
        currentTasks.clear();
        if (areaTasks.containsKey(area)) {
            currentTasks.addAll(areaTasks.get(area));
        }
        taskAdapter.notifyDataSetChanged();
    }

    private void showInputDialog(String title, String message, OnInputConfirmed callback) {
        // Inflate the custom layout (dialog_input.xml)
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_input, null);

        final EditText input = dialogView.findViewById(R.id.editTextInput);

        // Set up the dialog builder
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

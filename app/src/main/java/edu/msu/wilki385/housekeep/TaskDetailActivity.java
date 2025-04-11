package edu.msu.wilki385.housekeep;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TaskDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        // Get the task name passed from the previous activity
        String task = getIntent().getStringExtra("TASK_NAME");

        TextView taskDetailText = findViewById(R.id.taskDetailText);
        taskDetailText.setText(task);
    }
}

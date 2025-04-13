package edu.msu.wilki385.housekeep;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import edu.msu.wilki385.housekeep.collections.Task;

public class HomeTaskActivity extends AppCompatActivity {

    private TaskPhotoManager photoManager;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.POST_NOTIFICATIONS},
                    100
                );
            }
        }

        photoManager = new TaskPhotoManager();

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
        Button takePhotoButton = dialogView.findViewById(R.id.takePhotoButton);
        Button setReminderButton = dialogView.findViewById(R.id.setReminderButton);
        Button editButton = dialogView.findViewById(R.id.editButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);
        Button closeButton = dialogView.findViewById(R.id.closeButton);

        titleView.setText(task);
        descriptionView.setText("This task needs to be completed soon.");

        int idx1 = currentTasks.indexOf(task);
        if (idx1 != -1) {
            String taskId = taskObjects.get(idx1).getId();
            photoManager.loadTaskPhoto(taskId, imageView);
        } else {
            imageView.setImageDrawable(null);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        takePhotoButton.setOnClickListener(v -> {
            int idx2 = currentTasks.indexOf(task);
            if (idx2 != -1) {
                String taskId = taskObjects.get(idx2).getId();
                photoManager.captureTaskPhoto(taskId, imageView);
            } else {
                Toast.makeText(HomeTaskActivity.this, "Task not found", Toast.LENGTH_SHORT).show();
            }
        });

        setReminderButton.setOnClickListener(v -> pickDateTime(task));

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

    private class TaskPhotoManager {

        private ActivityResultLauncher<Uri> photoLauncher;
        private String pendingTaskId;
        private ImageView pendingImageView;

        public TaskPhotoManager() {
            photoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        Toast.makeText(HomeTaskActivity.this, "Photo captured for task", Toast.LENGTH_SHORT).show();
                        if (pendingTaskId != null && pendingImageView != null) {
                            loadTaskPhoto(pendingTaskId, pendingImageView);
                        }
                    } else {
                        Toast.makeText(HomeTaskActivity.this, "Photo capture cancelled", Toast.LENGTH_SHORT).show();
                    }
                    pendingTaskId = null;
                    pendingImageView = null;
                }
            );
        }

        public void captureTaskPhoto(String taskId, ImageView imageView) {
            pendingTaskId = taskId;
            pendingImageView = imageView;

            ContentValues values = new ContentValues();
            String imageTitle = "housekeep" + userId + houseId + taskId;
            values.put(MediaStore.Images.Media.TITLE, imageTitle);
            values.put(MediaStore.Images.Media.DISPLAY_NAME, imageTitle);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            Uri currentPhotoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (currentPhotoUri != null) {
                photoLauncher.launch(currentPhotoUri);
            } else {
                Toast.makeText(HomeTaskActivity.this, "Failed to create MediaStore entry for photo", Toast.LENGTH_SHORT).show();
            }
        }

        public void loadTaskPhoto(String taskId, ImageView imageView) {
            String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME};
            String selection = MediaStore.Images.Media.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = {"housekeep" + userId + houseId + taskId + "%"};
            String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

            Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            );

            try (cursor) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    imageView.setImageURI(contentUri);
                } else {
                    imageView.setImageDrawable(null);
                }
            } catch (Exception e) {
                imageView.setImageDrawable(null);
            }
        }
    }

    private void scheduleReminder(String taskName, long triggerTimeMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("taskName", taskName);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            taskName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                );
            } else {
                Intent permIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(permIntent);
                Toast.makeText(this,
                        "Please enable reminders for this app",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String taskName = intent.getStringExtra("taskName");

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(
                    "REMINDER_CHANNEL_ID",
                    "Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                );
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(channel);
            }

            Notification notification = new NotificationCompat.Builder(context, "REMINDER_CHANNEL_ID")
                .setContentTitle("Housekeep Reminder")
                .setContentText(taskName)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

            notificationManager.notify(1001, notification);
        }
    }

    private void pickDateTime(String task) {
        final Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);
        int day = now.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(this,
            (view, year1, month1, day1) -> {
                now.set(Calendar.YEAR, year1);
                now.set(Calendar.MONTH, month1);
                now.set(Calendar.DAY_OF_MONTH, day1);

                int hour = now.get(Calendar.HOUR_OF_DAY);
                int minute = now.get(Calendar.MINUTE);

                TimePickerDialog timePicker = new TimePickerDialog(this,
                    (timeView, hour1, minute1) -> {
                        now.set(Calendar.HOUR_OF_DAY, hour1);
                        now.set(Calendar.MINUTE, minute1);
                        now.set(Calendar.SECOND, 0);
                        now.set(Calendar.MILLISECOND, 0);

                        long triggerTime = now.getTimeInMillis();
                        scheduleReminder(task, triggerTime);
                        Toast.makeText(this,
                            "Reminder set for: " + now.getTime(),
                            Toast.LENGTH_LONG).show();

                    }, hour, minute, false);

                timePicker.show();

            }, year, month, day);

        datePicker.show();
    }
}

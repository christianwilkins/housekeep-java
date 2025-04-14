package edu.msu.wilki385.housekeep;

import android.graphics.Bitmap;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

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
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.io.File;

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

    // Added fields for audio recording and playback
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;
    private String pendingAudioTaskId = null;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

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

                // Instead of getting the task text from currentTasks,
                // retrieve the corresponding Task object.
                String taskName = currentTasks.get(position);
                Task task = taskObjects.get(position);

                taskText.setText(taskName);
                boolean isDone = task.isDone();

                // Update checkbox state and styling accordingly.
                checkBox.setChecked(isDone);
                if (isDone) {
                    taskText.setPaintFlags(taskText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    taskText.setPaintFlags(taskText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

                // Update task's done status on checkbox toggle.
                checkBox.setOnClickListener(v -> {
                    boolean checked = checkBox.isChecked();
                    task.setDone(checked);

                    db.collection("users").document(userId)
                            .collection("houses").document(houseId)
                            .collection("tasks").document(task.getId())
                            .update("done", checked)
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Task status updated", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Failed to update task: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                    if (checked) {
                        taskText.setPaintFlags(taskText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    } else {
                        taskText.setPaintFlags(taskText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    }
                    notifyDataSetChanged();
                });

                // When a task row is clicked, show its detail dialog.
                taskRow.setOnClickListener(v -> showTaskDetailDialog(taskName));

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
        // New single button for audio note
        Button audioNoteButton = dialogView.findViewById(R.id.audioNoteButton);

        titleView.setText(task);
        descriptionView.setText("This task needs to be completed soon.");

        int idx1 = currentTasks.indexOf(task);
        if (idx1 != -1) {
            String taskId = taskObjects.get(idx1).getId();
            photoManager.loadTaskPhoto(taskId, imageView, descriptionView);
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
                photoManager.captureTaskPhoto(taskId, imageView, descriptionView);
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

        // When the Audio Note button is tapped, open the audio note modal.
        audioNoteButton.setOnClickListener(v -> {
            int idx = currentTasks.indexOf(task);
            if (idx != -1) {
                String taskId = taskObjects.get(idx).getId();
                showAudioNoteDialog(taskId);
            } else {
                Toast.makeText(HomeTaskActivity.this, "Task not found", Toast.LENGTH_SHORT).show();
            }
        });

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
        Task task = new Task(taskId, taskName, false);
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            // Handle POST_NOTIFICATIONS permission if needed.
        } else if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingAudioTaskId != null) {
                    startRecordingAudio(pendingAudioTaskId);
                    pendingAudioTaskId = null;
                }
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Helper interface for input dialogs
    interface OnInputConfirmed {
        void onConfirmed(String text);
    }

    ////////////////////// Audio Note Modal Dialog //////////////////////

    /**
     * Opens a modal dialog that allows the user to record, stop, and play an audio note.
     */
    private void showAudioNoteDialog(String taskId) {
        LayoutInflater inflater = getLayoutInflater();
        View audioDialogView = inflater.inflate(R.layout.dialog_audio_note, null);

        TextView audioStatusText = audioDialogView.findViewById(R.id.audioStatusText);
        Button toggleRecordButton = audioDialogView.findViewById(R.id.toggleRecordButton);
        Button playRecordingButton = audioDialogView.findViewById(R.id.playRecordingButton);
        Button closeAudioDialogButton = audioDialogView.findViewById(R.id.closeAudioDialogButton);

        // Build the file name and complete path.
        String fileName = "housekeep_audio_" + userId + "_" + houseId + "_" + taskId + ".3gp";
        String filePath = getExternalCacheDir().getAbsolutePath() + "/" + fileName;
        File audioFile = new File(filePath);

        // Check if a recording file already exists
        if (audioFile.exists()) {
            audioStatusText.setText("Audio recording exists.");
            playRecordingButton.setEnabled(true);
            // Set the global variable so that playAudio() can use it.
            audioFilePath = filePath;
        } else {
            audioStatusText.setText("No recording yet.");
            playRecordingButton.setEnabled(false);
        }

        AlertDialog audioDialog = new AlertDialog.Builder(this)
                .setView(audioDialogView)
                .create();

        toggleRecordButton.setOnClickListener(v -> {
            if (!isRecording) {
                // Start recording if permission is granted.
                if (checkAndRequestAudioPermission()) {
                    // Start recording for this task.
                    startRecordingAudio(taskId);
                    toggleRecordButton.setText("Stop Recording");
                    audioStatusText.setText("Recording in progress...");
                } else {
                    pendingAudioTaskId = taskId;
                }
            } else {
                // Stop the recording.
                stopRecordingAudio();
                toggleRecordButton.setText("Record");
                audioStatusText.setText("Recording saved:\n" + audioFilePath);
                playRecordingButton.setEnabled(true);
            }
        });

        playRecordingButton.setOnClickListener(v -> {
            if (audioFilePath != null) {
                playAudio(audioFilePath);
            } else {
                Toast.makeText(this, "No recording available", Toast.LENGTH_SHORT).show();
            }
        });

        closeAudioDialogButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecordingAudio();
            }
            audioDialog.dismiss();
        });

        audioDialog.show();
    }

    ////////////////////// Audio Recording and Playback Methods //////////////////////

    /**
     * Checks if RECORD_AUDIO permission is granted. If not, requests it.
     */
    private boolean checkAndRequestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION
            );
            return false;
        }
        return true;
    }

    /**
     * Starts audio recording for the given task.
     */
    private void startRecordingAudio(String taskId) {
        // Create a unique filename and store in external cache.
        String fileName = "housekeep_audio_" + userId + "_" + houseId + "_" + taskId + ".3gp";
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/" + fileName;

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to start recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Stops the audio recording if active.
     */
    private void stopRecordingAudio() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                Toast.makeText(this, "Recording saved:\n" + audioFilePath, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Plays the audio from the specified file path.
     */
    private void playAudio(String audioPath) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Playing audio...", Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                Toast.makeText(this, "Playback complete", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to play audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    ////////////////////// End of Audio Methods //////////////////////

    ////////////////////// TaskPhotoManager (unchanged) //////////////////////
    private class TaskPhotoManager {

        private ActivityResultLauncher<Uri> photoLauncher;
        private String pendingTaskId;
        private ImageView pendingImageView;
        private TextView pendingDescriptionView;

        public TaskPhotoManager() {
            photoLauncher = registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    result -> {
                        if (result) {
                            Toast.makeText(HomeTaskActivity.this, "Photo captured for task", Toast.LENGTH_SHORT).show();
                            if (pendingTaskId != null && pendingImageView != null) {
                                loadTaskPhoto(pendingTaskId, pendingImageView, pendingDescriptionView);
                            }
                        } else {
                            Toast.makeText(HomeTaskActivity.this, "Photo capture cancelled", Toast.LENGTH_SHORT).show();
                        }
                        pendingTaskId = null;
                        pendingImageView = null;
                        pendingDescriptionView = null;
                    }
            );
        }

        public void captureTaskPhoto(String taskId, ImageView imageView, TextView descriptionView) {
            pendingTaskId = taskId;
            pendingImageView = imageView;
            pendingDescriptionView = descriptionView;

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

        public void loadTaskPhoto(String taskId, ImageView imageView, TextView descriptionView) {
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

                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), contentUri);
                        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
                        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
                        labeler.process(inputImage)
                                .addOnSuccessListener(labels -> {
                                    ImageLabel bestLabel = null;
                                    for (ImageLabel label : labels) {
                                        if (bestLabel == null || label.getConfidence() > bestLabel.getConfidence()) {
                                            bestLabel = label;
                                        }
                                    }
                                    if (bestLabel != null) {
                                        descriptionView.setText(bestLabel.getText() + " (" + String.format("%.2f", bestLabel.getConfidence()) + ")");
                                    } else {
                                        descriptionView.setText("No objects detected");
                                    }
                                })
                                .addOnFailureListener(e -> descriptionView.setText("ML Kit error"));
                    } catch (Exception e) {
                        descriptionView.setText("Error processing image");
                    }
                } else {
                    imageView.setImageDrawable(null);
                }
            } catch (Exception e) {
                imageView.setImageDrawable(null);
            }
        }
    }

    ////////////////////// Reminder Methods (unchanged) //////////////////////
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

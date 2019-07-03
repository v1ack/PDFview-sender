package com.vlack.pdfview.sender;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Activity для выбора PDF-файла
 */
public class FilePickerActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_PATH = "file_path";
    private static final String TAG = "FilePickerActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;

    private FileManager fileManager;

    private FilesAdapter filesAdapter;
    /**
     * Listener события клика на файл
     */
    private final FilesAdapter.OnFileClickListener onFileClickListener = new FilesAdapter.OnFileClickListener() {
        @Override
        public void onFileClick(File file) {
            if (file.isDirectory()) {
                fileManager.navigateTo(file);
                updateFileList();
            } else {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_FILE_PATH, file.getAbsolutePath());
                setResult(RESULT_OK, intent);

                finish();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_file_picker);

        RecyclerView recyclerView = findViewById(R.id.files_rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addItemDecoration(new DividerItemDecoration(this, layoutManager.getOrientation()));

        filesAdapter = new FilesAdapter();
        recyclerView.setAdapter(filesAdapter);

        initFileManager();
    }

    @Override
    protected void onStart() {
        super.onStart();

        filesAdapter.setOnFileClickListener(onFileClickListener);
    }

    @Override
    protected void onStop() {
        filesAdapter.setOnFileClickListener(null);

        super.onStop();
    }

    /**
     * Нажата кнопка "Назад". Пытаемся подняться на директорию выше, если не получается — закрываем Activity
     */
    @Override
    public void onBackPressed() {
        if (fileManager != null && fileManager.navigateUp()) {
            updateFileList();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Этот метод будет вызван, когда пользователь предоставит разрешения, или откажет в них
     *
     * @param requestCode  Код, который мы передали при запросе
     * @param permissions  Список разрешений
     * @param grantResults Список результатов
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted!");

                initFileManager();
            } else {
                Log.i(TAG, "Permission denied");

                requestPermissions(); // Запрашиваем ещё раз
            }
        }
    }

    /**
     * Запрашиваем разрешение
     */
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE
        );
    }

    /**
     * Получаем список файлов и передаём в адаптер
     */
    private void updateFileList() {
        List<File> files = fileManager.getFiles();
        List<File> filtered_files = new ArrayList<>();
        for (File file : files) {
            String[] filename = file.getName().split("[.]");
            if (filename[filename.length - 1].toLowerCase().equals("pdf") || file.isDirectory()) {
                filtered_files.add(file);
            }
        }
        Collections.sort(filtered_files, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                return file.getName().compareToIgnoreCase(t1.getName());
            }
        });
        filesAdapter.setFiles(filtered_files);
        filesAdapter.notifyDataSetChanged();
    }

    /**
     * Инициализируем менеджер файлов, проверяем разрешение
     */
    private void initFileManager() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Разрешение предоставлено
            fileManager = new FileManager(this);
            updateFileList();
        } else {
            requestPermissions();
        }
    }
}

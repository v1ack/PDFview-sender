package com.vlack.pdfview.sender;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Activity для выбора PDF-файла
 */
public class FilePickerActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_PATH = "file_path";
    private static final HashSet<String> ALLOWED_FILES = new HashSet<>(Arrays.asList("pdf", "txt", "md"));

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

        fileManager = new FileManager(this);
        updateFileList();
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
     * Получаем список файлов и передаём в адаптер
     */
    private void updateFileList() {
        File[] files = fileManager.getFiles();
        List<File> filtered_files = new ArrayList<>();
        for (File file : files) {
            String[] filename = file.getName().split("[.]");
            String extension = filename[filename.length - 1].toLowerCase();
            if (ALLOWED_FILES.contains(extension) || file.isDirectory())
                filtered_files.add(file);
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
}

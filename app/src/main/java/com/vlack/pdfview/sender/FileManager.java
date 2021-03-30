package com.vlack.pdfview.sender;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.Objects;

/**
 * Менеджер файлов
 */
class FileManager {
    private static final String TAG = "FileManager";
    private final File rootDirectory;
    private File currentDirectory;

    FileManager(Context context) {
        File directory;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            directory = Environment.getExternalStorageDirectory();
        } else {
            directory = ContextCompat.getDataDir(context);
        }

        rootDirectory = directory;

        assert directory != null;
        navigateTo(directory);
    }

    /**
     * Пробуем перейти в указанную директорию
     *
     * @param directory Директория
     * @return true если удалось, false при ошибке
     */
    boolean navigateTo(File directory) {
        // Проверим, является ли файл директорией
        if (!directory.isDirectory()) {
            Log.e(TAG, directory.getAbsolutePath() + " is not a directory!");

            return false;
        }

        // Проверим, не поднялись ли мы выше rootDirectory
        if (!directory.equals(rootDirectory) &&
                rootDirectory.getAbsolutePath().contains(directory.getAbsolutePath())) {
            Log.w(TAG, "Trying to navigate upper than root directory to " + directory.getAbsolutePath());

            return false;
        }

        currentDirectory = directory;

        return true;
    }

    /**
     * Подняться на один уровень выше
     */
    boolean navigateUp() {
        return navigateTo(Objects.requireNonNull(currentDirectory.getParentFile()));
    }

    /**
     * Получаем список файлов в текущей директории
     */
    File[] getFiles() {
        return currentDirectory.listFiles();
    }
}

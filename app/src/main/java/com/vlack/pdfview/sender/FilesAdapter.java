package com.vlack.pdfview.sender;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения файлов
 */
public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {

    private static final int TYPE_DIRECTORY = 0;
    private static final int TYPE_FILE = 1;
    private Context mContext;

    @Nullable
    private OnFileClickListener onFileClickListener;
    private List<File> files = new ArrayList<>();

    void setOnFileClickListener(@Nullable OnFileClickListener onFileClickListener) {
        this.onFileClickListener = onFileClickListener;
    }

    void setFiles(List<File> files) {
        this.files = files;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        View view;

        if (viewType == TYPE_DIRECTORY) {
            view = layoutInflater.inflate(R.layout.view_item_directory, parent, false);
        } else {
            view = layoutInflater.inflate(R.layout.view_item_file, parent, false);
        }

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = files.get(position);

        holder.nameTv.setText(file.getName());
        if (file.isFile()) {

            holder.sizeTv.setText(Formatter.formatShortFileSize(mContext, file.length()));
        }
        holder.itemView.setTag(file);
    }


    @Override
    public int getItemCount() {
        return files.size();
    }

    @Override
    public int getItemViewType(int position) {
        File file = files.get(position);

        if (file.isDirectory()) {
            return TYPE_DIRECTORY;
        } else {
            return TYPE_FILE;
        }
    }

    /**
     * Listener кликов на файлы
     */
    public interface OnFileClickListener {
        void onFileClick(File file);
    }

    /**
     * View holder
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameTv;
        private final TextView sizeTv;

        ViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();

            nameTv = itemView.findViewById(R.id.name_tv);
            sizeTv = itemView.findViewById(R.id.size_tv);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    File file = (File) view.getTag();

                    if (onFileClickListener != null) {
                        onFileClickListener.onFileClick(file);
                    }
                }
            });
        }
    }
}

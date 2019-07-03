package com.vlack.pdfview.sender;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения файлов
 */
public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {

    private static final int TYPE_DIRECTORY = 0;
    private static final int TYPE_FILE = 1;

    @Nullable
    private OnFileClickListener onFileClickListener;
    private List<File> files = new ArrayList<>();

    public void setOnFileClickListener(@Nullable OnFileClickListener onFileClickListener) {
        this.onFileClickListener = onFileClickListener;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
    public void onBindViewHolder(ViewHolder holder, int position) {
        File file = files.get(position);

        holder.nameTv.setText(file.getName());
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

        public ViewHolder(View itemView) {
            super(itemView);

            nameTv = itemView.findViewById(R.id.name_tv);

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

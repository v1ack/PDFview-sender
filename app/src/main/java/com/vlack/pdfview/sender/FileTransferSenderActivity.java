package com.vlack.pdfview.sender;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.UpdateFrom;

import java.io.File;

public class FileTransferSenderActivity extends AppCompatActivity implements OnClickListener {
    private static final int FILE_MANAGER_CODE = 1;
    private static final int CHANGE_STATE = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_DISCONNECTED = 4;
    public static final int STATE_CONNECTED_SENDING = 5;
    public static final String PARAM_PINTENT = "pendingIntent";
    private static final String TAG = "FileTransferActivity";
    private Button buttonConnect, buttonCancel, buttonChooseFile;
    private ProgressBar sentProgressBar;
    private ImageView imageConnection;
    private Context mContext;
    private long currentTransId;

    private boolean mIsBound = false;
    private FileTransferSender mFTSender;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "Service disconnected");
            mFTSender = null;
            mIsBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Service connected");
            mFTSender = ((FileTransferSender.SenderBinder) service).getService();
            mFTSender.registerFileAction(getFileAction());
        }
    };

    AppUpdater appUpdater;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ft_sender_activity);

        appUpdater = new AppUpdater(this)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo("v1ack", "v1ack.github.io");
        appUpdater.start();

        mContext = getApplicationContext();
        buttonConnect = findViewById(R.id.connectButton);
        buttonConnect.setOnClickListener(this);
        buttonCancel = findViewById(R.id.cancel);
        buttonCancel.setOnClickListener(this);
        buttonChooseFile = findViewById(R.id.choiceFile);
        buttonChooseFile.setOnClickListener(this);
        imageConnection = findViewById(R.id.watchImage);
        sentProgressBar = findViewById(R.id.fileTransferProgressBar);
        sentProgressBar.setMax(100);

        initializeFT();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
            startActivity(
                    new Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            uri
                    )
            );

        }
    }

    protected void onDestroy() {
        destroyFT();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private static final int PICK_PDF_FILE = 3;

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");

        startActivityForResult(intent, PICK_PDF_FILE);
    }

    public void onClick(View v) {
        if (v.equals(buttonCancel)) {
            if (mIsBound) {
                try {
                    mFTSender.cancelFileTransfer((int) currentTransId);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, R.string.ILLEGAL_ARGUMENT, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(mContext, R.string.no_binding, Toast.LENGTH_SHORT).show();
            }
        } else if (v.equals(buttonConnect)) {
            if (mFTSender != null) {
                mFTSender.connect();
            } else {
                Toast.makeText(getApplicationContext(), R.string.not_bound, Toast.LENGTH_SHORT).show();
            }
        } else if (v.equals(buttonChooseFile)) {
            Intent intent = new Intent(this, FilePickerActivity.class);
            startActivityForResult(intent, FILE_MANAGER_CODE);
//            openFile();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultData == null) {
            return;
        }
        if (requestCode == FILE_MANAGER_CODE) {
            String path = resultData.getStringExtra("file_path");
            if (path.equals("null")) {
                return;
            }
            changeState(STATE_CONNECTED_SENDING);

            File file = new File(path);
            String mFileSize = Formatter.formatShortFileSize(mContext, file.length());
            Toast.makeText(mContext, getString(R.string.sending_file_toast, file.getName(), mFileSize), Toast.LENGTH_SHORT).show();
            if (mIsBound) {
                try {
                    currentTransId = mFTSender.sendFile(path);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, R.string.ILLEGAL_ARGUMENT, Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == CHANGE_STATE)
            changeState(resultCode);
    }

    private FileTransferSender.FileAction getFileAction() {
        return new FileTransferSender.FileAction() {
            @Override
            public void onFileActionError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sentProgressBar.setProgress(0);
                        currentTransId = -1;
                        Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_SHORT).show();
                        onActivityResult(CHANGE_STATE, STATE_DISCONNECTED, getIntent());
                    }
                });
            }

            @Override
            public void onFileActionCancel() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sentProgressBar.setProgress(0);
                        currentTransId = -1;
                        Toast.makeText(getApplicationContext(), R.string.sending_cancelled, Toast.LENGTH_SHORT).show();
                        changeState(STATE_CONNECTED);
                    }
                });
            }

            @Override
            public void onFileActionProgress(final long progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sentProgressBar.setProgress((int) progress);
                    }
                });
            }

            @Override
            public void onFileActionTransferComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sentProgressBar.setProgress(0);
                        currentTransId = -1;
                        Toast.makeText(getApplicationContext(), R.string.transfer_complete, Toast.LENGTH_SHORT).show();
                        changeState(STATE_CONNECTED);
                    }
                });
            }

            @Override
            public void onFileActionCancelAllComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sentProgressBar.setProgress(0);
                        currentTransId = -1;
                    }
                });
            }

        };
    }

    public void changeState(int state) {
        switch (state) {
            case STATE_CONNECTED: {
                imageConnection.setImageResource(R.drawable.ic_image_connected);
                buttonChooseFile.setVisibility(View.VISIBLE);
                buttonChooseFile.setEnabled(true);
                buttonConnect.setVisibility(View.GONE);
                buttonCancel.setVisibility(View.GONE);
                sentProgressBar.setVisibility(View.GONE);
                break;
            }
            case STATE_CONNECTED_SENDING: {
                buttonCancel.setVisibility(View.VISIBLE);
                sentProgressBar.setVisibility(View.VISIBLE);
                buttonChooseFile.setEnabled(false);
                break;
            }
            case STATE_DISCONNECTED: {
                imageConnection.setImageResource(R.drawable.ic_image_not_connected);
                buttonChooseFile.setVisibility(View.INVISIBLE);
                buttonCancel.setVisibility(View.GONE);
                sentProgressBar.setVisibility(View.GONE);
                buttonConnect.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    private void initializeFT() {
        currentTransId = -1;

        sentProgressBar = findViewById(R.id.fileTransferProgressBar);
        sentProgressBar.setMax(100);

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(getApplicationContext(), " No SDCARD Present", Toast.LENGTH_SHORT).show();
            finish();
        }

        PendingIntent pi = createPendingResult(CHANGE_STATE, this.getIntent(), 0);
        mIsBound = bindService(new Intent(getApplicationContext(), FileTransferSender.class).putExtra(PARAM_PINTENT, pi),
                this.mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void destroyFT() {
        currentTransId = -1;

        if (mIsBound && mFTSender != null) {
            mFTSender.stopRunningInForeground();
            unbindService(mServiceConnection);
        }
    }
}

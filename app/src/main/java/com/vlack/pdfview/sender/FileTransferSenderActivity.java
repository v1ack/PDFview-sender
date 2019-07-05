package com.vlack.pdfview.sender;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.vlack.pdfview.sender.FileTransferSender.FileAction;
import com.vlack.pdfview.sender.FileTransferSender.SenderBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileTransferSenderActivity extends AppCompatActivity implements OnClickListener {
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_DISCONNECTED = 4;
    public static final String PARAM_PINTENT = "pendingIntent";
    private static final String TAG = "FileTransferActivity";
    private static final int FILE_MANAGER_CODE = 1;
    private static final int CHANGE_STATE = 2;
    private Button mBtnConn, mBtnCancel, mBtnCancelAll, mBtnChoice;
    private ProgressBar mSentProgressBar;
    private ImageView mImgConnected;
    private Context mContext;
    private long currentTransId;
    private List<Long> mTransactions = new ArrayList<>();

    private boolean mIsBound = false;
    private FileTransferSender mSenderService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service disconnected");
            mSenderService = null;
            mIsBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            Log.d(TAG, "Service connected");
            mSenderService = ((SenderBinder) binder).getService();
            mSenderService.registerFileAction(getFileAction());
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ft_sender_activity);
        mContext = getApplicationContext();
        mBtnConn = findViewById(R.id.connectButton);
        mBtnConn.setOnClickListener(this);
        mBtnCancel = findViewById(R.id.cancel);
        mBtnCancel.setOnClickListener(this);
        mBtnCancelAll = findViewById(R.id.cancelAll);
        mBtnCancelAll.setOnClickListener(this);
        mBtnChoice = findViewById(R.id.choiceFile);
        mBtnChoice.setOnClickListener(this);
        mImgConnected = findViewById(R.id.connectedImg);
        PendingIntent pi = createPendingResult(CHANGE_STATE, this.getIntent(), 0);
        mIsBound = bindService(new Intent(getApplicationContext(), FileTransferSender.class).putExtra(PARAM_PINTENT, pi),
                this.mServiceConnection, Context.BIND_AUTO_CREATE);
        mSentProgressBar = findViewById(R.id.fileTransferProgressBar);
        mSentProgressBar.setMax(100);
    }

    protected void onDestroy() {
        if (mIsBound && mSenderService != null) {
            unbindService(mServiceConnection);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void onClick(View v) {
        if (v.equals(mBtnCancel)) {
            if (mIsBound) {
                try {
                    mSenderService.cancelFileTransfer((int) currentTransId);
                    mTransactions.remove(currentTransId);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, R.string.ILLEGAL_ARGUMENT, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(mContext, R.string.no_binding, Toast.LENGTH_SHORT).show();
            }
        } else if (v.equals(mBtnCancelAll)) {
            if (mSenderService != null) {
                mSenderService.cancelAllTransactions();
                mTransactions.clear();
            } else {
                Toast.makeText(mContext, R.string.no_binding, Toast.LENGTH_SHORT).show();
            }
        } else if (v.equals(mBtnConn)) {
            if (mSenderService != null) {
                mSenderService.connect();
            } else {
                Toast.makeText(getApplicationContext(), R.string.not_bound, Toast.LENGTH_SHORT).show();
            }
        } else if (v.equals(mBtnChoice)) {
            Intent intent = new Intent(this, FilePickerActivity.class);
            startActivityForResult(intent, FILE_MANAGER_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }
        if (requestCode == FILE_MANAGER_CODE) {
            String path = data.getStringExtra("file_path");
            if (path.equals("null")) {
                return;
            }
            mBtnCancel.setVisibility(View.VISIBLE);
            mBtnCancelAll.setVisibility(View.VISIBLE);
            mSentProgressBar.setVisibility(View.VISIBLE);

            File file = new File(path);
            String mFileSize = Formatter.formatShortFileSize(mContext, file.length());
            Toast.makeText(mContext, getString(R.string.sending_file_toast, file.getName(), mFileSize), Toast.LENGTH_SHORT).show();
            if (mIsBound) {
                try {
                    int trId = mSenderService.sendFile(path);
                    mTransactions.add((long) trId);
                    currentTransId = trId;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, R.string.ILLEGAL_ARGUMENT, Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == CHANGE_STATE) {
            if (resultCode == STATE_CONNECTED) {
                mImgConnected.setVisibility(View.VISIBLE);
                mBtnChoice.setVisibility(View.VISIBLE);
            } else if (resultCode == STATE_DISCONNECTED) {
                mImgConnected.setVisibility(View.INVISIBLE);
                mBtnChoice.setVisibility(View.INVISIBLE);
                mBtnCancel.setVisibility(View.GONE);
                mBtnCancelAll.setVisibility(View.GONE);
                mSentProgressBar.setVisibility(View.GONE);
            }
        }
    }

    private FileAction getFileAction() {
        return new FileAction() {
            @Override
            public void onFileActionError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress(0);
                        mTransactions.remove(currentTransId);
                        Toast.makeText(mContext, R.string.error, Toast.LENGTH_SHORT).show();
                        onActivityResult(CHANGE_STATE, STATE_DISCONNECTED, getIntent());
                    }
                });
            }

            @Override
            public void onFileActionProgress(final long progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress((int) progress);
                    }
                });
            }

            @Override
            public void onFileActionTransferComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress(0);
                        mTransactions.remove(currentTransId);
                        Toast.makeText(mContext, R.string.transfer_completed, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFileActionCancelAllComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress(0);
                        mTransactions.remove(currentTransId);
                    }
                });
            }
        };
    }

    public void changeState(boolean connected) {
        if (connected) {
            mImgConnected.setVisibility(View.VISIBLE);
        } else {
            mImgConnected.setVisibility(View.INVISIBLE);
        }
    }
}

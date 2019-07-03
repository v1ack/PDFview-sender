package com.vlack.pdfview.sender;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;
import com.samsung.android.sdk.accessoryfiletransfer.SAFileTransfer;
import com.samsung.android.sdk.accessoryfiletransfer.SAFileTransfer.EventListener;
import com.samsung.android.sdk.accessoryfiletransfer.SAft;

public class FileTransferSender extends SAAgent {
    private static final String TAG = "FileTransferSender";
    private static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
    private final IBinder mSenderBinder = new SenderBinder();
    public PendingIntent pi;
    private int trId = -1;
    private int errCode = SAFileTransfer.ERROR_NONE;
    private SAPeerAgent mPeerAgent = null;
    private SAFileTransfer mSAFileTransfer = null;
    private EventListener mCallback = null;
    private FileAction mFileAction = null;

    public FileTransferSender() {
        super(TAG, SASOCKET_CLASS);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "On Create of Sample FileTransferSender Service");
        mCallback = new EventListener() {
            @Override
            public void onProgressChanged(int transId, int progress) {
                Log.d(TAG, "onProgressChanged : " + progress + " for transaction : " + transId);
                if (mFileAction != null) {
                    mFileAction.onFileActionProgress(progress);
                }
            }

            @Override
            public void onTransferCompleted(int transId, String fileName, int errorCode) {
                errCode = errorCode;
                Log.d(TAG, "onTransferCompleted: tr id : " + transId + " file name : " + fileName + " error : "
                        + errorCode);
                if (errorCode == SAFileTransfer.ERROR_NONE) {
                    mFileAction.onFileActionTransferComplete();
                } else {
                    mFileAction.onFileActionError();
                }
            }

            @Override
            public void onTransferRequested(int id, String fileName) {
                // No use at sender side
            }

            @Override
            public void onCancelAllCompleted(int errorCode) {
                if (errorCode == SAFileTransfer.ERROR_NONE) {
                    mFileAction.onFileActionCancelAllComplete();
                } else if (errorCode == SAFileTransfer.ERROR_TRANSACTION_NOT_FOUND) {
                    Toast.makeText(getBaseContext(), R.string.onCancelAllCompletedNotFound, Toast.LENGTH_SHORT).show();
                } else if (errorCode == SAFileTransfer.ERROR_NOT_SUPPORTED) {
                    Toast.makeText(getBaseContext(), R.string.onCancelAllCompletedNotSupported, Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "onCancelAllCompleted: Error Code " + errorCode);
            }
        };
        SAft saft = new SAft();
        try {
            saft.initialize(this);
        } catch (SsdkUnsupportedException e) {
            if (e.getType() == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
                Toast.makeText(getBaseContext(), R.string.DEVICE_NOT_SUPPORTED, Toast.LENGTH_SHORT).show();
            } else if (e.getType() == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
                Toast.makeText(getBaseContext(), R.string.LIBRARY_NOT_INSTALLED, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getBaseContext(), R.string.UNKNOWN_ERROR, Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
            return;
        } catch (Exception e1) {
            Toast.makeText(getBaseContext(), R.string.CANNOT_INIT, Toast.LENGTH_SHORT).show();
            e1.printStackTrace();
            return;
        }
        mSAFileTransfer = new SAFileTransfer(FileTransferSender.this, mCallback);
    }

    @Override
    public IBinder onBind(Intent intent) {
        pi = intent.getParcelableExtra(FileTransferSenderActivity.PARAM_PINTENT);
        return mSenderBinder;
    }

    @Override
    public void onDestroy() {
        try {
            mSAFileTransfer.close();
            mSAFileTransfer = null;
        } catch (RuntimeException e) {
            Log.e(TAG, e.getMessage());
        }
        super.onDestroy();
        Log.i(TAG, "FileTransferSender Service is Stopped.");
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        if (peerAgents != null) {
            for (SAPeerAgent peerAgent : peerAgents)
                mPeerAgent = peerAgent;
        } else {
            Log.e(TAG, "No peer Aget found:" + result);
            Toast.makeText(getBaseContext(), R.string.NO_AGENT_FOUND, Toast.LENGTH_SHORT).show();
            sendState(false);
        }
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
        Log.d(TAG, "Peer agent updated- result: " + result + " trId: " + trId);
        for (SAPeerAgent peerAgent : peerAgents)
            mPeerAgent = peerAgent;
        if (result == SAAgent.PEER_AGENT_UNAVAILABLE) {
            if (errCode != SAFileTransfer.ERROR_CONNECTION_LOST) {
                try {
                    cancelFileTransfer(trId);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Toast.makeText(getBaseContext(), R.string.ILLEGAL_ARGUMENT, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        Log.i(TAG, "onServiceConnectionResponse: result - " + result);
        if (socket == null) {
            if (result == SAAgent.CONNECTION_ALREADY_EXIST) {
                Toast.makeText(getBaseContext(), R.string.CONNECTION_ALREADY_EXIST, Toast.LENGTH_SHORT).show();
                sendState(true);
            } else {
                Toast.makeText(getBaseContext(), R.string.CONNECTION_NOT_MADE, Toast.LENGTH_SHORT).show();
                sendState(false);
            }
        } else {
            Toast.makeText(getBaseContext(), R.string.CONNECTION_OK, Toast.LENGTH_SHORT).show();
            sendState(true);
        }
    }

    public void connect() {
        if (mPeerAgent != null) {
            requestServiceConnection(mPeerAgent);
        } else {
            super.findPeerAgents();
            Toast.makeText(getBaseContext(), R.string.NO_PEER_AGENT_FOUND, Toast.LENGTH_SHORT).show();
            sendState(false);
        }
    }

    public int sendFile(String mSelectedFileName) {
        if (mSAFileTransfer != null && mPeerAgent != null) {
            trId = mSAFileTransfer.send(mPeerAgent, mSelectedFileName);
            return trId;
        } else {
            Toast.makeText(getBaseContext(), R.string.NO_PEER_FOUND_ON_SEND, Toast.LENGTH_SHORT).show();
            sendState(false);
            findPeerAgents();
            return -1;
        }
    }

    public void cancelFileTransfer(int transId) {
        if (mSAFileTransfer != null) {
            mSAFileTransfer.cancel(transId);
        }
    }

    public void cancelAllTransactions() {
        if (mSAFileTransfer != null) {
            mSAFileTransfer.cancelAll();
        }
    }

    public void registerFileAction(FileAction action) {
        this.mFileAction = action;
    }

    private void sendState(boolean connected) {
        try {
            if (connected) {
                pi.send(FileTransferSenderActivity.STATE_CONNECTED);
            } else {
                pi.send(FileTransferSenderActivity.STATE_DISCONNECTED);
            }
        } catch (PendingIntent.CanceledException w) {
            Log.i(TAG, w.toString());
        }
    }

    public interface FileAction {
        void onFileActionError();

        void onFileActionProgress(long progress);

        void onFileActionTransferComplete();

        void onFileActionCancelAllComplete();
    }

    public class SenderBinder extends Binder {
        public FileTransferSender getService() {
            return FileTransferSender.this;
        }
    }

    public class ServiceConnection extends SASocket {
        public ServiceConnection() {
            super(ServiceConnection.class.getName());
        }

        @Override
        protected void onServiceConnectionLost(int reason) {
            Log.e(TAG, "onServiceConnectionLost: reason-" + reason);
            if (mSAFileTransfer != null) {
                mFileAction.onFileActionError();
            }
            mPeerAgent = null;
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
        }
    }
}

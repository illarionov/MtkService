package ru0xdc.mtk.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Created by alexey on 28.05.14.
 */
public class MtkService extends Service {
    private static final String TAG = "MtkService";

    static final int ID_GET_FLAG = 1;
    static final int ID_GET_FLAG_RESPONSE = 2;
    static final int ID_TURN_ON_CELL_STATUS_RESPONSE = 3;
    static final int ID_REQUEST_TURN_OFF_CELL_STATUS = 4;
    static final int ID_TURN_OFF_CELL_STATUS_RESPONSE = 5;
    static final int ID_TIMEOUT = 6;
    static final int ID_REQUEST_RESULT_RECEIVED = 7;


    public static final int UMTS_CELL_STATUS_INDEX_OLD = 47;
    public static final int UMTS_CELL_STATUS_INDEX = 90;
    private static final int FLAG_OFFSET_BIT = 0x08;
    private static final int FLAG_OR_DATA = 0xf7;

    private Phone mPhone;

    private HandlerThread mHandlerThread;
    private RequestHandler mRequestHandler;
    private Handler mNetworkInfoHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mPhone = PhoneFactory.getDefaultPhone();
        mHandlerThread = new HandlerThread("HandlerThread");
        mHandlerThread.start();

        mRequestHandler = new RequestHandler(mHandlerThread.getLooper());
        mNetworkInfoHandler = new Handler(mHandlerThread.getLooper(), mNetworkInfoResponseHandler);

        try {
            mPhone.getClass()
                    .getMethod("registerForNetworkInfo", Handler.class, int.class, Object.class)
                    .invoke(mPhone, mNetworkInfoHandler, ID_TURN_ON_CELL_STATUS_RESPONSE, null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mPhone.getClass()
                    .getMethod("unregisterForNetworkInfo", Handler.class)
                    .invoke(mPhone, mNetworkInfoHandler);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        mHandlerThread.quit();
        mHandlerThread = null;
        mRequestHandler = null;
        mNetworkInfoHandler = null;
        mPhone = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IMtkServiceMode.Stub mBinder = new IMtkServiceMode.Stub() {

        @Override
        public CsceEMServCellSStatusInd getCsceEMServCellSStatusInd() throws RemoteException {
            mRequestHandler.startRequest();
            return mRequestHandler.waitResult();
        }
    };

    public final Handler.Callback mNetworkInfoResponseHandler = new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            String data[];
            AsyncResult ar;
            switch (msg.what) {
                case ID_TURN_ON_CELL_STATUS_RESPONSE:
                    ar = (AsyncResult) msg.obj;
                    data = (String[]) ar.result;
                    int type = Integer.valueOf(data[0]);
                    switch (type) {
                        case UMTS_CELL_STATUS_INDEX_OLD:
                            synchronized (MtkService.this) {
                                CsceEMServCellSStatusInd r = new CsceEMServCellSStatusInd(data[1]);
                                Log.v(TAG, "UMTS_CELL_STATUS_INDEX_OLD: " + r);
                                mRequestHandler.onResult(r);
                            }
                            break;
                        default:
                            Log.v(TAG, "EVENT_NW_INFO data: " + Arrays.toString(data));
                            break;
                    }
                    break;
                default:
                    ar = (AsyncResult) msg.obj;
                    data = (String[]) ar.result;
                    Log.v(TAG, "msg" + msg.what + " data: " + Arrays.toString(data));
                    break;
            }
            return false;
        }
    };

    public class RequestHandler extends Handler {
        private int mFlag;

        private volatile CsceEMServCellSStatusInd mCellsStatusInd;

        private final ConditionVariable mRequestCondvar = new ConditionVariable();

        private volatile boolean mWaitResult;

        public RequestHandler(Looper looper) {
            super(looper);
        }

        public void startRequest() {
            synchronized (this) {
                if (!mWaitResult) {
                    mCellsStatusInd = null;
                    this.obtainMessage(ID_GET_FLAG).sendToTarget();
                }
                mRequestCondvar.close();
            }
        }

        public CsceEMServCellSStatusInd waitResult() {
            mRequestCondvar.block();
            return mCellsStatusInd;
        }

        void onResult(CsceEMServCellSStatusInd result) {
            this.obtainMessage(ID_REQUEST_RESULT_RECEIVED, result).sendToTarget();
        }

        private void onError() {
            synchronized (this) {
                this.removeMessages(ID_TIMEOUT);
                mWaitResult = false;
                mRequestCondvar.open();
            }
        }

        private void sendATCommand(String[] atCommand, int msg) {
            mPhone.invokeOemRilRequestStrings(atCommand, obtainMessage(msg));
        }

        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;

            switch (msg.what) {
                case ID_GET_FLAG:
                    sendATCommand(new String[]{"AT+EINFO?", "+EINFO"}, ID_GET_FLAG_RESPONSE);
                    this.sendMessageDelayed(this.obtainMessage(ID_TIMEOUT), 5000);
                    break;
                case ID_GET_FLAG_RESPONSE:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        String data[] = (String[]) ar.result;
                        Log.v(TAG, Arrays.toString(data));

                        mFlag = Integer.valueOf(data[0].substring(8));
                        mFlag = mFlag | FLAG_OFFSET_BIT;
                        String[] atCommand = {"AT+EINFO=" + mFlag + "," + UMTS_CELL_STATUS_INDEX_OLD + ",0", "+EINFO"};
                        sendATCommand(atCommand, ID_TURN_ON_CELL_STATUS_RESPONSE);
                    } else {
                        Log.e(TAG, "e", ar.exception);
                        onError();
                    }
                    break;
                case ID_TURN_ON_CELL_STATUS_RESPONSE:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        onError();
                    }
                    // Wait for result
                    break;
                case ID_REQUEST_RESULT_RECEIVED:
                    synchronized (this) {
                        mCellsStatusInd = (CsceEMServCellSStatusInd) msg.obj;
                        this.obtainMessage(ID_REQUEST_TURN_OFF_CELL_STATUS).sendToTarget();
                    }
                    break;
                case ID_REQUEST_TURN_OFF_CELL_STATUS:
                    mFlag = mFlag & FLAG_OR_DATA;
                    String[] atCommandturnOffCellStatus = {"AT+EINFO=" + mFlag, ""};
                    sendATCommand(atCommandturnOffCellStatus, ID_TURN_OFF_CELL_STATUS_RESPONSE);
                    break;
                case ID_TURN_OFF_CELL_STATUS_RESPONSE:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.e(TAG, "e", ar.exception);
                        onError();
                    } else {
                        String data[] = (String[]) ar.result;
                        Log.v(TAG, Arrays.toString(data));
                        synchronized (this) {
                            mWaitResult = false;
                            mRequestCondvar.open();
                        }
                    }
                    break;
                case ID_TIMEOUT:
                    onError();
                    break;
                default:
                    break;
            }
        }
    }
}

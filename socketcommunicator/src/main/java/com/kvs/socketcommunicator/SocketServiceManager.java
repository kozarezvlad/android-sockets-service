package com.kvs.socketcommunicator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import static android.content.Context.BIND_AUTO_CREATE;
import static com.kvs.socketcommunicator.SocketConnectionErrors.SERVER_CONNECTION_ERROR;

/**
 * Created by KVS on 23.01.17.
 */

public final class SocketServiceManager<Response, Request> implements ClientSocketService.ClientSocketServiceCallbacks {

    private ISocketServiceListener<Response> socketServiceListener;
    private ClientSocketService socketService;

    private Context context;
    private int serverPort;
    private String serverIP;
    private boolean isBounded;

    //Handler messages
    private final int MESSAGE_CONNECTED = 110;
    private final int MESSAGE_DISCONNECTED = 111;
    private final int MESSAGE_DATA_RECEIVED = 112;
    private final int MESSAGE_CONNECTION = 113;
    private final int MESSAGE_FAILED = 114;
    private final int MESSAGE_SUCCESSFUL_REQUEST = 115;

    //create manager as singleton in application class
    public SocketServiceManager(Context context, int serverPort, String serverIP, Class<Response> responseClass) {
        this.context = context;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        Intent intentService = new Intent(context, ClientSocketService.class);
        intentService.putExtra(ClientSocketService.INTENT_IP, serverIP);
        intentService.putExtra(ClientSocketService.INTENT_PORT, serverPort);
        intentService.putExtra(ClientSocketService.INTENT_CLAZZ, responseClass);
        context.startService(intentService);
    }

    /*******
     * Required calls
     *******/
    //bind with Activity lifecycle onStart();
    public void bindService(Context context) {
        if (!isBounded) {
            Intent intentService = new Intent(context, ClientSocketService.class);
            context.bindService(intentService, mConnection, BIND_AUTO_CREATE);
        }
    }

    //unbind with Activity onStop();
    public void unbindService(Context context) {
        if (isBounded) {
            context.unbindService(mConnection);
            isBounded = false;
        }
    }

    public void setSocketServiceListener(ISocketServiceListener<Response> socketServiceListener) {
        this.socketServiceListener = socketServiceListener;
    }

    /***************************/

    public void request(Request data) {
        if (socketService != null && isBounded) {
            socketService.request(data);
        } else {
            socketServiceListener.failed(SERVER_CONNECTION_ERROR);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            isBounded = false;
            socketService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            isBounded = true;
            ClientSocketService.ClientSocketBinder clientSocketBinder = (ClientSocketService.ClientSocketBinder) service;
            socketService = clientSocketBinder.getServiceInstance();
            socketService.setClientSocketServiceListener(SocketServiceManager.this);
        }
    };


    private Handler serviceCallbackHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_CONNECTED:
                    socketServiceListener.connectionEstablished();
                    break;
                case MESSAGE_DISCONNECTED:
                    socketServiceListener.disconnected();
                    break;
                case MESSAGE_DATA_RECEIVED:
                    socketServiceListener.dataReceived((Response) msg.obj);
                    break;
                case MESSAGE_CONNECTION:
                    socketServiceListener.connection();
                    break;
                case MESSAGE_FAILED:
                    socketServiceListener.failed((SocketConnectionErrors) msg.obj);
                    break;
                case MESSAGE_SUCCESSFUL_REQUEST:
                    socketServiceListener.successfulRequest();
                    break;
            }
        }
    };

    @Override
    public void connectionEstablished() {
        serviceCallbackHandler.sendEmptyMessage(MESSAGE_CONNECTED);
    }

    @Override
    public void disconnected() {
        serviceCallbackHandler.sendEmptyMessage(MESSAGE_DISCONNECTED);
    }

    @Override
    public void connection() {
        serviceCallbackHandler.sendEmptyMessage(MESSAGE_CONNECTION);
    }

    @Override
    public void dataReceived(Object data) {
        Message message = new Message();
        message.what = MESSAGE_DATA_RECEIVED;
        message.obj = data;
        serviceCallbackHandler.sendMessage(message);
    }

    @Override
    public void requestSuccessful() {
        serviceCallbackHandler.sendEmptyMessage(MESSAGE_SUCCESSFUL_REQUEST);
    }

    @Override
    public void failed(SocketConnectionErrors error) {
        Message message = new Message();
        message.what = MESSAGE_FAILED;
        message.obj = error;
        serviceCallbackHandler.sendMessage(message);
    }


//    private boolean isServiceRunning(Class<?> serviceClass, Activity activity) {
//        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (serviceClass.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }
}

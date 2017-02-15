package com.kvs.socketcommunicator;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by KVS on 19.01.17.
 */

public final class ClientSocketService<Response, Request> extends Service implements SocketConnectionState.SocketConnectionListener {

    private static final String TAG = ClientSocketService.class.getSimpleName();
    public static final String INTENT_PORT = "INTENT_PORT";
    public static final String INTENT_CLAZZ = "INTENT_CLAZZ";
    public static final String INTENT_IP = "INTENT_IP";

    private Socket socket;
    private SocketAddress socketAddress;
    private BufferedReader input;
    private Thread socketStartingThread;
    private SocketConnectionState connectionState;
    private IBinder mBinder = new ClientSocketBinder();
    private boolean isBinded = false;

    private Class<Response> clazz;
    private boolean isConnected = false;
    private boolean isDestroyed;
    private Gson gson;
    private int serverPort;
    private String serverIp; //"10.0.3.2"
    private ExecutorService executorService = Executors.newSingleThreadExecutor(); //for request one by one

    interface ClientSocketServiceCallbacks<Response> {
        void connectionEstablished();

        void disconnected();

        void connection();

        void dataReceived(Response data);

        void requestSuccessful();

        void failed(SocketConnectionErrors error);
    }

    private ClientSocketServiceCallbacks<Response> clientSocketServiceListener;

    @Override
    public void onCreate() {
        super.onCreate();
        this.gson = new Gson();
        socket = new Socket();
        isDestroyed = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.serverPort = intent.getIntExtra(INTENT_PORT, 0);
        this.serverIp = intent.getStringExtra(INTENT_IP);
        clazz = (Class<Response>) intent.getSerializableExtra(INTENT_CLAZZ);
        socketAddress = new InetSocketAddress(serverIp, serverPort);
        connectionState = new SocketConnectionState();
        connectionState.setListener(this);
        socketStartingThread = new Thread(new SocketStartingThread());
        socketStartingThread.start();
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        socketStartingThread = null;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        isBinded = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        isBinded = true;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        isBinded = false;
        return true;
    }

    public class ClientSocketBinder extends Binder {
        ClientSocketService getServiceInstance() {
            return ClientSocketService.this;
        }
    }

    public void setClientSocketServiceListener(ClientSocketServiceCallbacks<Response> listener) {
        this.clientSocketServiceListener = listener;
    }

    public void request(Request data) {
//        for (int i = 0; i < 20; i++) {
//            executorService.execute(new RequestThread(data, i));
//        }
        executorService.execute(new RequestThread(data, 0));
    }

    private class SocketStartingThread implements Runnable {
        @Override
        public void run() {
            while (!isConnected && !isDestroyed) {
                try {
                    Thread.sleep(500); //try to connect every 500 millis
                    connectionState.setState(SocketConnectionState.States.CONNECTION);
                    socket.connect(socketAddress);
                    connectionState.setState(SocketConnectionState.States.CONNECTED);
                    isConnected = true;
                    new Thread(new CommunicationThread()).start();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    socket = new Socket();
                    isConnected = false;
//                    if (clientSocketServiceListener != null)
//                        clientSocketServiceListener.failed(SocketConnectionErrors.SERVER_CONNECTION_ERROR);
//                    if (e1.getMessage().equals("Already connected")) {
//                        socket = new Socket();
//                        isConnected = false;
//                    }
//                    if (e1.getMessage().equals("Socket is closed") || e1.getMessage().equals("Socket closed")) {
//                        socket = new Socket();
//                        isConnected = false;
//                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private class CommunicationThread implements Runnable {

        @Override
        public void run() {
            try {
                if (connectionState.getState() == SocketConnectionState.States.CONNECTED) {
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    while (!isDestroyed && isConnected) {
                        String message = input.readLine();
                        if (message != null) {
                            Response response = gson.fromJson(message, clazz);
//                            if (!isBinded)
//                                NotificationBuilder.showNotification(getApplicationContext(), message);
                            if (clientSocketServiceListener != null)
                                clientSocketServiceListener.dataReceived(response);
                        } else if (socket.getInputStream().read() == -1) {
                            socket.close();
                            connectionState.setState(SocketConnectionState.States.DISCONNECTED);
                        }
                    }
                } else {
                    connectionState.setState(SocketConnectionState.States.DISCONNECTED);
                }
            } catch (IOException e) {
                e.printStackTrace();
                connectionState.setState(SocketConnectionState.States.DISCONNECTED);
            }
        }
    }

    private class RequestThread implements Runnable {

        private Request data;
        private int id;

        public RequestThread(Request data, int id) {
            this.data = data;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                if (connectionState.getState() == SocketConnectionState.States.CONNECTED) {
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeBytes(gson.toJson(data));
                    outputStream.flush();
                    Log.e(TAG, "thread with id: " + id + " complete");
                    if (clientSocketServiceListener != null)
                        clientSocketServiceListener.requestSuccessful();
                } else {
                    if (clientSocketServiceListener != null)
                        clientSocketServiceListener.failed(SocketConnectionErrors.REQUEST_FAILED_CONNECTION_ERROR);
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (clientSocketServiceListener != null)
                    clientSocketServiceListener.failed(SocketConnectionErrors.REQUEST_FAILED);
            }
        }
    }

    @Override
    public void connectionStateChanged(SocketConnectionState.States state) {
        switch (state) {
            case CONNECTED:
                if (clientSocketServiceListener != null)
                    clientSocketServiceListener.connectionEstablished();
                break;
            case DISCONNECTED:
                if (clientSocketServiceListener != null)
                    clientSocketServiceListener.disconnected();
                isConnected = false;
                socketStartingThread = new Thread(new SocketStartingThread());
                socketStartingThread.start(); //try to connect until service alive
                break;
            case CONNECTION:
                if (clientSocketServiceListener != null)
                    clientSocketServiceListener.connection();
                break;
        }
    }

}

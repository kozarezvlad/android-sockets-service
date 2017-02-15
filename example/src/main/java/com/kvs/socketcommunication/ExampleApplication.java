package com.kvs.socketcommunication;

import android.app.Application;

import com.kvs.socketcommunication.entity.BaseResponse;
import com.kvs.socketcommunicator.SocketServiceManager;

/**
 * Created by kvs on 15.02.17.
 */

public class ExampleApplication extends Application {

    private SocketServiceManager socketServiceManager;

    private final int PORT = 9090; //test localhost port
    private final String SERVER_IP = "10.0.3.2"; //for testing on Genymotion emulator

    @Override
    public void onCreate() {
        super.onCreate();

        socketServiceManager = new SocketServiceManager(this, PORT, SERVER_IP, BaseResponse.class);
    }

    public SocketServiceManager getSocketServiceManager() {
        return socketServiceManager;
    }
}

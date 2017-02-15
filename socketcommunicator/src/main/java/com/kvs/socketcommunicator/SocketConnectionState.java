package com.kvs.socketcommunicator;

/**
 * Created by KVS on 25.01.17.
 */

public class SocketConnectionState {

    enum States {
        CONNECTED, DISCONNECTED, CONNECTION
    }

    interface SocketConnectionListener {
        void connectionStateChanged(States state);
    }

    private SocketConnectionListener listener;
    private States state = States.DISCONNECTED;

    public void setState(States state) {
        this.state = state;
        listener.connectionStateChanged(state);
    }

    public States getState() {
        return state;
    }

    public void setListener(SocketConnectionListener listener) {
        this.listener = listener;
    }
}

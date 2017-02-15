package com.kvs.socketcommunicator;

/**
 * Created by KVS on 23.01.17.
 */

public interface ISocketServiceListener<T> {

    void connectionEstablished();

    void disconnected();

    void dataReceived(T data);

    void failed(SocketConnectionErrors error);

    void successfulRequest();

    void connection();

}

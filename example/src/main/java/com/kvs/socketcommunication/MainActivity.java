package com.kvs.socketcommunication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.kvs.socketcommunication.fragments.DataFragment;
import com.kvs.socketcommunicator.SocketServiceManager;

public class MainActivity extends AppCompatActivity {

    public SocketServiceManager socketServiceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        socketServiceManager = ((ExampleApplication) getApplication()).getSocketServiceManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        getSupportFragmentManager().beginTransaction().
                replace(R.id.activity_main_container, DataFragment.newInstance()).commit();
    }

    @Override
    public void onStart() {
        super.onStart();
        socketServiceManager.bindService(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        socketServiceManager.unbindService(this);
    }

    public SocketServiceManager getSocketServiceManager() {
        return socketServiceManager;
    }
}

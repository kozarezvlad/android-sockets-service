package com.kvs.socketcommunication.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kvs.socketcommunication.MainActivity;
import com.kvs.socketcommunication.R;
import com.kvs.socketcommunication.entity.BaseRequest;
import com.kvs.socketcommunicator.ISocketServiceListener;
import com.kvs.socketcommunicator.SocketConnectionErrors;

/**
 * Created by kvs on 15.02.17.
 */

public class DataFragment extends Fragment implements ISocketServiceListener, View.OnClickListener {

    private TextView txtReceivedData;
    private Button sendRequestButton;

    public static DataFragment newInstance() {
        return new DataFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_data_list_layout, null);
        txtReceivedData = (TextView) v.findViewById(R.id.text_received_data);
        sendRequestButton = (Button) v.findViewById(R.id.btn_send_request);
        sendRequestButton.setOnClickListener(this);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send_request:
                BaseRequest testRequest = new BaseRequest();
                ((MainActivity)getActivity()).getSocketServiceManager().request(testRequest);
                break;
        }
    }

    //Socket connection callbacks
    @Override
    public void connectionEstablished() {

    }

    @Override
    public void disconnected() {

    }

    @Override
    public void dataReceived(Object data) {
        txtReceivedData.append(data.toString());
    }

    @Override
    public void failed(SocketConnectionErrors error) {

    }

    @Override
    public void successfulRequest() {
        Toast.makeText(getActivity(), "Successfully request", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void connection() {

    }
}

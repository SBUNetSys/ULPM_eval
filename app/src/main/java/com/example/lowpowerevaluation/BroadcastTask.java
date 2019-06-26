package com.example.lowpowerevaluation;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

class BroadcastTask extends AsyncTask<String, Void, Void> {
    private static final String TAG = "BroadcastTask";

    protected Void doInBackground(String... params) {
        Socket socket = null;
        DataOutputStream dataOutputStream = null;

        try {
            InetAddress serverAddr = InetAddress.getByName("192.168.1.6");
            socket = new Socket(serverAddr, 3000);
            socket.setTcpNoDelay(true);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            StringBuilder msg = new StringBuilder();
            for (String param : params) {
                msg.append(param);
                msg.append("#");
            }
            dataOutputStream.writeUTF(msg.toString());
            Log.d(TAG, msg.toString());
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
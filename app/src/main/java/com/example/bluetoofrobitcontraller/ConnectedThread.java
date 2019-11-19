package com.example.bluetoofrobitcontraller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;

class ConnectedThread extends Thread {
    private MainActivity mainActivity;
    private final BluetoothSocket mmSocket;
    private final OutputStream mmOutStream;
    private boolean registered = false;

    public ConnectedThread(MainActivity mainActivity, BluetoothSocket socket) {
        this.mainActivity = mainActivity;
        mmSocket = socket;
        OutputStream tmpOut = null;
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmOutStream = tmpOut;
    }

    public void write(String message) {
        byte[] msgBuffer = message.getBytes();
        try {
            mmOutStream.write(msgBuffer);
        } catch (IOException e) {
            Log.e("write()","Error writing: " + e.getMessage());
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
            mainActivity.showControlButtons("devices");
            Toast.makeText(mainActivity.getApplicationContext(), "Successfully disconnected", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("cancel()","Error disconnecting: " + e.getMessage());
        }
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Intent intent1 = new Intent(mainActivity, MainActivity.class);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if(registered) {
                            mainActivity.unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        mainActivity.startActivity(intent1);
                        mainActivity.finish();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if(registered) {
                            mainActivity.unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        mainActivity.startActivity(intent1);
                        mainActivity.finish();
                        break;
                }
            }
        }
    };
}

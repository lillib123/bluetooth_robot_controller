package com.example.bluetoofrobitcontraller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String DEVICES = "devices";
    public static final String CONTROLLER = "controller";
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mBluetoothSocket;
    ConnectedThread connectedThread;
    private Handler mHandler;
    private static final int BT_ACTIVATE_REQUEST = 1;
    private static final int MESSAGE_READ = 3;
    UUID SERIAL_UUID;
    private ListView list;
    private Button connectButton;
    private Button goButton;
    private Button leftButton;
    private Button rightButton;
    private Button downButton;
    private Button stopButton;
    private Button disconnectButton;
    String selectedDeviceAddress;
    boolean connection = false;
    private boolean registered = false;
    Vibrator v2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        v2 = (Vibrator) getSystemService(MainActivity.VIBRATOR_SERVICE);


        list = (ListView) findViewById(R.id.list);
        connectButton = (Button) findViewById(R.id.button_connect);
        goButton = (Button) findViewById(R.id.button_go);
        rightButton = (Button) findViewById(R.id.button_right);
        leftButton = (Button) findViewById(R.id.button_left);
        downButton = (Button) findViewById(R.id.button_down);
        stopButton = (Button) findViewById(R.id.button_stop);
        disconnectButton = (Button) findViewById(R.id.button_disconnect);

        //prompt to turn bluetooth on if it is off
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        turnBluetoothOn();

        //get list of connected devices
        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        final HashMap nameToAddress = new HashMap();

        List<String> deviceNameList = new ArrayList<>();

        for(BluetoothDevice device : pairedDevices) {
            deviceNameList.add(device.getName());
            nameToAddress.put(device.getName(), device.getAddress());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNameList);


        // Assign adapter to ListView
        list.setAdapter(adapter);

        // ListView Item Click Listener
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // ListView Clicked item index
                int itemPosition = position;

                // store clicked address
                String  itemValue = (String) list.getItemAtPosition(position);
                selectedDeviceAddress = nameToAddress.get(itemValue).toString();
            }

        });

        connectButton.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                v2.vibrate(60);
                if (connection) {
                    try {
                        mBluetoothSocket.close();
                        connection = false;
                        Toast.makeText(getApplicationContext(), "already connected to a device", Toast.LENGTH_LONG).show();

                    } catch (IOException error) {
                        Toast.makeText(getApplicationContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        //do the connecting
                        if (mBluetoothAdapter.isEnabled()) {
                            if (selectedDeviceAddress != null) {
                                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(selectedDeviceAddress);

//                                UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // bluetooth serial port service
                                SERIAL_UUID = device.getUuids()[0].getUuid(); //if you don't know the UUID of the bluetooth device service, you can get it like this from android cache

                                BluetoothSocket socket = null;

                                try {
                                    socket = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
                                    connectedThread = new ConnectedThread(socket);
                                } catch (Exception e) {
                                    Log.e("","Error creating socket");
                                }

                                try {
                                    socket.connect();
                                    Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                                    Log.e("","Connected");
                                    showControlButtons(CONTROLLER);
                                } catch (IOException e) {
                                    Log.e("",e.getMessage());
                                    try {
                                        Log.e("","trying fallback...");
                                        socket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                                        socket.connect();
                                        Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                                        Log.e("","Connected using fallback method");
                                        showControlButtons(CONTROLLER);
                                    } catch (Exception e2) {
                                        Toast.makeText(getApplicationContext(), "Cannot connect to this device", Toast.LENGTH_LONG).show();
                                        Log.e("", "Couldn't establish Bluetooth connection!");
                                    }
                                }
                            } else {
                                Log.e("","BT device not selected");
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

            }
        });

        goButton.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectedThread.write("2"); //49
            }
        });

        stopButton.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectedThread.write("1"); //50
            }

        });
        disconnectButton.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectedThread.cancel();
            }
        });
    }

    private void showControlButtons(String view) {
        if (view == DEVICES) {
            connectButton.setVisibility(View.VISIBLE);
            goButton.setVisibility(View.GONE);
            rightButton.setVisibility(View.GONE);
            leftButton.setVisibility(View.GONE);
            downButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.GONE);
            disconnectButton.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
        } else if (view == CONTROLLER) {
            connectButton.setVisibility(View.GONE);
            goButton.setVisibility(View.VISIBLE);
            rightButton.setVisibility(View.VISIBLE);
            leftButton.setVisibility(View.VISIBLE);
            downButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(View.VISIBLE);
            disconnectButton.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
        }
    }

    private void turnBluetoothOn() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();

        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BT_ACTIVATE_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case BT_ACTIVATE_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "bluetooth activated", Toast.LENGTH_LONG).show();
                    //***

                } else {
                    Toast.makeText(getApplicationContext(), "bluetooth not activated", Toast.LENGTH_LONG).show();
                    finish();
                }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()


            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    String data = new String(buffer, 0 , bytes);

                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, data).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
                Log.e("write()","Writing: " + msgBuffer);
            } catch (IOException e) {
                Log.e("write()","Error writing: " + e.getMessage());
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                showControlButtons(DEVICES);
                Toast.makeText(getApplicationContext(), "Successfully disconnected", Toast.LENGTH_LONG).show();
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
                    Intent intent1 = new Intent(MainActivity.this, MainActivity.class);

                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            if(registered) {
                                unregisterReceiver(mReceiver);
                                registered=false;
                            }
                            startActivity(intent1);
                            finish();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            if(registered) {
                                unregisterReceiver(mReceiver);
                                registered=false;
                            }
                            startActivity(intent1);
                            finish();
                            break;
                    }
                }
            }
        };
    }
}


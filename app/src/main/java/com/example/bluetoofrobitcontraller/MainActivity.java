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
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
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
    public static final String FORWARD = "forward";
    public static final String RIGHT = "right";
    public static final String REVERSE = "reverse";
    public static final String LEFT = "left";
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mBluetoothSocket;
    ConnectedThread connectedThread;
    private Handler mHandler;
    private static final int BT_ACTIVATE_REQUEST = 1;
    private static final int MESSAGE_READ = 3;
    UUID SERIAL_UUID;
    private ListView list;
    private Button connectButton;
    private TextView positionText;
    private Button disconnectButton;
    private Button startWeaponButton;
    private Button stopWeaponButton;
    private View trackView;
    String selectedDeviceAddress;
    boolean connection = false;
    private boolean registered = false;
    Vibrator v2;
    private float viewCenterX;
    private float viewCenterY;
    private float currentDistanceX = 0;
    private float currentDistanceY = 0;
    String currentMovement = "stopped";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        v2 = (Vibrator) getSystemService(MainActivity.VIBRATOR_SERVICE);

        list = (ListView) findViewById(R.id.list);
        connectButton = (Button) findViewById(R.id.button_connect);
        positionText = (TextView) findViewById(R.id.position);
        disconnectButton = (Button) findViewById(R.id.button_disconnect);
        startWeaponButton = (Button) findViewById(R.id.button_weapon_on);
        stopWeaponButton = (Button) findViewById(R.id.button_weapon_off);
        trackView = (View) findViewById(R.id.view);

        getCenterOfView();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        turnBluetoothOn();

        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        final HashMap nameToAddress = new HashMap();

        List<String> deviceNameList = new ArrayList<>();

        for(BluetoothDevice device : pairedDevices) {
            deviceNameList.add(device.getName());
            nameToAddress.put(device.getName(), device.getAddress());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNameList);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition = position;

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
                        if (mBluetoothAdapter.isEnabled()) {
                            if (selectedDeviceAddress != null) {
                                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(selectedDeviceAddress);

                                SERIAL_UUID = device.getUuids()[0].getUuid();

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
                                    showControlButtons(CONTROLLER);
                                } catch (IOException e) {
                                    Log.e("socket.connect()", e.getMessage());
                                    try {
                                        Log.e("","trying fallback...");
                                        socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                                        socket.connect();
                                        Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                                        showControlButtons(CONTROLLER);
                                    } catch (Exception e2) {
                                        Toast.makeText(getApplicationContext(), "Cannot connect to this device", Toast.LENGTH_LONG).show();
                                        Log.e("fallback connect", "Couldn't establish Bluetooth connection!");
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

        startWeaponButton.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectedThread.write("6");
            }
        });

        stopWeaponButton.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectedThread.write("7");
            }
        });

        disconnectButton.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectedThread.cancel();
            }
        });

        trackView.setOnTouchListener(handleTouch);
    }

    private void getCenterOfView() {
        viewCenterX = trackView.getX() + trackView.getWidth()  / 2;
        viewCenterY = trackView.getY() + trackView.getHeight() / 2;
    }

    private View.OnTouchListener handleTouch = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            getCenterOfView();

            int x = (int) event.getX();
            int y = (int) event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    currentDistanceX = x - viewCenterX;
                    currentDistanceY = y - viewCenterY;
                    sendMovementSignal(currentDistanceX, currentDistanceY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    currentDistanceX = x - viewCenterX;
                    currentDistanceY = y - viewCenterY;
                    sendMovementSignal(currentDistanceX, currentDistanceY);
                    break;
                case MotionEvent.ACTION_UP:
                    sendStopSignal();
                    break;
            }

            return true;
        }
    };

    private void sendMovementSignal(float currentDistanceX, float currentDistanceY) {

        boolean xNegative = currentDistanceX > 0 ? false : true;
        boolean yNegative = currentDistanceY < 0 ? false : true;

        double theta = Math.toDegrees(Math.atan(Math.abs(currentDistanceX)/Math.abs(currentDistanceY)));

        if (!yNegative && theta <= 45 && currentMovement != FORWARD) {
            positionText.setText(FORWARD);
            connectedThread.write("1");
            currentMovement = FORWARD;
        } else if (!xNegative && theta > 45 && currentMovement != RIGHT) {
            positionText.setText(RIGHT);
            connectedThread.write("2");
            currentMovement = RIGHT;
        } else if (yNegative && theta <= 45 && currentMovement != REVERSE) {
            positionText.setText(REVERSE);
            connectedThread.write("3");
            currentMovement = REVERSE;
        } else if (xNegative && theta > 45 && currentMovement != LEFT) {
            positionText.setText(LEFT);
            connectedThread.write("4");
            currentMovement = LEFT;
        }
    }

    private void sendStopSignal() {
        positionText.setText("stopped");
        connectedThread.write("5");
    }

    private void showControlButtons(String view) {
        if (view == DEVICES) {
            connectButton.setVisibility(View.VISIBLE);
            disconnectButton.setVisibility(View.GONE);
            startWeaponButton.setVisibility(View.GONE);
            stopWeaponButton.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            trackView.setVisibility(View.GONE);
        } else if (view == CONTROLLER) {
            connectButton.setVisibility(View.GONE);
            disconnectButton.setVisibility(View.VISIBLE);
            startWeaponButton.setVisibility(View.VISIBLE);
            stopWeaponButton.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
            trackView.setVisibility(View.VISIBLE);
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
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
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


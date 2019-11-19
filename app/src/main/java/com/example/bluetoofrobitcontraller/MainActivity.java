package com.example.bluetoofrobitcontraller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String CONTROLLER = "controller";
    private static final int BT_ACTIVATE_REQUEST = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private ConnectedThread connectedThread;
    private String selectedDeviceAddress;
    private Vibrator v2;
    private float currentDistanceX = 0;
    private float currentDistanceY = 0;
    private String currentMovement = "stopped";
    private HashMap nameToAddress = new HashMap();

    private ListView list;
    private Button connectButton;
    private Button disconnectButton;
    private Button startWeaponButton;
    private Button stopWeaponButton;
    private View trackView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        v2 = (Vibrator) getSystemService(MainActivity.VIBRATOR_SERVICE);
        list = (ListView) findViewById(R.id.list);
        connectButton = (Button) findViewById(R.id.button_connect);
        disconnectButton = (Button) findViewById(R.id.button_disconnect);
        startWeaponButton = (Button) findViewById(R.id.button_weapon_on);
        stopWeaponButton = (Button) findViewById(R.id.button_weapon_off);
        trackView = (View) findViewById(R.id.view);

        getCenterOfView();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        turnBluetoothOn();
        getBondedDevices();

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String  itemValue = (String) list.getItemAtPosition(position);
                selectedDeviceAddress = nameToAddress.get(itemValue).toString();
            }

        });

        connectButton.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                v2.vibrate(60);
                try {
                    if (mBluetoothAdapter.isEnabled()) {
                        if (selectedDeviceAddress != null) {
                            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(selectedDeviceAddress);
                            UUID SERIAL_UUID = device.getUuids()[0].getUuid();
                            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
                            connectedThread = new ConnectedThread(MainActivity.this, socket);

                            try {
                                socket.connect();
                                Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                                showControlButtons(CONTROLLER);
                            } catch (IOException e) {
                                Log.e("socket.connect()", "failed to connect: " + e.getMessage());
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
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private void getBondedDevices() {
        List<String> deviceNameList = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices;
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice device : pairedDevices) {
            deviceNameList.add(device.getName());
            nameToAddress.put(device.getName(), device.getAddress());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNameList);
        list.setAdapter(adapter);
    }

    private List<Float> getCenterOfView() {
        List<Float> center = null;
        try {
            center = Arrays.asList(trackView.getX() + trackView.getWidth()  / 2, trackView.getY() + trackView.getHeight() / 2);
        } catch (Exception e) {
            Log.e("getX and getY", "could not get screen coordinates: " + e.getMessage());
        }
        return center;
    }

    private View.OnTouchListener handleTouch = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
        List<Float> centerCoordinates = getCenterOfView();
        Float viewCenterX = centerCoordinates.get(0);
        Float viewCenterY = centerCoordinates.get(1);

        int x = (int) event.getX();
        int y = (int) event.getY();

        if (viewCenterX != 0 && viewCenterY != 0) {
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
        }

            return true;
        }
    };

    private void sendMovementSignal(float currentDistanceX, float currentDistanceY) {

        String FORWARD = "forward";
        String RIGHT = "right";
        String REVERSE = "reverse";
        String LEFT = "left";

        boolean xNegative = currentDistanceX > 0 ? false : true;
        boolean yNegative = currentDistanceY < 0 ? false : true;

        double theta = Math.toDegrees(Math.atan(Math.abs(currentDistanceX)/Math.abs(currentDistanceY)));

        if (!yNegative && theta <= 45 && currentMovement != FORWARD) {
            trackView.setBackgroundResource(R.drawable.uparrow);
            connectedThread.write("1");
            currentMovement = FORWARD;
        } else if (!xNegative && theta > 45 && currentMovement != RIGHT) {
            trackView.setBackgroundResource(R.drawable.rightarrow);
            connectedThread.write("2");
            currentMovement = RIGHT;
        } else if (yNegative && theta <= 45 && currentMovement != REVERSE) {
            trackView.setBackgroundResource(R.drawable.downarrow);
            connectedThread.write("3");
            currentMovement = REVERSE;
        } else if (xNegative && theta > 45 && currentMovement != LEFT) {
            trackView.setBackgroundResource(R.drawable.leftarrow);
            connectedThread.write("4");
            currentMovement = LEFT;
        }
    }

    private void sendStopSignal() {
        trackView.setBackgroundResource(R.drawable.circle);
        currentMovement = "stop";
        connectedThread.write("5");
    }

    public void showControlButtons(String view) {
        if (view == "devices") {
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
                    getBondedDevices();

                } else {
                    Toast.makeText(getApplicationContext(), "bluetooth was not activated", Toast.LENGTH_LONG).show();
                    finish();
                }
        }
    }

}


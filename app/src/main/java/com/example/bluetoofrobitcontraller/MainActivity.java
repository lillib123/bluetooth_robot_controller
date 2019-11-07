package com.example.bluetoofrobitcontraller;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    private static final int BT_ACTIVATE_REQUEST = 1;
    private ListView list;
    String selectedDeviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        list = (ListView) findViewById(R.id.list);

        //prompt to turn bluetooth on if it is off
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device does not support bluetoof", Toast.LENGTH_LONG).show();

        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BT_ACTIVATE_REQUEST);
        }

        //get list of connected devices
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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

                // Show Alert with Address
                Toast.makeText(getApplicationContext(),"Address: " + nameToAddress.get(itemValue), Toast.LENGTH_LONG).show();

            }

        });
    }

}

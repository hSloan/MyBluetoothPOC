package com.example.mybluetoothpoc

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts

@SuppressWarnings("deprecation")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.v("MainActivity", "Started on create")

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager == null) {
            Log.e("MainActivity", "bluetoothManager is null")
        }
        val bluetoothAdapter = bluetoothManager.getAdapter()
        if (bluetoothAdapter == null) {
            //Device does not support bluetooth
            Log.e("MainActivity", "bluetoothManager is null")
        }

        val registerForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result -> if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                println("intent is ${intent}")
            }
        }

        // Use button to make sure Bluetooth is enabled
        val enableBluetoothBtn = findViewById<Button>(R.id.enableBtButton)
        enableBluetoothBtn.setOnClickListener {
            if (bluetoothAdapter?.isEnabled == false) {
                Log.v("MainActivity", "Enabling bluetooth...")
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                registerForResult.launch(enableBtIntent)
            }
            Log.v("MainActivity", "Bluetooth is enabled")
        }

        // Scan for paired devices
        val scanPairedBtn = findViewById<Button>(R.id.scanPairedBtn)
        scanPairedBtn.setOnClickListener {
            val pairedDeviceList: LinearLayout = findViewById(R.id.pairedList)
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            pairedDevices?.forEach{ device ->
                val deviceName = device.name
                // val deviceHardwareAddress = device.address // MAC Address
                val deviceTextView = TextView(this)
                deviceTextView.text = deviceName
                pairedDeviceList.addView(deviceTextView)
            }
        }

        //Discover New Devices
        val discoverBtn = findViewById<Button>(R.id.discoverBtn)
        discoverBtn.setOnClickListener{
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(receiver, filter)
            Log.v("MainActivity", "Receiver registered")
            // Pre-check to make sure device is not already discovering.
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
            // Thread.sleep(5000) // TODO: is this necessary?
            if (bluetoothAdapter.isDiscovering()) {
                Log.v("MainActivity", "Discovery started.")
                Thread.sleep(20000)
                Log.v("MainActivity", "Cancelling discovery...")
                val discoveryDisabled = bluetoothAdapter.cancelDiscovery()
                if (discoveryDisabled) {
                    Log.v("MainActivity", "Discovery cancelled.")
                } else {
                    Log.v("MainActivity", "Discovery cancellation unsuccessful.")
                }
            } else {
                /* ContactsContract.CommonDataKinds.Note: If bluetooth adapter never started,
                ** it's likely that location is not enabled by the user.
                ** Check within Settings -> App -> "this app name" ->
                ** permissions on the android device */
                Log.v("MainActivity", "Discovery was not started.")
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            Log.v("MainActivity", "BroadcastReceiver running...")
            val action: String? = intent?.action
            Log.v("MainActivity", "BroadcastReceiver: action is $action")
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    Log.v("MainActivity", "Bluetooth device found!")
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC Address
                    val deviceTextView = TextView(ctx)
                    deviceTextView.text = deviceName
                    val pairedDeviceList: LinearLayout = findViewById(R.id.pairedList)
                    pairedDeviceList.addView(deviceTextView)
                    Log.v("MainActivity", "Added discovered device to view.")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager == null) {
            Log.e("MainActivity", "bluetoothManager is null")
        }
        val bluetoothAdapter = bluetoothManager.getAdapter()
        if (bluetoothAdapter == null) {
            //Device does not support bluetooth
            Log.e("MainActivity", "bluetoothManager is null")
        }
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery()
        }
        //Unregister the ACTION_FOUND receiver
        unregisterReceiver(receiver)
        Log.v("MainActivity", "Unregistered receiver.")
    }
}
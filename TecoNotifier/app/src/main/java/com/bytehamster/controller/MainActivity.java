package com.bytehamster.controller;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 42;
    private static final int REQUEST_PERMISSIONS = 43;
    private static final boolean DEBUG = false;

    private BluetoothAdapter mBluetoothAdapter;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (started && DEBUG) {
                    Intent mServiceIntent = new Intent(MainActivity.this, VibratorService.class);
                    mServiceIntent.setAction("NOTIFICATION");
                    mServiceIntent.putExtra("PACKAGE", "com.bytehamster.changelog");
                    mServiceIntent.putExtra("TITLE", "test");
                    startService(mServiceIntent);
                } else {
                    started = true;
                    Intent mServiceIntent = new Intent(MainActivity.this, VibratorService.class);
                    mServiceIntent.setAction("START");
                    startService(mServiceIntent);
                    Toast.makeText(MainActivity.this, R.string.started, Toast.LENGTH_LONG).show();
                }
            }
        });

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mRecyclerView = findViewById(R.id.list);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = getUserInstalledApplications(this);
        Collections.sort(apps, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo a1, ApplicationInfo a2) {
                return a1.loadLabel(pm).toString().compareTo(a2.loadLabel(pm).toString());
            }
        });
        mAdapter = new MyAdapter(apps, this);
        mRecyclerView.setAdapter(mAdapter);


        if (!checkNotificationEnabled()) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } else {
            requestPermissions();
        }
    }

    private List<ApplicationInfo> getUserInstalledApplications(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> installedApplications =
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        Iterator<ApplicationInfo> it = installedApplications.iterator();
        while (it.hasNext()) {
            ApplicationInfo appInfo = it.next();
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                it.remove();
            }
        }
        return installedApplications;
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS);
        } else {
            enableBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBluetooth();
            }
        }
    }

    private void enableBluetooth() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private boolean checkNotificationEnabled() {
        try {
            if(Settings.Secure.getString(getContentResolver(),
                    "enabled_notification_listeners").contains(getPackageName())) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

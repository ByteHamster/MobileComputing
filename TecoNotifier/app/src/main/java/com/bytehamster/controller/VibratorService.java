package com.bytehamster.controller;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

/**
 * @author Hans-Peter Lehmann
 * @version 1.0
 */
public class VibratorService extends Service {
    private static final long SCAN_PERIOD = 60000;
    private static final UUID SERVICE_UUID = UUID.fromString("713D0000-503E-4C75-BA94-3148F18D941E");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("713D0003-503E-4C75-BA94-3148F18D941E");

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();
    private BluetoothGatt mBluetoothGatt;
    private boolean running = false;
    private Queue<Vibrator> currentPatterns = new ArrayDeque<>();
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VibratorService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals("START")) {
            running = true;
            new Thread() {
                public void run() {
                    scanLeDevice();
                }
            }.start();
            startForeground(10, getNotification(getString(R.string.conecting)).build());
            updateNotification(R.string.conecting);
        } else if (intent.getAction().equals("NOTIFICATION")) {
            String packageName = intent.getStringExtra("PACKAGE");
            String title = intent.getStringExtra("TITLE");
            Vibrator v = new Vibrator(packageName, title, this);
            if (v.hasPattern()) {
                currentPatterns.add(v);
                if (currentPatterns.size() == 1) {
                    // Added new row of patterns
                    if (!wakeLock.isHeld()) {
                        wakeLock.acquire();
                    }
                    sendData(v.nextPattern());
                }
            }
        } else if (intent.getAction().equals("STOP")) {
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    private void sendData(int pattern) {
        try {
            sendData(0xff * (pattern & 1),
                    0xff * ((pattern>>1) & 1),
                    0xff * ((pattern>>2) & 1),
                    0xff * ((pattern>>3) & 1));
        } catch (IOException e) {
            e.printStackTrace();
            updateNotification(e.getMessage());
        }
    }

    private NotificationCompat.Builder getNotification(String text) {

        Intent notificationIntent = new Intent(this, VibratorService.class);
        notificationIntent.setAction("STOP");
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel("a", "Channel", NotificationManager.IMPORTANCE_HIGH);
        mNotificationManager.createNotificationChannel(mChannel);
        return new NotificationCompat.Builder(this, "a")
                .setContentTitle(getString(R.string.notification))
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "Stop", pendingIntent);
    }

    private void updateNotification(int text) {
        updateNotification(getString(text));
    }

    private void updateNotification(String text) {
        if (running) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(10, getNotification(text).build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBluetoothGatt != null) {
            running = false;
            mBluetoothGatt.disconnect();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scanLeDevice() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothGatt == null) {
                    updateNotification(R.string.device);
                }
                stopScan();
            }
        }, SCAN_PERIOD);
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    private void stopScan() {
        mHandler.removeCallbacksAndMessages(null);
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.d("BLE", device.getAddress() + " " + device.getName());

                    if (device.getAddress().equals("EB:5A:33:20:90:1F")) {
                        stopScan();
                        connect(device);
                    }
                }
            };

    private void connect(BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
                updateNotification(R.string.connected);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                updateNotification(R.string.disconnected);

                new Thread() {
                    public void run() {
                        scanLeDevice();
                    }
                }.start();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (currentPatterns.isEmpty() && wakeLock.isHeld()) {
                wakeLock.release();
            }

            new Thread() {
                public void run() {
                    while (!currentPatterns.isEmpty() && !currentPatterns.peek().hasPattern()) {
                        currentPatterns.poll();
                    }
                    if (!currentPatterns.isEmpty() && currentPatterns.peek().hasPattern()) {
                        sendData(currentPatterns.peek().nextPattern());
                    }
                }
            }.start();
        }
    };

    private void sendData(int b1, int b2, int b3, int b4) throws IOException {
        if (mBluetoothGatt == null) {
            throw new IOException(getString(R.string.disconnected));
        }
        BluetoothGattService Service = mBluetoothGatt.getService(SERVICE_UUID);
        if (Service == null) {
            throw new IOException(getString(R.string.service));
        }
        BluetoothGattCharacteristic characteristic = Service.getCharacteristic(CHARACTERISTIC_UUID);
        if (characteristic == null) {
            throw new IOException(getString(R.string.characteristic));
        }

        characteristic.setValue(new byte[] {(byte) b1, (byte) b2, (byte) b3, (byte) b4});
        boolean status = mBluetoothGatt.writeCharacteristic(characteristic);

        if (!status) {
            throw new IOException(getString(R.string.send));
        }
    }
}

/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kevalpatel2106.remotebluetoothspeaker.bluetooth;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kevalpatel2106.remotebluetoothspeaker.Commands;
import com.kevalpatel2106.remotebluetoothspeaker.tts.TTS;

import org.greenrobot.eventbus.EventBus;

import java.util.Objects;

/**
 * Sample usage of the A2DP sink bluetooth profile. At startup, this activity sets the Bluetooth
 * adapter in pairing mode for {@link #DISCOVERABLE_TIMEOUT_SEC} ms.
 * <p>
 * NOTE: While in pairing mode, pairing requests are auto-accepted - at this moment there's no
 * way to block specific pairing attempts while in pairing mode. This is known limitation that is
 * being worked on.
 */
public class BluetoothA2DPService extends Service {
    private static final String ARG_BT_STATE = "bt_state";

    private static final String TAG = BluetoothA2DPService.class.getSimpleName();
    private static final String ADAPTER_FRIENDLY_NAME = "JarvisBT";

    private static final int FOREGROUND_NOTIFICATION_ID = 123;
    private static final int DISCOVERABLE_TIMEOUT_SEC = 60;


    /**
     * Handle an intent that is broadcast by the Bluetooth A2DP sink profile whenever a device
     * connects or disconnects to it.
     * Action is {@link A2dpSinkHelper#ACTION_CONNECTION_STATE_CHANGED} and
     * extras describe the old and the new connection states. You can use it to indicate that
     * there's a device connected.
     */
    private final BroadcastReceiver mSinkProfileStateChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(A2dpSinkHelper.ACTION_CONNECTION_STATE_CHANGED)) {
                BluetoothDevice device = A2dpSinkHelper.getDevice(intent);
                if (device != null) {

                    String deviceName = Objects.toString(device.getName(), "a device");
                    int newState = A2dpSinkHelper.getCurrentProfileState(intent);

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        TTS.speak(BluetoothA2DPService.this, "Connected to " + deviceName);
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        TTS.speak(BluetoothA2DPService.this, "Disconnected from " + deviceName);
                    }
                }

                updateStatus();
            }
        }
    };

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothProfile mA2DPSinkProxy;

    /**
     * Handle an intent that is broadcast by the Bluetooth adapter whenever it changes its
     * state (after calling enable(), for example).
     * Action is {@link BluetoothAdapter#ACTION_STATE_CHANGED} and extras describe the old
     * and the new states. You can use this intent to indicate that the device is ready to go.
     */
    private final BroadcastReceiver mAdapterStateChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            updateStatus();
        }
    };

    public static void passCommand(Context context, String command) {
        Intent intent = new Intent(context, BluetoothA2DPService.class);
        intent.putExtra(BluetoothA2DPService.ARG_BT_STATE, command);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "No default Bluetooth adapter. Device likely does not support bluetooth.");
            stopSelf();
            return;
        }

        //Register receivers
        registerReceiver(mAdapterStateChangeReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mSinkProfileStateChangeReceiver, new IntentFilter(A2dpSinkHelper.ACTION_CONNECTION_STATE_CHANGED));

        //Make service foreground.
        //So it won't get killed.
        makeForeground();

        //Set initial state
        turnOnIfNot();
        initA2DPSink();
    }

    /**
     * Assign notification foreground.
     */
    private void makeForeground() {
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Playing music")
                .setSmallIcon(android.R.drawable.ic_menu_add)
                .build();
        startForeground(FOREGROUND_NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Handle all the commands.
        if (intent.getStringExtra(ARG_BT_STATE) != null) {
            switch (intent.getStringExtra(ARG_BT_STATE)) {
                case Commands.TURN_ON_BLUETOOTH:    //Turn on BT
                    turnOnIfNot();
                    break;
                case Commands.TURN_OFF_BLUETOOTH:   //Turn off BT
                    turnOffIfNot();
                    break;
                case Commands.DISCONNECT_ALL_DEVICE:    //Disconnect all devices.
                    disconnectConnectedDevices();
                    break;
                case Commands.UNPAIR_ALL_DEVICE:
                    unpairAllDevices();
                    break;
                case Commands.MAKE_DISCOVERABLE:
                    turnOnIfNot();
                    enableDiscoverable();
                    break;
                case Commands.VOLUME_DOWN:
                    volumeDown();
                    break;
                case Commands.VOLUME_UP:
                    volumeUp();
                    break;
                default:
                    turnOnIfNot();
                    break;
            }
        }

        updateStatus();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        killService();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        killService();
    }

    /**
     * Release all the resources before killing the service.
     */
    private void killService() {
        //Stop foreground.
        stopForeground(true);

        //Unregister all the receiver.
        unregisterReceiver(mAdapterStateChangeReceiver);
        unregisterReceiver(mSinkProfileStateChangeReceiver);

        turnOffIfNot();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Initiate the A2DP sink.
     */
    private void initA2DPSink() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth adapter not available or not enabled.");
            return;
        }

        mBluetoothAdapter.setName(ADAPTER_FRIENDLY_NAME);   //Set the name of the bluetooth adapter
        mBluetoothAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                mA2DPSinkProxy = proxy;
            }

            @Override
            public void onServiceDisconnected(int profile) {
            }
        }, A2dpSinkHelper.A2DP_SINK_PROFILE);
    }

    /**
     * Enable the current {@link BluetoothAdapter} to be discovered (available for pairing) for
     * the next {@link #DISCOVERABLE_TIMEOUT_SEC} ms.
     */
    private void enableDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_TIMEOUT_SEC);
        startActivity(discoverableIntent);

        TTS.speak(this, "Bluetooth is discoverable for 60 seconds.");
    }

    /**
     * Disconnect all the connected devices.
     */
    private void disconnectConnectedDevices() {
        if (mA2DPSinkProxy == null || mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "disconnectConnectedDevices: " + mA2DPSinkProxy + " " + mBluetoothAdapter);
            return;
        }
        for (BluetoothDevice device : mA2DPSinkProxy.getConnectedDevices()) {
            Log.i(TAG, "Disconnecting device " + device);
            A2dpSinkHelper.disconnect(mA2DPSinkProxy, device);
        }
    }

    /**
     * Unpair all the connected devices.
     */
    private void unpairAllDevices() {
        Log.d(TAG, "unpairAllDevices: ");
        if (mA2DPSinkProxy == null || mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            return;
        }
        for (BluetoothDevice device : mA2DPSinkProxy.getConnectedDevices()) {
            Log.i(TAG, "Disconnecting device " + device);
            A2dpSinkHelper.unpairDevice(mBluetoothAdapter);
        }
        TTS.speak(this, "Removed paired devices.");
    }

    /**
     * Turn on the bluetooth if it is not enabled already.
     */
    private void turnOnIfNot() {
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();

            TTS.speak(BluetoothA2DPService.this, "Bluetooth is turned on.");
        }
    }

    /**
     * Turn off bluetooth if bluetooth is enabled.
     */
    private void turnOffIfNot() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.closeProfileProxy(A2dpSinkHelper.A2DP_SINK_PROFILE, mA2DPSinkProxy);
            mBluetoothAdapter.disable();

            TTS.speak(BluetoothA2DPService.this, "Bluetooth is turning off.");
        }
    }

    /**
     * Increase volume by one point.
     */
    private void volumeUp() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                == audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) return;

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) + 1,
                AudioManager.FLAG_SHOW_UI);
        audioManager.setStreamVolume(AudioManager.STREAM_RING,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) + 1,
                AudioManager.FLAG_SHOW_UI);
    }

    /**
     * Decrease volume by one point.
     */
    private void volumeDown() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return;

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) - 1,
                AudioManager.FLAG_SHOW_UI);
        audioManager.setStreamVolume(AudioManager.STREAM_RING,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) - 1,
                AudioManager.FLAG_SHOW_UI);
    }

    /**
     * This will update status to the socket.
     */
    private void updateStatus() {
        EventBus.getDefault().post("Connected devices: "
                + (mA2DPSinkProxy ==null? "0":mA2DPSinkProxy.getConnectedDevices())
                + "<br/>Bluetooth: " + (mBluetoothAdapter.isEnabled() ? "Enable" : "Disable"));
    }
}
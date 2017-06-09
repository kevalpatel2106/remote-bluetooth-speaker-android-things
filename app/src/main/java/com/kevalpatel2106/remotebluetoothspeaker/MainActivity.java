package com.kevalpatel2106.remotebluetoothspeaker;

import android.app.Activity;
import android.os.Bundle;

import com.kevalpatel2106.remotebluetoothspeaker.bluetooth.BluetoothA2DPService;
import com.kevalpatel2106.remotebluetoothspeaker.server.SocketWriter;
import com.kevalpatel2106.remotebluetoothspeaker.server.WebServer;
import com.kevalpatel2106.remotebluetoothspeaker.tts.TTS;

import java.io.IOException;

/**
 * Skeleton of an Android Things activity.
 *
 * @author 'https://github.com/kevalpatel2106'
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initialize TTS so the tts responses don't take too much time.
        TTS.speak(this, "I am ready.");

        //Start the web server
        try {
            new WebServer(this, getAssets());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Start the bluetooth A2DP service.
        BluetoothA2DPService.passCommand(this, Commands.TURN_ON_BLUETOOTH);
    }
}

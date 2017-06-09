package com.kevalpatel2106.remotebluetoothspeaker.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Created by Keval Patel on 25/04/17.
 * This class initialize and maintain the connection with TTS engine.
 *
 * @author 'https://github.com/kevalpatel2106'
 */

public class TTS {
    private static final String TAG = TTS.class.getSimpleName();
    private static final String UTTERANCE_ID = "com.kevalpatel2106.UTTERANCE_ID";

    private static TextToSpeech mTTSEngine;

    /**
     * Stop current utterance and flush the TTS queue.
     */
    public static void flush() {
        if (mTTSEngine != null) mTTSEngine.stop();
    }

    /**
     * Pass the text and let TTS speak.
     *
     * @param text text to speak
     */
    public static void speak(Context context, final String text) {
        Log.d(TAG, "Speak : " + text);
        if (mTTSEngine == null) {
            mTTSEngine = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    if (i == TextToSpeech.SUCCESS) {
                        mTTSEngine.setLanguage(Locale.US);
                        mTTSEngine.setPitch(1f);
                        mTTSEngine.setSpeechRate(1f);

                        mTTSEngine.speak(text, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
                    } else {
                        mTTSEngine = null;
                    }
                }
            });
        } else {
            mTTSEngine.speak(text, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
        }
    }
}
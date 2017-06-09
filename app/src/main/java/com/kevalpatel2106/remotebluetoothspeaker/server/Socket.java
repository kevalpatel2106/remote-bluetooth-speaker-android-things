/*
 *  Copyright 2017 Keval Patel.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.kevalpatel2106.remotebluetoothspeaker.server;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.kevalpatel2106.remotebluetoothspeaker.bluetooth.BluetoothA2DPService;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;

/**
 * Created by Keval on 19-May-17.
 * Socket to maintain the connection for commands.
 */

class Socket extends WebSocket {
    private static final String TAG = Socket.class.getSimpleName();
    private final Context mContext;

    Socket(@NonNull NanoHTTPD.IHTTPSession handshakeRequest, Context context) {
        super(handshakeRequest);
        mContext = context;
    }

    @Override
    protected void onPong(WebSocketFrame webSocketFrame) {
        //Do nothing
    }

    @Override
    protected void onMessage(WebSocketFrame webSocketFrame) {
        String command = webSocketFrame.getTextPayload();
        Log.d(TAG, "onMessage: WebSocket Command ->" + command);
        BluetoothA2DPService.passCommand(mContext, command);    //Send command to BT
    }

    @Override
    protected void onClose(WebSocketFrame.CloseCode closeCode, String s, boolean b) {
        //Do nothing
    }

    @Override
    protected void onException(IOException e) {
        Log.d(TAG, "onException: " + e.getMessage());
    }
}

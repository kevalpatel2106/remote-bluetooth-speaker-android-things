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
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.io.InputStream;

import fi.iki.elonen.IWebSocketFactory;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketResponseHandler;

/**
 * Created by Keval Patel on 14/05/17.
 * This is a small web server running on your Raspberry PI. Connect to the local ip of the
 * raspberry Pi with port 8085 and issue the commands.
 *
 * @author Keval {https://github.com/kevalpatel2106}
 * @see <a href='https://github.com/NanoHttpd/nanohttpd'>'https://github.com/NanoHttpd/nanohttpd'</a>
 */

public final class WebServer extends NanoHTTPD {
    private static final String TAG = WebServer.class.getSimpleName();
    @NonNull
    private final AssetManager mAssetManager;

    private Socket mSocket;
    private WebSocketResponseHandler mResponseHandler;


    /**
     * Start the web server.
     *
     * @param assetManager {@link AssetManager} to load html wepages from assets.
     * @throws IOException If failed to initialize.
     */
    public WebServer(@NonNull final Context context,
                     @NonNull AssetManager assetManager) throws IOException {
        super(8085);

        mAssetManager = assetManager;

        //Create socket
        mResponseHandler = new WebSocketResponseHandler(new IWebSocketFactory() {

            @Override
            public WebSocket openWebSocket(IHTTPSession handshake) {
                mSocket = new Socket(handshake, context);
                return mSocket;
            }
        });

        //Start the server
        start();

        EventBus.getDefault().register(this);
        Log.d(TAG, "WebServer: Starting server.");
    }

    /**
     * Handle the HTML request.
     */
    @Override
    public Response serve(IHTTPSession session) {
        NanoHTTPD.Response ws = mResponseHandler.serve(session);
        if (ws == null) {
            String uri = session.getUri();
            try {
                switch (uri) {
                    case "/":
                        return getHTMLResponse();
                    case "/css/style.css":
                        InputStream inputStream = mAssetManager.open("css/style.css");
                        return new NanoHTTPD.Response(Response.Status.OK, "text/css", inputStream);
                    case "/script/script.js":
                        inputStream = mAssetManager.open("script/script.js");
                        return new NanoHTTPD.Response(Response.Status.OK, "text/javascript", inputStream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ws;
    }

    /**
     * Get the HTML webpage from the assets folder and return the fixed length {@link fi.iki.elonen.NanoHTTPD.Response}.
     *
     * @return Fixed length {@link fi.iki.elonen.NanoHTTPD.Response}
     * @throws IOException If asset not found or failed to read asset file
     */
    @NonNull
    private Response getHTMLResponse() throws IOException {
        InputStream inputStream = mAssetManager.open("home.html");
        return new NanoHTTPD.Response(Response.Status.OK, "text/html", inputStream);
    }

    /**
     * Write the text on the socket.
     * It is advisable to make this method synchronised.
     *
     * @param msg String message to write on socket
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public synchronized void writeMessage(@Nullable final String msg) {
        Log.d(TAG, "writeMessage: " + msg);
        if (msg == null || mSocket == null) return;
        try {
            mSocket.send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

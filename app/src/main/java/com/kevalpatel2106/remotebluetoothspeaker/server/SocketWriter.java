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

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

/**
 * Created by Keval on 19-May-17.
 * Interface to write bytes on the socket.
 *
 * @author Keval {https://github.com/kevalpatel2106}
 * @see WebServer
 */

public interface SocketWriter extends Serializable{

    /**
     * Write the text on the socket.
     * It is advisable to make this method synchronised.
     *
     * @param msg String message to write on socket
     */
    void writeMessage(@Nullable String msg);
}

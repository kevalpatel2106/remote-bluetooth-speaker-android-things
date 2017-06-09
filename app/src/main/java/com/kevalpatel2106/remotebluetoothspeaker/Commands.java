package com.kevalpatel2106.remotebluetoothspeaker;

/**
 * Created by Keval Patel on 09/06/17.
 * This class contains commands that you can give to bluetooth
 *
 * @author 'https://github.com/kevalpatel2106'
 */

public class Commands {
    //Bluetooth control
    public static final String TURN_ON_BLUETOOTH = "turn_on";
    public static final String TURN_OFF_BLUETOOTH = "turn_off";

    //Visibility control
    public static final String MAKE_DISCOVERABLE = "make_discoverable";

    //Connected/Paired devices control
    public static final String DISCONNECT_ALL_DEVICE = "disconnect_device";
    public static final String UNPAIR_ALL_DEVICE = "unpair_all_device";

    //Volume control
    public static final String VOLUME_UP = "volume_up";
    public static final String VOLUME_DOWN = "volume_down";
}

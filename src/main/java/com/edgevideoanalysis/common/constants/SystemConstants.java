package com.edgevideoanalysis.common.constants;

public class SystemConstants {

    private SystemConstants() {}

    public static final int ONLINE_STATUS_ONLINE = 1;
    public static final int ONLINE_STATUS_OFFLINE = 0;

    public static final int LED_STATUS_ON = 1;
    public static final int LED_STATUS_OFF = 0;

    public static final int COMMAND_STATUS_PENDING = 0;
    public static final int COMMAND_STATUS_EXECUTING = 1;
    public static final int COMMAND_STATUS_SUCCESS = 2;
    public static final int COMMAND_STATUS_FAILED = 3;

    public static final String COMMAND_TYPE_LED_ON = "led_on";
    public static final String COMMAND_TYPE_LED_OFF = "led_off";

    public static final String SENSOR_TYPE_TEMPERATURE = "temperature";
    public static final String SENSOR_TYPE_HUMIDITY = "humidity";
    public static final String SENSOR_TYPE_ILLUMINATION = "illumination";
    public static final String SENSOR_TYPE_VOLTAGE = "voltage";
    public static final String SENSOR_TYPE_CURRENT = "current";
}

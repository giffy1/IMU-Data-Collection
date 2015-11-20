package com.microsoft.band.client;

/**
 * Constants used for communication between components of the handheld application. For example,
 * the Main UI can send messages to start/stop the sensor service.
 *
 * @see SensorService
 */
class Constants {
    public interface ACTION {
        String START_FOREGROUND = "edu.umass.cs.mygestures.action.start-foreground";
        String STOP_FOREGROUND = "edu.umass.cs.mygestures.action.stop-foreground";
    }

    public interface MESSAGE {
        int REGISTER_CLIENT = 0;
        int UNREGISTER_CLIENT = 1;
        int SENSOR_STARTED = 2;
        int SENSOR_STOPPED = 3;
        int STATUS = 4;
    }

    public interface KEY {
        String STATUS = "edu.umass.cs.mygestures.key.status";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 101;
    }
}

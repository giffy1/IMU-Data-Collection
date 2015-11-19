package com.microsoft.band.sdk.sampleapp.client;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.SampleRate;

import java.util.ArrayList;

/**
 * Created by snoran on 11/18/15.
 */
public class SensorService extends Service implements BandAccelerometerEventListener, BandGyroscopeEventListener {

    /** used for debugging purposes */
    private static final String TAG = SensorService.class.getName();

    /** Buffer of timestamps for gyroscope data */
    private long[] gyroTimestamps;

    /** Buffer of timestamps for accelerometer data */
    private long[] accelTimestamps;

    /** Buffer of values for gyroscope data */
    private float[] gyroValues;

    /** Buffer of values for accelerometer data */
    private float[] accelValues;

    /** Index into gyroscope data */
    private int gyroIndex;

    /** Index into accelerometer data */
    private int accelIndex;

    /** Buffer size - the number of samples collected before saved to disk */
    private static final int BUFFER_SIZE = 256;

    /** The object which receives sensor data from the Microsoft Band */
    private BandClient client = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * asynchronous task for connecting to the Microsoft Band accelerometer and gyroscope sensors
     */
    private class SensorSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    //appendToUI("Band is connected.\n", txtStatus);
                    client.getSensorManager().registerAccelerometerEventListener(SensorService.this, SampleRate.MS128);
                    client.getSensorManager().registerGyroscopeEventListener(SensorService.this, SampleRate.MS128);
                } else {
                    //appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n", txtStatus);
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                //appendToUI(exceptionMessage, txtStatus);

            } catch (Exception e) {
                //appendToUI(e.getMessage(), txtStatus);
            }
            return null;
        }
    }

    //TODO: calls to appendToUI should change to calls to send info to the UI which can be done using message Handler
    /**
     * Connects the mobile device to the Microsoft Band
     * @return True if successful, False otherwise
     * @throws InterruptedException if the connection is interrupted
     * @throws BandException if the band SDK version is not compatible or the Microsoft Health band is not installed
     */
    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                //appendToUI("Band isn't paired with your phone.\n", txtStatus);
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        //appendToUI("Band is connecting...\n", txtStatus);
        return ConnectionState.CONNECTED == client.connect().await();
    }

    public void startSensors() {
        new SensorSubscriptionTask().execute();
    }

    public void unregisterSensors() {
        if (client != null) {
            try {
                client.getSensorManager().unregisterAllListeners();
            } catch (BandIOException e) {
                //appendToUI(e.getMessage(), txtStatus);
            }
        }
    }

    public void disconnectBand() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
    }

    @Override
    public void onBandAccelerometerChanged(BandAccelerometerEvent event) {
        synchronized (this) { //add sensor data to the appropriate buffer
            accelTimestamps[accelIndex] = event.getTimestamp();
            accelValues[3 * accelIndex] = event.getAccelerationX();
            accelValues[3 * accelIndex + 1] = event.getAccelerationY();
            accelValues[3 * accelIndex + 2] = event.getAccelerationZ();
            accelIndex++;
            if (accelIndex >= BUFFER_SIZE) {
                //TODO: Send to client (i.e. UI but also broadcast to file writer)
                //client.sendSensorData(Sensor.TYPE_ACCELEROMETER, accelTimestamps.clone(), accelValues.clone());
                accelIndex = 0;
            }
        }
    }

    //TODO: I noticed that BandAccelerometerEvent does not have getAngularVelocity() methods which makes sense
    //TODO: But...BandGyroscopeEvent does have getAcceleration() methods. Could we use that to get equivalent timestamps?
    @Override
    public void onBandGyroscopeChanged(BandGyroscopeEvent event) {
        synchronized (this) {
            gyroTimestamps[gyroIndex] = event.getTimestamp();
            gyroValues[3 * gyroIndex] = event.getAngularVelocityX();
            gyroValues[3 * gyroIndex + 1] = event.getAngularVelocityY();
            gyroValues[3 * gyroIndex + 2] = event.getAccelerationZ();
            gyroIndex++;
            if (gyroIndex >= BUFFER_SIZE) {
                //TODO: Send to client (i.e. UI but also broadcast to file writer)
                //client.sendSensorData(Sensor.TYPE_GYROSCOPE, gyroTimestamps.clone(), gyroValues.clone());
                gyroIndex = 0;
            }
        }
    }
}

//TODO: Kept this from my previous SensorService for showing a notification and registering sensors, etc. in the onStartCommand
   /* @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.START_SERVICE)){
            Intent recordIntent = new Intent(this, SensorService.class);
            recordIntent.setAction(Constants.ACTION.RECORD_LABEL);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, recordIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

            //notify the user that the application has started - the user can also record labels using the notification
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("My Gestures")
                    .setTicker("My Gestures")
                    .setContentText("Collecting sensor data...")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setOngoing(true)
                    .setVibrate(new long[]{0, 50, 100, 50, 100, 50, 100, 400, 100, 300, 100, 350, 50, 200, 100, 100, 50, 600}) //I LOVE THIS!!!
                    .setPriority(Notification.PRIORITY_MAX)
                    .addAction(android.R.drawable.ic_btn_speak_now, "Record Label", pendingIntent).build();

            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification); //id is arbitrary, so we choose id=1

            registerSensors();
        } else if (intent.getAction().equals(Constants.ACTION.RECORD_LABEL)) {
            labelTimestamp = SystemClock.elapsedRealtimeNanos();
            startListening();
        } else if (intent.getAction().equals(Constants.ACTION.STOP_SERVICE)) {
            unregisterSensors();
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }*/
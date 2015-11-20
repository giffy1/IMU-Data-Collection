package com.microsoft.band.client;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.SampleRate;

import java.io.BufferedWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by snoran on 11/18/15.
 * <p>
 * The SensorService is responsible for starting and stopping the sensors on the Band and receiving
 * the sensor information periodically. It is a foreground service, so that the user can close the
 * application on the phone and continue to receive data from the wearable device. Because the
 * {@link BandGyroscopeEvent} also receives accelerometer readings, we only need to register a
 * {@link BandGyroscopeEventListener} and no {@link BandAccelerometerEventListener}. This should
 * be compatible with both the Microsoft Band and Microsoft Band 2.
 * </p>
 *
 * @see Service#startForeground(int, Notification)
 * @see BandClient
 * @see BandGyroscopeEventListener
 * @see FileUtil
 */
public class SensorService extends Service implements BandGyroscopeEventListener {

    /** used for debugging purposes */
    private static final String TAG = SensorService.class.getName();

    /** The object which receives sensor data from the Microsoft Band */
    private BandClient client = null;

    /** Name of the file to which to write the accelerometer data */
    private static final String FILENAME = "sensor_data";

    /** Buffered writer used to log the accelerometer data */
    private final BufferedWriter writer = FileUtil.getFileWriter(FILENAME);

    /** Messenger used by clients */
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    /** List of bound clients/activities to this service */
    private ArrayList<Messenger> mClients = new ArrayList<>();

    /** indicates whether the sensor service is running or not */
    private static boolean isRunning = false;

    /**
     * Handler to handle incoming messages
     */
    private static class IncomingHandler extends Handler {
        private final WeakReference<SensorService> mService;

        IncomingHandler(SensorService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE.REGISTER_CLIENT:
                    mService.get().mClients.add(msg.replyTo);
                    break;
                case Constants.MESSAGE.UNREGISTER_CLIENT:
                    mService.get().mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Sends a status message to all clients, removing any inactive clients if necessary.
     * @param status the status message
     */
    private void sendStatusToClients(String status) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send message value
                Bundle b = new Bundle();
                b.putString(Constants.KEY.STATUS, status);
                Message msg = Message.obtain(null, Constants.MESSAGE.STATUS);
                msg.setData(b);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    protected static boolean isRunning(){
        return isRunning;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.START_FOREGROUND)){
            isRunning = true;
            // create option to stop the service from the notification
            Intent stopIntent = new Intent(this, SensorService.class);
            stopIntent.setAction(Constants.ACTION.STOP_FOREGROUND);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

            // notify the user that the foreground service has started
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setTicker(getString(R.string.app_name))
                    .setContentText(getString(R.string.msg_service_started))
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setOngoing(true)
                    .setVibrate(new long[]{0, 50, 150, 200})
                    .setPriority(Notification.PRIORITY_MAX)
                    .addAction(android.R.drawable.ic_delete, getString(R.string.stop_service), pendingIntent).build();

            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);

            startSensors();
        } else if (intent.getAction().equals(Constants.ACTION.STOP_FOREGROUND)) {
            isRunning = false;
            unregisterSensors();
            disconnectBand();
            if (writer != null)
                FileUtil.closeWriter(writer);
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    /**
     * asynchronous task for connecting to the Microsoft Band accelerometer and gyroscope sensors.
     * Errors may arise if the Band does not support the Band SDK version or the Microsoft Health
     * application is not installed on the mobile device.
     **
     * @see com.microsoft.band.BandErrorType#UNSUPPORTED_SDK_VERSION_ERROR
     * @see com.microsoft.band.BandErrorType#SERVICE_ERROR
     * @see BandClient#getSensorManager()
     * @see com.microsoft.band.sensors.BandSensorManager
     */
    private class SensorSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    sendStatusToClients(getString(R.string.status_connected));
                    client.getSensorManager().registerGyroscopeEventListener(SensorService.this, SampleRate.MS16);
                } else {
                    sendStatusToClients(getString(R.string.status_not_connected));
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = getString(R.string.err_unsupported_sdk_version);
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = getString(R.string.err_service);
                        break;
                    default:
                        exceptionMessage = getString(R.string.err_default) + e.getMessage();
                        break;
                }
                Log.e(TAG, exceptionMessage);
                sendStatusToClients(exceptionMessage);

            } catch (Exception e) {
                sendStatusToClients(getString(R.string.err_default) + e.getMessage());
            }
            return null;
        }
    }

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
                sendStatusToClients(getString(R.string.status_not_paired));
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        sendStatusToClients(getString(R.string.status_connecting));
        return ConnectionState.CONNECTED == client.connect().await();
    }

    /**
     * registers the band's accelerometer/gyroscope sensors to the sensor service
     */
    public void startSensors() {
        new SensorSubscriptionTask().execute();
    }

    /**
     * unregisters the sensors from the sensor service
     */
    public void unregisterSensors() {
        if (client != null) {
            try {
                client.getSensorManager().unregisterAllListeners();
            } catch (BandIOException e) {
                sendStatusToClients(getString(R.string.err_default) + e.getMessage());
            }
        }
    }

    /**
     * disconnects the sensor service from the Microsoft Band
     */
    public void disconnectBand() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException | BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
    }

    @Override
    public void onBandGyroscopeChanged(BandGyroscopeEvent event) {
        synchronized(writer){
            Object[] data = new Object[]{event.getTimestamp(),
                                       event.getAccelerationX(), event.getAccelerationY(), event.getAccelerationZ(),
                                       event.getAngularVelocityX(), event.getAngularVelocityY(), event.getAngularVelocityZ()};
            String sample = TextUtils.join(",", data);
            Log.d(TAG, sample);
            FileUtil.writeToFile(sample, writer);
        }
    }
}
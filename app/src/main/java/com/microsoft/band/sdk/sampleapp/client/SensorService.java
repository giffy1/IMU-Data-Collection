package com.microsoft.band.sdk.sampleapp.client;

import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;

/**
 * Created by snoran on 11/18/15.
 */
public class SensorService implements BandAccelerometerEventListener, BandGyroscopeEventListener {
    @Override
    public void onBandAccelerometerChanged(BandAccelerometerEvent bandAccelerometerEvent) {
        //TODO: Send to UI
    }

    @Override
    public void onBandGyroscopeChanged(BandGyroscopeEvent bandGyroscopeEvent) {
        //TODO: Send to UI
    }
}

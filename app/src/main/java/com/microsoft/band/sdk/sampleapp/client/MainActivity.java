//Copyright (c) Microsoft Corporation All rights reserved.  
// 
//MIT License: 
// 
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
// 
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software. 
// 
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package com.microsoft.band.sdk.sampleapp.client;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.SampleRate;

import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	private BandClient client = null;
	private Button btnStart;
	private TextView txtAccelerometer, txtGyroscope, txtStatus;

	private SensorService sensorService;
	
	/*private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {
            	appendToUI(String.format(" X = %.3f \n Y = %.3f\n Z = %.3f", event.getAccelerationX(),
            			event.getAccelerationY(), event.getAccelerationZ()), txtAccelerometer);
            }
        }
    };

	private BandGyroscopeEventListener mGyroscopeEventListener = new BandGyroscopeEventListener() {
		@Override
		public void onBandGyroscopeChanged(final BandGyroscopeEvent event) {
			if (event != null) {
				appendToUI(String.format(" X = %.3f \n Y = %.3f\n Z = %.3f", event.getAngularVelocityX(),
						event.getAngularVelocityY(), event.getAngularVelocityZ()), txtGyroscope);
			}
		}
	};*/
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtAccelerometer = (TextView) findViewById(R.id.txtAccel);
		txtGyroscope = (TextView) findViewById(R.id.txtGyro);
		txtStatus = (TextView) findViewById(R.id.txtStatus);

		sensorService = new SensorService();

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				txtAccelerometer.setText("");
				txtGyroscope.setText("");
				txtStatus.setText("");
				sensorService.startSensors();
			}
		});
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		txtAccelerometer.setText("");
		txtGyroscope.setText("");
		txtStatus.setText("");
	}
	
    @Override
	protected void onPause() {
		super.onPause();
	}


    @Override
    protected void onDestroy() {
		sensorService.disconnectBand();
        super.onDestroy();
    }

	private void appendToUI(final String string, final TextView textView) {
		this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	textView.setText(string);
            }
        });
	}


}


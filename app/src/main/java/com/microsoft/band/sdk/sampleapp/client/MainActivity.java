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
				new SensorSubscriptionTask().execute();
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
		if (client != null) {
			try {
				client.getSensorManager().unregisterAllListeners();
				//TODO: I believe onPause happens when the activity loses focus, so we shouldn't shut off the sensor service here
			} catch (BandIOException e) {
				appendToUI(e.getMessage(), txtStatus);
			}
		}
	}

	/**
	 * asynchronous task for connecting to the Microsoft Band accelerometer and gyroscope sensors
	 */
	private class SensorSubscriptionTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (getConnectedBandClient()) {
					appendToUI("Band is connected.\n", txtStatus);
					client.getSensorManager().registerAccelerometerEventListener(sensorService, SampleRate.MS128);
					client.getSensorManager().registerGyroscopeEventListener(sensorService, SampleRate.MS128);
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n", txtStatus);
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
				appendToUI(exceptionMessage, txtStatus);

			} catch (Exception e) {
				appendToUI(e.getMessage(), txtStatus);
			}
			return null;
		}
	}

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
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
				appendToUI("Band isn't paired with your phone.\n", txtStatus);
				return false;
			}
			client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
		} else if (ConnectionState.CONNECTED == client.getConnectionState()) {
			return true;
		}
		
		appendToUI("Band is connecting...\n", txtStatus);
		return ConnectionState.CONNECTED == client.connect().await();
	}
}


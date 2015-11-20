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
package com.microsoft.band.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.app.Activity;
import android.widget.Button;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class MainActivity extends Activity {

	private TextView txtAccelerometer, txtGyroscope, txtStatus;

	/**
	 * Messenger service for exchanging messages with the background service
	 */
	private Messenger mService = null;
	/**
	 * Variable indicating if this activity is connected to the service
	 */
	private boolean mIsBound;
	/**
	 * Messenger receiving messages from the background service to update UI
	 */
	private final Messenger mMessenger = new Messenger(new IncomingHandler(this));

	/**
	 * Handler to handle incoming messages
	 */
	static class IncomingHandler extends Handler {
		private final WeakReference<MainActivity> mMainActivity;

		IncomingHandler(MainActivity mainActivity) {
			mMainActivity = new WeakReference<>(mainActivity);
		}
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case Constants.MESSAGE.SENSOR_STARTED:
				{
					mMainActivity.get().updateStatus("sensor started.");
					break;
				}
				case Constants.MESSAGE.SENSOR_STOPPED:
				{
					mMainActivity.get().updateStatus("sensor stopped.");
					break;
				}
				case Constants.MESSAGE.STATUS:
				{
					mMainActivity.get().updateStatus(msg.getData().getString(Constants.KEY.STATUS));
				}
				default:
					super.handleMessage(msg);
			}
		}
	}

	/**
	 * Connection with the service
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			updateStatus("Attached to the sensor service.");
			mIsBound = true;
			try {
				Message msg = Message.obtain(null, Constants.MESSAGE.REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even do anything with it
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been unexpectedly disconnected - process crashed.
			mIsBound = false;
			mService = null;
			updateStatus("Disconnected from the sensor service.");
		}
	};

	/**
	 * Binds this activity to the service if the service is already running
	 */
	private void bindToServiceIfIsRunning() {
		//If the service is running when the activity starts, we want to automatically bind to it.
		if (SensorService.isRunning()) {
			doBindService();//
			updateStatus("Request to bind service");
		}
	}

	/**
	 * Binds the activity to the background service
	 */
	void doBindService() {
		bindService(new Intent(this, SensorService.class), mConnection, Context.BIND_AUTO_CREATE);
		updateStatus("Binding to Service...");
	}

	/**
	 * Unbind this activity from the background service
	 */
	void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with it, then now is the time to unregister.
			if (mService != null) {
				try {
					Message msg = Message.obtain(null, Constants.MESSAGE.UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has crashed.
				}
			}
			// Detach our existing connection.
			unbindService(mConnection);
			updateStatus("Unbinding from Service...");
		}
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtAccelerometer = (TextView) findViewById(R.id.txtAccel);
		txtGyroscope = (TextView) findViewById(R.id.txtGyro);
		txtStatus = (TextView) findViewById(R.id.txtStatus);

		bindToServiceIfIsRunning();

		Button btnStart = (Button) findViewById(R.id.btnStart);
		btnStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!mIsBound) {
					doBindService();
				}
				if(mIsBound) {
					Intent startIntent = new Intent(MainActivity.this, SensorService.class);
					startIntent.setAction(Constants.ACTION.START_FOREGROUND);
					startService(startIntent);
				}
			}
		});

		Button btnStop = (Button) findViewById(R.id.btnStop);
		btnStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!mIsBound) {
					doBindService();
				}
				if (mIsBound) {
					Intent stopIntent = new Intent(MainActivity.this, SensorService.class);
					stopIntent.setAction(Constants.ACTION.STOP_FOREGROUND);
					startService(stopIntent);
				}
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
		doUnbindService();
        super.onDestroy();
    }

	/**
	 * Updates the status message on the main UI
	 * @param string the new status message
	 */
	private void updateStatus(final String string) {
		this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	txtStatus.setText(string);
            }
        });
	}


}


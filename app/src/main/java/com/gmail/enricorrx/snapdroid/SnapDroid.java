package com.gmail.enricorrx.snapdroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Scanner;

import snaphttpd.client.SnapHttpClient;
import snaphttpd.server.Resource;
import snaphttpd.server.SnapHttpServer;

/*
    Second Activity, starts all threads that communicate with Snap/Snap4Arduino
*/
@SuppressWarnings("deprecation")
public class SnapDroid extends AppCompatActivity implements SensorEventListener {
	private SnapHttpClient httpLigth, httpStopButton, httpStartButton, httpProximity;
	private SnapHttpClient httpAzimuth, httpPitch, httpRoll;
	private SnapHttpServer server;
	private HttpChecker checker;
	private String IP;
	private final static int PORT=42001;
	private SensorManager mSensorManager;
	private Sensor mLigth, mOrientation, mProximity;
	private Vibrator mVibrator;
	private int pitch,roll,azimuth;
	private int pPitch, pRoll, pAzimuth; //previous values
	private float ligth, proximity;
	private boolean mustSend, mustReceive;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_snapdroid);

		// Keep the screen on
		getWindow().addFlags(WindowManager
				.LayoutParams.FLAG_KEEP_SCREEN_ON);

		final TextView iptv = findViewById(R.id.iptv);
		// Set iptv's text
		iptv.setText(String.format(
				getString(R.string.SnapDroid_IP), getMyIpAddress()));

		final Button startB = findViewById(R.id.startB);
		// Set button listener
		startB.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				if(mustSend)
					httpStartButton.send("pushed");
			}
		});

		final Button stopB = findViewById(R.id.stopB);
		// Set button listener
		stopB.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				if(mustSend)
					httpStopButton.send("pushed");
			}
		});

		// Retrieve information by Intent
		Intent intent = getIntent();
		mustSend = intent.getBooleanExtra("client",false);
		mustReceive = intent.getBooleanExtra("server",false);
		IP=intent.getStringExtra("IP");

		// Get an instance of Sensor Manager
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		// Get default sensors
		mLigth = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		// Initialize the server with Resource class, listening on port 8080
		server=new SnapHttpServer(new Resource() {
			// Anonymous class
			@Nullable
			@Override
			public String send(@NonNull String path) {
				// Sensors:
				if(path.equalsIgnoreCase("/+azimuth")) {
					return String.valueOf(azimuth);
				}
				if(path.equalsIgnoreCase("/+pitch")) {
					return String.valueOf(pitch);
				}
				if(path.equalsIgnoreCase("/+roll")) {
					return String.valueOf(roll);
				}
				if(path.equalsIgnoreCase("/+start-button"))
					return String.valueOf(startB.isPressed());
				if(path.equalsIgnoreCase("/+stop-button"))
					return String.valueOf(stopB.isPressed());
				if(path.equalsIgnoreCase("/+proximity"))
					return String.valueOf(proximity);
				if(path.equalsIgnoreCase("/+light"))
					return String.valueOf(ligth);

				// Commands:
				if(path.equalsIgnoreCase("/ping"))
					return "pong";
				if(path.contains("/vibrate/")){
					// Scan for parameter: /vibrate/param
					Scanner sc = new Scanner(path);
					sc.useDelimiter("/");
					int time;
					try{
						if(sc.next().equalsIgnoreCase("vibrate")) {
							time = sc.nextInt();
							// Vibrate for time milliseconds
							mVibrator.vibrate(time);
						}
					}catch(Exception e){
						// Do nothing
						return "BAD COMMAND";
					}
					// Return an acknowledgment message
					return "OK";
				}

                // For other sensors simply write:
				/*
                	if(path.equalsIgnoreCase("/resource"))
                    	return myValue;
                */

				// null --> 404 Not Found
				return null;
			}
		},8080);
	}

	@Override
	protected void onStart(){
		super.onStart();

		// Register sensors callback
		mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mLigth, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);

		// Start the server
		if(mustReceive)
			server.start();

		// Start all clients
		if(mustSend){
			// Start AsyncTask that check service availability
			checker = new HttpChecker();
			checker.execute();

			// Start sensors clients
			httpLigth = new SnapHttpClient(IP, PORT,"ligth");
			httpLigth.start();

			httpAzimuth = new SnapHttpClient(IP, PORT,"azimuth");
			httpPitch = new SnapHttpClient(IP, PORT,"pitch");
			httpRoll = new SnapHttpClient(IP, PORT,"roll");
			httpAzimuth.start();
			httpRoll.start();
			httpPitch.start();

			httpStopButton = new SnapHttpClient(IP, PORT, "stop-button");
			httpStopButton.start();

			httpStartButton = new SnapHttpClient(IP, PORT, "start-button");
			httpStartButton.start();

			httpProximity=new SnapHttpClient(IP,PORT,"proximity");
			httpProximity.start();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		// Unregister all listeners
		mSensorManager.unregisterListener(this);

		// Stop the server
		if(mustReceive)
			server.stop();

		// Stop the clients
		if(mustSend){
			// Cancel the AsyncTask
			checker.cancel(true);

			// Stop all sensor clients
			httpLigth.stop();

			httpAzimuth.stop();
			httpRoll.stop();
			httpPitch.stop();

			httpStopButton.stop();
			httpStartButton.stop();

			httpProximity.stop();
		}
	}

	@Override
	public void onSensorChanged(@NonNull SensorEvent event) {
		if(event.sensor.getType()==Sensor.TYPE_LIGHT) {
			ligth = event.values[0];
			if(mustSend)
				httpLigth.send(String.valueOf(ligth));
			return;
		}

		if((event.sensor.getType()==Sensor.TYPE_ORIENTATION)) {
			// Round values
			azimuth = Math.round(event.values[0]);
			pitch = Math.round(event.values[1]);
			roll = Math.round(event.values[2]);
			// Send value if it changes by at least 1 unit
			if(azimuth!= pAzimuth) {
				pAzimuth = azimuth;
				if(mustSend)
					httpAzimuth.send(String.valueOf(azimuth));
			}
			if(pitch!= pPitch) {
				pPitch = pitch;
				if(mustSend)
					httpPitch.send(String.valueOf(pitch));
			}
			if(roll!= pRoll) {
				pRoll = roll;
				if(mustSend)
					httpRoll.send(String.valueOf(roll));
			}
			return;
		}

		if(event.sensor.getType()==Sensor.TYPE_PROXIMITY){
			proximity = event.values[0];
			if(mustSend)
				httpProximity.send(String.valueOf(proximity));
			//noinspection UnnecessaryReturnStatement
			return;
		}
	}

	// Get the IPv4 address
	public static String getMyIpAddress() {
		try {
			// Search interfaces
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				// Search addresses
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException ignored) {}
		return null;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@SuppressLint("StaticFieldLeak")
	private class HttpChecker extends AsyncTask<Void, Integer, Void> {
		@Nullable
		protected Void doInBackground(Void...voids) {
			while(!isCancelled()) {
				// Check host availability
				if(!serviceAvailabilityCheck())
					return null; // Call onPostExecute()
				try {
					// Sleep 1500 milliseconds
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					Log.d("httpChecker",e.getMessage());
				}
			}
			return null;
		}

		protected void onProgressUpdate(Integer... progress) {}

		protected void onPostExecute(Void v) {
			// Executing on UI thread
			Toast.makeText(SnapDroid.this, "Host unreachable!", Toast.LENGTH_LONG).show();
			finish();
		}

		private boolean serviceAvailabilityCheck() {
			try (Socket s = new Socket()) {
				// Connect the socket with a timeout of 1500 milliseconds
				s.connect(new InetSocketAddress(IP,PORT), 1000);
				return true;
			} catch (IOException ex) {
				// The service is unavailable
				return false;
			}
		}
	}
}

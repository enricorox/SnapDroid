package com.gmail.enricorrx.snapdroid;


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

public class SnapDroid extends AppCompatActivity implements SensorEventListener {
    private SnapHttpClient httpLigth, httpStopButton, httpStartButton, httpProximity;
    private SnapHttpClient httpAzimuth, httpPitch, httpRoll;
    SnapHttpServer server;
    private String IP;
    private final static int PORT=42001;
    SensorManager mSensorManager;
    private Sensor mLigth, mOrientation;
    private Vibrator mVibrator;
    private int pitch,roll,azimuth;
    private int pPitch, pRoll, pAzimuth; //previous values
    private boolean mustSend, mustReceive;
    private float ligth, proximity;
    private Sensor mProximity;
    private HttpChecker checker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snapdroid);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //keep the screen on

        final TextView iptv = findViewById(R.id.iptv);
        iptv.setText("SnapDroid IP: "+getMyIpAddress());

        final Button startB = findViewById(R.id.startB);
        startB.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(mustSend)
                    httpStartButton.send("pushed");
            }
        });

        final Button stopB = findViewById(R.id.stopB);
        stopB.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(mustSend)
                    httpStopButton.send("pushed");
            }
        });

        Intent intent = getIntent();
        mustSend = intent.getBooleanExtra("client",false);
        mustReceive = intent.getBooleanExtra("server",false);
        IP=intent.getStringExtra("IP");

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mLigth = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


        server=new SnapHttpServer(new Resource() {
            //anonymous class
            @Nullable
            @Override
            public String send(@NonNull String path) {
                //Sensors:
                if(path.equalsIgnoreCase("/+azimuth")) {
                    //updateOrientationAngles();
                    return String.valueOf(azimuth);
                }
                if(path.equalsIgnoreCase("/+pitch")) {
                    //updateOrientationAngles();
                    return String.valueOf(pitch);
                }
                if(path.equalsIgnoreCase("/+roll")) {
                    //updateOrientationAngles();
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
                //Commands:
                if(path.equalsIgnoreCase("/ping"))
                    return "pong";
                if(path.contains("/vibrate/")){
                    Scanner sc = new Scanner(path);
                    sc.useDelimiter("/");
                    int time;
                    try{
                        if(sc.next().equalsIgnoreCase("vibrate")) {
                            time = sc.nextInt();
                            mVibrator.vibrate(time);
                        }
                    }catch(Exception e){
                        return "BAD COMMAND";
                    }
                    return "OK";
                }


                /* For other sensors simply write
                if(path.equalsIgnoreCase("/resource"))
                    return String.valueOf(resource);
                */

                return null; //it's ok to return null, means 404
            }
        },8080);
    }

    @Override
    protected void onStart(){
        super.onStart();

        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mLigth, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);

        if(mustReceive)
            server.start();

        if(mustSend){
            checker = new HttpChecker();
            checker.execute();

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

        if(mustReceive)
            server.stop();

        mSensorManager.unregisterListener(this); //unregisters all listeners!

        if(mustSend){
            checker.cancel(true);

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

        if((event.sensor.getType()==Sensor.TYPE_ORIENTATION)) { //(azimuth,pitch,roll)
            //rounds values
            azimuth = Math.round(event.values[0]);
            pitch = Math.round(event.values[1]);
            roll = Math.round(event.values[2]);
            //filter
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
            return;
        }

    }

    public static String getMyIpAddress() {
        try {
            //search interfaces
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                //search addresses
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            //Log.e("Socket exception in GetIP Address of Utilities", ex.toString());
        }
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class HttpChecker extends AsyncTask<Void, Integer, Void> {
        @Nullable
        protected Void doInBackground(Void...voids) {
            while(!isCancelled()) {
                if(!hostAvailabilityCheck())
                    return null;
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Log.d("httpChecker",e.getMessage());
                }
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Void v) {
            Toast.makeText(SnapDroid.this, "Host unreachable!", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    public boolean hostAvailabilityCheck() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(IP,PORT), 1000);
            return true;
        } catch (IOException ex) {
            return false;
        }

    }

}

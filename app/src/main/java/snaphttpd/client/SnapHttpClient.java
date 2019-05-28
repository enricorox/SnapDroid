package snaphttpd.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedDeque;

/*
	Client for Android's sensors
	It communicates with Snap4Arduino's server
*/
public class SnapHttpClient implements Runnable{
	@NonNull
	private final ConcurrentLinkedDeque<String> dataQueue;
	@Nullable
	private InetAddress serverAddress=null;
	private final int serverPort;
	private final String name, broadcast;
	// Volatile variable can't be cached
	private volatile boolean stopped;

	public SnapHttpClient(String serverAddress, int serverPort, String name){
		try {
			// Get server address
			this.serverAddress = InetAddress.getByName(serverAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		stopped = true;
		this.serverPort = serverPort;
		this.dataQueue = new ConcurrentLinkedDeque<>();

		// Name is the sensor identifier
		this.name = name;
		// Build broadcast message once
		broadcast = "GET " + "/broadcast=+" + this.name + " HTTP/1.1\r\n" +
				"Host: " + serverAddress + ":" + serverPort + "\r\n" +
				"Connection: keep-alive\r\n" +
				"\r\n";
	}

	private void waitRequest() {
		try {
			Log.d(name, "Waiting new request...");

			// Wait until new data or client is stopped
			while (dataQueue.isEmpty() || stopped)
				synchronized (this) {
					wait();
				}
		}catch(InterruptedException ignored){}
	}

	@Override
	public void run(){
		stopped=false;
		// Run forever until stopped
		while(!isStopped()) {
			Socket socket;
			try {
				socket = new Socket();
				//-------------------------------------------
				// WARNING: Snap4Arduino's Socket has extremely low timeout!!! Risk of TCP [RST] flag!
				// Connect the Socket
				socket.connect(new InetSocketAddress(serverAddress, serverPort), 0);
				Log.d(name,"Connected!");

				// Make buffers
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), false);

				// Start the session
				while (true) {//---session starts---
					waitRequest();
					Log.d(name,"Response acquired! :)");

					// Stop if stopped==true
					if(isStopped()){
						Log.d(name,"Stopping session...");
						break;
					}

					//Update variable on Snap4Arduino and broadcast message
					String request;
					for(int i=0;i<2;i++) {
						// Choose the request
						if(i==0)
							request = forgeRequestData(dataQueue.getLast());
						else
							request = broadcast;

						Log.d(name,"Sending request...");
						// Send the request
						out.print(request);
						out.flush();
						//------------------------------------

						// Read the status-line
						String response = in.readLine();
						if (response == null) { //socket closed by server
							Log.d(name, "[RST] Connection reset!");
							break;
						}

						// Read and IGNORE the request
						// assuming Transfer-Encoding: chunked
						// assuming Connection: keep-alive
						while (!response.equals("0")) {
							response = in.readLine();
						}

						// Read the last \r\n
						in.readLine();

						// Remove data after message is sent
						if(i==1)
							dataQueue.removeLast();
					}
				}//---session ends---
				out.close();
				in.close();
				socket.close();
			}catch(Exception e){
				e.printStackTrace();
			}//--end try-catch
		}//--end while(!isStopped())
		Log.d(name,"Stopped.");
	}//--end run()

	// Start client in new thread
	public void start(){
		Thread t=new Thread(this);
		t.start();
		Log.d(name,"Thread " + name + " started!");
	}

	public void send(String data){
		// If stopped do nothing
		if(isStopped())
			return;

		// Save the data
		dataQueue.addFirst(data);
		// Notify the client
		synchronized (this) {
			notify();
		}

		Log.d(name,"New request! Pending: " + dataQueue.size());
	}

	// Build the request for updating the variable
	private String forgeRequestData(String s){
		return "GET /vars-update=+"+ name +"="+s + " HTTP/1.1\r\n"+
				"Host: "+serverAddress+":"+serverPort+"\r\n"+
				"Connection: keep-alive\r\n"+
				"\r\n";
	}

	// Stop the client
	public void stop(){
		stopped = true;
		// Client stopped, notify the client
		synchronized (this) {
			notify();
		}
		Log.d(name,"Stopping...");
	}
	public boolean isStopped(){
		return stopped;
	}
}

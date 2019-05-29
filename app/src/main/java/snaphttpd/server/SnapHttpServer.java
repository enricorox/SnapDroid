package snaphttpd.server;
import android.support.annotation.NonNull;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

// Minimal HTTP server class.
public class SnapHttpServer implements Runnable{
	private final String TAG = "snap-server";
	// Server socket port
	private final int port;
	// Tcp connection queue's length
	private final int backlog;
	// Volatile variable can't be cached
	private volatile boolean stopped;
	private ServerSocket serverSocket;
	private final List<HttpSession> openSessions;
	private Resource resource;

	// Construct server listening at port aport with optional debug
	public SnapHttpServer(Resource r, int aport) {
		// Initialization
		resource =r;
		port=aport;
		stopped=true;
		backlog=5;
		// List of active sessions
		openSessions = Collections.synchronizedList(new LinkedList<HttpSession>());
	}

	// Start the server
	public void run() {
		stopped=false;
		Log.d(TAG,"Starting server on port " + port);
		Log.d(TAG,"Connect to "+ getMyIpAddress()+":"+port);

		try{
			// Initialize the listening socket
			serverSocket = new ServerSocket(port,backlog);
			// We can reuse that port
			serverSocket.setReuseAddress(true);

			// Run forever until stopped
			while(!isStopped()) {
				// Accept connection, suspensive call!
				Socket clientSocket = serverSocket.accept();

				// Make a new HTTP session
				HttpSession s = new HttpSession(this, clientSocket);
				// Add it in the active sessions list
				synchronized (openSessions) {
					openSessions.add(s);
				}
				// Start that session in a new thread
				Thread t = new Thread(s);
				t.start();
			}
		} catch (Exception e) {
			// If server is stopped, return
			if(isStopped()) {
				Log.d(TAG,"Server killed.");
				return;
			}
			// If there are errors print them
			e.printStackTrace();
		}
	}

	// Callback from HttpSession
	@NonNull
	Response requestHandler(@NonNull Request req) {
		// If we don't have implemented nothing
		if(!req.isGet() || resource == null)
			return new Response(501,"NOT IMPLEMENTED");		

		// Get a body from Resource object
		String body = resource.send(req.getPath());

		// If there isn't a body
		if(body==null)
			return new Response(404,"404 NOT FOUND").setKeepAlive(false);

		// If all is OK
		return new Response(200,body); 
	}	
	
	// Start the server in a new thread
	public void start() {
		Thread t=new Thread(this); //"this" is Runnable
		t.start();
		Log.d(TAG,"Server started");
	}

	
	// Stop the server and all open sessions
	public void stop() {
		Log.d(TAG,"Stopping...");
		stopped=true;

		// Stop all sessions
		synchronized(openSessions) {
			for (HttpSession openSession : openSessions) {
				openSession.stop();
			}
		}

		// Close the socket
		try {			
			serverSocket.close();
			Log.d(TAG,"Server socket closed!");
		} catch (Exception e) {
			Log.d(TAG,"Error closing Server socket!");
			e.printStackTrace();
		}
	}

	// Remove a stopped session
	void removeSession(HttpSession s) {
		synchronized(openSessions) {
			openSessions.remove(s);
		}
	}

	public boolean isStopped() {
		return stopped;
	}

	public boolean isRunning() {
		return !stopped;
	}

	// Get IPv4 address
	public static String getMyIpAddress() {
        try {
        	// Search interfaces
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                // Search addresses
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address))
						return inetAddress.getHostAddress();
                }
            }
        } catch (SocketException ignored) {}
        return null; 
	}
}

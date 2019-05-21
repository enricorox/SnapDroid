package snaphttpd.server;
import android.support.annotation.NonNull;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;



/**
 * Minimal HTTP server class.
 * <p>You must implement giveBody(String body) to handle GET request only.
 * <p>you can override handleRequest(Request req) for handling all request. 
 * 
 * @author Enrico R.
 * @see HttpSession 
 * @see Request
 * @see Response
 */
public class SnapHttpServer implements Runnable{
	private final String TAG="snap-server";
	private final int port;//server socket port
	private final int backlog;//queue length of tcp connection
	private final int maxThread;//max number of thread handling connections
	private volatile boolean stopped; //thread-safe
	private ServerSocket serverSocket; //NOTE stop() can close it!
	private final List<HttpSession> openSessions;
	private Resource resource;

	/**
	 * Construct server listening at port aport with optional debug
	 * @param r String resource that maps path to resource. Must be thread-safe
	 * @param aport Listening port
	 */
	public SnapHttpServer(Resource r, int aport) {
		//initialization 
		resource =r;
		port=aport;
		stopped=true;
		backlog=5;
		maxThread=15;
		openSessions = Collections.synchronizedList(new LinkedList<HttpSession>());
	}
	/**
	 * Start the server synchronously.
	 */
	public void run() {
		Log.d(TAG,"Starting server on port " + port);
		Log.d(TAG,"Connect to "+ getMyIpAddress()+":"+port);
		stopped=false;
		try{
			serverSocket = new ServerSocket(port,backlog);			
			serverSocket.setReuseAddress(true); // we can reuse that port
            //serverSocket.setSoTimeout(1000);
			while(!isStopped()) {
				try {
					Socket clientSocket = serverSocket.accept(); //suspensive call!
					//SOCKET HANDLING
					HttpSession s = new HttpSession(this, clientSocket);
					synchronized (openSessions) {
						openSessions.add(s);
					}
					Thread t = new Thread(s);
					t.start();
				}catch(SocketTimeoutException e){
					Log.d(TAG,"Socket timeout"); //TODO not properly catched
				}
			}
		} catch (Exception e) {
			if(isStopped()) {
				Log.d(TAG,"Server killed.");
				return;
			}
			e.printStackTrace();
		}
	}
	/**
	 *
	 */
	//override in subclasses!
	//handle only get by default
	@NonNull
	Response requestHandler(@NonNull Request req) {
		if(!req.isGet() || resource ==null)
			return new Response(501,"NOT IMPLEMENTED");		

		String body= resource.send(req.getPath()); //custom body?
		
		if(body==null)//no body
			return new Response(404,"404 NOT FOUND").setKeepAlive(false);
		
		return new Response(200,body); 
	}	
	
	/**
	 * Start the server asynchronously.
	 */
	public void start() {
		Thread t=new Thread(this); //"this" is Runnable
		t.start();
		Log.d(TAG,"Server started");
	}

	
	// is thread-safe? YES
	public void stop() {
		Log.d(TAG,"Stopping...");
		stopped=true;	
		synchronized(openSessions) {
			Iterator<HttpSession> it=openSessions.iterator();
			while(it.hasNext()) {
				it.next().stop();
			}
		}
		try {			
			serverSocket.close();
			Log.d(TAG,"Server socket closed!");
		} catch (Exception e) {
			Log.d(TAG,"Error closing Server socket!");
			e.printStackTrace();
		}
	}
	//callback from httpSession 
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
	public static String getMyIpAddress() {
        try {
        	//search interfaces
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                //search addresses
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address))
						return inetAddress.getHostAddress();
                }
            }
        } catch (SocketException ex) {
            //Log.e("Socket exception in GetIP Address of Utilities", ex.toString());
        }
        return null; 
	}
	
}

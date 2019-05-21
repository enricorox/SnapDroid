package snaphttpd.server;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

//task handling one socket 
public 	class HttpSession implements Runnable{
	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;
	private SnapHttpServer httpd;
	private final String TAG="snap-server-session";
	private boolean stopped;
	public HttpSession(SnapHttpServer server, Socket aClientSocket) {
		httpd=server;
		clientSocket=aClientSocket;		
	}
	public void run() {
		try {
			Log.d(TAG,"["+clientSocket.getInetAddress()+":"+clientSocket.getPort()+"] accepted");		
			out = new PrintWriter(clientSocket.getOutputStream(), true);
		    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		    //Request req= new Request();
			clientSocket.setSoTimeout(3000);
		    while(!isStopped()) {
				Request req = new Request();
		    	//req.reset(); //new request			    					
				while(req.parseRequest(in.readLine())); //reading request line by line
				//NOTE: [Chromium] in.readLine() return null after first response
				if(req.isSocketBroken()) {
					Log.d(TAG,"Broken Socket! Nothing to do.");
					break;					
				}
				Log.d(TAG,"***new request***");								
				//handling request					
				Response res;
				
				if(req.isGood()) {
					Log.d(TAG,"Good request");
					/*
					if(Thread.activeCount()>httpd.maxThread) {
						res = new Response(503, "503 SERVICE UNAVAILABLE (SERVER OVERLOADED)").setKeepAlive(false);
						out.println(res.toString());
						break;
					}else
					*/
						res=httpd.requestHandler(req);
					res.setProtocol(req.getProtocol()); //set the same protocol
					res.setKeepAlive(req.hasKeepAlive());
					out.println(res.toString());
					if(!req.hasKeepAlive())
						break;
				}else {
					Log.d(TAG,"Bad request");
					res = new Response(400, "400 BAD REQUEST");
					out.println(res.toString());
					break;
				}
		    }		
			//closing resources
		    stop();
		}catch(IOException e){
			if(e instanceof SocketTimeoutException){
				Log.d(TAG,"Client socket timeout.");
				stop();
			}
			if(clientSocket.isConnected()) {
				stop();
				Log.d(TAG,"Client socket closed.");
				return;
			}
			if(httpd.isStopped()) {				
				return;
			}
			
			e.printStackTrace();
			Log.d(TAG,"WEIRD ERROR(S)");
		}
	}
	
	public void stop() {
		stopped=true;
		try {
			out.close();
			in.close();
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.d(TAG,"["+clientSocket.getInetAddress()+":"+clientSocket.getPort()+"] closed");
		httpd.removeSession(this);
	}

	public boolean isStopped() {
		return stopped;
	}
}

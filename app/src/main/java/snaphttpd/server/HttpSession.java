package snaphttpd.server;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

// Manage one HTTP session
public 	class HttpSession implements Runnable{
	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;
	private SnapHttpServer httpd;
	private final String TAG="snap-server-session";
	private volatile boolean stopped;

	public HttpSession(SnapHttpServer server, Socket clientSocket) {
		// Save a reference to the caller SnapHttpServer
		httpd = server;
		// Socket for this session
		this.clientSocket = clientSocket;
	}

	public void run() {
		try {
			Log.d(TAG,"["+clientSocket.getInetAddress()+":"
					+clientSocket.getPort()+"] accepted");

			// Buffering
			out = new PrintWriter(clientSocket.getOutputStream(), true);
		    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		    // Set timeout to 3000 milliseconds
			clientSocket.setSoTimeout(3000);

			// Run forever until stopped
		    while(!isStopped()) {
				Request req = new Request();

				// Parse the request line by line
				//noinspection StatementWithEmptyBody
				while(req.parseRequest(in.readLine()));

				// Check if there are errors
				if(req.isSocketBroken()) {
					Log.d(TAG,"Broken Socket! Nothing to do.");
					// Stop the session
					break;					
				}
				Log.d(TAG,"***new request***");

				// If we don't have errors
				if(req.isGood()) {
					Log.d(TAG,"Good request");

					// Get a response from SnapHttpServer
					Response res = httpd.requestHandler(req);
					// Set the same protocol
					res.setProtocol(req.getProtocol());
					// Set keep-alive
					res.setKeepAlive(true);
					// Send the response
					out.println(res.toString());

					// If request doesn't have keep-alive,
					// stop the session
					if(!req.hasKeepAlive())
						break;
				}else {
					// Request malformed
					Log.d(TAG,"Bad request");
					// Make a 400 Response
					Response res = new Response(400, "400 BAD REQUEST");
					// Insert connection close
					res.setKeepAlive(false);
					// Send response
					out.println(res.toString());
					// Stop session
					break;
				}
		    }
			// Stop the session
		    stop();
		}catch(IOException e){
			// Catch socket timeout
			if(e instanceof SocketTimeoutException){
				Log.d(TAG,"Client socket timeout.");
				stop();
				return;
			}

			// If server is stopping
			if(httpd.isStopped()) {
				stop();
				return;
			}
			
			e.printStackTrace();
		}
	}

	// Stop the session and close the resources
	public void stop() {
		// Change the flag
		stopped=true;
		// Close resource
		try {
			out.close();
			in.close();
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Log socket closed
		Log.d(TAG,"["+clientSocket.getInetAddress()+":"
				+clientSocket.getPort()+"] closed");

		// Remove from server's active sessions
		httpd.removeSession(this);
	}

	public boolean isStopped() {
		return stopped;
	}
}

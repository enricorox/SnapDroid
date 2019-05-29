package snaphttpd.server;

import android.support.annotation.Nullable;

import java.util.Scanner;

public class Request{
	@Nullable
	private String method,path,protocol;
	private boolean first,keepAlive;
	// Errors:
	private boolean unknownProtocol,unknownMethod,brokenSocket,badRequestLine;
	
	public Request() {
		// Value to parse
		method = null;
		path = null;
		protocol = null;

		// Default flags
		first = true;
		keepAlive = true;

		// There is no error
		unknownProtocol = false;
		unknownMethod = false;
		brokenSocket = false;
		badRequestLine = false;
	}

	// Parse request line by line
	// return false if request is completed
	boolean parseRequest(@Nullable String s) {
		// String can't be null
		if(s==null) {			
			brokenSocket=true;
			return false;
		}

		// If there is a request-line
		if(first) { 			
			if(!s.isEmpty()) {
				first=false;
				// Parsing request-line:
				try {
					Scanner sc=new Scanner(s);
					method=sc.next();
					path=sc.next();
					protocol=sc.next();
					sc.close();

					// Check method
					if( method == null ||
							!(method.equalsIgnoreCase("GET")
							|| method.equalsIgnoreCase("HEAD")
							|| method.equalsIgnoreCase("POST")
							|| method.equalsIgnoreCase("PUT")
							|| method.equalsIgnoreCase("DELETE")
							|| method.equalsIgnoreCase("TRACE")
							|| method.equalsIgnoreCase("CONNECT")))
						 unknownMethod = true;

					// Check protocol
					if(!(protocol.equalsIgnoreCase("HTTP/1.1")
							|| protocol.equalsIgnoreCase("HTTP/1.0")
							|| protocol.equalsIgnoreCase("HTTP/0.9")))
						unknownProtocol=true;
				} catch (Exception e) {
					// Request-line malformed: scanner has not enough element
					badRequestLine=true;										
				}
			}
			// If first line is empty, does nothing
			return true;
		}

		// We have a not-first line

		// Request (without body) ends with empty line
		// CRLF is omitted by readLine()
		if(s.isEmpty())
			return false; //Parser has finished

		// Parse connection header
		if(s.contains("Connection: keep-alive"))
			keepAlive=true;
		if(s.contains("Connection: close"))
			keepAlive=false;
		
		// Other header are IGNORED!
		return true;
	}

	@Nullable
	public String getMethod() {
		return method;
	}

	@Nullable
	public String getPath() {
		return path;
	}

	@Nullable
	public String getProtocol() {
		return protocol;
	}

	// Return true if there is one or more null line
	public boolean isSocketBroken() {
		return brokenSocket;
	}

	// Return true if there are no errors
	public boolean isGood() {
		return !(unknownProtocol || unknownMethod || brokenSocket || badRequestLine);
	}

	public boolean hasKeepAlive() {
		return keepAlive;
	}

	// Return true if we have a GET method
	public boolean isGet() {
		if(method==null)
			return false;
		return method.equalsIgnoreCase("GET");
	}
}	

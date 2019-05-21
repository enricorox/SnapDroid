package snaphttpd.server;

import android.support.annotation.Nullable;

import java.util.Scanner;

public class Request{
	//private LinkedList<Header> headers=new LinkedList<Header>();
	@Nullable
	private String method,path,protocol;//TODO: change to StringBuilder to improve performance!!!
	private boolean first,keepAlive;
	//errors:
	private boolean unknownProtocol,unknownMethod,brokenSocket,badRequestLine;
	
	public Request() {
		reset();
	}
	//reset all flags and strings
	public void reset() {
		unknownProtocol=false;
		unknownMethod=false;
		brokenSocket=false;
		badRequestLine=false;
		first=true;
		keepAlive=true;
		method=null;
		path=null;
		protocol=null;
	}
	//Parser
	//return false if request is completed
	public boolean parseRequest(@Nullable String s) {
		//d("request-line-by-line: "+s);		
		//ERROR HANDLING:
		if(s==null) {			
			brokenSocket=true;
			return false;
		}
		if(first) { 			
			if(!s.isEmpty()) {
				first=false;
				//parsing request-line:
				try {
					Scanner sc=new Scanner(s);
					method=sc.next();
					path=sc.next();
					protocol=sc.next();
					sc.close();
					
					if(!(method.equalsIgnoreCase("GET")
							|| method.equalsIgnoreCase("HEAD")
							|| method.equalsIgnoreCase("POST")
							|| method.equalsIgnoreCase("PUT")
							|| method.equalsIgnoreCase("DELETE")
							|| method.equalsIgnoreCase("TRACE")
							|| method.equalsIgnoreCase("CONNECT")))
						 unknownMethod=true;
					if(!(protocol.equalsIgnoreCase("HTTP/1.1")
							|| protocol.equalsIgnoreCase("HTTP/1.0")
							|| protocol.equalsIgnoreCase("HTTP/0.9")))
						unknownProtocol=true;											
				} catch (Exception e) {
					//request-line malformed: scanner has not enough element
					badRequestLine=true;										
				}
				//if first line is empty, does nothing
				//---
			}	
			return true;
		}
		//request (without body) ends with single CRLF line
		//CRLF is omitted by readLine()
		if(s.isEmpty())
			return false; //Parser has finished
		//Header parsing
		if(s.contains("Connection: keep-alive"))
			keepAlive=true;
		if(s.contains("Connection: close"))
			keepAlive=false;
		
		//NOTE: Other header are ignored!
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
	public boolean isSocketBroken() {
		return brokenSocket;
	}
	public boolean isGood() {
		return !(unknownProtocol || unknownMethod || brokenSocket || badRequestLine);
	}
	public boolean hasKeepAlive() {
		return keepAlive;
	}	
	public boolean isGet() {
		if(method==null)
			return false;
		return method.equalsIgnoreCase("GET");
	}
	
}	

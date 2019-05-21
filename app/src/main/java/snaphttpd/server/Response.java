package snaphttpd.server;

import android.support.annotation.NonNull;

public class Response{
	private boolean keepAlive,cors; //CrossOriginResourceSharing
	private int code;//status code
	private String body,protocol;//message-body
	public Response(int acode) {
		this(acode,null);
	}
	public Response(int acode,String abody) {		
		code=acode;
		body=abody;

		//default parameters
		protocol="HTTP/1.1";
		cors=true;
		keepAlive=true; //HTTP/1.1 compliant
	}
	@NonNull
	public Response setProtocol(String p) {
		protocol=p;
		return this;
	}
	@NonNull
	public Response setCors(boolean on) {
		cors=on;
		return this;
	}
	@NonNull
	public Response setKeepAlive(boolean on) {
		keepAlive=on;
		return this;
	}
	@NonNull
	public String toString() {
		//building status-line
		StringBuilder r=new StringBuilder(protocol);
		r.append(" ");
		//choose status code & status phrase
		switch(code) {
		case 200: r.append("200 OK\r\n"); break;									
		case 400: r.append("400 Bad Request\r\n"); break;				
		case 404: r.append("404 Not Found\r\n"); break;			
		case 501: r.append("501 Not Implemented\r\n"); break;
		case 503: r.append("503 Service Unavailable\r\n"); break;
		default: r.append("500 Internal Server Error\r\n"); break;				 										
		}
		//building headers
		r.append("Cache-Control: no-store\r\n");
		if(cors)
			r.append("Access-Control-Allow-Origin: *\r\n");
		if(keepAlive)
			r.append("Connection: keep-alive\r\n");
		else
			r.append("Connection: close\r\n");
		
		if(body!=null) {
			r.append("Content-Length: "+body.length()+"\r\n");
			r.append("Content-Type: text/html\r\n\r\n");//end of headers
			r.append(body);
		}else
			r.append("\r\n");//end of headers
		return r.toString();
	}
}

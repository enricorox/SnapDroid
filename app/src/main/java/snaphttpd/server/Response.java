package snaphttpd.server;

import android.support.annotation.NonNull;

// Model of an HTTP Response
public class Response{
	private boolean keepAlive,cors;
	private int code;
	private String body,protocol;

	public Response(int code) {
		this(code,null);
	}

	public Response(int code,String body) {
		// Set status code
		this.code = code;
		// Set body
		this.body = body;

		// Default parameters:
		// Set protocol
		protocol="HTTP/1.1";
		// Set Cross Origin Resource Sharing
		cors=true;
		// Set keep-alive
		keepAlive=true;
	}

	@NonNull
	public Response setProtocol(String p) {
		protocol=p;
		return this;
	}

	@NonNull
	public Response setCors(boolean on) {
		cors = on;
		return this;
	}

	@NonNull
	public Response setKeepAlive(boolean on) {
		keepAlive = on;
		return this;
	}

	// Return the response
	@NonNull
	public String toString() {
		// Choose status-line
		StringBuilder r=new StringBuilder(protocol);
		r.append(" ");

		// Choose status code and status phrase
		switch(code) {
		case 200: r.append("200 OK\r\n"); break;									
		case 400: r.append("400 Bad Request\r\n"); break;				
		case 404: r.append("404 Not Found\r\n"); break;			
		case 501: r.append("501 Not Implemented\r\n"); break;
		case 503: r.append("503 Service Unavailable\r\n"); break;
		default: r.append("500 Internal Server Error\r\n"); break;				 										
		}

		// Choose headers
		r.append("Cache-Control: no-store\r\n");
		if(cors)
			r.append("Access-Control-Allow-Origin: *\r\n");
		if(keepAlive)
			r.append("Connection: keep-alive\r\n");
		else
			r.append("Connection: close\r\n");

		// If there is a body
		if(body!=null) {
			r.append("Content-Length: ").append(body.length()).append("\r\n");
			r.append("Content-Type: text/html\r\n");
			// End of headers
			r.append("\r\n");
			// Insert body
			r.append(body);
		}else {
			// End of headers
			r.append("\r\n");
		}
		return r.toString();
	}
}

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

public class SnapHttpClient implements Runnable{
    @NonNull
    private final ConcurrentLinkedDeque<String> queue;

    @Nullable
    private InetAddress serverAddress=null;
    private final int serverPort;
    private final String TAG;
    @NonNull
    private final String broadcast;
    private boolean stopped;

    public SnapHttpClient(String serverAddress, int serverPort, String name){
        try {
            this.serverAddress= InetAddress.getByName(serverAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.serverPort=serverPort;
        this.queue= new ConcurrentLinkedDeque<>();
        TAG=name;
        stopped = true;
        broadcast="GET " + "/broadcast=+" + TAG + " HTTP/1.1\r\n"+
                "Host: "+serverAddress+":"+serverPort+"\r\n"+
                "Connection: keep-alive\r\n"+
                "\r\n";
    }

    private void waitRequest() {
        try {
            Log.d(TAG, "Waiting new request...");
            while (queue.isEmpty() || stopped)
                synchronized (this) {
                    wait();
                }
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        while(!isStopped()) {
            Socket socket;
            try {
                socket = new Socket();
                //-------------------------------------------
                //WARNING: Snap4Arduino's Socket has extremely low timeout!!! Risk of TCP [RST] flag!
                socket.connect(new InetSocketAddress(serverAddress, serverPort), 0);
                Log.d(TAG,"Connected!");
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), false);
                boolean sessionIsOpen = true;
                while (sessionIsOpen) {//---session starts---
                    waitRequest();
                    Log.d(TAG,"Response acquired! :)");
                    if(isStopped()){//stopped triggered!
                        Log.d(TAG,"Stopping session...");
                        break;
                    }
                    //Update variable and broadcast message
                    String request;
                    for(int i=0;i<2;i++) {
                        if(i==0)
                            request = forgeRequestData(queue.getLast());
                        else
                            request = broadcast;
                        Log.d(TAG,"sending request...");
                        out.print(request);
                        out.flush();
                        //-------------------------------------------

                        String response = in.readLine(); //status-line
                        if (response == null) { //socket closed by server
                            Log.d(TAG, "[RST] Connection reset!");
                            sessionIsOpen=false;
                            break;
                        }


                        while (!response.equals("0")) { //assuming transfer-encoding: chunked
                            response = in.readLine();
                            //Log.d(TAG, "RES: " + response);
                            if (response.equalsIgnoreCase("Connection: close"))
                                sessionIsOpen = false;
                        }
                        in.readLine(); //read the last \r\n
                        if(i==1)
                            queue.removeLast();
                    }

                }//---session ends---
                out.close();
                in.close();
                socket.close();
            }catch(Exception e){
                e.printStackTrace();
            }//--end catch
        }//--end while(isStopped())
        Log.d(TAG,"Stopped.");
    }//--end run()

    public void start(){
        stopped=false;
        Thread t=new Thread(this);
        t.start();
        Log.d(TAG,"Thread "+TAG+" started!");
    }

    public void send(String data){
        if(isStopped())
            return;

        queue.addFirst(data);
        synchronized (this) {
            notify();
        }
        Log.d(TAG,"New request! Pending: "+queue.size());
    }

    private String forgeRequestData(String s){
        return "GET /vars-update=+"+TAG+"="+s + " HTTP/1.1\r\n"+
                "Host: "+serverAddress+":"+serverPort+"\r\n"+
                "Connection: keep-alive\r\n"+
                "\r\n";
    }


    public void stop(){
        stopped =true;
        synchronized (this) {
            notify();
        }
        Log.d(TAG,"Stopping...");
    }
    public boolean isStopped(){
        return stopped;
    }
}

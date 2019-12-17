package com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiClient {
    public static final int DEFAULT_TIMEOUT = 2000;
    int keepAliveDelay = 3000;
    int keepAliveBuffer = 2000;//mills added onto keepAliveDelay. will be the sockets SO Timeout
    Socket socket;
    String application;
    String systemName;
    //if true, just handshake, then disconnect
    public boolean ping;
    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;

    public String serverName;
    Thread readLoopThread,keepAliveThread;
    WifiEventHandler eventHandler;
    WifiClient thisClient;

    AtomicBoolean connectionOpen = new AtomicBoolean(false);
    public String ip;
    public int port;
    Thread handshakeThread;
    int timeout;


    public WifiClient(String ip,int port,int timeout,String systemName,String application,boolean ping,WifiEventHandler eventHandler) {
        this.thisClient = this;
        this.eventHandler = eventHandler;
        this.application = application;
        this.systemName = systemName;
        this.ping = ping;
        this.ip = ip;
        this.port = port;
        socket = new Socket();
        this.timeout = timeout;

        if(ping){
            // do nothing, must run startHandshake after creating this object to ping

        }else{//runs in new Thread - handshakeThread if not a ping
            handshakeThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    startHandshake();
                }
            });handshakeThread.start();
        }



    }








    public void startHandshake(){
                //try to connect to server
                try {
                    socket.connect(new InetSocketAddress(ip,port),timeout);
                    eventHandler.clientConnectionMade(thisClient);
                    connectionOpen.set(true);
                } catch (IOException e) {
                    //e.printStackTrace();
                    //on fail, 'disconnect' and notify
                    disconnect(e);//should not run because connection is closed
                    eventHandler.clientFailedToConnect(thisClient,e);

                }
                //if fail... skip all of the following

                //create object streams
                if(connectionOpen.get())try {
                    objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                } catch (IOException e) {
                    disconnect(e);
                }
                if(connectionOpen.get())try {
                    objectInputStream = new ObjectInputStream(socket.getInputStream());
                } catch (IOException e) {
                    disconnect(e);
                }
                //create a message
                Message message = new Message();
                //server will read this message, then send server info, if it sends it, application is same.
                if(connectionOpen.get()){
                    message.ping=ping;
                    message.systemName=systemName;
                    message.application=application;
                }
                //send the handshake message
                if(connectionOpen.get())try {
                    objectOutputStream.writeObject(message);
                    eventHandler.clientHandshakeMessageSent(thisClient);
                } catch (IOException | NullPointerException e) {
                    disconnect(e);
                }
                //try to read a handshake message from server (might not ever come), connection might be terminated server side if application is not the same.
                if(connectionOpen.get())try {
                    message=(Message)objectInputStream.readObject();
                    eventHandler.clientHandshakeMessageRead(thisClient);
                    serverName=message.systemName;
                    eventHandler.clientHandshakeSuccessful(thisClient);
                    if(ping){
                        disconnect(null);
                    }else{
                        //start loops
                        socket.setSoTimeout(keepAliveDelay+keepAliveBuffer);
                        if(connectionOpen.get())startReadLoop();
                        if(connectionOpen.get())startKeepAliveLoop();
                    }
                }catch (ClassNotFoundException | NullPointerException | IOException e) {
                    disconnect(e);
                }

    }


    public void startReadLoop(){
        Runnable readLoopRunnable = new Runnable() {
            @Override
            public void run() {
                //while connection is open/ intended to be open
                while(connectionOpen.get()){
                    try {
                        //read message (block until received)
                        Message message = (Message) objectInputStream.readObject();
                        //handle message
                        eventHandler.clientHandleMessage(thisClient,message);
                    } catch (IOException | ClassNotFoundException e) {
                        disconnect(e);
                    }
                }
            }
        };
        readLoopThread = new Thread(readLoopRunnable);
        if(connectionOpen.get())readLoopThread.start();
    }
    public void startKeepAliveLoop(){
        Runnable keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                //while connection is open/ intended to be open
                while(connectionOpen.get()){
                    //send message every x mills to keep connection alive
                    try {
                        //send message
                        objectOutputStream.writeObject(new Message(true));
                    } catch (IOException e) {
                        //on fail, disconnect.
                        disconnect(e);
                    }
                    try {
                        //wait x mills
                        Thread.sleep(keepAliveDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        keepAliveThread = new Thread(keepAliveRunnable);
        if(connectionOpen.get())keepAliveThread.start();
    }

    //disconnect end network related loops, close socket, notify
    public void disconnect(Exception exception){
        //don't run if already disconnected, or disconnecting
        if(connectionOpen.get())try {
            //end all loops
            connectionOpen.set(false);
            //close socket
            socket.close();
            //send to event handler
            eventHandler.clientDisconnected(this,new Exception[]{exception});
        } catch (IOException e) {
            System.err.println("serverConnection error disconnect method");
            eventHandler.clientDisconnected(this,new Exception[]{e,exception});
        }
    }






}

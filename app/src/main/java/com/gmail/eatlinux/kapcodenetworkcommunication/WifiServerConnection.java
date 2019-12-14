package com.gmail.eatlinux.kapcodenetworkcommunication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiServerConnection {

    int keepAliveDelay = 3000;
    int keepAliveBuffer = 2000;//mills added onto keepAliveDelay. will be the sockets SO Timeout
    WifiServer server;
    Socket socket;
    Thread handshakeThread,readLoopThread,keepAliveThread;
    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;
    String systemName;
    String application;
    String clientName;
    String clientIp;
    int port;
    WifiEventHandler eventHandler;
    WifiServerConnection thisConnection;
    AtomicBoolean connectionOpen = new AtomicBoolean(false);
    boolean ping = false;
    public WifiServerConnection(Socket socket,WifiServer server,String systemName,String application,WifiEventHandler eventHandler){
        //must not run any "intimate code" here. must not change connection, or block.
        //runs on wifiServer Thread
        connectionOpen.set(true);
        this.socket=socket;
        this.server=server;
        this.systemName = systemName;
        this.application = application;
        this.eventHandler=eventHandler;
        thisConnection=this;
        this.clientIp = socket.getInetAddress().getHostAddress();
        this.port = socket.getLocalPort();
    }


    //in new Thread...
    //reads client info (name,application,ping-true/false)
    //compare application
    //send server info (name,application)
    //if ping, disconnect, else, start keepAlive and read loops... keep the connection
    public void startHandshake(){
        eventHandler.serverAcceptedConnection(this);
        Runnable handshakeRunnable = new Runnable() {
            @Override
            public void run() {
                //create objects streams.
                try {
                    objectOutputStream=new ObjectOutputStream(socket.getOutputStream());
                } catch (IOException e) {
                    System.out.println("wifiServer error creating objectOutputStream!");
                    e.printStackTrace();
                }



                if(connectionOpen.get())try {
                    objectInputStream=new ObjectInputStream(socket.getInputStream());
                } catch (IOException e) {
                    System.out.println("wifiServer error creating objectInputStream!");
                    e.printStackTrace();
                }
                //create message
                Message message = null;

                //read clients info
                if(connectionOpen.get())try {
                    message = (Message)objectInputStream.readObject();
                    eventHandler.serverHandshakeMessageRead(thisConnection);
                    clientName=message.systemName;
                    ping=message.ping;

                } catch (IOException e) {
                    System.out.println("wifiServer error IO reading handshake message");
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    System.out.println("wifiServer error ClassNotFound reading handshake message");
                    e.printStackTrace();
                }



                //set info
                //only send info to client if application is the same.
                if(connectionOpen.get())if(message!=null&&message.application.equals(application)){
                    message=new Message();
                    message.systemName = systemName;
                    message.ping=false;
                    message.application=application;
                    //send message.
                    if(connectionOpen.get())try {
                        objectOutputStream.writeObject(message);
                        eventHandler.serverHandshakeMessageSent(thisConnection);
                        eventHandler.serverHandshakeSuccessful(thisConnection);
                    } catch (IOException e) {
                        System.out.println("wifiServer error sending handshake Message!");
                        e.printStackTrace();
                    }
                }

                //if not ping, start keepAlive/read loops, keep connection....
                if(connectionOpen.get())if(!ping){
                    if(connectionOpen.get())try {
                        socket.setSoTimeout(keepAliveDelay+keepAliveBuffer);
                    } catch (SocketException e) {
                        disconnect(e);
                    }
                    //start loops
                    if(connectionOpen.get())startReadLoop();
                    if(connectionOpen.get())startKeepAliveLoop();
                    //notify
                    eventHandler.serverKeepConnection(thisConnection);
                }else{//nothing is sent if ping not true, disconnect() and notify
                    eventHandler.serverConnectionIsAPing(thisConnection);
                    disconnect(null);
                }
            }
        };
    //create and start the new handshake thread
    handshakeThread = new Thread(handshakeRunnable);
        if(connectionOpen.get())handshakeThread.start();

    }


    public void startReadLoop(){
        Runnable readLoopRunnable = new Runnable() {
            @Override
            public void run() {
                while(connectionOpen.get()){
                    try {
                        //read message (block until received)
                        Message message = (Message) objectInputStream.readObject();
                        //handle message
                        eventHandler.serverHandleMessage(thisConnection,message);
                    } catch (IOException | ClassNotFoundException e) {
                        disconnect(e);
                    }
                }
            }
        };
        readLoopThread = new Thread(readLoopRunnable);
        if(connectionOpen.get())readLoopThread.start();
    }

    //send a message to client every x mills, to keep connection alive
    public void startKeepAliveLoop(){
        Runnable keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                while(connectionOpen.get()){
                    try {
                        //send message
                        objectOutputStream.writeObject(new Message(true));
                        //server.stopServer();
                    } catch (IOException e) {
                        disconnect(e);
                    }
                    try {
                        //wait
                        Thread.sleep(keepAliveDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        keepAliveThread = new Thread(keepAliveRunnable);
        keepAliveThread.start();
    }

    //disconnect and stop all network related loops, remove connection from list in thread safe manner.
    public void disconnect(Exception exception){
        if(connectionOpen.get())try {
            connectionOpen.set(false);
            socket.close();
            server.accessConnectionsList(thisConnection,WifiServer.REMOVE);
            eventHandler.serverDisconnected(this,new Exception[]{exception});
        } catch (IOException e) {
            System.out.println("serverConnection error disconnect method");
            eventHandler.serverDisconnected(this,new Exception[]{e,exception});
            server.accessConnectionsList(thisConnection,WifiServer.REMOVE);
        }

    }
}

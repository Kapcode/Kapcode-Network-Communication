package com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiServer {
    //todo some how reliable pace incoming connections... if they come in to quick to attempt handshake, they just get dropped i think?
    public static final int REMOVE = 0;
    public static final int ADD = 1;
    public static final int ADD_IF_NOT_CONTAINS = 2;
    public static final int CLEAR = 3;
    public static final int GET_SIZE = 4;
    ServerSocket serverSocket;
    int port;
    AtomicBoolean serverIsRunning = new AtomicBoolean(false);
    String application,systemName;
    WifiEventHandler eventHandler;
    Thread acceptLoopThread;
    WifiServer thisServer;

    //thread safe, synchronize editing the list...
    private volatile List<WifiServerConnection> connectionsList = Collections.synchronizedList(new ArrayList<WifiServerConnection>());
    //method for all access of connectionsList Thread safe
    public synchronized Object accessConnectionsList(WifiServerConnection connection, int action){
        switch (action){
            case ADD:
                break;
            case ADD_IF_NOT_CONTAINS:
                //check if list contains connection
                if(!connectionsList.contains(connection)){
                    //add to list
                    connectionsList.add(connection);
                }else{
                    System.out.println("list contains this connection");
                }
                break;
            case REMOVE:
                connectionsList.remove(connection);
                break;
            case CLEAR:
                connectionsList.clear();
                break;
                //GET_SIZE not needed, returns size always when done
        }
        //System.out.println("server: CONNECTIONS LIST SIZE="+connectionsList.size());
        return connectionsList.size();
    }


    //create and start a server
    public WifiServer(final int port, String systemName, String application, final WifiEventHandler eventHandler) {
        this.port=port;
        this.eventHandler = eventHandler;
        this.systemName = systemName;
        this.application=application;
        thisServer=this;
        //accept new connections, and run in new threads
        acceptLoopThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //bind port
                try {
                    serverSocket = new ServerSocket(port);
                    eventHandler.serverPortBound(thisServer);
                    serverIsRunning.set(true);
                } catch (IOException e) {
                    System.err.println("server: error on binding!");
                    stopServer();
                }

                acceptLoop();
            }
        });
        acceptLoopThread.start();
    }

    //accept new connections, and run in new threads
    public void acceptLoop(){
        //while server is running/supposed to be running
        while(serverIsRunning.get()){
            try {
                Socket socket = serverSocket.accept();
                //create a connection object to handle this new connection to client
                WifiServerConnection connection = new WifiServerConnection(socket,this,systemName,application,eventHandler);
                //add new connection to list of all connections if it does not contain it already, in a thread safe manner.
                accessConnectionsList(connection,ADD_IF_NOT_CONTAINS);
                //start the connections handshake process
                connection.startHandshake();//will start on a new thread to allow constructor to finish.
            } catch (IOException e) {
                if(serverIsRunning.get()) {
                    System.err.println("server: error accepting! " + e.toString());
                    //TODO decide how to handle this, server should stay running i think?
                    //e.printStackTrace();
                    //serverIsRunning.set(false);
                }else{//most likely caused by server.stopServer()
                    eventHandler.serverStopped();
                }

            }
        }
    }

    public void stopServer(){
        eventHandler.stoppingServer();
        ArrayList<WifiServerConnection> connectionsListCopy = new ArrayList<>();
        connectionsListCopy.addAll(connectionsList);
        for(WifiServerConnection connection: connectionsListCopy){
            connection.disconnect(null);
        }
        accessConnectionsList(null,CLEAR);

        serverIsRunning.set(false);
        try {
            serverSocket.close();

        } catch (IOException e) {
            System.err.println("server error stopping server!");
            e.printStackTrace();
        }
        //block for 5 seconds to allow clients to SO timeout
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public WifiServer restartServer() throws InterruptedException {
        stopServer();
        //start a new server using old params
        WifiServer server = new WifiServer(port,systemName,application,eventHandler);
        return server;

    }


}

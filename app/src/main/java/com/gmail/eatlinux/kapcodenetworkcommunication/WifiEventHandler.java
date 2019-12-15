package com.gmail.eatlinux.kapcodenetworkcommunication;

import java.io.EOFException;
import java.net.SocketException;

public class WifiEventHandler {
    //TODO use an actual logger instead of this madness
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    //public static final String ANSI_WHITE = "\u001B[37m";

    public void serverPortBound(WifiServer server){System.out.println("SERVER: port bound");}
    public void serverAcceptedConnection(WifiServerConnection serverConnection){System.out.println("SERVER_CONNECTION:"+" accepted");}
    public void serverHandshakeMessageRead(WifiServerConnection serverConnection){System.out.println("SERVER_CONNECTION:"+" handshake message read");}
    public void serverHandshakeMessageSent(WifiServerConnection serverConnection){System.out.println("SERVER_CONNECTION:"+serverConnection.clientName+": handshake message sent");}
    public void serverHandshakeSuccessful(WifiServerConnection serverConnection){
        System.out.println(ANSI_GREEN+"SERVER_CONNECTION:"+serverConnection.clientName+": handshake successful"+ANSI_RESET);
        System.out.println("SERVER: ConnectedDevices="+serverConnection.server.accessConnectionsList(null,WifiServer.GET_SIZE));
    }
    public void serverKeepConnection(WifiServerConnection connection){
        System.out.println(ANSI_GREEN + "SERVER: Keeping connection to:"+ connection.clientName+":"+connection.clientIp+":"+connection.port+ ANSI_RESET);
    }
    public void serverConnectionIsAPing(WifiServerConnection serverConnection){System.out.println("SERVER_CONNECTION:"+serverConnection.clientName+": is a ping");}
    public void stoppingServer(){
        //TODO add userInitiated flag, to tell difference between possible issue, and user expected action?
        System.out.println(ANSI_YELLOW+"SERVER: STOPPING...."+ANSI_RESET);
    }
    public void serverStopped(){
        System.out.println(ANSI_YELLOW+"SERVER: STOPPED!"+ANSI_RESET);
    }
    public void serverDisconnected(WifiServerConnection serverConnection, Exception[] exceptions){
        StringBuilder out = new StringBuilder("SERVER-DISCONNECTED-FROM-CLIENT: " + serverConnection.clientName + " : ERRORS:");
        if(exceptions[0]!=null)for(Exception exception: exceptions){
            out.append(exception.toString()).append(" ");
            if(exception instanceof EOFException || exception instanceof SocketException){
                out.append("::NORMAL!  ");
            }
        }
        if(out.toString().contains("NORMAL") |! out.toString().contains("Exception")){
            System.out.println(ANSI_YELLOW + out + ANSI_RESET);
        }else{
            System.err.println(out);
        }
        System.out.println("SERVER: ConnectedDevices="+serverConnection.server.accessConnectionsList(null,WifiServer.GET_SIZE));
        //Socket Exception or EOF means socket closed via client
    }
    public void serverHandleMessage(WifiServerConnection serverConnection,Message message){
        String messageText = "ping";
        if(!message.ping){
            //handle message, change messageText
            String suffix = "'"+serverConnection.systemName+"' <-- Message from Client:'"+serverConnection.clientName+"' Message='"+messageText+"'";
            System.out.println("SERVER:"+suffix);
        }

    }




    public void clientDisconnected(WifiClient client, Exception[] exceptions){
        StringBuilder out = new StringBuilder("CLIENT-DISCONNECTED: " + client.serverName + " : ERRORS:");
        if(exceptions[0]!=null)for(Exception exception: exceptions){
            out.append(exception.toString()).append(" ");
            if(exception instanceof EOFException || exception instanceof SocketException){
                out.append("::NORMAL!  ");
            }
        }
        if(out.toString().contains("NORMAL") |! out.toString().contains("Exception")){
            System.out.println(ANSI_YELLOW + out + ANSI_RESET);
        }else{
            System.err.println(out);
        }


        //Socket Exception or EOF means socket closed via server.
    }
    public void clientConnectionMade(WifiClient client){System.out.println("CLIENT:"+" connection made");}
    public void clientHandshakeMessageSent(WifiClient client){System.out.println("CLIENT:"+" handshake message sent");}
    public void clientHandshakeMessageRead(WifiClient client){System.out.println("CLIENT:"+" handshake message read");}
    public void clientHandshakeSuccessful(WifiClient client){System.out.println(ANSI_GREEN+"CLIENT:"+client.serverName+": handshake successful"+ANSI_RESET);}

    public void clientFailedToConnect(WifiClient client,Exception exception){
        if(!client.ping){
            System.out.println(ANSI_YELLOW + "CLIENT" + ": FAILED TO CONNECT TO SERVER :ERROR:"+exception + ANSI_RESET);
        }
    }


    public void clientHandleMessage(WifiClient client,Message message){
        String messageText = "ping";
        if(!message.ping){
            //handle message, change messageText
            String suffix = "'"+client.systemName+"' <-- Message from Server:'"+client.serverName+"' Message='"+messageText+"'";
            System.out.println("CLIENT:"+suffix);
        }

    }


    public void scannerFoundServer(WifiClient client){
        System.out.println(ANSI_GREEN + "SCANNER: Found server: " + client.serverName + ":" +client.ip+":"+client.port + ANSI_RESET);
    }
    public void scannerLostServer(String ip,int port){
        System.out.println(ANSI_YELLOW + "SCANNER: Lost server: " + ip+":"+port + ANSI_RESET);
    }

}

package com.gmail.eatlinux.kapcodenetworkcommunication;

import java.io.Serializable;

public class Message implements Serializable {
    public Message(){}
    public Message(Boolean ping){this.ping=ping;}
    //class must be the same on client and server.



    //handshake information
    //name of application sending message (include version?)
    String application;
    //name of system sending the message
    String systemName;
    //if true, disconnect after sending info...
    boolean ping=false;


}

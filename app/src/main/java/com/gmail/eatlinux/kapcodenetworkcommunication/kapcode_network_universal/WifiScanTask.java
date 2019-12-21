package com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal;

public class WifiScanTask implements Runnable{
    //a task that looks for a server on a given ip:port
    //on find, adds to scanner.identifiedServersList, then in turn to UI

    WifiScanner scanner;
    WifiClient client = null;
    WifiEventHandler eventHandler;
    String ip;
    String name;
    String application;
    int port;
    int timeout;
    int threadNumber;
    String lastDeviceAddressUsed = "";






    public WifiScanTask(WifiScanner scanner, WifiEventHandler eventHandler, int timeout, int threadNumber){
        this.scanner=scanner;
        this.eventHandler=eventHandler;
        this.timeout=timeout;
        //append this int at end of ip-
        this.threadNumber = threadNumber;
    }

    @Override
    public void run() {
        //get the address from scanner. every scan will be of the current address.
        String deviceAddress = scanner.getDeviceAddress();
        boolean updateAddress = !deviceAddress.equals(lastDeviceAddressUsed);//check .equals() once.
        //only update if changed.
        if(updateAddress){
            lastDeviceAddressUsed=deviceAddress;
            String[] address = scanner.getDeviceAddress().split(":");
            name=address[0];
            application = address[1];
            ip=trim_ip(address[2])+threadNumber;
            port=Integer.parseInt(address[3]);
        }
        //create a new client if null
        if(client==null){
            client = new WifiClient(ip,port,timeout,name,application,true,eventHandler);
        }else if(updateAddress){
            //update info.
            client.ip = ip;
            client.port=port;
            client.systemName=name;
            client.application=application;
        }

        client.startHandshake();

        if (client.serverName != null) {
            //notified only if list does not contain this ip...
            scanner.addIdentifiedServerToList(client.serverName, client.ip, client.port, eventHandler, client);

        } else {
            //connection failed...
            //remove from list
            if(scanner.identifiedServersListContains(client.ip,client.port)){
                eventHandler.scannerLostServer(client.ip,client.port);
                scanner.removeIdentifiedServerFromListByAddress(client.ip, client.port);
            }

        }
        scanner.tasksCompleted.incrementAndGet();
    }

    //trim_ip("192.168.0.1"); /10.x.x.x and 127.x.x.x compatible too
    //return 192.168.0.
    private String trim_ip(String ip){
        String[] parts = ip.split("[.]");
        return parts[0]+"."+parts[1]+"."+parts[2]+".";
    }
}

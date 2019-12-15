package com.gmail.eatlinux.kapcodenetworkcommunication;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WifiScanner implements Runnable {
    public static final int MILLS_DELAY_BETWEEN_CHECKING_TOTAL_PROCESS_COMPLETION = 1000;
    public static final int REMOVE = 0;
    public static final int ADD = 1;
    public static final int ADD_IF_NOT_CONTAINS = 2;
    public static final int CLEAR = 3;
    public static final int GET_SIZE = 4;
    public static final int COPY = 5;
    public static volatile AtomicBoolean stopScanners = new AtomicBoolean(false);
    public static volatile AtomicBoolean paused = new AtomicBoolean(false);
    private static List<Object[]> identifiedServersList = (List<Object[]>) Collections.synchronizedList(new ArrayList<Object[]>());//Object[3] String serverName,String ip, int port
    private int port,timeout;
    static volatile ExecutorService executorService = null;
    private String systemName,application,ip;
    private WifiEventHandler eventHandler;
    private static AtomicInteger tasksCountCompleted;
    private static int tasksCountTotal;
    private Socket socket;
    static ArrayList<WifiScanner> runnableList;
    //add = true
    //ifNotContains = true means server will only be added if list does not contain server (by ip address)
    //remove = false
    //synchronize adding and removing
    private static synchronized Object accessIdentifiedServerList(String serverName, String ip, int port, int action,WifiEventHandler eventHandler,WifiClient client){
        //TODO wrap with an object? create object class to handle this instead of using an object[]
        int startSize = identifiedServersList.size();
        Object[] server = new Object[]{serverName,ip,port};
        switch (action){
            case ADD:
                break;
            case ADD_IF_NOT_CONTAINS:
                boolean contains = false;
                for(Object o:identifiedServersList){
                    //cast object array, get index 1, ip, if ip.equals(thisip)
                    String oip = (String)((Object[])o)[1];
                    if (ip.equals(oip)) {
                        contains = true;
                        break;
                    }
                }
                if(!contains){
                    identifiedServersList.add(server);
                    eventHandler.scannerFoundServer(client);
                }
                break;
            case REMOVE:
                //remove by ip:port
                //find all objects that have this ip.
                ArrayList<Object> serversToRemove = new ArrayList<>();
                for(Object o:identifiedServersList){
                    String oip = (String)((Object[])o)[1];
                    if(ip.equals(oip))serversToRemove.add(o);
                }
                //remove them
                for(Object o:serversToRemove)identifiedServersList.remove(o);//suspicious warning
                break;
            case CLEAR:
                identifiedServersList.clear();
                break;
            case COPY:
                ArrayList<Object[]> listCopy = new ArrayList<>();
                listCopy.addAll(identifiedServersList);
                return listCopy;

        }
        if(identifiedServersList.size()!=startSize)System.out.println("client: identified_servers= "+identifiedServersList.size());
        return identifiedServersList.size();
    }
    public static void addIdentifiedServerToList(String serverName,String ip,int port,WifiEventHandler eventHandler,WifiClient client){
        accessIdentifiedServerList(serverName,ip,port,ADD_IF_NOT_CONTAINS,eventHandler,client);
    }
    public static void removeIdentifiedServerFromListByAddress(String ip,int port){
        accessIdentifiedServerList("",ip,port,REMOVE,null,null);
    }
    public static int getIdentifiedServersListSize(){
        return (int) accessIdentifiedServerList(null,null,0,GET_SIZE,null,null);
    }
    public static ArrayList<Object[]> getCopyOfIdentifiedServersList(){
        return (ArrayList<Object[]>) accessIdentifiedServerList(null,null,0,COPY,null,null);
    }





    public WifiScanner(String ip,int port,String systemName,String application,int timeout,WifiEventHandler eventHandler){
        this.ip=ip;
        this.port=port;
        this.application = application;
        this.systemName=systemName;
        this.timeout=timeout;
        this.eventHandler=eventHandler;
    }

    @Override
    public void run() {
        startScanOf(ip,port);
    }


    private void startScanOf(String ip,int port) {

            long startTime = System.currentTimeMillis();
            //System.out.println("scanning..."+ip+":"+port);
            WifiClient client = new WifiClient(ip, port, timeout, systemName, application, true, eventHandler);
            socket=client.socket;
            //start the handshake... do this only in scanner class. it is automatically done on new thread when creating object if ping is false.
            //this runs on this thread. (blocks until finished/timeout)
            client.startHandshake();
            //if connected to a server, save its info
            if (client.serverName != null) {
                //notified only if list does not contain this ip...
                addIdentifiedServerToList(client.serverName, client.ip, client.port, eventHandler, client);
                //todo connection success!, now must create a timer, to make sure we are not continuously hitting this server, only periodically to ensure it is still available.. probably will need to delay longer than timeout duration.
                //todo must do this with either elapsed time, or timer objects... not thread.sleep. need to free this thread up after each connection attempt to allow scanner to finish job.
                //cpu usage evens out after a while but change is still needed to reduce network load. battery usage.

            } else {
                //connection failed...
                //remove from list
                removeIdentifiedServerFromListByAddress(client.ip, client.port);
            }
            tasksCountCompleted.incrementAndGet();//this task is done


            long elapsedTimeSinceStart = System.currentTimeMillis() - startTime;
            long differenceBetweenTimeoutAndElapsed = timeout - elapsedTimeSinceStart;
            try {
                if (differenceBetweenTimeoutAndElapsed > 0 & !executorService.isShutdown() & !executorService.isTerminated())
                    Thread.sleep(differenceBetweenTimeoutAndElapsed);
            } catch (InterruptedException e) {
                e.printStackTrace();//TODO
                // (Re-)Cancel if current thread also interrupted
                executorService.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }

    }

    //blocks until scanning is complete.
    //use ip of this device to get set of ips to scan for servers
    public static void startScanningIpRange(String this_system_ip,int port,int threadCount,String systemName,String application,int timeout,WifiEventHandler eventHandler,boolean scanIndefinitely){//
            stopScanners.set(false);
            //parse ip
            //254 because 255-this device... //todo exclude router as well
            ArrayList<String> ipsToScan = new ArrayList<>(254);
            tasksCountTotal = 254;


            tasksCountCompleted = new AtomicInteger(0);
            String[] ipParts = this_system_ip.split("[.]");
            String ipPrefix = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".";
            //populate ip list
            for (int i = 0; i < 256; i++) {
                String ipToScan = ipPrefix + i;
                //if not this ip, add to list. don't scan this device...
                if (!ipToScan.equals(this_system_ip)) {
                    ipsToScan.add(ipToScan);
                    //System.out.println("added "+ ipToScan);
                } else {
                    System.out.println("Ignored this device " + ipToScan);
                }
            }

            //create a thread pool
            executorService = Executors.newFixedThreadPool(threadCount);
            //create and populate a list of runnable WifiScanners
            runnableList = new ArrayList<>(tasksCountTotal);
            for (String currentIp : ipsToScan) {
                //populate list of scanners// runnable(s)
                runnableList.add(new WifiScanner(currentIp, port, systemName, application, timeout, eventHandler));
            }


            while (!stopScanners.get()) {
                //add each runnable //scanner to the que...
                long startTime = System.currentTimeMillis();
                for (Runnable runnable : runnableList) executorService.execute(runnable);
                System.out.println("ALL TASKS STARTED! ");
                //loop until all tasks are completed.  (BLOCK) until all tasks complete
                while (!stopScanners.get()) {
                    if (tasksCountCompleted.get() >= tasksCountTotal) {
                        //tasks done,
                        System.out.println("ALL TASKS COMPLETED! " + (System.currentTimeMillis() - startTime));
                        tasksCountCompleted.set(0);

                        //block while paused
                        while(paused.get()){
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        break;
                    } else {
                        //sleep to avoid thrash
                        try {
                            Thread.sleep(MILLS_DELAY_BETWEEN_CHECKING_TOTAL_PROCESS_COMPLETION);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            // (Re-)Cancel if current thread also interrupted
                            executorService.shutdownNow();
                            // Preserve interrupt status
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                if (!scanIndefinitely) break;


            }
            //once scan is stopped with stopScanners.set(true) shut down thread pool
            executorService.shutdown();
            while (!executorService.isTerminated()) {
            }
            System.out.println("Finished all scanner threads");
            stopScanners.set(false);
            executorService=null;

    }
    public static void waitForShutdown(){
        //todo... maybe join() the thread that the scanner is running on, pass it via an argument on scanIpRange()  ??  instead of this...
         try{
             while (!executorService.isTerminated()||stopScanners.get()) {
             }
        }catch(NullPointerException e){
            System.out.println("NULL");
            while(stopScanners.get()){}
        }
    }

    //should only be running this when changing information, or are already connected to a server.
    //should be using pause.set(true); if not wanting to kill all threads in pool, or change network scanning in.
    public static void shutDownNow(){
        System.out.println("STOPPING");
        stopScanners.set(true);
        try{
            executorService.shutdownNow();
        }catch (NullPointerException e){

        }
        //todo concurency errors.. not thread safe, and list might be set null during iteration!
        if(runnableList!=null)for(WifiScanner r: runnableList) {
            try {
                r.socket.close();
            } catch (IOException | NullPointerException e) {
                //e.printStackTrace();
            }
        }
        waitForShutdown();
        System.out.println("STOPPED");
    }





}






package com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class WifiScanner {
    public static final int STOP=0,START=1,PAUSE=2,REMOVE = 0,ADD = 1,ADD_IF_NOT_CONTAINS = 2,CLEAR = 3,GET_SIZE = 4,COPY = 5,CONTAINS = 6;
    public AtomicInteger goal;
    AtomicInteger tasksCompleted;
    @SuppressWarnings("FieldCanBeLocal")
    private final int taskTotal = 255;
    private ExecutorService executorService;
    @SuppressWarnings("FieldCanBeLocal")
    private Thread managerThread;
    private WifiEventHandler eventHandler;
    private int threadCount;
    private int connectionTimeout;
    //only referenced right before starting execution of runnable's. changes to this during will result in the next run reflecting this change.
    private volatile String deviceAddress;
    ArrayList<WifiScanTask> taskList;
    private List<Object[]> identifiedServersList;//Object[3] String serverName,String ip, int port

    public String getDeviceAddress(){
        //get without setting.
        return accessDeviceAddress("");
    }
    public void setDeviceAddress(String address_deviceName_application_ip_port){
        accessDeviceAddress(address_deviceName_application_ip_port);
    }
    private synchronized String accessDeviceAddress(String address_deviceName_application_ip_port){
        if(address_deviceName_application_ip_port.length()>2){
            deviceAddress=address_deviceName_application_ip_port;
        }
        return deviceAddress;
    }




    //SYNC LISTS
    private synchronized Object accessIdentifiedServerList(String serverName, String ip, int port, int action,WifiEventHandler eventHandler,WifiClient client){
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
            case CONTAINS:
                //loop over list
                //check if contains ip and port
                for(Object[] arr: identifiedServersList){
                    //0 = name
                    //1 = ip
                    //2 = port
                    if(arr[1].equals(ip) && arr[2].equals(port)){
                        return true;
                    }
                }
                return false;



        }
        if(identifiedServersList.size()!=startSize)System.out.println("client: identified_servers= "+identifiedServersList.size());
        return identifiedServersList.size();
    }
    public void addIdentifiedServerToList(String serverName,String ip,int port,WifiEventHandler eventHandler,WifiClient client){
        accessIdentifiedServerList(serverName,ip,port,ADD_IF_NOT_CONTAINS,eventHandler,client);
    }
    public void removeIdentifiedServerFromListByAddress(String ip,int port){
        accessIdentifiedServerList("",ip,port,REMOVE,null,null);
    }
    public boolean identifiedServersListContains(String ip,int port){
        return (boolean)accessIdentifiedServerList("",ip,port,CONTAINS,null,null);
    }

    public void clearIdentifiedServersList(){
        accessIdentifiedServerList(null,null,0,CLEAR,null,null);
    }
    public void getIdentifiedServersListSize(){
        accessIdentifiedServerList(null,null,0,GET_SIZE,null,null);
    }






    //manages a thread-pool with WifiScanTasks to look for a server within a given ip range.
    //a separate thread for managing starts,stops,pauses will look for a 'goal', and try to complete it...




    //use name:ip:port,2000,254
    public WifiScanner(String address_deviceName_application_ip_port, int connectionTimeout, int threadCount, WifiEventHandler eventHandler){
        goal = new AtomicInteger(START);
        setDeviceAddress(address_deviceName_application_ip_port);
        this.eventHandler=eventHandler;
        this.threadCount=threadCount;
        this.connectionTimeout = connectionTimeout;
        if(tasksCompleted == null)tasksCompleted=new AtomicInteger(0);
        if(identifiedServersList==null)identifiedServersList = (List<Object[]>) Collections.synchronizedList(new ArrayList<Object[]>());//Object[3] String serverName,String ip, int port
        //don't block, finish creating Object, use thread.join if you want to block until this loop exits.
        managerThread=new Thread(new Runnable() {
            @Override
            public void run() {
                //start a manager loop
                while(true){
                    switch (goal.get()){
                        case START:
                            start();//if already initialized, will just reuse thread-pool. ..runs another round of trys on all ip's...
                            break;
                        case STOP:
                            //if not terminated, or null
                            stop();
                            break;
                        case PAUSE:
                            //block until not pause
                            while(goal.get()==PAUSE){
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                    }
                }
            }
        });
        managerThread.start();





    }




    private void start(){
        //create a pool if null
        if(executorService==null)executorService = Executors.newFixedThreadPool(threadCount);
        //make list of WifiScanTasks
        if(taskList==null){
            taskList = new ArrayList<>();
            int threadNumber = 0;
            while(threadNumber<=taskTotal){
                taskList.add(new WifiScanTask(this,eventHandler,connectionTimeout,threadNumber));
                threadNumber++;
            }
        }
        //loop over taskList, execute all
        for(WifiScanTask task:taskList)executorService.execute(task);
        System.out.println("ALL TASKS STARTED");
        while(tasksCompleted.get()<taskTotal){//block until all tasks complete, sleep to avoid thrash
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        tasksCompleted.set(0);
        System.out.println("ALL TASKS COMPLETE");
    }

    private void stop(){
        //todo shutdown() and set executorService null after terminated()
    }










}



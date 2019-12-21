package com.gmail.eatlinux.kapcodenetworkcommunication.kapcode_network_universal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class WifiScanner {
    public static final int STOP=0,START=1,PAUSE=2,REMOVE = 0,ADD = 1,ADD_IF_NOT_CONTAINS = 2,CLEAR = 3,GET_SIZE = 4,COPY = 5,CONTAINS = 6;
    public AtomicInteger goal;
    AtomicInteger tasksCompleted;
    private final int taskTotal = 255;
    private ExecutorService executorService;
    private Thread managerThread;
    private WifiEventHandler eventHandler;
    private int threadCount;
    private int connectionTimeout;
    //only referenced right before starting execution of runnable's. changes to this during will result in the next run reflecting this change.
    private volatile String deviceAddress;
    ArrayList<WifiScanTask> taskList;
    public String getDeviceAddress(){
        //get without setting.
        return accessDeviceAddress(null);
    }
    public void setDeviceAddress(String address_deviceName_application_ip_port){
        accessDeviceAddress(address_deviceName_application_ip_port);
    }
    private synchronized String accessDeviceAddress(String address_deviceName_application_ip_port){
        if(address_deviceName_application_ip_port!=null){
            deviceAddress=address_deviceName_application_ip_port;
        }
        return deviceAddress;
    }
    //key is ip
    //value is {port,name}
    private volatile HashMap<String,Object[]> identifiedServersMap = new HashMap<>(10);

    private synchronized Object accessIdentifiedServersMap(int action,String key,Object[] value,WifiClient client,WifiEventHandler eventHandler){
        if(action == CONTAINS )return identifiedServersMap.containsKey(key);
        switch (action){
            case ADD_IF_NOT_CONTAINS:
                if(!identifiedServersMap.containsKey(key)){
                    identifiedServersMap.put(key,value);
                    eventHandler.scannerFoundServer(client);
                }
                break;
            case REMOVE:
                identifiedServersMap.remove(key);
                break;
            case CLEAR:
                for(String temp_key: identifiedServersMap.keySet()){
                    eventHandler.scannerLostServer(temp_key,(int)identifiedServersMap.get(temp_key)[0]);
                }
                identifiedServersMap.clear();
            case COPY:
                return identifiedServersMap.clone();

        }

        return identifiedServersMap.size();


    }

    public void addIdentifiedServerToMap(String serverName,String ip,int port,WifiEventHandler eventHandler,WifiClient client){
        accessIdentifiedServersMap(ADD_IF_NOT_CONTAINS,ip,new Object[]{port,serverName},client,eventHandler);
    }
    public void removeIdentifiedServerFromMapByAddress(String ip){
        accessIdentifiedServersMap(REMOVE,ip,null,null,null);
    }
    //todo
    public boolean identifiedServersMapContains(String ip){
        return (boolean)accessIdentifiedServersMap(CONTAINS,ip,null,null,null);
    }

    public void clearIdentifiedServersMap(WifiEventHandler eventHandler){
        accessIdentifiedServersMap(CLEAR,null,null,null,eventHandler);
    }
    public int getIdentifiedServersMapSize(){
        return (int) accessIdentifiedServersMap(GET_SIZE,null,null,null,null);
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
        //if(identifiedServersList==null)identifiedServersList = (List<Object[]>) Collections.synchronizedList(new ArrayList<Object[]>());//Object[3] String serverName,String ip, int port
        //if(identifiedServersMap==null)identifiedServersMap=new HashMap<>();
                //map is created in global declaration
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



package edu.usf.cutr.gtfsrtvalidator.background;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by nipuna on 6/17/15.
 */
public class BackgroundSingleton {

    boolean backgroundProcessStarted = false;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    //Keeping the class as a singleton because there should be only one thread running at a time.
    private BackgroundSingleton() {}

    //Eager initialization of the object the getInstance doesn't have to be syncronized.
    private static BackgroundSingleton _instance = new BackgroundSingleton();

    public static BackgroundSingleton getInstance()
    {
        return _instance;
    }

    public synchronized void StartBackgrounProcess(){
        if(backgroundProcessStarted){

        }else{
            executor.scheduleAtFixedRate(new RefreshCountTask(), 0, 10, TimeUnit.SECONDS);
            backgroundProcessStarted = true;
        }

    }

    public void StopBackgrounProcess(){
        executor.shutdown();
        backgroundProcessStarted = false;
    }
}

/**
 * **********************************************************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this fileexcept in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * **********************************************************************************************************************
 */

package edu.usf.cutr.gtfsrtvalidator.background;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackgroundMultiThread {

    private static HashMap<String, ScheduledExecutorService> runningTasks = new HashMap<>();


    public static void StartBackgroundTask(String[] urls) {
        for (String url : urls) {
            startBackgroundTask(url);
        }
    }

    public static ScheduledExecutorService startBackgroundTask(String url) {
        //(new Thread(new RefreshCountTask(), url)).start();

        //final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        //scheduler.scheduleAtFixedRate(new RefreshCountTask(), 10, 10, TimeUnit.SECONDS);

        if (!runningTasks.containsKey(url)) {
            ScheduledExecutorService scheduler = getScheduledExecutor(url);
            runningTasks.put(url, scheduler);
            return scheduler;
        }else {
            return runningTasks.get(url);
        }

    }

    private static ScheduledExecutorService getScheduledExecutor(String url) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new RefreshCountTask(url), 0, 5, TimeUnit.SECONDS);
        return scheduler;
    }


    //private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

}

/*class MyThread extends Thread {

  public MyThread (String s) {
    super(s);
  }

  public void run() {
    System.out.println("Run: "+ getName());
  }
}


 class TestThread {
  public static void main (String arg[]) {

    Scanner input = new Scanner(System.in);
    System.out.println("Please input the number of Threads you want to create: ");
    int n = input.nextInt();
    System.out.println("You selected " + n + " Threads");

    for (int x=0; x<n; x++)
    {
        MyThread temp= new MyThread("Thread #" + x);
        temp.start();
        System.out.println("Started Thread:" + x);
    }
}
}
*/
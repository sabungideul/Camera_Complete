package com.example.camera;

import android.app.job.JobParameters;
import android.util.Log;

public class JobService extends MainActivity{

    private static final String TAG ="JobService";
    private boolean jobCancelled = false;
    boolean ture;
    //서비스시작
    public boolean onStartJob(JobParameters params){
        Log.d("onStartjob : ","onStartjob");

        doBackgroundWork(params);

        return ture;
    }
    public boolean onStopJob(JobParameters params){
        Log.d("onStopJob : ","onStopJob");
        jobCancelled = true;
        return true;
    }

    private void doBackgroundWork(final JobParameters params) {
      new Thread(new Runnable() {
          @Override
          public void run() {
              for(int i = 0; i < 15; i++){
                  Log.d("run : ","run");
                  if(jobCancelled){
                      return;
                  }
                  try{
                      Thread.sleep(1000);
                  }
                  catch(InterruptedException e){
                      e.printStackTrace();
                  }

              }
              Log.d("END : ","Job Finished");
          }
    }).start();
    }
}

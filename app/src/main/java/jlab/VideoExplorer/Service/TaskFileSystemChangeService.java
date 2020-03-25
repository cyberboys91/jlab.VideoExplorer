package jlab.VideoExplorer.Service;

import android.os.IBinder;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.app.Notification;
import java.util.concurrent.Semaphore;
import jlab.VideoExplorer.Interfaces;
import android.support.annotation.Nullable;
import jlab.VideoExplorer.Resource.Resource;
import jlab.VideoExplorer.Resource.Directory;
import jlab.VideoExplorer.Resource.FileResource;

/*
 * Created by Javier on 27/09/2017.
 */

public class TaskFileSystemChangeService extends Service {

    protected String currentProcessing;
    protected int countElemntsTotal, countElementsProcessed;
    protected boolean isPreparing, running = false, finish = true;
    protected long countBytes, bytesProcessed;
    private Semaphore mutex = new Semaphore(1);
    protected static boolean taskSuccessfully = false;
    public static final int TIME_INTERVAL_TASK_REFRESH = 1000;
    protected Interfaces.IRefreshListener refreshListener = new Interfaces.IRefreshListener() {
        @Override
        public void refresh() {

        }
    };
    private Interfaces.IFinishListener finishListener = new Interfaces.IFinishListener() {
        @Override
        public void refresh(boolean successfully) {

        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    protected void beginRefresh() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        }).start();
    }

    private void refresh() {
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (isRunning() && !isFinish()) {
            refreshListener.refresh();
            try {
                Thread.sleep(TIME_INTERVAL_TASK_REFRESH);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mutex.release();
    }

    protected void preparing(Resource resource) {
        if (finish)
            return;
        countElemntsTotal++;
        if (resource.isDir()) {
            Directory dir = (Directory) resource;
            dir.openSynchronic(null);
            int size = dir.getCountElements();
            for (int i = 0; i < size; i++) {
                preparing(dir.getResource(i));
            }
        } else
            countBytes += ((FileResource) resource).mSize;
    }

    public void stopService() {
        finish = true;
        running = false;
        finishListener.refresh(taskSuccessfully);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        finish = true;
        running = false;
    }

    protected void startService(Context context, int id, Notification notification) {
        if (!running) {
            finish = false;
            currentProcessing = "";
            countElemntsTotal = 0;
            countElementsProcessed = 0;
            bytesProcessed = 0;
            countBytes = 0;
        }
    }

    public boolean isFinish() {
        return finish;
    }

    public boolean isPreparing() {
        return isPreparing;
    }

    public int getCountElementsProcessed() {
        return countElementsProcessed;
    }

    public int getCountElemntsTotal() {
        return countElemntsTotal;
    }

    public long getBytesProcessed() {
        return bytesProcessed;
    }

    public long getCountBytes() {
        return countBytes;
    }

    public String getCurrentProcessing() {
        return currentProcessing;
    }

    public boolean isRunning() {
        return running;
    }

    public void setisRunning(boolean running) {
        this.running = running;
    }

    public void setRefreshListener(Interfaces.IRefreshListener newListener) {
        refreshListener = newListener;
    }

    public void setFinishListener(Interfaces.IFinishListener newListener) {
        finishListener = newListener;
    }
}

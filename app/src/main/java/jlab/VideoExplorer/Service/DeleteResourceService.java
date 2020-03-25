package jlab.VideoExplorer.Service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.app.Notification;
import jlab.VideoExplorer.Resource.Resource;

/*
 * Created by Javier on 27/09/2017.
 */
public class DeleteResourceService extends TaskFileSystemChangeService{

    private static Context mcontext;
    private static int idNotification;
    private static Notification notification;
    private static Intent mIntent;
    private static boolean started = false;

    public void beginDelete(Resource resource) {
        if (!running) {
            try {
                beginRefresh();
                running = true;
                isPreparing = true;
                preparing(resource);
                isPreparing = false;
                taskSuccessfully = delete(resource);
            } catch (Exception ignored) {
                ignored.printStackTrace();
            } finally {
                stopService();
            }
        }
    }

    @Override
    public void startService(Context context, int id, Notification notification) {
        super.startService(context, id, notification);
        if (!running) {
            mcontext = context;
            idNotification = id;
            DeleteResourceService.notification = notification;
            mIntent = new Intent(context, DeleteResourceService.class);
            started = true;
            context.startService(mIntent);
        }
    }

    @Override
    public void onCreate() {
        if (started) {
            super.onCreate();
            startForeground(idNotification, notification);
        } else stopService(getServiceIntent());
    }

    private static Intent getServiceIntent() {
        return new Intent().setComponent(new ComponentName("jlab.FastFileTransfer",
                "jlab.FastFileTransfer.Service.DeleteResourceService"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Override
    public void stopService() {
        super.stopService();
        mcontext.stopService(mIntent);
    }

    private boolean delete(Resource resource)
    {
        return resource.delete(new OnDeleteListener() {
            @Override
            public void delete(String name, long bytes) {
                currentProcessing = name;
                bytesProcessed += bytes;
                countElementsProcessed++;
            }
        });
    }

    public interface OnDeleteListener
    {
        void delete(String name, long bytes);
    }
}

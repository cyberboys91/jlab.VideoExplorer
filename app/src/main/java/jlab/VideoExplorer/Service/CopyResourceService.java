package jlab.VideoExplorer.Service;

import java.io.File;

import android.content.ComponentName;

import java.io.IOException;
import android.content.Intent;
import android.content.Context;
import jlab.VideoExplorer.*;
import java.io.FileInputStream;
import android.app.Notification;
import java.io.FileOutputStream;
import jlab.VideoExplorer.Resource.*;

/*
 * Created by Javier on 16/09/2017.
 */
public class CopyResourceService extends TaskFileSystemChangeService {

    private final int BUFFER_SIZE = 5242880; //5MB
    private boolean isMoving = false;
    private static Context mcontext;
    private static int idNotification;
    private static Notification notification;
    private static boolean started = false;
    private static DeleteResourceService.OnDeleteListener onDeleteListener = new DeleteResourceService.OnDeleteListener() {
        @Override
        public void delete(String name, long bytes) {
        }
    };
    public void beginCopy(Directory copyHere, Resource resourceToCopy, boolean isMoving) {
        if (!running) {
            try {
                beginRefresh();
                running = true;
                isPreparing = true;
                this.isMoving = isMoving && !containsResource(copyHere, resourceToCopy.getRelUrl());
                preparing(resourceToCopy);
                isPreparing = false;
                taskSuccessfully = false;
                copy(copyHere, resourceToCopy);
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
            CopyResourceService.notification = notification;
            started = true;
            context.startService(getServiceIntent());
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
                "jlab.FastFileTransfer.Service.CopyResourceService"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Override
    public void stopService() {
        super.stopService();
        mcontext.stopService(getServiceIntent());
    }

    private boolean containsResource(Directory copyHere, String relUrl) {
        copyHere.openSynchronic(null);
        int size = copyHere.getContent().size();
        for (int i = 0; i < size; i++)
            if (copyHere.getResource(i).getRelUrl().equals(relUrl))
                return true;
        return false;
    }

    private boolean copy(Directory copyHere, Resource resourceToCopy) {
        if (finish)
            return false;
        currentProcessing = resourceToCopy.getName();
        boolean copied = true;
        countElementsProcessed++;
        if (resourceToCopy.isDir()) {
            Directory dirToCopy = (Directory) resourceToCopy;
            boolean created = copyHere.newFolder(dirToCopy.getName());

            LocalDirectory dir = new LocalDirectory(dirToCopy.getName(),
                    String.format("%s%s%s", copyHere.getRelUrl(), File.separatorChar,
                            dirToCopy.getName()), null, resourceToCopy.isHidden(), 0);
            taskSuccessfully |= created;
            if (!created)
                return false;
            dirToCopy.openSynchronic(null);
            int size = dirToCopy.getCountElements();
            for (int i = 0; i < size; i++)
                copied &= copy(dir, dirToCopy.getResource(i));
            if (isMoving && copied)
                dirToCopy.delete(onDeleteListener);
        } else {
            copyHere.newFile(resourceToCopy.getName(), false);
            File fileIn = new File(resourceToCopy.getRelUrl()),
                    fileOut = new File(String.format("%s%s%s", copyHere.getRelUrl(),
                            File.separatorChar, resourceToCopy.getName()));

            copied = false;
            if (!resourceToCopy.getRelUrl().equals(fileOut.getPath()))
                copied = copy(fileIn, fileOut);

            taskSuccessfully |= copied;
            if (copied && Utils.getUriForFile((FileResource) resourceToCopy) != null)
                Utils.addFileToContentThread(mcontext, fileOut.getAbsolutePath());

            if (isMoving && copied)
                resourceToCopy.delete(onDeleteListener);
        }
        return copied;
    }

    public boolean copy(File fileIn, File fileOut) {
        try {
            FileInputStream reader = new FileInputStream(fileIn);
            FileOutputStream writer = new FileOutputStream(fileOut);

            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = reader.read(buffer)) > 0) {
                if (finish)
                    break;
                writer.write(buffer, 0, count);
                bytesProcessed += count;
            }
            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
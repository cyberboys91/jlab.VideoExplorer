package jlab.VideoExplorer.Service;

import android.net.Uri;
import android.view.View;
import android.os.IBinder;
import java.util.ArrayList;
import android.app.Service;
import jlab.VideoExplorer.R;
import android.content.Intent;
import android.content.Context;
import android.app.Notification;
import jlab.VideoExplorer.Utils;
import android.content.ComponentName;
import jlab.VideoExplorer.Interfaces;
import android.graphics.BitmapFactory;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import jlab.VideoExplorer.Resource.RemoteFile;

/*
 * Created by Javier on 22/9/2016.
 */
public class ResourceDownloaderService extends Service {
    private static ArrayList<ResourceInDownload> downloadsQueeue = new ArrayList<>();
    private static final String monitor = "DownloadMonitor";
    private static boolean proccesingDownload = false, running = false;
    private static ResourceInDownload current;
    private static NotificationManager notMgr;
    private final static int NOTIFICATION_ID = 910129024, TIME_INTERVAL_NOT_DOWNLOAD = 1000;
    private static Context context;
    private static Notificator notificator;
    private static Notification.Builder notBuilder;
    private static String pathDownload,  packName;
    private static Interfaces.IDownloadRefreshListener downloadCompleteListener = new Interfaces.IDownloadRefreshListener() {
        @Override
        public void complete(final ResourceInDownload resource, ResourceInDownload.DownloadStatus status) {
            synchronized (monitor) {
                int msg = getMessage(status);
                Snackbar downSnackbar = Utils.createSnackBar(String.format("%s: \"%s\", %s",
                        Utils.getString(R.string.the_file), resource.getNameResource(), Utils.getString(msg)));
                if (downSnackbar != null) {
                    if (status == ResourceInDownload.DownloadStatus.SUCCESSFUL) {
                        final Uri uri = resource.getUriDownloadFile();
                        Utils.addFileToContent(context, resource.getDownloadFile().getAbsolutePath());
                        downSnackbar.setAction(R.string.open, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    Intent newIntent = new Intent();
                                    newIntent.setAction(Intent.ACTION_VIEW);
                                    newIntent.setDataAndType(uri,
                                            resource.getMimeType());
                                    if (Utils.currentActivity != null)
                                        Utils.currentActivity.startActivity(newIntent);
                                } catch (Exception ignored) {
                                    ignored.printStackTrace();
                                }
                            }
                        });
                    }
                    downSnackbar.show();
                }
                if (downloadsQueeue.size() != 0)
                    processQueue();
                else
                    stopService();
            }
        }
    };

    @Override
    public void onCreate() {
        if (running) {
            super.onCreate();
            if (notBuilder == null)
                notBuilder = new Notification.Builder(this)
                        .setContentTitle(getString(R.string.begin_download))
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_download))
                        .setContentText(getString(R.string.begin_download));
            startForeground(NOTIFICATION_ID, Utils.getNotification(notBuilder));
        } else
            stopService(getServiceIntent());
    }

    private static Intent getServiceIntent() {
        return new Intent().setComponent(new ComponentName("jlab.VideoPlayer",
                "jlab.VideoExplorer.Service.ResourceDownloaderService"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        proccesingDownload = false;
        running = false;
    }

    public static void startService(Context context) {
        ResourceDownloaderService.context = context;
        ResourceDownloaderService.notMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (!running) {
            pathDownload = Utils.pathStorageDownload;
            packName = Utils.getPackageName();
            running = true;
            context.startService(getServiceIntent());
        }
    }

    public static void stopService() {
        proccesingDownload = false;
        running = false;
        context.stopService(getServiceIntent());
    }

    private static void enqueueResource(final RemoteFile resource) {
        try {
            synchronized (monitor) {
                if (running) {
                    downloadsQueeue.add(new ResourceInDownload(resource, downloadCompleteListener));
                    Utils.showSnackBar(R.string.enqueue_file_to_downloads);
                    if (!proccesingDownload)
                        processQueue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void notifyDownloads() {
        if (notificator != null)
            notificator.killed();
        notificator = new Notificator();
        notificator.begin();
    }


    private static int getMessage(ResourceInDownload.DownloadStatus status) {
        switch (status) {
            case FAILED:
                return R.string.has_dont_been_downloaded;
            case SUCCESSFUL:
                return R.string.has_been_downloaded;
            default:
                return R.string.pending_to_download;
        }
    }

    public static void showDownloadDialog(final RemoteFile res, final Context context) {
        AlertDialog.Builder db = new AlertDialog.Builder(context);
        db = db.setTitle(R.string.question).setMessage(String.format("%s %s: \"%s\"?",
                context.getString(R.string.want_download), ((res.isDir()) ? context.getString(R.string.the_folder) : context.getString(R.string.the_file)), res.getName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                startService(context);
                                enqueueResource(res);
                            }
                        }).start();
                    }
                });
        db.setCancelable(false).show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    private static void processQueue() {
        synchronized (monitor) {
            final ResourceInDownload elem = downloadsQueeue.get(0);
            if (elem.getStatus() == ResourceInDownload.DownloadStatus.ENQUEUE) {
                proccesingDownload = true;
                current = elem;
                downloadsQueeue.remove(0);
                notifyDownloads();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        elem.download();
                    }
                }).start();
            }
        }
    }

    public static String getPathDownload() {
        return pathDownload;
    }

    public static String getPackName() {
        return packName;
    }

    private static class Notificator {
        private boolean killed;

        public Notificator() {
            killed = false;
        }

        public void begin() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!killed && proccesingDownload && (current.getStatus() == ResourceInDownload.DownloadStatus.RUNNING
                            || current.getStatus() == ResourceInDownload.DownloadStatus.ENQUEUE)) {
                        if (notBuilder != null) {
                            int size = downloadsQueeue.size(), total = current.getTotalBytes(),
                                    part = current.getCountBytesReaded();
                            notBuilder.setContentTitle(String.format("%s %s %s",
                                    size == 0 ? context.getString(R.string.not_any) : size,
                                    size < 2 ? context.getString(R.string.file) : context.getString(R.string.files),
                                    context.getString(R.string.enqueue)))
                                    .setProgress(total, part, total == part)
                                    .setContentText(current.getNameResource())
                                    .setContentInfo(String.format("%s", total != 0
                                            ? Utils.getPercent(part, total) + "%" : ""));
                            notMgr.notify(NOTIFICATION_ID, Utils.getNotification(notBuilder));
                        }
                        try {
                            Thread.sleep(TIME_INTERVAL_NOT_DOWNLOAD);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        public void killed() {
            killed = true;
        }
    }
}
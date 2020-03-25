package jlab.VideoExplorer.Activity.Fragment;

import android.view.View;
import android.os.Bundle;
import jlab.VideoExplorer.R;
import android.content.Context;
import android.widget.TextView;
import android.app.Notification;
import android.app.DialogFragment;
import android.widget.ProgressBar;
import android.view.LayoutInflater;
import jlab.VideoExplorer.Utils;
import android.graphics.BitmapFactory;
import android.app.NotificationManager;
import android.content.DialogInterface;
import jlab.VideoExplorer.Interfaces;
import android.support.v7.app.AlertDialog;
import jlab.VideoExplorer.Resource.Resource;
import jlab.VideoExplorer.Activity.DirectoryActivity;
import jlab.VideoExplorer.Service.DeleteResourceService;
import static jlab.VideoExplorer.Service.TaskFileSystemChangeService.TIME_INTERVAL_TASK_REFRESH;


public class DeleteFragment extends DialogFragment {

    private static Resource mresource;
    private static TextView mtvCurrent;
    private static TextView mtvCount;
    private static TextView mtvPercent;
    private static TextView mtvData;
    private static ProgressBar mpbProgress;
    private static int NOTIFICATION_ID = 910198;
    private static NotificationManager mnotMgr;
    private static boolean interrupted = false;
    public static DeleteResourceService deleteService = new DeleteResourceService();

    private static Interfaces.IRefreshListener mrefreshListener = new Interfaces.IRefreshListener() {
        @Override
        public void refresh() {

        }
    };
    private static Interfaces.ICopyRefresh mdeleteRefresh = new Interfaces.ICopyRefresh() {
        @Override
        public void refresh(Runnable run) {

        }
    };
    private Thread mthreadDelete = new Thread(new Runnable() {
        @Override
        public void run() {
            deleteService.beginDelete(mresource);
        }
    });

    public static void setDeleteRefresh(Interfaces.ICopyRefresh copyRefresh) {
        DeleteFragment.mdeleteRefresh = copyRefresh;
    }

    public static void setRefreshListener(Interfaces.IRefreshListener refreshListener) {
        DeleteFragment.mrefreshListener = refreshListener;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setCancelable(false);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        mnotMgr = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        DirectoryActivity.setDeleteFragmentFinish(new Interfaces.IRefreshListener() {
            @Override
            public void refresh() {
                try {
                    dismiss();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public AlertDialog onCreateDialog(Bundle saveInstance) {
        mresource = (Resource) getArguments().getSerializable(Utils.RESOURCE_FOR_DELETE);
        final LayoutInflater inflater = LayoutInflater.from(getActivity().getBaseContext());
        View fragmentView = inflater.inflate(R.layout.copy_delete_fragment, null);
        mtvCurrent = (TextView) fragmentView.findViewById(R.id.tvCurrentCopy);
        mtvPercent = (TextView) fragmentView.findViewById(R.id.tvPercentCopy);
        mtvData = (TextView) fragmentView.findViewById(R.id.tvDataCopy);
        mtvCount = (TextView) fragmentView.findViewById(R.id.tvCountCopy);
        mpbProgress = (ProgressBar) fragmentView.findViewById(R.id.pbProgressCopy);

        final Notification.Builder notBuilder = new Notification.Builder(getActivity().getApplicationContext())
                .setSmallIcon(R.drawable.img_delete)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.img_not_delete))
                .setContentTitle(getString(R.string.deleting))
                .setContentText(mresource.getName());

        if (deleteService.isFinish()) {
            interrupted = false;
            deleteService.startService(inflater.getContext(), NOTIFICATION_ID, Utils.getNotification(notBuilder));
        }

        final String deleteComplete = getString(R.string.delete_complete),
                deleteInterrupted = getString(R.string.delete_interrupted),
                msgComplete = String.format("%s \"%s\" %s", getString(mresource.isDir() ? R.string.the_folder : R.string.the_file),
                        mresource.getName(), getString(R.string.deleted_successfully)),
                notDeleted = String.format("%s \"%s\" %s", getString(mresource.isDir() ? R.string.the_folder : R.string.the_file),
                        mresource.getName(), getString(R.string.not_deleted));
        deleteService.setRefreshListener(new Interfaces.IRefreshListener() {
            @Override
            public void refresh() {
                mdeleteRefresh.refresh(new Runnable() {
                    @Override
                    public void run() {
                        mtvCurrent.setText(deleteService.getCurrentProcessing());
                        mtvCount.setText(String.format("%s/%s", deleteService.getCountElementsProcessed(),
                                deleteService.getCountElemntsTotal()));
                        if (deleteService.isPreparing() && mpbProgress.isIndeterminate()) {
                            mpbProgress.setIndeterminate(true);
                            notBuilder.setProgress(0, 0, true);
                        } else {
                            final boolean overflow = deleteService.getCountBytes() > Integer.MAX_VALUE;
                            int countBytes = (int) (deleteService.getCountBytes() / (overflow ? 1024 : 1)),
                                    progress = (int) (deleteService.getBytesProcessed() / (overflow ? 1024 : 1));
                            String percent = String.format("%s", Utils.getPercent(progress, countBytes) + "%");
                            notBuilder.setContentText(deleteService.getCurrentProcessing())
                                    .setProgress(countBytes, progress, false)
                                    .setContentInfo(percent);
                            mpbProgress.setMax(countBytes);
                            mpbProgress.setProgress(progress);
                            if (countBytes != 0)
                                mtvPercent.setText(percent);
                        }
                        mnotMgr.notify(NOTIFICATION_ID, Utils.getNotification(notBuilder));
                        mtvData.setText(String.format("%s/%s", Utils.getSizeString(deleteService.getBytesProcessed(), 1),
                                Utils.getSizeString(deleteService.getCountBytes(), 1)));
                    }
                });
            }
        });
        deleteService.setFinishListener(new Interfaces.IFinishListener() {
            @Override
            public void refresh(final boolean successfully) {
                try {
                    mrefreshListener.refresh();
                } catch (Exception exp) {
                    exp.printStackTrace();
                } finally {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(TIME_INTERVAL_TASK_REFRESH);
                                notBuilder.setProgress(0, 0, false)
                                        .setContentTitle(interrupted ? deleteInterrupted : deleteComplete)
                                        .setContentText(interrupted ? null : (successfully ? msgComplete : notDeleted))
                                        .setContentInfo("");
                                mnotMgr.notify(NOTIFICATION_ID, Utils.getNotification(notBuilder));
                                Thread.sleep(Utils.FIVE_SECONDS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                mnotMgr.cancel(NOTIFICATION_ID);
                            }
                        }
                    }).start();
                    Utils.showSnackBar(interrupted ? deleteInterrupted : (successfully ? msgComplete : notDeleted));
                }
            }
        });
        mtvCurrent.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mthreadDelete.start();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                    Utils.showSnackBar(R.string.delete_interrupted);
                }
            }
        });

        return new AlertDialog.Builder(inflater.getContext())
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        interrupted = true;
                        if (isRunning())
                            deleteService.stopService();
                    }
                })
                .setPositiveButton(R.string.hide, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mrefreshListener.refresh();
                    }
                })
                .setTitle(R.string.delete)
                .setView(fragmentView)
                .create();
    }

    public static boolean isRunning() {
        return deleteService.isRunning();
    }

    public static void setIsRunning(boolean isRunning) {
        deleteService.setisRunning(isRunning);
    }
}
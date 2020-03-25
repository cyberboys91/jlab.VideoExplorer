package jlab.VideoExplorer.Service;

import android.view.KeyEvent;
import android.content.Intent;
import jlab.VideoExplorer.Utils;
import android.content.Context;
import android.app.PendingIntent;
import android.media.AudioManager;
import jlab.VideoExplorer.Interfaces;
import jlab.VideoExplorer.Resource.*;
import android.content.ComponentName;
import android.media.RemoteControlClient;
import android.content.BroadcastReceiver;
import static jlab.VideoExplorer.Service.MediaPlayerService.play;
import static jlab.VideoExplorer.Service.MediaPlayerService.pause;
import static jlab.VideoExplorer.Service.MediaPlayerService.seekTo;
import static jlab.VideoExplorer.Service.MediaPlayerService.running;
import static jlab.VideoExplorer.Service.MediaPlayerService.playNext;
import static jlab.VideoExplorer.Service.MediaPlayerService.isPlaying;
import static jlab.VideoExplorer.Service.MediaPlayerService.playPrevious;
import static jlab.VideoExplorer.Service.MediaPlayerService.getCurrentPosition;

/*
 * Created by Javier on 3/10/2016.
 */
public class BackMediaPlayerReceiver extends BroadcastReceiver {

    private static AudioManager maMgr;
    private static Interfaces.ICloseListener monCloseListener;
    private static Interfaces.IRefreshListener mrefreshListener;
    private static ComponentName mcomponentName;
    private static RemoteControlClient mremoteControlClient;
    public static final short SEEK_MAX_COUNT = 10000, SEEK_MIN_COUNT = 3000;
    private static long lastPlayOrPauseTime;
    private static final short intervalTime = 500;

    public BackMediaPlayerReceiver() {

    }

    public static void register(Context context) {
        mcomponentName = new ComponentName(context.getPackageName(), BackMediaPlayerReceiver.class.getName());
        maMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        maMgr.registerMediaButtonEventReceiver(mcomponentName);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mcomponentName);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, mediaButtonIntent, 0);
        mremoteControlClient = new RemoteControlClient(mediaPendingIntent);
        maMgr.registerRemoteControlClient(mremoteControlClient);
    }

    private static void unregister() {
        maMgr.unregisterMediaButtonEventReceiver(mcomponentName);
        maMgr.unregisterRemoteControlClient(mremoteControlClient);
    }

    public static int setCloseAndRefreshListener(Interfaces.ICloseListener closeListener,
                                                 Interfaces.IRefreshListener refreshListener) {
        int newId = 0;
        if (monCloseListener != null && closeListener.getId() != monCloseListener.getId()) {
            newId = monCloseListener.getId() + 1;
            monCloseListener.close();
        } else if (monCloseListener != null)
            newId = monCloseListener.getId();
        monCloseListener = closeListener;
        mrefreshListener = refreshListener;
        return newId;
    }

    public static void closeListener() {
        if (monCloseListener != null)
            monCloseListener.close();
    }

    public static void close() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                closeListener();
                monCloseListener = null;
                MediaPlayerService.stopService();
                unregister();
            }
        }).start();
    }

    public static boolean checkPlayNextOrSeekInTime(long time) {
        boolean result = time - lastPlayOrPauseTime < intervalTime;
        if (result)
            playNext();
        lastPlayOrPauseTime = time;
        return result;
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        boolean match = true, isNextOrPrev = false;
        switch (intent.getAction()) {
            case Utils.MEDIA_FF:
                seekTo(getCurrentPosition() + SEEK_MAX_COUNT);
                break;
            case Utils.MEDIA_REW:
                seekTo(getCurrentPosition() - SEEK_MAX_COUNT);
                break;
            case Utils.MEDIA_NEXT:
                isNextOrPrev = true;
                playNext();
                break;
            case Utils.MEDIA_PREV:
                isNextOrPrev = true;
                playPrevious();
                break;
            case Utils.MEDIA_PLAY_OR_PAUSE:
                if (isPlaying())
                    pause();
                else play();
                break;
            case Utils.MEDIA_CLOSE:
                close();
                return;
            default:
                match = false;
                break;
        }
        if (!match) {
            if(running()) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                    match = true;
                    pause();
                } else {
                    KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        match = true;
                        switch (event.getKeyCode()) {
                            case KeyEvent.KEYCODE_HEADSETHOOK:
                            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                                isNextOrPrev = checkPlayNextOrSeekInTime(event.getEventTime());
                                if (!isNextOrPrev) {
                                    if (isPlaying())
                                        pause();
                                    else play();
                                }
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PLAY:
                                isNextOrPrev = checkPlayNextOrSeekInTime(event.getEventTime());
                                if (!isNextOrPrev)
                                    play();
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                                isNextOrPrev = checkPlayNextOrSeekInTime(event.getEventTime());
                                if (!isNextOrPrev)
                                    pause();
                                break;
                            case KeyEvent.KEYCODE_MEDIA_NEXT:
                                isNextOrPrev = true;
                                playNext();
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                isNextOrPrev = true;
                                playPrevious();
                                break;
                            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                                seekTo(getCurrentPosition() + SEEK_MAX_COUNT);
                                break;
                            default:
                                match = false;
                                break;
                        }
                    }
                }
            }
        }
        if (match && !isNextOrPrev && mrefreshListener != null)
            mrefreshListener.refresh();
    }

    public static void notifyLoaded(FileResource fsFile) {
        if (monCloseListener != null)
            monCloseListener.loaded(fsFile);
    }

    public static void refresh() {
        if (mrefreshListener != null)
            mrefreshListener.refresh();
    }
}
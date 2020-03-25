package jlab.VideoExplorer.Service;

import android.net.Uri;
import android.os.IBinder;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.PowerManager;
import jlab.VideoExplorer.*;
import android.graphics.Bitmap;
import android.app.Notification;
import android.app.PendingIntent;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.view.SurfaceHolder;
import android.widget.RemoteViews;
import android.net.wifi.WifiManager;
import android.content.ComponentName;
import android.graphics.BitmapFactory;
import android.app.NotificationManager;
import android.content.ContentResolver;

import jlab.VideoExplorer.Resource.*;

/*
 * Created by Javier on 09/04/2017.
 */

public class MediaPlayerService extends Service {

    private static AudioManager maMgr;
    private static MediaPlayer mediaPlayer = new MediaPlayer();
    private static final String monitor = "";
    private static boolean mpIsAlive = false, loaded = false, error = false,
            running = false, isPlayingWhenLossFocus = true, isAudio;
    private static Context mcontext;
    private static NotificationManager mnotMgr;
    private static int bufferPercentage = 0;
    private static int videoWidth = 0, videoHeight = 0, duration = 0, NOTIFICATION_ID = 910130;
    private static int mindexCurrentElemPlaying;
    private static WifiManager.WifiLock mWifiLock;
    public static Directory mdirectory;
    private static Interfaces.IElementRefreshListener mrefreshListener;
    private static ContentResolver resolver;
    private static int thumbFront;
    private static Bitmap thumbBack;
    public static int currentBrightness = -1;

    private static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int i) {
            audioFocusChange(i);
        }
    };

    private static Interfaces.ICreateDialogListener monCreateDialogListener = new Interfaces.ICreateDialogListener() {
        @Override
        public void createDialog(int msgId) {

        }
    };

    public static Bitmap getThumbBack() {
        return thumbBack;
    }

    public static int getThumbFront() {
        return thumbFront;
    }

    public static int getBufferPercentage() {
        return bufferPercentage;
    }

    public static int getIndexCurrentPlaying() {
        return mindexCurrentElemPlaying;
    }

    public static void setIndexCurrentPlaying(int current) {
        mindexCurrentElemPlaying = current;
    }

    public static int getVideoWidth() {
        return videoWidth;
    }

    public static int getVideoHeight() {
        return videoHeight;
    }

    public static void setOnCreateDialogListener(Interfaces.ICreateDialogListener newOnCreateDialogListener) {
        monCreateDialogListener = newOnCreateDialogListener;
    }

    private static void notifyLoaded(FileResource fsFile) {
        loaded = true;
        BackMediaPlayerReceiver.notifyLoaded(fsFile);
        notifyLoaded(fsFile, isPlaying());
    }

    public static void stopService() {
        running = false;
        mdirectory = null;
        if (isPlaying())
            mediaPlayer.pause();
        if (mcontext != null)
            mcontext.stopService(getServiceIntent());
        if (maMgr != null)
            maMgr.abandonAudioFocus(onAudioFocusChangeListener);
    }

    public static boolean startService(Context context, boolean isAudio,
                                       Interfaces.IElementRefreshListener refreshListener) {
        mrefreshListener = refreshListener;
        mcontext = context;
        boolean result = running;
        MediaPlayerService.isAudio = isAudio;
        if (!running) {
            mnotMgr = (NotificationManager) mcontext.getSystemService(NOTIFICATION_SERVICE);
            maMgr = (AudioManager) mcontext.getSystemService(Context.AUDIO_SERVICE);
            BackMediaPlayerReceiver.register(mcontext);
            running = true;
            mcontext.startService(getServiceIntent());
        }
        return result;
    }

    public static void setRefreshListener(Interfaces.IElementRefreshListener newRefresh) {
        mrefreshListener = newRefresh;
    }

    private static Intent getServiceIntent() {
        return new Intent().setComponent(new ComponentName("jlab.VideoExplorer",
                "jlab.VideoExplorer.Service.MediaPlayerService"));
    }

    public static void setDisplay(SurfaceHolder holder) {
        synchronized (monitor) {
            try {
                if (mediaPlayer != null) {
                    //No cambiar el orden
                    mediaPlayer.setDisplay(holder);
                    mediaPlayer.setScreenOnWhilePlaying(holder != null);
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }

    public void onCreate() {
        if (running) {
            resolver = getContentResolver();
            super.onCreate();
            startForeground(NOTIFICATION_ID, Utils.getNotification(
                    new Notification.Builder(this)
                            .setSmallIcon(isAudio
                                    ? R.drawable.img_audio_not
                                    : R.drawable.img_video_not)
                            .setContentTitle(isAudio
                                    ? getString(R.string.ap_activity_name)
                                    : getString(R.string.vp_activity_name))
                            .setContentText(getString(R.string.preparing_for_play))
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), isAudio
                                    ? R.drawable.icon_audio
                                    : R.drawable.icon_video))));
        } else
            stopService(getServiceIntent());
    }

    public static void initMediaPlayer(final FileResource fsFile) {
        loaded = false;
        error = false;
        mediaPlayer.reset();
        try {
            bufferPercentage = videoWidth = videoHeight = 0;
            mediaPlayer.setDataSource(mcontext, Uri.parse(fsFile.getAbsUrl()));
            mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
                    if (videoWidth == 0 && videoHeight == 0) {
                        videoWidth = width;
                        videoHeight = height;
                    }
                }
            });
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    synchronized (monitor) {
                        mpIsAlive = true;
                        duration = mediaPlayer.getDuration();
                        mediaPlayer.setWakeMode(mcontext, PowerManager.PARTIAL_WAKE_LOCK);
                        start();
                        notifyLoaded(fsFile);
                    }
                }
            });
            mediaPlayer.setWakeMode(mcontext, PowerManager.PARTIAL_WAKE_LOCK);
            if (fsFile.isRemote()) {
                mWifiLock = ((WifiManager) mcontext.getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "jlab.MediaPlayer.WifiLock");
                mWifiLock.acquire();
            }
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    error();
                    return true;
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playNext();
                }
            });
            mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    bufferPercentage = percent;
                }
            });
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
            error();
        }
    }

    public static boolean running() {
        return running;
    }

    public static boolean loaded() {
        return loaded;
    }

    private static void error() {
        synchronized (monitor) {
            error = true;
            mpIsAlive = false;
            notifyLoaded(getCurrentPlaying());
            monCreateDialogListener.createDialog(R.string.dont_start);
        }
    }

    public static boolean isError() {
        return error;
    }

    private static void start() {
        maMgr.requestAudioFocus(onAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mediaPlayer.start();
    }

    public static boolean pause() {
        boolean paused = false;
        synchronized (monitor) {
            if (running && !error && isPlaying()) {
                mediaPlayer.pause();
                paused = true;
            } else if (error)
                monCreateDialogListener.createDialog(R.string.dont_start);
        }
        if (paused)
            notifyPause();
        return paused;
    }

    public static boolean play() {
        boolean playing = false;
        synchronized (monitor) {
            if (running && !error && !isPlaying()) {
                start();
                playing = true;
            } else if (error)
                monCreateDialogListener.createDialog(R.string.dont_start);
        }
        if (playing)
            notifyPlay();
        return playing;
    }

    public static boolean isPlaying() {
        return mpIsAlive && mediaPlayer != null && loaded && mediaPlayer.isPlaying();
    }

    public static void seekTo(int miliseconds) {
        synchronized (monitor) {
            try {
                if (error)
                    monCreateDialogListener.createDialog(R.string.dont_start);
                else if (running && mpIsAlive && mediaPlayer != null && loaded)
                    mediaPlayer.seekTo(miliseconds);
            } catch (IllegalStateException ignored) {
                ignored.printStackTrace();
            }
        }
    }

    public static int getDuration() {
        if (mpIsAlive && mediaPlayer != null && loaded)
            return duration;
        return 0;
    }

    public static int getCurrentPosition() {
        if (mpIsAlive && mediaPlayer != null && loaded)
            return mediaPlayer.getCurrentPosition();
        return 0;
    }

    public static int getAudioSessionId() {
        synchronized (monitor) {
            return mpIsAlive && mediaPlayer != null ? mediaPlayer.getAudioSessionId() : 0;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        killMediaPlayer();
        stopForeground(true);
        running = false;
        mdirectory = null;
        currentBrightness = -1;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        DownloadImageTask.freeCache();
        Utils.freeUnusedMemory();
    }

    public static void resetMediaPlayer(FileResource fsFile) {
        synchronized (monitor) {
            if (mediaPlayer != null) {
                mpIsAlive = false;
                initMediaPlayer(fsFile);
            }
        }
    }

    public static void killMediaPlayer() {
        synchronized (monitor) {
            if (mediaPlayer != null) {
                try {
                    mpIsAlive = false;
                    loaded = false;
                    mediaPlayer.release();
                    maMgr.abandonAudioFocus(onAudioFocusChangeListener);
                    if (mWifiLock != null)
                        mWifiLock.release();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mediaPlayer = new MediaPlayer();
                }
            }
        }
    }

    private static void audioFocusChange(int i) {
        switch (i) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (isPlayingWhenLossFocus && !isPlaying()) {
                    play();
                    if (mrefreshListener != null)
                        mrefreshListener.refresh(getCurrentPlaying(), mindexCurrentElemPlaying, true);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (isPlaying()) {
                    isPlayingWhenLossFocus = true;
                    pause();
                    if (mrefreshListener != null)
                        mrefreshListener.refresh(getCurrentPlaying(), mindexCurrentElemPlaying, false);
                } else
                    isPlayingWhenLossFocus = false;
                break;
            default:
                break;
        }
    }

    public static void notifyPause() {
        try {
            mnotMgr.notify(NOTIFICATION_ID,
                    Utils.getNotification(getNotificationBuilder(getCurrentPlaying(), false)));
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    private static void notifyPlay() {
        try {
            mnotMgr.notify(NOTIFICATION_ID,
                    Utils.getNotification(getNotificationBuilder(getCurrentPlaying(), true)));
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    private static Notification.Builder getNotificationBuilder(final FileResource file, boolean isplaying) {
        RemoteViews remoteViews = new RemoteViews(Utils.getPackageName(), R.layout.media_player_notification);
        remoteViews.setOnClickPendingIntent(R.id.ivMediaCloseNotify, getPendintIntent(Utils.MEDIA_CLOSE));
        remoteViews.setOnClickPendingIntent(R.id.ivMediaNextNotify, getPendintIntent(Utils.MEDIA_NEXT));
        remoteViews.setOnClickPendingIntent(R.id.ivMediaPrevNotify, getPendintIntent(Utils.MEDIA_PREV));
        remoteViews.setOnClickPendingIntent(R.id.ivMediaSeekRewNotify, getPendintIntent(Utils.MEDIA_REW));
        remoteViews.setOnClickPendingIntent(R.id.ivMediaSeekFFNotify, getPendintIntent(Utils.MEDIA_FF));
        remoteViews.setOnClickPendingIntent(R.id.ivMediaPlayPauseNotify, getPendintIntent(Utils.MEDIA_PLAY_OR_PAUSE));

        remoteViews.setTextViewText(R.id.tvResourceName, file.getName());
        thumbFront = R.color.transparent;
        if (!file.isRemote()) {
            thumbBack = DownloadImageTask.get(file.thumbUrl());
            if (thumbBack == null) {
                if (resolver != null)
                    thumbBack = Utils.getThumbnailForUriFile(resolver, file.thumbUrl(), file);
                else
                    thumbBack = Utils.getThumbnailForUriFile(file.thumbUrl(), file);
                if (thumbBack != null)
                    DownloadImageTask.put(file.thumbUrl(), thumbBack);
            }
            if (thumbBack == null)
                remoteViews.setImageViewResource(R.id.ivResourceIcon, file.isAudio()
                        ? R.drawable.icon_audio : R.drawable.icon_video);
            else {
                remoteViews.setImageViewBitmap(R.id.ivResourceIcon, thumbBack);
                thumbFront = file.isAudio()
                        ? R.drawable.img_small_audio : R.drawable.img_small_play_video;
            }
        } else {
            thumbBack = null;
            remoteViews.setImageViewResource(R.id.ivResourceIcon,
                    file.isAudio() ? R.drawable.icon_audio : R.drawable.icon_video);
        }

        remoteViews.setImageViewResource(R.id.ivResourceIconType, thumbFront);
        remoteViews.setImageViewResource(R.id.ivMediaPlayPauseNotify,
                isplaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        remoteViews.setOnClickPendingIntent(R.id.llnotificationArea,
                PendingIntent.getActivity(mcontext, 0, getMediaIntent(file), 0));

        return new Notification.Builder(mcontext)
                .setSmallIcon(file.isAudio() ? R.drawable.img_audio_not : R.drawable.img_video_not)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContent(remoteViews);
    }

    public static Intent getMediaIntent(FileResource file) {
        Intent intent = new Intent(file.isAudio() ? Utils.ACTION_AUDIO : Utils.ACTION_VIDEO);
        intent.setDataAndType(Uri.parse(file.getAbsUrl()), file.getMimeType());
        intent.putExtra(Utils.INDEX_CURRENT_KEY, mindexCurrentElemPlaying);
        intent.putExtra(Utils.DIRECTORY_KEY, mdirectory);
        intent.putExtra(Utils.HOST_SERVER_KEY, Utils.hostServer);
        intent.putExtra(Utils.PORT_SERVER_KEY, Utils.portServer);
        return intent;
    }

    public static Intent getMediaIntent() {
        return getMediaIntent(getCurrentPlaying());
    }

    public static FileResource getCurrentPlaying() {
        return (FileResource) mdirectory.getResource(mindexCurrentElemPlaying);
    }

    public static void playPrevious() {
        if (running && loaded()) {
            if (mdirectory != null)
                mindexCurrentElemPlaying = mindexCurrentElemPlaying > 0
                        ? mindexCurrentElemPlaying - 1
                        : mdirectory.getContent().size() - 1;
            playCurrent();
        }
    }

    public static void playNext() {
        if (running && loaded()) {
            if (mdirectory != null)
                mindexCurrentElemPlaying = (mindexCurrentElemPlaying + 1) % mdirectory.getContent().size();
            playCurrent();
        }
    }

    public static void playCurrent() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final FileResource currentPlaying = getCurrentPlaying();
                    notifyLoaded(currentPlaying, false);
                    resetMediaPlayer(currentPlaying);
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        }).start();
    }

    public static void notifyLoaded(FileResource file, boolean isplaying) {
        mnotMgr.notify(NOTIFICATION_ID, Utils.getNotification(getNotificationBuilder(file, isplaying)));
        if (mdirectory != null)
            try {
                mrefreshListener.refresh(file, mindexCurrentElemPlaying, isplaying);
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
    }

    public static void notifyLoaded() {
        notifyLoaded(getCurrentPlaying(), isPlaying());
    }

    private static PendingIntent getPendintIntent(String action) {
        return PendingIntent.getBroadcast(mcontext, 0, new Intent(action), 0);
    }

    public static void refreshProperties() {
        if (mdirectory != null)
            notifyLoaded(getCurrentPlaying(), isPlaying());
    }

    public static void setAudioVolume(int volume) {
        if (maMgr != null)
            maMgr.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    volume,
                    0);
    }

    public static int getAudioVolume() {
        return maMgr == null ? 0 : maMgr.getStreamVolume(AudioManager.STREAM_MUSIC);
    }
}
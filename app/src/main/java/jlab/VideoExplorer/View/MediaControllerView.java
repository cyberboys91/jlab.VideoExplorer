package jlab.VideoExplorer.View;

import java.net.URL;
import android.net.Uri;
import android.view.View;
import android.os.Bundle;
import android.view.KeyEvent;
import android.content.Context;
import android.util.AttributeSet;
import jlab.VideoExplorer.Utils;
import android.widget.MediaController;
import android.support.annotation.NonNull;
import jlab.VideoExplorer.Interfaces;
import jlab.VideoExplorer.Resource.Resource;
import jlab.VideoExplorer.Resource.Directory;
import jlab.VideoExplorer.Resource.LocalFile;
import jlab.VideoExplorer.Resource.RemoteFile;
import jlab.VideoExplorer.Resource.FileResource;
import jlab.VideoExplorer.Resource.LocalDirectory;
import jlab.VideoExplorer.Resource.RemoteDirectory;
import jlab.VideoExplorer.Activity.DirectoryActivity;
import jlab.VideoExplorer.Service.MediaPlayerService;
import jlab.VideoExplorer.Activity.MediaPlayerActivity;

import static jlab.VideoExplorer.Service.MediaPlayerService.play;
import static jlab.VideoExplorer.Service.MediaPlayerService.playNext;
import static jlab.VideoExplorer.Service.MediaPlayerService.mdirectory;
import static jlab.VideoExplorer.Service.MediaPlayerService.playCurrent;
import static jlab.VideoExplorer.Service.MediaPlayerService.notifyLoaded;
import static jlab.VideoExplorer.Service.MediaPlayerService.playPrevious;
import static jlab.VideoExplorer.Service.MediaPlayerService.setIndexCurrentPlaying;

/*
 * Created by Javier on 25/9/2016.
 */
public class MediaControllerView extends MediaController{

    private MediaPlayerActivity activityPlayer;
    private Bundle bundleIntent;
    private Uri uriIntent;
    private boolean isShow;
    private Interfaces.IHideListener monHideListener = new Interfaces.IHideListener() {
        @Override
        public void hide() {

        }
    };

    public MediaControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMediaPlayer(new DefaultMediaPlayerControl(this));
        setnextAndPrevButtons();
    }

    public MediaControllerView(MediaPlayerActivity actPlayer) {
        super(actPlayer, true);
        setMediaPlayer(new DefaultMediaPlayerControl(this));
        setnextAndPrevButtons();
        activityPlayer = actPlayer;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            activityPlayer.close();
            return true;
        }
        return !(keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE)
                && activityPlayer.dispatchKeyEvent(event);
    }

    public void setOnHideListener(Interfaces.IHideListener newonHideListener) {
        this.monHideListener = newonHideListener;
    }

    public void setUriBundleIntentAndView(Uri uri, Bundle bundle, View view) {
        setAnchorView(view);
        this.bundleIntent = bundle;
        this.uriIntent = uri;
    }

    public void load(Bundle saved) {
        if (saved != null && (mdirectory = (Directory) saved.getSerializable(Utils.DIRECTORY_KEY)) != null) {
            notifyLoaded();
            if (!MediaPlayerService.loaded())
                activityPlayer.loadingVisible();
        } else {
            if (bundleIntent != null && bundleIntent.containsKey(Utils.HOST_SERVER_KEY)) {
                Utils.setHostPortAndUrlServer(bundleIntent.getString(Utils.HOST_SERVER_KEY),
                        bundleIntent.getString(Utils.PORT_SERVER_KEY));

                mdirectory = bundleIntent.containsKey(Utils.DIRECTORY_KEY)
                        ? (Directory) bundleIntent.getSerializable(Utils.DIRECTORY_KEY)
                        : DirectoryActivity.getDirectory();

                if (mdirectory != null) {
                    loadOnlySelector(bundleIntent.getInt(Utils.INDEX_CURRENT_KEY, 0));
                    activityPlayer.loadingVisible();
                    playCurrent();
                }
            } else {
                setIndexCurrentPlaying(0);
                mdirectory = new RemoteDirectory("Media", "jlab", null);
                loadSize();
            }
        }
    }

    private void setnextAndPrevButtons() {
        setPrevNextListeners(new OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevious();
            }
        });
    }

    public void superHide() {
        isShow = false;
        super.hide();
    }

    @Override
    public void hide() {
        monHideListener.hide();
    }

    public void showForever() {
        try {
            isShow = true;
            show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isShow() {
        return isShow;
    }

    private void loadSize() {
        final int[] size = {0};
        try {
            final URL finalUrlRes = new URL(uriIntent.toString());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Utils.setHostPortAndUrlServer(uriIntent.getHost(), Integer.toString(uriIntent.getPort()));
                        FileResource elem = uriIntent.getScheme().equals(Utils.FILE_SCHEME)
                                ? new LocalFile(FileResource.getNameFromUrl(uriIntent.getPath())
                                , uriIntent.getPath(), null, null, size[0], 0, false)
                                : new RemoteFile(FileResource.getNameFromUrl(uriIntent.getPath())
                                , uriIntent.getPath(), size[0]);
                        if (elem.isRemote() && elem instanceof RemoteFile)
                            ((RemoteFile) elem).setAbsUrl(uriIntent.toString());
                        mdirectory.getContent().add(elem);
                        activityPlayer.loadingVisible();
                        playCurrent();
                        size[0] = finalUrlRes.openConnection().getContentLength();
                        elem.mSize = size[0];
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        MediaPlayerService.notifyLoaded();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            showForever();
        }
    }

    public Directory getDirectory()
    {
        return mdirectory;
    }

    private void loadOnlySelector(int indexFsfile) {
        int index = indexFsfile;
        Directory dirFilter = mdirectory.isRemote() ?
                new RemoteDirectory(mdirectory.getName(), mdirectory.getRelUrl(), null)
                : new LocalDirectory(mdirectory.getName(), mdirectory.getRelUrl(), null, false, 0, 0, 0);
        for (int i = 0; i < mdirectory.getCountElements(); i++) {
            Resource elem = mdirectory.getResource(i);
            if (!elem.isDir() && activityPlayer.select(((FileResource) elem).getMimeType())) {
                dirFilter.getContent().add(elem);
            } else if (i < index) {
                indexFsfile--;
            }
        }
        setIndexCurrentPlaying(indexFsfile);
        mdirectory = dirFilter;
    }

    class DefaultMediaPlayerControl implements MediaPlayerControl {
        private MediaControllerView mediaControllerView;

        public DefaultMediaPlayerControl(MediaControllerView mediaControllerView) {
            this.mediaControllerView = mediaControllerView;
        }

        @Override
        public void start() {
            if (!isPlaying()) {
                if (play()) {
                    mediaControllerView.showForever();
                }
            }
        }

        @Override
        public void pause() {
            if (isPlaying()) {
                if (MediaPlayerService.pause()) {
                    mediaControllerView.showForever();
                }
            }
        }

        @Override
        public int getDuration() {
            return MediaPlayerService.getDuration();
        }

        @Override
        public int getCurrentPosition() {
            return MediaPlayerService.getCurrentPosition();
        }

        @Override
        public void seekTo(int pos) {
            MediaPlayerService.seekTo(pos);
        }

        @Override
        public boolean isPlaying() {
            return MediaPlayerService.isPlaying();
        }

        @Override
        public int getBufferPercentage() {
            return MediaPlayerService.getBufferPercentage();
        }

        @Override
        public boolean canPause() {
            return true;
        }

        @Override
        public boolean canSeekBackward() {
            return true;
        }

        @Override
        public boolean canSeekForward() {
            return true;
        }

        @Override
        public int getAudioSessionId() {
            return MediaPlayerService.getAudioSessionId();
        }
    }
}
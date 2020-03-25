package jlab.VideoExplorer.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import jlab.VideoExplorer.R;
import android.widget.TextView;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.view.SurfaceHolder;
import jlab.VideoExplorer.Utils;
import android.app.DownloadManager;
import android.app.NotificationManager;
import jlab.VideoExplorer.Interfaces;
import android.support.annotation.NonNull;
import jlab.VideoExplorer.DownloadImageTask;
import jlab.VideoExplorer.View.ImageDownload;
import jlab.VideoExplorer.Resource.FileResource;

import android.support.v4.widget.SwipeRefreshLayout;
import jlab.VideoExplorer.View.MediaControllerView;
import jlab.VideoExplorer.Service.MediaPlayerService;
import jlab.VideoExplorer.Service.BackMediaPlayerReceiver;
import static jlab.VideoExplorer.Service.MediaPlayerService.playCurrent;

/*
 * Created by Javier on 25/9/2016.
 */

public class MediaPlayerActivity extends Activity implements Interfaces.IElementSelectorListener,
        Interfaces.ICreateDialogListener, Interfaces.IStatusChangeListener,
        Interfaces.IElementRefreshListener, Interfaces.IAudioPlayerCreateListener,
        Interfaces.ICloseListener, SurfaceHolder.Callback {

    protected TextView mtvNameRes;
    protected TextView mtvSizeRes;
    protected static DownloadManager mdMgr;
    protected static NotificationManager mnotMgr;
    protected SwipeRefreshLayout msrlRefresh;
    protected ImageDownload mivDownload;
    protected String mprefixMimeType = Utils.AUDIO;
    protected View mvideSpace;
    protected MediaControllerView mediaController;
    protected SurfaceView msurfaceView;
    protected View mProperties;
    protected boolean isAudio;
    private Toast toast;
    private int id = -1;
    private boolean loaded = false;
    private final String loadingMonitor = "";
    private static boolean isClosed = true;
    private static final String OGG_MIMETYPE = "application/ogg";
    private ImageView mivResThumbBack;
    private Bitmap bmAlbumThumbnail;
    private Bundle saveInstance;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.saveInstance = savedInstanceState;
        Utils.currentActivity = this;
        isClosed = false;
        if (savedInstanceState != null && savedInstanceState.containsKey(Utils.ID))
            this.id = savedInstanceState.getInt(Utils.ID);
        if (this.isAudio = this.mprefixMimeType.equals(Utils.AUDIO))
            setContentView(R.layout.activity_audio_player);
        else
            setContentView(R.layout.activity_video_player);
        this.msurfaceView = ((SurfaceView) findViewById(R.id.svSpaceShow));
        this.mivDownload = (ImageDownload) findViewById(R.id.ivDownload);
        //TODO: Continuar
        this.mtvNameRes = (TextView) findViewById(R.id.tvResourceName);
        this.mtvNameRes.setTextColor(Color.WHITE);
        this.mtvNameRes.setSingleLine(true);
//        this.mtvNameRes.setHorizontallyScrolling(true);
//        this.mtvNameRes.setHorizontalFadingEdgeEnabled(true);
//        this.mtvNameRes.setEllipsize(TextUtils.TruncateAt.MARQUEE);
//        this.mtvNameRes.setFocusable(true);
//        this.mtvNameRes.requestFocus();
//        this.mtvNameRes.setMarqueeRepeatLimit(-1);
        this.mivResThumbBack = (ImageView) findViewById(R.id.ivResourceThumbnailBack);
//        this.mivResThumbFront = (ImageView) findViewById(R.id.ivResourceThumbnailFront);
        this.mtvSizeRes = (TextView) findViewById(R.id.tvResourceComment);
        this.msrlRefresh = (SwipeRefreshLayout) findViewById(R.id.srlRefresh);
        this.msrlRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!isAudio)
                    mediaController.superHide();
                mProperties.clearAnimation();
                msrlRefresh.setRefreshing(false);
                loadingVisible();
                playCurrent();
            }
        });

        if (mdMgr == null)
            mdMgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (mnotMgr == null)
            mnotMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        this.mProperties = findViewById(R.id.Properties);
        this.mProperties.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        this.mvideSpace = findViewById(R.id.flMediaSpace);
        if (this.mivDownload != null && getIntent().getScheme().equals(Utils.FILE_SCHEME))
            this.mivDownload.setVisibility(View.INVISIBLE);

        createMediaController();
        this.msurfaceView.getHolder().addCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.viewForSnack = mediaController;
        MediaPlayerService.refreshProperties();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        boolean isRunning = createAndRegisterAudioService();
        if (saveInstance != null)
            mediaController.load(isRunning ? saveInstance : null);
        else {
            mediaController.load(isRunning ? getIntent().getExtras() : null);
            saveInstance = new Bundle();
            save(saveInstance);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public static boolean isClosed() {
        return isClosed;
    }

    private boolean createAndRegisterAudioService() {
        int newId = BackMediaPlayerReceiver.setCloseAndRefreshListener(this, new Interfaces.IRefreshListener() {
            @Override
            public void refresh() {
                mediaController.showForever();
            }
        });
        if (this.id == -1)
            this.id = newId;
        return MediaPlayerService.startService(getApplicationContext(), isAudio, this);
    }

    @Override
    public boolean select(String possible) {
        return possible.length() > mprefixMimeType.length()
                && (possible.substring(0, mprefixMimeType.length()).equals(mprefixMimeType)
                || (isAudio && possible.equals(OGG_MIMETYPE)));
    }

    @Override
    public void createDialog(final int msgId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isClosed) {
                    if (toast == null)
                        toast = Toast.makeText(MediaPlayerActivity.this, msgId, Toast.LENGTH_LONG);
                    toast.show();
                } else
                    Utils.showSnackBar(msgId);
            }
        });
    }

    @Override
    public void loadingVisible() {
        loaded = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(Utils.TIME_WAIT_LOADING);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (loadingMonitor) {
                            if (!loaded)
                                msrlRefresh.setRefreshing(true);
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void loadingInvisible() {
        synchronized (loadingMonitor) {
            loaded = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    msrlRefresh.setRefreshing(false);
                }
            });
        }
    }

    @Override
    public void refreshListView() {

    }

    @Override
    public void refresh(final FileResource resource, final int position, final boolean isplaying) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setPropertiesDataLayout(resource, position, isplaying);
                mediaController.showForever();
                if (!isAudio) {
                    if (MediaPlayerService.isError())
                        msurfaceView.setVisibility(View.INVISIBLE);
                    else if (msurfaceView.getVisibility() == View.INVISIBLE)
                        msurfaceView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void loaded(final FileResource fsFile) {
        loaded = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                msrlRefresh.setRefreshing(false);
                mediaController.showForever();
                if (!isAudio) {
                    if (MediaPlayerService.isError())
                        msurfaceView.setVisibility(View.INVISIBLE);
                    else if (msurfaceView.getVisibility() == View.INVISIBLE)
                        msurfaceView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void create(FileResource fsFile) {
        MediaPlayerService.resetMediaPlayer(fsFile);
    }

    public void createMediaController() {
        Intent intent = getIntent();
        mediaController = new MediaControllerView(this);
        mediaController.setUriBundleIntentAndView(intent.getData(), intent.getExtras(), this.mvideSpace);
        MediaPlayerService.setOnCreateDialogListener(this);
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void close() {
        finish();
        onDestroy();
    }

    @Override
    protected void onDestroy() {
        isClosed = true;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        isClosed = true;
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        save(outState);
    }

    private void save(Bundle bundle) {
        bundle.putInt(Utils.ID, this.id);
        bundle.putSerializable(Utils.DIRECTORY_KEY, mediaController.getDirectory());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        DownloadImageTask.freeCache();
        Utils.freeUnusedMemory();
    }

    protected void setPropertiesDataLayout(final FileResource resource, int position, boolean isPlaying) {
        if(this.mivDownload != null) {
            this.mivDownload.dMgr = mdMgr;
            this.mivDownload.resource = resource;
        }
        this.mtvNameRes.setText(resource.getName());
        this.mtvSizeRes.setText(resource.sizeToString());
        if (isAudio && resource.isAudio()) {
            Bitmap copy = bmAlbumThumbnail;
            bmAlbumThumbnail = Utils.getArtThumbnailFromAudioFile(resource.thumbUrl(), -1, -1);
            if (bmAlbumThumbnail != null)
                mivResThumbBack.setImageBitmap(bmAlbumThumbnail);
            else
                mivResThumbBack.setImageResource(R.drawable.img_audio_background);
            if (copy != null)
                copy.recycle();
        }
    }
}
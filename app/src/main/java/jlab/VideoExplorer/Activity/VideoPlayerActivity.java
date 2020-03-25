package jlab.VideoExplorer.Activity;

import android.graphics.Matrix;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.Surface;
import jlab.VideoExplorer.R;
import android.widget.ImageView;
import android.view.MotionEvent;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.view.SurfaceHolder;
import android.util.DisplayMetrics;
import android.widget.LinearLayout;
import jlab.VideoExplorer.Utils;

import android.content.pm.ActivityInfo;
import android.view.animation.Animation;
import jlab.VideoExplorer.Interfaces;
import android.content.res.Configuration;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import jlab.VideoExplorer.Resource.FileResource;
import jlab.VideoExplorer.Service.MediaPlayerService;
import jlab.VideoExplorer.View.VideoSwipeRefreshLayout;
import jlab.VideoExplorer.Service.BackMediaPlayerReceiver;

/*
 * Created by Javier on 20/5/2017.
 */

public class VideoPlayerActivity extends MediaPlayerActivity implements SurfaceHolder.Callback {
    private SurfaceHolder holder;
    private ImageView rotateScreen;
    private TextView mtvMediaSeek;
    private LinearLayout llVolume, llBrightness;
    private ProgressBar pbVolume, pbBrightness;
    private static DisplayMetrics displaymetrics;
    private static final int MAX_VOLUME = 15;
    private static final int MAX_BRIGHTNESS = 255;
    private static Window window;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.mprefixMimeType = Utils.VIDEO;
        super.onCreate(savedInstanceState);
        window = getWindow();
        try {
            if(MediaPlayerService.currentBrightness == -1)
                MediaPlayerService.currentBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            setBrightness(MediaPlayerService.currentBrightness);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        this.mtvMediaSeek = (TextView)findViewById(R.id.tvMediaSeek);
        this.llVolume = (LinearLayout) findViewById(R.id.llVolume);
        this.llBrightness = (LinearLayout) findViewById(R.id.llBrightness);
        this.pbVolume = (ProgressBar) findViewById(R.id.pbVolume);
        this.pbBrightness = (ProgressBar) findViewById(R.id.pbBrightness);
        this.pbVolume.setMax(MAX_VOLUME);
        this.pbBrightness.setMax(MAX_BRIGHTNESS);
        this.msurfaceView.setZOrderOnTop(false);
        this.holder = this.msurfaceView.getHolder();
        this.holder.addCallback(this);
        this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.mediaController.setOnHideListener(new Interfaces.IHideListener() {
            @Override
            public void hide() {
                mediaController.superHide();
            }
        });
        setOnTouchAndOnClick();
        mProperties.startAnimation(AnimationUtils.loadAnimation(VideoPlayerActivity.this,
                R.anim.alpha_out));

        if (MediaPlayerService.running())
            setFixedSurfaceVideo(MediaPlayerService.getVideoWidth(), MediaPlayerService.getVideoHeight());

        ((VideoSwipeRefreshLayout) msrlRefresh).setMediaRefreshListener(new Interfaces.IMediaDataRefreshListener() {
            @Override
            public void refreshVolume(int volume) {
                MediaPlayerService.setAudioVolume(volume);
                pbVolume.setProgress(volume);
                mediaController.superHide();
                llVolume.startAnimation(AnimationUtils.loadAnimation(VideoPlayerActivity.this, R.anim.alpha_out));
            }

            @Override
            public void refreshBrightness(boolean isUpper) {
                int brightness = Math.max(0, Math.min(getBrightness() + (isUpper ? MAX_VOLUME : -1 * MAX_VOLUME), MAX_BRIGHTNESS));
                setBrightness(brightness);
                pbBrightness.setProgress(brightness);
                mediaController.superHide();
                llBrightness.startAnimation(AnimationUtils.loadAnimation(VideoPlayerActivity.this, R.anim.alpha_out));
            }

            @Override
            public void refreshSeek(int countSeek) {
                int updatePosition = Math.max(0, Math.min(MediaPlayerService.getCurrentPosition() + countSeek, MediaPlayerService.getDuration()));
                MediaPlayerService.seekTo(updatePosition);
                mtvMediaSeek.setText(Utils.getDurationString(updatePosition));
                mtvMediaSeek.setAnimation(AnimationUtils.loadAnimation(VideoPlayerActivity.this, R.anim.alpha_out));
            }
        });

        rotateScreen = (ImageView) findViewById(R.id.ivRotateScreen);
        rotateScreen.setVisibility(enabledScreenAutoRotation() ? View.INVISIBLE : View.VISIBLE);
        rotateScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                setRequestedOrientation(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
                        ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });
    }

    private boolean enabledScreenAutoRotation() {
        return android.provider.Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
    }

    private void setOnTouchAndOnClick() {
        mvideSpace.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    rotateScreen.setVisibility(enabledScreenAutoRotation() ? View.INVISIBLE : View.VISIBLE);
                    if (mediaController.isShow()) {
                        mProperties.clearAnimation();
                        llVolume.clearAnimation();
                        llBrightness.clearAnimation();
                        mediaController.superHide();
                    } else
                        showViews(AnimationUtils.loadAnimation(VideoPlayerActivity.this, R.anim.alpha_in_out));
                }
                return true;
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_MUTE:
                    pbVolume.setProgress(MediaPlayerService.getAudioVolume());
                    llVolume.startAnimation(AnimationUtils.loadAnimation(VideoPlayerActivity.this, R.anim.alpha_out));
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        super.surfaceCreated(surfaceHolder);
        msurfaceView.setWillNotDraw(false);
        setDisplay(this.holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        MediaPlayerService.pause();
        BackMediaPlayerReceiver.refresh();
        setDisplay(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setDisplay(SurfaceHolder holder) {
        MediaPlayerService.setDisplay(holder);
    }

    @Override
    public void loaded(FileResource fsFile) {
        super.loaded(fsFile);
        if (!isClosed())
            setFixedSurfaceVideo(MediaPlayerService.getVideoWidth(), MediaPlayerService.getVideoHeight());
    }

    private void setFixedSurfaceVideo(double width, double height) {
        displaymetrics = Utils.getDimensionScreen();
        int w = displaymetrics.widthPixels, h = displaymetrics.heightPixels;
        double divH = width / w, divW = height / h;
        if (divH != 0 && divW != 0)
            this.holder.setFixedSize((int) (width / divW), (int) (height / divH));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setFixedSurfaceVideo(MediaPlayerService.getVideoWidth(), MediaPlayerService.getVideoHeight());
    }

    private void showViews(Animation animation) {
        mediaController.showForever();
        mProperties.startAnimation(animation);
        llVolume.startAnimation(animation);
        llBrightness.startAnimation(animation);
        pbVolume.setProgress(MediaPlayerService.getAudioVolume());
        pbBrightness.setProgress(getBrightness());
    }

    @Override
    public void close() {
//        setDisplay(null);
        super.close();
    }

    public static DisplayMetrics getDisplaymetrics() {
        return displaymetrics;
    }

    public static int getBrightness() {
        return (int) (Math.abs(window.getAttributes().screenBrightness) * 255);
    }

    public static void setBrightness(int brightness) {
        try {
            MediaPlayerService.currentBrightness = brightness;
            WindowManager.LayoutParams layoutpars = window.getAttributes();
            layoutpars.screenBrightness = brightness / 255f;
            window.setAttributes(layoutpars);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }
}
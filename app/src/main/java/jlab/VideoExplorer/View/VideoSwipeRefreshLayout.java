package jlab.VideoExplorer.View;

import android.content.Context;
import android.view.MotionEvent;
import android.util.AttributeSet;
import android.view.ViewConfiguration;
import jlab.VideoExplorer.Interfaces;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import jlab.VideoExplorer.Service.MediaPlayerService;
import jlab.VideoExplorer.Activity.VideoPlayerActivity;
import jlab.VideoExplorer.Service.BackMediaPlayerReceiver;

/**
 * Created by Javier on 28/5/2018.
 */

public class VideoSwipeRefreshLayout extends SwipeRefreshLayout {
    private final int mTouchSlop;
    private float mPrevY, mDownX, mPrevX;

    private Interfaces.IMediaDataRefreshListener mediaRefreshListener = new Interfaces.IMediaDataRefreshListener() {
        @Override
        public void refreshVolume(int volume) {

        }

        @Override
        public void refreshBrightness(boolean isUpper) {

        }

        @Override
        public void refreshSeek(int countSeek) {

        }
    };

    public VideoSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setMediaRefreshListener(Interfaces.IMediaDataRefreshListener mediaRefreshListener) {
        this.mediaRefreshListener = mediaRefreshListener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPrevY = MotionEvent.obtain(event).getY();
                mPrevX = mDownX = MotionEvent.obtain(event).getX();
                break;

            case MotionEvent.ACTION_MOVE:
                final float eventY = event.getY(), eventX = event.getX();
                boolean isUpper = eventY < mPrevY, isVolOrBright = mPrevY != -1 && Math.abs(eventY - mPrevY) >= mTouchSlop;
                if (isVolOrBright) {
                    mPrevY = eventY;
                    if (mDownX > VideoPlayerActivity.getDisplaymetrics().widthPixels / 2) {
                        int volume = MediaPlayerService.getAudioVolume() + (isUpper ? 1 : -1);
                        if ((!isUpper || volume <= 15) && (isUpper || volume >= 0))
                            mediaRefreshListener.refreshVolume(volume);
                    } else
                        mediaRefreshListener.refreshBrightness(isUpper);
                    mPrevX = -1;
                } else if (mPrevX != -1 && Math.abs(eventX - mPrevX) > mTouchSlop) {
                    mediaRefreshListener.refreshSeek(BackMediaPlayerReceiver.SEEK_MIN_COUNT * (mPrevX < eventX ? 1 : -1));
                    mPrevX = eventX;
                    mPrevY = -1;
                }
        }
        return false;
    }
}
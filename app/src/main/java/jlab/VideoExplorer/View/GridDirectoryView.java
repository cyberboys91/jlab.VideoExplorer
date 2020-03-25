package jlab.VideoExplorer.View;

import android.view.View;
import android.os.Handler;
import android.content.Context;
import android.widget.GridView;
import android.widget.ImageView;
import android.util.AttributeSet;
import android.widget.AdapterView;
import android.widget.AbsListView;
import jlab.VideoExplorer.Activity.DirectoryActivity;
import jlab.VideoExplorer.Interfaces;
import jlab.VideoExplorer.R;
import jlab.VideoExplorer.Resource.AlbumDirectory;
import jlab.VideoExplorer.Resource.Directory;
import jlab.VideoExplorer.Resource.Resource;
import jlab.VideoExplorer.Resource.FileResource;
import jlab.VideoExplorer.Utils;

/*
 * Created by Javier on 26/9/2016.
 */

public class GridDirectoryView extends GridView implements AbsListView.OnScrollListener,
        Interfaces.IListContent {

    private int last;
    private int first;
    private int antFirst;
    public boolean scrolling = false;
    private Handler handler = new Handler();
    private Directory mdirectory;
    private String relUrlDirectoryRoot;
    private String nameDirectoryRoot;
    private ResourceDetailsAdapter mAdapter;
    private DirectoryActivity mListener;

    public GridDirectoryView(Context context) {
        super(context);
        mAdapter = new ResourceDetailsAdapter();
        setAdapter(mAdapter);
    }

    public GridDirectoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAdapter = new ResourceDetailsAdapter();
        setAdapter(mAdapter);
    }

    public GridDirectoryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mAdapter = new ResourceDetailsAdapter();
        setAdapter(mAdapter);
    }

    public void loadItemClickListener() {
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               openResource(mdirectory.getResource(position), position);
            }
        });
        setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long id) {
                return mListener.onResourceLongClick(mdirectory.getResource(position), position);
            }
        });
        setOnScrollListener(this);
    }

    @Override
    public void openResource(Resource res, int position)
    {
        scrolling = false;
        try {
            Utils.Variables var = Utils.stackVars.get(Utils.stackVars.size() - 1);
            var.BeginPosition = getFirstVisiblePosition();
            if (res.isDir()) {
                Utils.stackVars.add(new Utils.Variables(res.getRelUrl(), res.getName(), 0));
                loadDirectory();
                mListener.onDirectoryClick(res.getName(), res.getRelUrl());
            } else
                mListener.onFileClick((FileResource) res, position);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE && mdirectory != null && mdirectory.loaded()) {
            scrollingStop(absListView);
            scrolling = false;
        }
        scrolling = scrollState == SCROLL_STATE_FLING;
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        antFirst = first != firstVisibleItem ? first : antFirst;
        first = firstVisibleItem;
        last = firstVisibleItem + visibleItemCount - 1;
    }

    private void scrollingStop(AbsListView view) {
        try {
            boolean up = antFirst < first;
            int length = getChildCount();
            int index = up ? first : last;
            for (int i = up ? 0 : length - 1; (up ? i < length : i >= 0)
                    && (up ? index <= last : index >= 0); i += up ? 1 : -1) {
                Resource elem = mdirectory.getResource(index);
                index += up ? 1 : -1;
                if (!elem.isDir() && ((FileResource) elem).isThumbnailer())
                    mListener.loadThumbnailForFile((FileResource) elem,
                            (ImageView) view.getChildAt(i).findViewById(R.id.ivResourceIcon),
                            (ImageView) view.getChildAt(i).findViewById(R.id.ivFavorite), true, false);
                else if (elem instanceof AlbumDirectory && ((AlbumDirectory) elem).getCountElements() > 0)
                    mListener.loadThumbnailForFile((FileResource) ((AlbumDirectory) elem).getResource(0),
                            (ImageView) view.getChildAt(i).findViewById(R.id.ivResourceIcon),
                            (ImageView) view.getChildAt(i).findViewById(R.id.ivFavorite), true, true);
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    @Override
    public void onViewRemoved(View child) {
        if(child != null) {
            ImageView ivIcon = (ImageView) child.findViewById(R.id.ivResourceIcon);
            ivIcon.setImageDrawable(null);
        }
        super.onViewRemoved(child);
    }

    public void setHandler(Handler handler) {
         this.handler = handler;
    }

    public void setRelUrlDirectoryRoot(String nameRoot, String relUrlRoot)
    {
        this.relUrlDirectoryRoot = relUrlRoot;
        this.nameDirectoryRoot = nameRoot;
    }

    public void loadParentDirectory() {
        if (!Utils.stackVars.isEmpty()) {
            Utils.stackVars.remove(Utils.stackVars.size() - 1);
            loadDirectory();
        }
    }

    @Override
    public int getFirstVisiblePosition() {
        return first;
    }

    @Override
    public boolean scrolling() {
        return scrolling;
    }

    @Override
    public ResourceDetailsAdapter getResourceDetailsAdapter() {
        return this.mAdapter;
    }

    public void loadDirectory() {
        if (Utils.stackVars.isEmpty()) {
            Utils.stackVars.add(new Utils.Variables(getContext().getString(R.string.all_video), Utils.RELURL_SPECIAL_DIR, 0));
            mListener.onDirectoryClick(getContext().getString(R.string.all_video), Utils.RELURL_SPECIAL_DIR);
        }

        Utils.Variables vars = Utils.stackVars.get(Utils.stackVars.size() - 1);
        mdirectory = mListener.getDirectory(vars.NameDirectory, vars.RelUrlDirectory);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(Utils.TIME_WAIT_LOADING);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (Directory.monitor) {
                    if (!mdirectory.loaded())
                        handler.sendEmptyMessage(Utils.LOADING_VISIBLE);
                }
            }
        }).start();
        mAdapter.clear();
        handler.sendEmptyMessage(Utils.REFRESH_LISTVIEW);
        mdirectory.openAsynchronic(handler);
    }

    @Override
    public void loadContent() {
        mAdapter.clear();
        mAdapter.addAll(mdirectory.getContent());
    }

    public final boolean isEmpty() {
        return mdirectory.getContent().isEmpty();
    }

    public Directory getDirectory()
    {
        return mdirectory;
    }

    @Override
    public void setDirectory(Directory directory) {
        this.mdirectory = directory;
    }

    public void setListeners(DirectoryActivity activityDirectory)
    {
        this.mAdapter.setonGetSetViewListener(activityDirectory);
        this.mListener = activityDirectory;
    }
}
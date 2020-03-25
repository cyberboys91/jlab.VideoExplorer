package jlab.VideoExplorer.Resource;

import android.net.Uri;
import android.os.Handler;
import jlab.VideoExplorer.R;
import jlab.VideoExplorer.Utils;
import jlab.VideoExplorer.Interfaces;

/*
 * Created by Javier on 25/10/2017.
 */
public class SearchDirectory extends Directory {
    private Directory mDir;
    private String mpattern;
    private boolean reload = false;
    private Interfaces.IAddResourceListener addListener = new Interfaces.IAddResourceListener() {

        @Override
        public void add(String name, String path, String comment, String album, long size, long modification) {

        }

        @Override
        public void add(Resource resource) {

        }

        @Override
        public void clear() {

        }
    };

    public SearchDirectory(String relUrl, boolean isRemote) {
        super(Utils.NAME_SEARCH, Utils.RELURL_SEARCH, null, 0, false);
        loadParamsForSearch(relUrl, isRemote);
        this.isRemote = isRemote;
    }

    private void loadParamsForSearch(String relUrl, boolean isRemote) {
        Uri uri = Uri.parse(relUrl);
        String name = uri.getQueryParameter(Utils.NAME_KEY),
                dirRelUrl = Resource.strDecode(uri.getQueryParameter(Utils.PATH_KEY));
        mpattern = Resource.strDecode(uri.getQueryParameter(Utils.PATTERN_KEY));
        mDir = isRemote
                ? new RemoteDirectory(name, dirRelUrl, null)
                : (dirRelUrl.equals(Utils.RELURL_SPECIAL_DIR)
                    ? getSpecialDirectory(name)
                    : new AlbumDirectory(name, dirRelUrl, null, 0, false));
    }

    private Directory getSpecialDirectory(String name) {
        Directory result = null;
        if (name.equals(Utils.getString(R.string.all_images)))
            result = new ImagesLocalDirectory(name);
        else if (name.equals(Utils.getString(R.string.all_music)))
            result = new MusicLocalDirectory(name);
        else if (name.equals(Utils.getString(R.string.all_video)))
            result = new VideosLocalDirectory(name);
        else if (name.equals(Utils.getString(R.string.all_documents)))
            result = new DocumentsDirectory(name);
        else if (name.equals(Utils.getString(R.string.downloads_folder)))
            result = new DownloadLocalDirectory(name);
        else if (name.equals(Utils.getString(R.string.camera_folder)))
            result = new CameraVideosDirectory(name);
        else if(name.equals(Utils.getString(R.string.favorite_folder)))
            result = new FavoritesDirectory(name);
        else if(name.equals(Utils.getString(R.string.albums_folder)))
            result = new AlbumsDirectory(name);
        return result;
    }

    public String getPattern() {
        return this.mpattern;
    }

    @Override
    public void openSynchronic(Handler handler) {
        synchronized (monitor) {
            try {
                if (!loaded) {
                    reload = false;
                    if (getPattern().equals("")) {
                        mDir.openSynchronic(null);
                        setContent(mDir.getContent());
                    } else {
                        clear();
                        search(mDir, mpattern);
                    }
                    if (reload) {
                        loaded = true;
                        return;
                    }
                }
                Utils.lostConnection = false;
            } catch (Exception ignored) {
                Utils.lostConnection = true;
                ignored.printStackTrace();
            } finally {
                loaded = true;
                if (handler != null)
                    handler.sendEmptyMessage(Utils.LOADING_INVISIBLE);
            }
        }
    }

    private void search(Directory dir, String pattern) {
        if (reload)
            return;
        dir.openSynchronic(null);
        int size = dir.getCountElements();
        for (int i = 0; i < size; i++) {
            if (reload)
                return;
            Resource current = dir.getResource(i);
            int index = current.getName().toLowerCase().indexOf(pattern);
            if (index >= 0) {
                current.setIndexPattern(index);
                addResource(current);
                addListener.add(current);
            }
            if (current.isDir())
                search((Directory) current, pattern);
        }
    }

    @Override
    public boolean isMultiColumn() {
        return mDir.isMultiColumn();
    }

    public void setAddListener(Interfaces.IAddResourceListener newListener) {
        this.addListener = newListener;
    }

    public void resetPattern(final String newPattern, final Handler handler) {
        if (getPattern().equals(newPattern) && loaded) {
            Utils.lostConnection = false;
            if (handler != null)
                handler.sendEmptyMessage(Utils.LOADING_INVISIBLE);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    reload = true;
                    while (!loaded) ;
                    loaded = false;
                    mpattern = newPattern;
                    openAsynchronic(handler);

                }
            }).start();
        }
    }

    @Override
    protected void clear() {
        super.clear();
        addListener.clear();
    }
}
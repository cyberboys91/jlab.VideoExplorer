package jlab.VideoExplorer.Resource;

import android.os.Handler;

import jlab.VideoExplorer.R;
import jlab.VideoExplorer.Utils;
import jlab.VideoExplorer.Interfaces;

import static jlab.VideoExplorer.Utils.getString;

/*
 * Created by Javier on 22/07/2017.
 */
public class LocalStorageDirectories extends LocalDirectory {

    private long totalStorageSpace;
    public LocalStorageDirectories() {
        super("", "", null, false, 0, 0, 0);
        this.totalStorageSpace = 0;
        this.ocupedSpace = 0;
    }

    public long getTotalStorageSpace() {
        return this.totalStorageSpace;
    }

    public long getTotalAvailableSpace() {
        return this.totalStorageSpace - this.ocupedSpace;
    }

    public Interfaces.IDetailsSpecialDirectory getVideosDirDetails() {
        return getDetailsSpecialDir(0);
    }

    public Interfaces.IDetailsSpecialDirectory getAlbumsDirDetails() {
        return getDetailsSpecialDir(1);
    }

    public Interfaces.IDetailsSpecialDirectory getFavoritesDirDetails() {
        return getDetailsSpecialDir(2);
    }

    public Interfaces.IDetailsSpecialDirectory getCameraDirDetails() {
        return getDetailsSpecialDir(3);
    }

    public Interfaces.IDetailsSpecialDirectory getDownloadDirDetails() {
        return getDetailsSpecialDir(4);
    }

    public VideosLocalDirectory getVideosDirectory() {
        return (VideosLocalDirectory) getResource(0);
    }

    public AlbumsDirectory getAlbumsDirectory() {
        return (AlbumsDirectory) getResource(1);
    }

    public FavoritesDirectory getFavoritesDirectory() {
        return (FavoritesDirectory) getResource(2);
    }

    public CameraVideosDirectory getCameraDirectory() {
        return (CameraVideosDirectory) getResource(3);
    }

    public DownloadLocalDirectory getDownloadDirectory() {
        return (DownloadLocalDirectory) getResource(4);
    }

    private Interfaces.IDetailsSpecialDirectory getDetailsSpecialDir(int index) {
        final LocalDirectory dir = (LocalDirectory) getResource(index);
        return new Interfaces.IDetailsSpecialDirectory() {
            @Override
            public int getCountElements() {
                return dir != null ? dir.getCountElements() : 0;
            }

            @Override
            public long getOcupedSpace() {
                return dir != null ? dir.getOcupedSpace() : 0;
            }
        };
    }

    @Override
    public void openAsynchronic(final Handler handler) {
        if (!loaded) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    openSynchronic(handler);
                }
            }).start();
        } else handler.sendEmptyMessage(Utils.LOADING_INVISIBLE);
    }

    @Override
    public void openSynchronic(Handler handler) {
        synchronized (monitor) {
            try {
                if (!loaded) {
                    clear();
                    //Special Directories
                    addResource(new VideosLocalDirectory(getString(R.string.all_video)));
                    addResource(new AlbumsDirectory(getString(R.string.albums_folder)));
                    addResource(new FavoritesDirectory(getString(R.string.favorite_folder)));
                    addResource(new CameraVideosDirectory(getString(R.string.camera_folder)));
                    addResource(new DownloadLocalDirectory(getString(R.string.downloads_folder)));
                    //.
                }
                Utils.lostConnection = false;
            } catch (Exception ignored) {
                Utils.lostConnection = true;
                ignored.printStackTrace();
            }
            synchronized (monitor) {
                loaded = true;
                if (handler != null)
                    handler.sendEmptyMessage(Utils.LOADING_INVISIBLE);
            }
        }
    }

    @Override
    protected void clear() {
        super.clear();
        ocupedSpace = 0;
        totalStorageSpace = 0;
        totalSpace = 0;
        loaded = false;
    }

    public void loadData() {
        synchronized (monitor) {
            for (int i = 0; i < getCountElements(); i++) {
                Resource resource = getResource(i);
                if (resource instanceof FilesLocalDirectory)
                    ((FilesLocalDirectory) resource).loadData();
            }
        }
    }
}
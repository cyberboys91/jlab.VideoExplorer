package jlab.VideoExplorer;

import android.net.Uri;
import android.os.Handler;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.content.ContentResolver;

import jlab.VideoExplorer.Activity.*;
import jlab.VideoExplorer.Resource.*;
import jlab.VideoExplorer.Service.ResourceInDownload;
import jlab.VideoExplorer.View.ResourceDetailsAdapter;

/*
 * Created by Javier on 30/07/2017.
 */
public class Interfaces {

    public interface IStatusChangeListener {

        void loadingVisible();

        void loadingInvisible();

        void refreshListView();
    }

    public interface IAudioPlayerCreateListener {
        void create(FileResource fsFile);
    }

    public interface INotifyControllersListener {

        void notifyPlay(FileResource file);

        void notifyPause(FileResource file);

        void notifySeek(int current, int max);

        void notify(FileResource file, boolean isplaying);
    }

    public interface ICloseListener {

        int getId();

        void close();

        void loaded(FileResource fsFile);
    }

    public interface IElementSelectorListener {
        boolean select(String possible);
    }

    public interface IElementRefreshListener {
        void refresh(FileResource resource, int position, boolean isplaying);
    }

    public interface ICreateDialogListener {
        void createDialog(int msgId);
    }

    public interface IHideListener {
        void hide();
    }

    public interface ILoadThumbnailForFile {
        void loadThumbnailForFile(FileResource file, ImageView ivIcon, ImageView ivFavorite, boolean setBackground, boolean isAlbum);
    }

    public interface IDetailsThumbnailerResource {
        Uri getUriThumbnails();

        int getColumnMicroKind();

        Bitmap getThumbnail(ContentResolver resolver, int id);
    }

    public interface IDetailsSpecialDirectory {

        int getCountElements();

        long getOcupedSpace();
    }

    public interface IGetDirectoryListener {
        Directory getDirectory(String name, String relUrl);
    }

    public interface IRemoteResourceClickListener {
        void onFileClick(FileResource res, int position);

        void onDirectoryClick(String name, String relurlDir);

        boolean onResourceLongClick(Resource resource, int position);
    }

    public interface IListContent {

        void loadContent();

        boolean isEmpty();

        void setListeners(DirectoryActivity activityDirectory);

        void setRelUrlDirectoryRoot(String s, String string);

        void setHandler(Handler handler);

        int getFirstVisiblePosition();

        void loadDirectory();

        int getNumColumns();

        void loadItemClickListener();

        void setNumColumns(int i);

        void setSelection(int pos);

        void loadParentDirectory();

        boolean scrolling();

        ResourceDetailsAdapter getResourceDetailsAdapter();

        Directory getDirectory();

        void setDirectory(Directory directory);

        void openResource(Resource res, int position);

    }

    public interface IRefreshListener {
        void refresh();
    }

    public interface IFinishListener {
        void refresh(boolean successfully);
    }

    public interface ICopyRefresh {
        void refresh(Runnable run);
    }

    public interface IAddResourceListener {
        void add(String name, String path, String comment, String album, long size, long modification);

        void add(Resource resource);

        void clear();
    }

    public interface ISelectListener {
        boolean select(String mimetype, String name, String path);
    }

    public interface IDownloadRefreshListener {
        void complete(ResourceInDownload resource, ResourceInDownload.DownloadStatus status);
    }

    public interface IMediaDataRefreshListener
    {
        void refreshVolume(int volume);
        void refreshBrightness(boolean isUpper);
        void refreshSeek(int countSeek);
    }
}

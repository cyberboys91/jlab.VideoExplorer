package jlab.VideoExplorer.Resource;

import android.os.Handler;
import java.util.Comparator;
import jlab.VideoExplorer.Utils;
import static java.util.Collections.sort;
import jlab.VideoExplorer.Service.DeleteResourceService;

/*
 * Created by Javier on 08/08/2018.
 */

public class AlbumDirectory extends Directory {
    public AlbumDirectory(String name, String relUrl, String comment, long modification, boolean isHidden) {
        super(name, relUrl, comment, modification, isHidden);
    }

    @Override
    public boolean isMultiColumn() {
        return true;
    }

    @Override
    public boolean delete(DeleteResourceService.OnDeleteListener deleteListener) {
        boolean result = true;
        for (int i = 0; i < getCountElements(); i++)
            result &= getResource(i).delete(deleteListener);
        return result;
    }

    @Override
    public void openSynchronic(Handler handler) {
        synchronized (monitor) {
            try {
                if (!loaded) {
                    clear();
                    VideosLocalDirectory allVideos = Utils.specialDirectories.getVideosDirectory();
                    allVideos.openSynchronic(null);
                    loadContentForDir(allVideos, getName());
                    sort(getContent(), new Comparator<Resource>() {
                        @Override
                        public int compare(Resource resource, Resource t1) {
                            return resource.getName().toLowerCase().compareTo(t1.getName().toLowerCase());
                        }
                    });
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

    private void loadContentForDir(Directory directory, String name) {
        for (int i = 0; i < directory.getCountElements(); i++) {
            Resource current = directory.getResource(i);
            if ((Utils.showHiddenFiles || !current.isHidden())
                    && !current.isDir() && ((FileResource) current).isVideo()
                    && name.equals(((FileResource) current).getParentName())
                    && (getRelUrl() + "/" + current.getName()).equals(current.getRelUrl())) {
                LocalFile localFile = (LocalFile) directory.getResource(i);
                addResource(localFile);
            }
        }
    }
}
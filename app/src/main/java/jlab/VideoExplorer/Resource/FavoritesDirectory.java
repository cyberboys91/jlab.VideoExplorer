package jlab.VideoExplorer.Resource;

import jlab.VideoExplorer.R;
import android.os.Handler;

import java.io.File;
import java.util.ArrayList;
import jlab.VideoExplorer.Utils;
import jlab.VideoExplorer.db.FavoriteDetails;

/*
 * Created by Javier on 07/08/2018.
 */

public class FavoritesDirectory extends FilesLocalDirectory {

    public FavoritesDirectory(String name) {
        super(name, R.color.orange);
    }

    @Override
    public void openSynchronic(Handler handler) {
        synchronized (monitor) {
            try {
                if (!loaded) {
                    clear();
                    ocupedSpace = 0;
                    count = 0;
                    ArrayList<FavoriteDetails> favoriteVideos = Utils.getFavorites();
                    for (int i = 0; i < favoriteVideos.size(); i++) {
                        FavoriteDetails current = favoriteVideos.get(i);
                        String nameRes = FileResource.getNameFromUrl(current.getPath());
                        LocalFile newRes = new LocalFile(nameRes
                                , current.getPath()
                                , current.getComment()
                                , current.getParentName()
                                , current.getSize()
                                , current.getModification(), nameRes.length() > 0 && nameRes.charAt(0) == '.');
                        if (new File(current.getPath()).exists() && (Utils.showHiddenFiles || !newRes.isHidden())) {
                            addResource(newRes);
                            ocupedSpace += current.getSize();
                        }
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

    @Override
    public boolean isMultiColumn() {
        return true;
    }

    @Override
    public void loadData() {
        openSynchronic(null);
    }
}

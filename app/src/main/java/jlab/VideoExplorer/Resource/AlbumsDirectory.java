package jlab.VideoExplorer.Resource;

import java.io.File;
import java.util.HashMap;
import android.os.Handler;
import java.util.Comparator;
import jlab.VideoExplorer.Utils;
import static java.util.Collections.sort;

/*
 * Created by Javier on 08/08/2018.
 */

public class AlbumsDirectory extends VideosLocalDirectory {

    private HashMap<String, AlbumDirectory> dict = new HashMap<>();

    public AlbumsDirectory(String name) {
        super(name);
    }

    @Override
    public void openSynchronic(Handler handler) {
        synchronized (monitor) {
            try {
                if (!loaded) {
                    clear();
                    dict = new HashMap<>();
                    super.openSynchronic(null);
                    for (String parent : dict.keySet()) {
                        addResource(dict.get(parent));
                        count++;
                    }
                    sort(getContent(), new Comparator<Resource>() {
                        @Override
                        public int compare(Resource resource, Resource t1) {
                            return resource.getName().toLowerCase().compareTo(t1.getName().toLowerCase());
                        }
                    });
                }
                Utils.lostConnection = false;
            } catch (Exception ignored) {
                ignored.printStackTrace();
                Utils.lostConnection = true;
            } finally {
                loaded = true;
                if (handler != null)
                    handler.sendEmptyMessage(Utils.LOADING_INVISIBLE);
            }
        }
    }

    @Override
    public void add(String name, String path, String comment, String parent, long size, long modification) {
        if (parent == null || parent.length() == 0 || (!Utils.showHiddenFiles && name.charAt(0) == '.'))
            return;
        String key = path.substring(0, path.length() - name.length() - 1);
        if (!dict.containsKey(key)) {
            AlbumDirectory albumDirectory = new AlbumDirectory(parent, key, "1", new File(key).lastModified(), false);
            albumDirectory.addResource(new LocalFile(name, path, comment, parent
                    , size, modification, name.length() > 0 && name.charAt(0) == '.'));
            albumDirectory.loaded = true;
            dict.put(key, albumDirectory);
        } else {
            AlbumDirectory albumDirectory = dict.get(key);
            albumDirectory.setComment(String.format("%s", albumDirectory.getCountElements() + 1));
            albumDirectory.addResource(new LocalFile(name, path, comment, parent
                    , size, modification, name.length() > 0 && name.charAt(0) == '.'));
        }
    }

    @Override
    public void loadData() {
        openSynchronic(null);
    }
}
package jlab.VideoExplorer.Resource;

import android.net.Uri;
import android.os.Handler;

import java.io.File;

import jlab.VideoExplorer.Utils;
import jlab.VideoExplorer.Interfaces;

/*
 * Created by Javier on 24/07/2017.
 */

public abstract class FilesLocalDirectory extends LocalDirectory implements Interfaces.IAddResourceListener {

    protected Uri uri;
    protected int count;
    private int color;
    protected Interfaces.ISelectListener onSelectListener = new Interfaces.ISelectListener() {
        @Override
        public boolean select(String mimetype, String name, String path) {
            return name != null && name.length() > 0
                    && (name.charAt(0) != '.' || Utils.showHiddenFiles)
                    && existAndIsFile(path);
        }
    };

    protected static boolean existAndIsFile(String path) {
        if (path == null)
            return false;
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    public FilesLocalDirectory(String name, int color) {
        super(name, Utils.RELURL_SPECIAL_DIR, null, false, 0, 0, 0);
        this.name = name;
        this.color = color;
    }

    public FilesLocalDirectory(String name, Uri uri, int color) {
        super(name, Utils.RELURL_SPECIAL_DIR, null, false, 0, 0, 0);
        this.uri = uri;
        this.color = color;
    }

    public FilesLocalDirectory(String name, Uri uri, int color, Interfaces.ISelectListener selectListener) {
        super(name, Utils.RELURL_SPECIAL_DIR, null, false, 0, 0, 0);
        this.uri = uri;
        this.color = color;
        onSelectListener = selectListener;
    }

    public void loadData() {
        Interfaces.IDetailsSpecialDirectory details = Utils.getCountAllLocalFiles(uri, onSelectListener, loadIDs());
        this.count = details.getCountElements();
        this.ocupedSpace = details.getOcupedSpace();
    }

    public int getColor() {
        return color;
    }

    @Override
    public void openSynchronic(Handler handler) {
        synchronized (monitor) {
            try {
                if (!loaded) {
                    count = 0;
                    ocupedSpace = 0;
                    clear();
                    getElems();
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

    protected boolean loadIDs() {
        return false;
    }

    protected void getElems() {
        Utils.getAllAudioOrVideoLocalFiles(uri, this, onSelectListener);
    }

    @Override
    public void add(String name, String path, String comment, String parent, long size, long modification) {
        LocalFile newFile = new LocalFile(name, path, comment, parent
                , size, modification, name.length() > 0 && name.charAt(0) == '.');
        addResource(newFile);
    }

    @Override
    public void add(Resource resource) {

    }

    @Override
    public void clear() {

    }

    public int getCountElements() {
        return count;
    }

    protected void addResource(LocalFile res) {
        super.addResource(res);
        count++;
    }

    @Override
    protected Resource removeResource(int index) {
        Resource resource = super.removeResource(index);
        count -= (resource != null) ? 1 : 0;
        return resource;
    }

    public long getOcupedSpace() {
        return ocupedSpace;
    }

    public long getTotalSpace()
    {
        return 0;
    }
}

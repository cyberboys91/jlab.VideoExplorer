package jlab.VideoExplorer.Resource;

import java.io.File;
import android.net.Uri;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Comparator;

import jlab.VideoExplorer.R;
import jlab.VideoExplorer.Utils;
import static java.util.Collections.sort;
import jlab.VideoExplorer.Service.DeleteResourceService;

/*
 * Created by Javier on 20/06/2017.
 */

public class LocalDirectory extends Directory {
    protected long ocupedSpace;
    protected long totalSpace;

    public LocalDirectory(String name, String relUrl, String comment, boolean isHidden, long ocupedSpace, long totalSpace, long creation) {
        super(name, relUrl, comment, creation, isHidden);
        this.isRemote = false;
        this.ocupedSpace = ocupedSpace;
        this.totalSpace = totalSpace;
    }

    public LocalDirectory(String name, String relUrl, String comment, boolean isHidden, long modification) {
        super(name, relUrl, comment, modification, isHidden);
        this.isRemote = false;
    }

    public void openSynchronic(Handler handler) {
        synchronized (monitor) {
            if (!loaded) {
                try {
                    clear();
                    ArrayList<Resource> dirsAux = new ArrayList<>();
                    ArrayList<Resource> filesAux = new ArrayList<>();
                    File[] childs = new File(getRelUrl()).listFiles();
                    if (childs == null) {
                        Utils.lostConnection = true;
                        synchronized (monitor) {
                            loaded = true;
                            if (handler != null)
                                handler.sendEmptyMessage(Utils.LOADING_INVISIBLE);
                        }
                        return;
                    }
                    for (File child : childs) {
                        if (!Utils.showHiddenFiles && child.isHidden())
                            continue;
                        if (child.isDirectory())
                            dirsAux.add(new LocalDirectory(child.getName(), child.getPath(),
                                    String.format("%s %s", child.list().length, Utils.getString(R.string.elements)),
                                    child.isHidden(), 0, 0, child.lastModified()));
                        else {
                            LocalFile newFile = new LocalFile(child.getName(), child.getAbsolutePath(), null, null
                                    , child.length(), child.lastModified(), child.isHidden());
                            newFile.setComment(newFile.getModificationDateShort());
                            filesAux.add(newFile);
                        }
                    }
                    sort(dirsAux, new Comparator<Resource>() {
                        @Override
                        public int compare(Resource resource, Resource t1) {
                            return resource.getName().toLowerCase().compareTo(t1.getName().toLowerCase());
                        }
                    });
                    sort(filesAux, new Comparator<Resource>() {
                        @Override
                        public int compare(Resource resource, Resource t1) {
                            return resource.getName().toLowerCase().compareTo(t1.getName().toLowerCase());
                        }
                    });
                    addResource(dirsAux);
                    addResource(filesAux);
                    Utils.lostConnection = false;
                } catch (Exception e) {
                    Utils.lostConnection = true;
                    e.printStackTrace();
                }
                synchronized (monitor) {
                    loaded = true;
                    if (handler != null)
                        handler.sendEmptyMessage(Utils.LOADING_INVISIBLE);
                }
            }
        }
    }

    @Override
    protected void addResource(Resource res) {
        super.addResource(res);
        this.ocupedSpace += res.isDir() ? 0 : ((FileResource) res).mSize;
    }

    @Override
    protected Resource removeResource(int index) {
        Resource res = super.removeResource(index);
        if (res != null)
            this.ocupedSpace -= res.isDir() ? 0 : ((FileResource) res).mSize;
        return res;
    }

    @Override
    public long getOcupedSpace() {
        return ocupedSpace;
    }

    @Override
    public long getTotalSpace() {
        return totalSpace;
    }

    @Override
    public boolean isMultiColumn() {
        return true;
    }

    @Override
    public boolean delete(DeleteResourceService.OnDeleteListener deleteListener) {
        openSynchronic(null);
        for (Resource child : getContent())
            child.delete(deleteListener);
        return super.delete(deleteListener);
    }

    public boolean newFolder(String name) {
        name = name.trim();
        File file = new File(this.getRelUrl(), name);
        return file.exists() || file.mkdir();
    }

    public boolean newFile(String name, boolean addToContent) {
        try {
            File newFile = new File(getRelUrl(), name);
            boolean result = newFile.createNewFile();
            if (result && addToContent) {
                Uri uri = Utils.getUriForFile(new LocalFile(newFile.getName(), newFile.getAbsolutePath(),
                        null, null, newFile.length(), newFile.lastModified(), newFile.isHidden()));
                if (uri != null)
                    Utils.addFileToContentThread(newFile.getAbsolutePath());
            }
            return result;
        } catch (Exception exp) {
//            Utils.requestWritePermission();
            return true;
        }
    }

    private boolean createFileFromUriTree()
    {
        return false;
    }
}
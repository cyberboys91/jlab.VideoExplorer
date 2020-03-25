package jlab.VideoExplorer.Resource;

import android.os.Handler;
import java.util.ArrayList;

import jlab.VideoExplorer.Utils;

/*
 * Created by Javier on 25/10/2017.
 */
public class Directory extends Resource {
    private ArrayList<Resource> Content;
    protected boolean loaded;
    public static final String monitor = "";

    public Directory(String name, String relUrl, String comment, long modification, boolean isHidden) {
        super(name, relUrl, comment, true, modification);
        this.Content = new ArrayList<>();
        this.isHidden = isHidden;
    }

    public ArrayList<Resource> getContent() {
        return this.Content;
    }

    public boolean isMultiColumn() {
        return false;
    }

    public boolean loaded() {
        return this.loaded;
    }

    public void openAsynchronic(final Handler handler) {
        if (!loaded) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    openSynchronic(handler);
                }
            }).start();
        }
        else handler.sendEmptyMessage(Utils.LOADING_INVISIBLE);
    }

    protected void addResource(Resource res) {
        this.Content.add(res);
    }

    protected Resource removeResource(int index) {
        return this.Content.remove(index);
    }

    protected void addResource(ArrayList<Resource> resources) {
        this.Content.addAll(resources);
    }

    public Resource getResource(int i) {
        if (i >= getCountElements())
            return null;
        return getContent().get(i);
    }

    public long getOcupedSpace() {
        return 0;
    }

    public long getTotalSpace() {
        return 0;
    }

    public int getCountElements() {
        return getContent().size();
    }

    public boolean newFile(String name, boolean addToContent) {
        return false;
    }

    public boolean newFolder(String name) {
        return false;
    }

    public void openSynchronic(Handler handler) {

    }

    protected void setContent(ArrayList<Resource> newContent) {
        clear();
        for (Resource resource : newContent)
            this.Content.add(resource);
    }

    public void setLoadedFalse() {
        loaded = false;
    }

    protected void clear() {
        this.Content.clear();
    }
}
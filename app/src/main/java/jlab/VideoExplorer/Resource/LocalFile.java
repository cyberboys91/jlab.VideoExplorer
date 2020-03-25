package jlab.VideoExplorer.Resource;

/*
 * Created by Javier on 23/12/2017.
 */

public class LocalFile extends FileResource {
    public LocalFile(String name, String relUrl, String comment, String parentName, long size, long modification, boolean isHidden) {
        super(name, relUrl, comment, parentName, size, modification, isHidden);
        this.isRemote = false;
    }
}

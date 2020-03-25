package jlab.VideoExplorer.Resource;

import jlab.VideoExplorer.R;
import jlab.VideoExplorer.Utils;
import android.provider.MediaStore;
import jlab.VideoExplorer.Interfaces;

/*
 * Created by Javier on 19/11/2017.
 */
public class DocumentsDirectory extends FilesLocalDirectory {

    public DocumentsDirectory(String name) {
        super(name, MediaStore.Files.getContentUri(Utils.EXTERNAL_VOLUMEN_NAME), R.color.black, new Interfaces.ISelectListener() {
            @Override
            public boolean select(String mimetype, String name, String path) {
                return mimetype != null && FileResource.isDocFromMimeType(mimetype) && existAndIsFile(path);
            }
        });
    }

    @Override
    public void add(String name, String path, String comment, String album, long size, long modification) {
        boolean isHidden = name.length() > 0 && name.charAt(0) == '.';
        if (isHidden && !Utils.showHiddenFiles)
            return;
        LocalFile newFile = new LocalFile(name, path, comment, "", size, modification, isHidden);
        newFile.setComment(newFile.getModificationDateShort());
        addResource(newFile);
    }
}
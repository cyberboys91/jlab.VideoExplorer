package jlab.VideoExplorer.Resource;

import jlab.VideoExplorer.R;
import android.provider.MediaStore;

/*
 * Created by Javier on 24/07/2017.
 */

public class MusicLocalDirectory extends FilesLocalDirectory {
    public MusicLocalDirectory(String name) {
        super(name, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, R.color.green);
    }
}

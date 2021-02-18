package jlab.VideoExplorer.Resource;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.core.content.FileProvider;

import java.io.FileNotFoundException;

/*
 * Created by Javier on 07/04/2018.
 */

public class ResourceProvider extends FileProvider {

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return super.openFile(uri, mode);
    }
}

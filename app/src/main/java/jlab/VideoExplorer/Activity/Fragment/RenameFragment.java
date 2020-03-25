package jlab.VideoExplorer.Activity.Fragment;
/*
 * Created by Javier on 03/09/2017.
 */

import android.os.Bundle;
import jlab.VideoExplorer.R;
import jlab.VideoExplorer.Utils;

public class RenameFragment extends ActionFragment {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.title = getString(R.string.rename);
        this.hint = this.resource.getName();
    }

    @Override
    protected void applyAction(String name) {
        boolean result = resource.renameTo(name);
        if (resource.isDir())
            Utils.showSnackBar(result
                    ? R.string.folder_rename
                    : R.string.folder_not_rename);
        else {
            Utils.showSnackBar(result
                    ? R.string.file_rename
                    : R.string.file_not_rename);
        }
    }
}

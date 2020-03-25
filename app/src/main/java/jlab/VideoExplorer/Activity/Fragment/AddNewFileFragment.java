package jlab.VideoExplorer.Activity.Fragment;

import android.os.Bundle;

import jlab.VideoExplorer.Resource.LocalDirectory;
import jlab.VideoExplorer.R;
import jlab.VideoExplorer.Utils;

/*
 * Created by Javier on 03/09/2017.
 */

public class AddNewFileFragment extends ActionFragment {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.title = getString(R.string.new_file);
        this.hint = "";
    }

    @Override
    protected void applyAction(String name) {
        if (((LocalDirectory) resource).newFile(name, true))
            Utils.showSnackBar(R.string.file_created);
        else Utils.showSnackBar(R.string.file_not_created);
    }
}

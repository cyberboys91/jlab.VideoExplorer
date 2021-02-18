package jlab.VideoExplorer.Activity.Fragment;

import android.os.Bundle;
import android.view.View;
import android.content.Context;
import android.widget.TextView;
import jlab.VideoExplorer.R;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import jlab.VideoExplorer.Utils;
import androidx.appcompat.app.AlertDialog;
import jlab.VideoExplorer.Resource.Resource;
import jlab.VideoExplorer.Resource.FileResource;


public class DetailsFragment extends DialogFragment {

    private Resource resource;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setCancelable(false);
        this.resource = (Resource) getArguments().getSerializable(Utils.RESOURCE_FOR_DETAILS_KEY);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public AlertDialog onCreateDialog(Bundle saveInstance) {
        Context context = getActivity().getBaseContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        final View detailsView = inflater.inflate(R.layout.resource_details_dialog, null, false);
        TextView name = (TextView) detailsView.findViewById(R.id.tvResourceNameDetails),
                path = (TextView) detailsView.findViewById(R.id.tvResourcePathDetails),
                size = (TextView) detailsView.findViewById(R.id.tvResourceSizeDetails),
                isDir = (TextView) detailsView.findViewById(R.id.tvResourceTypeDetails),
                modification = (TextView) detailsView.findViewById(R.id.tvResourceModificationDetails);
        name.setText(resource.getName());
        path.setText(resource.getRelUrl());
        isDir.setText(resource.isDir() ? getString(R.string.folder) : ((FileResource)resource).getMimeType());
        modification.setText(resource.getModificationDateLong());

        if (resource.isDir()) {
            LinearLayout ll = (LinearLayout) detailsView.findViewById(R.id.rlResourceDetails);
            ll.removeView(size);
            ll.removeView(ll.findViewById(R.id.tvResourceSizeDetailsTitle));
        } else
            size.setText(((FileResource) resource).sizeToString());

        return new AlertDialog.Builder(inflater.getContext())
                .setView(detailsView)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.accept), null).create();
    }
}
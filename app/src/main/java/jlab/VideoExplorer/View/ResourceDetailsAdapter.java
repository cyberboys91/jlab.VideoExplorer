package jlab.VideoExplorer.View;

import android.view.View;
import java.util.Collection;
import android.view.ViewGroup;
import jlab.VideoExplorer.Utils;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import jlab.VideoExplorer.Resource.Resource;

/*
 * Created by Javier on 1/10/2016.
 */
public class ResourceDetailsAdapter extends ArrayAdapter<Resource> {

    private OnGetSetViewListener monGetSetViewListener;

    public ResourceDetailsAdapter() {
        super(Utils.currentActivity, 0);
    }

    @Override
    public void add(Resource elem) {
        super.add(elem);
        notifyDataSetChanged();
    }

    @Override
    public void addAll(@NonNull Collection<? extends Resource> resources) {
        super.addAll(resources);
        notifyDataSetChanged();
    }

    @Override
    public void clear() {
        super.clear();
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        try {
            Resource resource = getItem(position);
            convertView = this.monGetSetViewListener.getView(parent, position, resource != null && resource.isDir());
            this.monGetSetViewListener.setView(convertView, resource, position);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void setonGetSetViewListener(OnGetSetViewListener newListener) {
        this.monGetSetViewListener = newListener;
    }

    public interface OnGetSetViewListener {
        View getView(ViewGroup parent, int position, boolean isDir);

        void setView(View view, Resource resource, int position);
    }
}

package jlab.VideoExplorer.View;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import jlab.VideoExplorer.R;

/*
 * Created by Javier on 25/9/2016.
 */
public class LocalPropertiesLayout extends RelativeLayout {
    public LocalPropertiesLayout(Context context) {
        super(context);
        View.inflate(context, R.layout.local_resource_details, this);
    }

    public LocalPropertiesLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        View.inflate(context, R.layout.local_resource_details, this);
    }

    public LocalPropertiesLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.local_resource_details, this);
    }
}
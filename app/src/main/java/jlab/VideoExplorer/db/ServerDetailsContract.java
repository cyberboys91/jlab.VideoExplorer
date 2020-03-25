package jlab.VideoExplorer.db;

import android.provider.BaseColumns;

/*
 * Created by Javier on 24/04/2017.
 */
public class ServerDetailsContract implements BaseColumns {
    public static final String SERVER_TABLE_NAME = "server";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String PATH_DOWNLOAD = "pathDownload";
    public static final String COMMENT = "comment";
    public static final String FAVORITE_TABLE_NAME = "favorite";
    public static final String PATH = "path";
    public static final String SIZE = "size";
    public static final String MODIFICATION_DATE = "modification";
    public static final String PARENT_NAME = "parent";
}
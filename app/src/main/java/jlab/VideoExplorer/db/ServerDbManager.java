package jlab.VideoExplorer.db;

import java.io.File;
import java.util.ArrayList;
import jlab.VideoExplorer.R;
import android.content.Context;
import android.database.Cursor;
import jlab.VideoExplorer.Utils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/*
 * Created by Javier on 24/04/2017.
 */
public class ServerDbManager extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "servers.db";
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + ServerDetailsContract.SERVER_TABLE_NAME;

    public ServerDbManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE " + ServerDetailsContract.SERVER_TABLE_NAME + " ("
                    + ServerDetailsContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ServerDetailsContract.HOST + " TEXT NOT NULL,"
                    + ServerDetailsContract.PORT + " TEXT NOT NULL,"
                    + ServerDetailsContract.PATH_DOWNLOAD + " TEXT NOT NULL,"
                    + ServerDetailsContract.COMMENT + " TEXT NOT NULL)");

            db.execSQL("CREATE TABLE " + ServerDetailsContract.FAVORITE_TABLE_NAME + " ("
                    + ServerDetailsContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ServerDetailsContract.PATH + " TEXT NOT NULL,"
                    + ServerDetailsContract.SIZE + " BIGINT NOT NULL,"
                    + ServerDetailsContract.MODIFICATION_DATE + " BIGINT NOT NULL,"
                    + ServerDetailsContract.PARENT_NAME + " TEXT,"
                    + ServerDetailsContract.COMMENT + " TEXT NOT NULL)");

            //TODO: Cambiar el comentario del servidor estandar.
            saveServerData(db, new ServerDetails(0, Utils.getString(R.string.my_files), Utils.portServer, "", ""));
//            saveServerData(db, new ServerDetails(1, Utils.hostServer, Utils.portServer,
//                    getFirstValidDownloadDir(), "comment"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFirstValidDownloadDir() {
        ArrayList<String> storagePath = Utils.getStoragesPath();
        String dirDownload = "";
        for (String dir : storagePath) {
            if (Utils.existAndMountDir(dir)) {
                dirDownload = dir;
                break;
            }
        }
        return dirDownload;
    }

    public long saveServerData(SQLiteDatabase sqLiteDatabase, ServerDetails server) {
        return sqLiteDatabase.insert(ServerDetailsContract.SERVER_TABLE_NAME, null, server.toContentValues());
    }

    public long saveFavoriteData(SQLiteDatabase sqLiteDatabase, FavoriteDetails favorite) {
        return sqLiteDatabase.insert(ServerDetailsContract.FAVORITE_TABLE_NAME, null, favorite.toContentValues());
    }

    public long saveServerData(ServerDetails server) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.insert(ServerDetailsContract.SERVER_TABLE_NAME, null, server.toContentValues());
    }

    public long saveFavoriteData(FavoriteDetails favorite) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.insert(ServerDetailsContract.FAVORITE_TABLE_NAME, null, favorite.toContentValues());
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
        onCreate(sqLiteDatabase);
    }

    public ArrayList<ServerDetails> getServersData() {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ArrayList<ServerDetails> result = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query(ServerDetailsContract.SERVER_TABLE_NAME, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            String host = cursor.getString(cursor.getColumnIndex(ServerDetailsContract.HOST)),
                    pathDownload = cursor.getString(cursor.getColumnIndex(ServerDetailsContract.PATH_DOWNLOAD)),
                    comment = cursor.getString(cursor.getColumnIndex(ServerDetailsContract.COMMENT)),
                    port = cursor.getString(cursor.getColumnIndex(ServerDetailsContract.PORT));
            int id = cursor.getInt(cursor.getColumnIndex(ServerDetailsContract._ID));
            result.add(new ServerDetails(id, host, port, pathDownload, comment));
        }
        cursor.close();
        return result;
    }

    public ArrayList<FavoriteDetails> getFavoriteData() {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ArrayList<FavoriteDetails> result = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query(ServerDetailsContract.FAVORITE_TABLE_NAME, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            String path = cursor.getString(cursor.getColumnIndex(ServerDetailsContract.PATH)),
                    comment = cursor.getString(cursor.getColumnIndex(ServerDetailsContract.COMMENT)),
                    parent = cursor.getString(cursor.getColumnIndex(ServerDetailsContract.PARENT_NAME));
            int id = cursor.getInt(cursor.getColumnIndex(ServerDetailsContract._ID));
            long size = cursor.getLong(cursor.getColumnIndex(ServerDetailsContract.SIZE)),
                    modification = cursor.getLong(cursor.getColumnIndex(ServerDetailsContract.MODIFICATION_DATE));
            result.add(new FavoriteDetails(id, path, comment, parent, size, modification));
        }
        cursor.close();
        return result;
    }

    public int updateServerData(int id, ServerDetails newServerDetails) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.update(ServerDetailsContract.SERVER_TABLE_NAME,
                newServerDetails.toContentValues(),
                ServerDetailsContract._ID + " LIKE ?",
                new String[]{Integer.toString(id)});
    }

    public int updateFavoriteData(long id, FavoriteDetails newFavoriteDetails) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.update(ServerDetailsContract.FAVORITE_TABLE_NAME,
                newFavoriteDetails.toContentValues(),
                ServerDetailsContract._ID + " LIKE ?",
                new String[]{Long.toString(id)});
    }

    public int deleteServerData(int id) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.delete(ServerDetailsContract.SERVER_TABLE_NAME,
                ServerDetailsContract._ID + " LIKE ?",
                new String[]{Integer.toString(id)});
    }

    public int deleteFavoriteData(long id) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.delete(ServerDetailsContract.FAVORITE_TABLE_NAME,
                ServerDetailsContract._ID + " LIKE ?",
                new String[]{Long.toString(id)});
    }

    public int updateFavoriteData(int id, String newPath, String newComment) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        File file = new File(newPath);
        return sqLiteDatabase.update(ServerDetailsContract.FAVORITE_TABLE_NAME,
                new FavoriteDetails(file.getPath(), newComment, file.getParent(), file.length()
                        , file.lastModified()).toContentValues(),
                ServerDetailsContract._ID + " LIKE ?",
                new String[]{Long.toString(id)});
    }
}
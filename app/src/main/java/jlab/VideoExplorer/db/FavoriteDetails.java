package jlab.VideoExplorer.db;

import android.os.Parcel;
import android.os.Parcelable;
import android.content.ContentValues;

/*
 * Created by Javier on 24/04/2017.
 */
public class FavoriteDetails implements Parcelable {

    private int id;
    private String path, comment, parentName;
    private long size;
    private long modification;

    public FavoriteDetails(String path, String comment, String parentName, long size, long modification) {
        this.path = path.trim();
        this.comment = comment != null ? comment.trim() : "";
        this.size = size;
        this.modification = modification;
        this.parentName = parentName;
    }

    public FavoriteDetails(int id, String path, String comment, String parentName, long size, long modification) {
        this.id = id;
        this.path = path.trim();
        this.comment = comment != null ? comment.trim() : "";
        this.size = size;
        this.modification = modification;
        this.parentName = parentName;
    }

    private FavoriteDetails(Parcel in) {
        this.id = in.readInt();
        this.path = in.readString();
        this.size = in.readLong();
        this.modification = in.readLong();
        this.comment = in.readString();
        this.parentName = in.readString();
    }

    public static final Creator<FavoriteDetails> CREATOR = new Creator<FavoriteDetails>() {
        @Override
        public FavoriteDetails createFromParcel(Parcel in) {
            return new FavoriteDetails(in);
        }

        @Override
        public FavoriteDetails[] newArray(int size) {
            return new FavoriteDetails[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.id);
        parcel.writeString(this.path);
        parcel.writeLong(this.size);
        parcel.writeLong(this.modification);
        parcel.writeString(this.comment);
        parcel.writeString(this.parentName);
    }

    public int getId() {
        return this.id;
    }

    public String getPath() {
        return this.path;
    }

    public long getSize() {
        return this.size;
    }

    public long getModification() {
        return this.modification;
    }

    public String getComment() {
        return this.comment;
    }

    public String getParentName() {
        return parentName;
    }

    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ServerDetailsContract.PATH, this.path);
        contentValues.put(ServerDetailsContract.SIZE, this.size);
        contentValues.put(ServerDetailsContract.MODIFICATION_DATE, this.modification);
        contentValues.put(ServerDetailsContract.COMMENT, this.comment);
        contentValues.put(ServerDetailsContract.PARENT_NAME, this.parentName);
        return contentValues;
    }
}

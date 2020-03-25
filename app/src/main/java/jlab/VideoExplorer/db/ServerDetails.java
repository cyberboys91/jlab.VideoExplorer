package jlab.VideoExplorer.db;

import android.os.Parcel;
import android.os.Parcelable;
import android.graphics.Bitmap;
import android.content.ContentValues;

/*
 * Created by Javier on 24/04/2017.
 */
public class ServerDetails implements Parcelable{

    private int id;
    private String host;
    private String port;
    private String pathDownload;
    private String comment;
    public Bitmap presentation;

    public ServerDetails(String host, String port, String pathDownload, String comment)
    {
        this.host = host.trim();
        this.port = port.trim();
        this.pathDownload = pathDownload.trim();
        this.comment = comment.trim();
    }

    public ServerDetails(int id, String host, String port, String pathDownload, String comment)
    {
        this.id = id;
        this.host = host;
        this.port = port;
        this.pathDownload = pathDownload;
        this.comment = comment;
    }

    private ServerDetails(Parcel in) {
        this.id = in.readInt();
        this.host = in.readString();
        this.port = in.readString();
        this.pathDownload = in.readString();
        this.comment = in.readString();
    }

    public String getPresentationUrl()
    {
        return String.format("http://%s:%s/p/", getHost(), getPort());
    }

    public static final Creator<ServerDetails> CREATOR = new Creator<ServerDetails>() {
        @Override
        public ServerDetails createFromParcel(Parcel in) {
            return new ServerDetails(in);
        }

        @Override
        public ServerDetails[] newArray(int size) {
            return new ServerDetails[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.id);
        parcel.writeString(this.host);
        parcel.writeString(this.port);
        parcel.writeString(this.pathDownload);
        parcel.writeString(this.comment);
    }

    public int getId()
    {
        return this.id;
    }

    public String getHost()
    {
        return this.host;
    }

    public String getComment(){
        return this.comment;
    }

    public String getPort()
    {
        return this.port;
    }

    public String getPathDownload()
    {
        return this.pathDownload;
    }

    public void setHost(String newhost)
    {
        this.host = newhost;
    }

    public void setComment(String newcomment){
        this.comment = newcomment;
    }

    public void setPort(String newport)
    {
        this.port = newport;
    }

    public void setPathDownload(String newpathdownload)
    {
        this.pathDownload = newpathdownload;
    }

    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ServerDetailsContract.HOST, this.host);
        contentValues.put(ServerDetailsContract.PORT, this.port);
        contentValues.put(ServerDetailsContract.PATH_DOWNLOAD, this.pathDownload);
        contentValues.put(ServerDetailsContract.COMMENT, this.comment);
        return contentValues;
    }
}

package jlab.VideoExplorer.Service;

import java.io.File;
import java.net.URL;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import jlab.VideoExplorer.Resource.RemoteFile;
import jlab.VideoExplorer.Resource.FileResource;
import jlab.VideoExplorer.Interfaces.IDownloadRefreshListener;

/*
 * Created by Javier on 06/01/2018.
 */
public class ResourceInDownload {
    private RemoteFile mresource;
    private DownloadStatus mstatus;
    private IDownloadRefreshListener mdownloadCompleteListener;
    private int mcountBytesReaded, mtotalBytes;
    private static final int BUFFER_SIZE = 5242880;//5MB //1048576;//1MB
    private File mDownloadDir;
    private File mDownloadFile;

    public ResourceInDownload(RemoteFile resource, IDownloadRefreshListener downloadCompleteListener) {
        this.mstatus = DownloadStatus.ENQUEUE;
        this.mresource = resource;
        this.mdownloadCompleteListener = downloadCompleteListener;
        this.mcountBytesReaded = this.mtotalBytes = 0;
    }

    public DownloadStatus getStatus() {
        return mstatus;
    }

    public int getCountBytesReaded() {
        return mcountBytesReaded;
    }

    public int getTotalBytes() {
        return mtotalBytes;
    }

    public String getNameResource() {
        return mresource.getName();
    }

    public RemoteFile getResRemoteFile() {
        return mresource;
    }

    public void download() {
        try {
            mstatus = DownloadStatus.RUNNING;
            String packName = ResourceDownloaderService.getPackName();
            if (packName != null) {
                mDownloadDir = new File(String.format("%s/Android/data/%s/files/",
                        ResourceDownloaderService.getPathDownload(), packName));
                mDownloadFile = new File(mDownloadDir, getNameForDownloadFile(mresource.getName()));
                if (mDownloadFile.createNewFile()) {
                    final URL mUrl = new URL(mresource.getAbsUrl());
                    final URLConnection conn = mUrl.openConnection();
                    mtotalBytes = conn.getContentLength();
                    fillContent(conn.getInputStream(), new FileOutputStream(mDownloadFile));
                    mstatus = DownloadStatus.SUCCESSFUL;
                } else mstatus = DownloadStatus.FAILED;
            } else
                mstatus = DownloadStatus.FAILED;
        } catch (Exception ignored) {
            ignored.printStackTrace();
            mstatus = DownloadStatus.FAILED;
        } finally {
            mdownloadCompleteListener.complete(this, mstatus);
        }
    }

    private String getNameForDownloadFile(String name) {
        int max = -1;
        boolean exist = false;
        String ext = FileResource.getExtension(name);
        name = name.substring(0, name.length() - ext.length() - 1);
        for (String childName : mDownloadDir.list()) {
            if (childName.indexOf(name) == 0) {
                String extChild = FileResource.getExtension(childName);
                if (extChild.equals(ext)) {
                    childName = childName.substring(0, childName.length() - extChild.length() - 1);
                    if (childName.length() == name.length())
                        exist = true;
                    String numberStr = "";
                    for (int i = name.length() + 1; i < childName.length() && childName.charAt(i) != ')'
                            && Character.isDigit(childName.charAt(i)); i++)
                        numberStr += childName.charAt(i);
                    if (!numberStr.equals("")) {
                        int number = Integer.parseInt(numberStr);
                        if (number > max)
                            max = number;
                    }
                }
            }
        }
        return exist
                ? String.format("%s(%s)%s", name, max + 1, (ext.length() > 0 ? "." : "") + ext)
                : String.format("%s%s", name, (ext.length() > 0 ? "." : "") + ext);
    }

    private void fillContent(InputStream in, FileOutputStream writter) throws IOException {
        int len = Math.min(mtotalBytes, BUFFER_SIZE);
        byte[] buffer = new byte[len];
        DataInputStream reader = new DataInputStream(in);
        while (mcountBytesReaded < mtotalBytes) {
            if(mcountBytesReaded + len > mtotalBytes)
                len = mtotalBytes - mcountBytesReaded;
            reader.readFully(buffer, 0, len);
            writter.write(buffer, 0, len);
            mcountBytesReaded += len;
        }
    }

    public File getDownloadFile() {
        return mDownloadFile;
    }

    public Uri getUriDownloadFile() {
        return Uri.fromFile(mDownloadFile);
    }

    public String getMimeType() {
        return mresource.getMimeType();
    }

    public enum DownloadStatus {
        ENQUEUE,
        RUNNING,
        FAILED,
        SUCCESSFUL
    }
}

package jlab.VideoExplorer.Resource;

import java.io.File;
import java.util.Date;
import android.net.Uri;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.io.Serializable;
import jlab.VideoExplorer.Utils;
import jlab.VideoExplorer.Service.DeleteResourceService;
import static jlab.VideoExplorer.Resource.FileResource.getMimeType;
import static jlab.VideoExplorer.Resource.FileResource.getExtension;

/*
 * Created by Javier on 20/06/2017.
 */

public abstract class Resource implements Serializable {

    protected String name, relUrl, comment;
    private boolean isDir;
    protected String mAbsUrl;
    protected boolean isRemote = false;
    protected boolean isHidden;
    protected long modification;
    protected String RelUrlEncode;
    private String urlServer;
    private int indexPattern;

    public Resource(String name, String relUrl, String comment, boolean isDirectory, long modification) {
        this.urlServer = Utils.urlServer;
        this.name = name;
        this.relUrl = relUrl;
        this.isDir = isDirectory;
        this.modification = modification;
        this.comment = comment;
    }

    public static String strEncode(String decode) {
        String result = "";
        for (int i = 0; i < decode.length(); i++) {
            Character elem = decode.charAt(i);
            if (elem == ' ')
                result += "%20";
            else if (elem == '/')
                result += '/';
            else {
                try {
                    result += URLEncoder.encode(elem.toString(), "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static String strDecode(String encode) {
        return Uri.decode(encode);
    }

    public String getName() {
        return name;
    }

    public boolean isDir() {
        return isDir;
    }

    public String getRelUrl() {
        return relUrl;
    }

    public boolean isRemote() {
        return isRemote;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public String getModificationDateShort() {
        if (modification != 0) {
            DateFormat formatter = DateFormat.getDateInstance();
            Date date = new Date(modification);
            return formatter.format(date);
        }
        return "";
    }

    public String getModificationDateLong() {
        if (modification != 0) {
            DateFormat formatter = DateFormat.getDateTimeInstance();
            Date date = new Date(modification);
            return formatter.format(date);
        }
        return "";
    }

    public boolean delete(DeleteResourceService.OnDeleteListener deleteListener) {
        if (isRemote())
            return true;
        try {
            File file = new File(this.relUrl);
            deleteListener.delete(getName(), file.length());
            return !file.exists() || file.delete();
        } catch (Exception exp) {
            exp.printStackTrace();
            return false;
        }
    }

    public boolean renameTo(String newName) {
        if (isRemote())
            return true;
        try {
            newName = newName.trim();
            File file = new File(getRelUrl()), newFile = new File(file.getParent(), newName);
            if (!newFile.exists() && file.renameTo(newFile)) {
                if (isDir())
                    Utils.updatePathMediaStore(getRelUrl(), newFile.getPath().length(),
                            new LocalDirectory(newName, newFile.getPath(), null, false, 0));
                else {
                    Uri newUri = Utils.getUriForFile(new LocalFile(newName,
                            newFile.getPath(), null, null, file.length(), file.lastModified(), isHidden())),
                            oldUri = Utils.getUriForFile((FileResource) this);
                    if (!newUri.equals(oldUri)) {
                        Utils.deleteFile(oldUri, getRelUrl());
                        Utils.addFileToContentThread(newFile.getAbsolutePath());
                    } else
                        Utils.updateFileInContent(oldUri, getRelUrl(), newName, newFile.getPath(),
                                getMimeType(getExtension(newName)));
                }
                return true;
            }
            return false;
        } catch (Exception exp) {
            exp.printStackTrace();
            return false;
        }
    }

    public String getAbsUrl() {
        if (isRemote()) {
            if (this.RelUrlEncode == null)
                this.RelUrlEncode = strEncode(relUrl);
            if (this.mAbsUrl == null)
                this.mAbsUrl = urlServer + (isDir() ? "/d" : "/f") + this.RelUrlEncode;
            return this.mAbsUrl;
        }
        if (this.mAbsUrl == null)
            this.mAbsUrl = "file:/" + this.relUrl;
        return this.mAbsUrl;
    }

    public void setIndexPattern(int indexPattern) {
        this.indexPattern = indexPattern;
    }

    public int getIndexPattern() {
        return indexPattern;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

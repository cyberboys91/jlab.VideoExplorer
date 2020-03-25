package jlab.VideoExplorer.Resource;

import java.io.File;
import android.net.Uri;
import jlab.VideoExplorer.Utils;
import android.webkit.MimeTypeMap;
import jlab.VideoExplorer.Service.DeleteResourceService;

/*
 * Created by Javier on 11/8/2016.
 */

public abstract class FileResource extends Resource {
    public long mSize = 0;
    private String mSizeToString, mExtension, mMimeType;
    public String mThumbUrl;
    private String parentName;
    public boolean thumbLoaded;
    private byte auxIsApk = -1, auxThumb = -1, auxIsAudio = -1, auxIsVideo = -1, auxIsPdf = -1, auxIsDoc = -1;
    private static MimeTypeMap map = MimeTypeMap.getSingleton();
    private static final String VOB_MIME_TYPE = "video/x-ms-vob";
    private boolean isFavorite = false;
    private long idFavorite = -9101;
    private boolean favoriteStateLoad = false;

    public FileResource(String name, String relUrl, String comment, long size) {
        super(name, relUrl, comment, false, 0);
        this.mSize = size;
        this.isHidden = false;
    }

    public FileResource(String name, String relUrl, String comment, String parentName, long size, long modification, boolean isHidden) {
        super(name, relUrl, comment, false, modification);
        this.mThumbUrl = relUrl;
        this.mAbsUrl = Utils.FILE_SCHEME + "://" + relUrl;
        this.mSize = size;
        this.isHidden = isHidden;
        this.parentName = parentName;
    }

    public static String getNameFromUrl(String url) {
        int i = url.length() - 1;
        for (; i >= 0; i--)
            if (url.charAt(i) == '/')
                break;
        char[] name = new char[url.length() - i - 1];
        for (int j = 0; j < name.length; j++)
            name[j] = url.charAt(++i);
        return new String(name);
    }

    private String sizeToStringHide() {
        return Utils.getSizeString(this.mSize, 2);
    }

    public String getExtension() {
        if (this.mExtension == null)
            this.mExtension = getExtension(getName());
        return this.mExtension;
    }

    public static String getExtension(String name) {
        String result = "";
        boolean exist = false;
        for (int i = name.length() - 1; i >= 0; i--) {
            String current = new String(new char[]{name.charAt(i)});
            if (exist = (current.equals(".")))
                break;
            result = current + result;
        }
        return exist ? result.toLowerCase() : "";
    }

    public String getMimeType() {
        if (this.mMimeType == null)
            this.mMimeType = getMimeType(getExtension());
        return this.mMimeType;
    }

    public String getParentName() {
        return parentName;
    }

    public long getModificationDate() {
        return modification;
    }

    public static String getMimeType(String ext) {
        String mimeType = ext.equals("vob") ? VOB_MIME_TYPE : map.getMimeTypeFromExtension(ext);
        return mimeType != null ? mimeType : "unknown/unknown";
    }

    public String sizeToString() {
        if (this.mSizeToString == null)
            this.mSizeToString = sizeToStringHide();
        return this.mSizeToString;
    }

    public String thumbUrl() {
        if (this.mThumbUrl == null && this.RelUrlEncode == null)
            this.RelUrlEncode = strEncode(relUrl);
        if (this.mThumbUrl == null)
            this.mThumbUrl = Utils.urlServer + "/t" + this.RelUrlEncode;
        return this.mThumbUrl;
    }

    public boolean isFavorite() {
        return this.isFavorite;
    }

    public void setIsFavorite(boolean favorite, long idFavorite) {
        this.isFavorite = favorite;
        this.idFavorite = idFavorite;
    }

    public long getIdFavorite() {
        return this.idFavorite;
    }

    public boolean isApk() {
        if (auxIsApk == -1)
            auxIsApk = (byte) (getExtension().equals("apk") ? 1 : 0);
        return auxIsApk == 1;
    }

    public boolean isImage() {
        if (auxThumb == -1)
            auxThumb = (byte) (getExtension().equals("jpg")
                    || getExtension().equals("png")
                    || getExtension().equals("bmp")
                    || getExtension().equals("jpeg")
                    || getExtension().equals("ico")
                    || getExtension().equals("jpe")
                    || getExtension().equals("jfi")
                    || getExtension().equals("jfif")
                    || getExtension().equals("dib")
                    || getExtension().equals("jif")
                    || getExtension().equals("apng")
                    || getExtension().equals("gif") ? 1 : 0);
        return auxThumb == 1;
    }

    public boolean isAudio() {
        if (auxIsAudio == -1)
            auxIsAudio = (byte) (getExtension().equals("vob") || getExtension().equals("ogg")
                    || getMimeType().substring(0, Utils.AUDIO.length()).equals(Utils.AUDIO) ? 1 : 0);
        return auxIsAudio == 1;
    }

    public boolean isVideo() {
        if (auxIsVideo == -1)
            auxIsVideo = (byte) (getMimeType().substring(0, Utils.VIDEO.length()).equals(Utils.VIDEO) ? 1 : 0);
        return auxIsVideo == 1;
    }

    public boolean isThumbnailer() {
        return isApk() || isImage() || (!isRemote && (isAudio() || isVideo()));
    }

    public static boolean isDocFromMimeType(String mimeType) {
        try {
            String ext = map.getExtensionFromMimeType(mimeType);
            return isDocFromExtension(ext);
        } catch (Exception exp) {
            exp.printStackTrace();
            return false;
        }
    }

    private static boolean isDocFromExtension(String ext) {
        if (ext == null)
            return false;
        switch (ext) {
            case "pdf":
            case "doc":
            case "docx":
            case "rtf":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
            case "epub":
                return true;
            default:
                return false;
        }
    }

    public boolean isDocument() {
        if (auxIsDoc == -1)
            auxIsDoc = (byte) (isDocFromExtension(getExtension()) ? 1 : 0);
        return auxIsDoc == 1;
    }

    public boolean isPdf() {
        if (auxIsPdf == -1)
            auxIsPdf = (byte) (getExtension().equals("pdf") ? 1 : 0);
        return auxIsPdf == 1;
    }

    @Override
    public boolean delete(DeleteResourceService.OnDeleteListener deleteListener) {
        boolean deleted = super.delete(deleteListener);
        if (deleted && !isRemote) {
            if (isFavorite())
                Utils.deleteFavoriteData(idFavorite);
            Uri uri = Utils.getUriForFile(this);
            if (uri != null)
                Utils.deleteFile(uri, relUrl);
        }
        return deleted;
    }

    @Override
    public boolean renameTo(String newName) {
        boolean renamed = super.renameTo(newName);
        if (renamed && isFavorite()) {
            File parent = new File(getRelUrl()).getParentFile();
            Utils.updateFavoriteData(idFavorite,
                    String.format("%s/%s", parent.getPath(), newName),
                    parent.getName());
        }
        return renamed;
    }

    public boolean getFavoriteStateLoad() {
        return favoriteStateLoad;
    }

    public void setFavoriteStateLoad() {
        this.favoriteStateLoad = true;
    }
}
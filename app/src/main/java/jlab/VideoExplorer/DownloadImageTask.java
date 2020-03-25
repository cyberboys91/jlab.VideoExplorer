package jlab.VideoExplorer;

import java.net.URL;
import java.io.InputStream;
import android.util.LruCache;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.media.ThumbnailUtils;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import jlab.VideoExplorer.Resource.FileResource;
import jlab.VideoExplorer.db.FavoriteDetails;

/*
 * Created by Javier on 7/9/2016.
 */
public class DownloadImageTask {
    private boolean isMultiColumn;
    private ImageView mResIcon, mivFavorite;
    private boolean refresh = true, setBackground = true;
    private static final int MaxThumbCountInCache = 200;
    private static LruCache<String, Bitmap> thumbnailsCache = new LruCache<>(MaxThumbCountInCache);
    public static OnSetImageIconUIThread monSetImageIcon = new OnSetImageIconUIThread() {
        @Override
        public void setImage(ImageView imageView, Bitmap bitmap) {

        }

        @Override
        public void setBackground(ImageView icon, Bitmap bitmap, int res) {

        }

        @Override
        public void setImage(ImageView imageView, Drawable drawable) {

        }

        @Override
        public void setImage(ImageView imageView, int idRes) {

        }

        @Override
        public void setImage(ImageView mImage, ImageView ivState, Bitmap bitmap) {

        }

        @Override
        public void setVisibility(ProgressBar pbLoading, int visibility) {

        }

        @Override
        public void runOnUserInterfaceThread(Runnable runnable) {

        }
    };

    public DownloadImageTask(ImageView rico, ImageView ivFavorite, int idResource, boolean setBackground, boolean isMultiColumn) {
        this.mResIcon = rico;
        this.mivFavorite = ivFavorite;
        this.mResIcon.setImageResource(idResource);
        this.setBackground = setBackground;
        this.isMultiColumn = isMultiColumn;
    }

    public static Bitmap get(String url) {
        return thumbnailsCache != null ? thumbnailsCache.get(url) : null;
    }

    public static void put(String url, Bitmap bitmap) {
        if (thumbnailsCache == null)
            thumbnailsCache = new LruCache<>(MaxThumbCountInCache);
        thumbnailsCache.put(url, bitmap);
    }

    public void load(final String url, final boolean isRemote, final FileResource file, final boolean isAlbum) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                refresh = false;
                Bitmap mIcon = null;
                try {
                    if (isRemote) {
                        InputStream in = new URL(url).openStream();
                        mIcon = BitmapFactory.decodeStream(in);
                        in.close();
                    } else if (file.isApk())
                        mIcon = ThumbnailUtils.extractThumbnail(Utils.getIconApk(url).getBitmap(),
                                Utils.THUMB_HEIGHT_MINI, Utils.THUMB_HEIGHT_MINI);
                    else
                        mIcon = Utils.getThumbnailForUriFile(url, file);
                    refresh = mIcon != null;
                } catch (Exception e) {
                    refresh = false;
                    e.printStackTrace();
                }
                if (refresh) {
                    put(url, mIcon);
                    if (setBackground && (file.isVideo() || file.isAudio()))
                        monSetImageIcon.setBackground(mResIcon, mIcon,
                                file.isVideo()
                                        ? (isMultiColumn
                                        ? R.drawable.img_very_small_play_video
                                        : R.drawable.img_small_play_video)
                                        : (isMultiColumn
                                        ? R.drawable.img_very_small_audio
                                        : R.drawable.img_small_audio));
                    else
                        monSetImageIcon.setImage(mResIcon, mIcon);

                    if (!isAlbum && !file.getFavoriteStateLoad() && mivFavorite != null) {
                        final boolean isFavorite = Utils.isFavorite(file);
                        monSetImageIcon.setImage(mivFavorite, isFavorite
                                ? R.drawable.img_favorite_checked
                                : R.drawable.img_favorite_not_checked);
                        monSetImageIcon.runOnUserInterfaceThread(new Runnable() {
                            @Override
                            public void run() {
                                mivFavorite.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (file.isFavorite())
                                            file.setIsFavorite(false, Utils.deleteFavoriteData(file.getIdFavorite()));
                                        else
                                            file.setIsFavorite(true, Utils.saveFavoriteData(new FavoriteDetails(file.getRelUrl(),
                                                    file.getComment(), file.getParentName(), file.mSize, file.getModificationDate())));
                                        monSetImageIcon.setImage(mivFavorite, file.isFavorite()
                                                ? R.drawable.img_favorite_checked
                                                : R.drawable.img_favorite_not_checked);
                                    }
                                });
                                mivFavorite.setOnLongClickListener(new View.OnLongClickListener() {
                                    @Override
                                    public boolean onLongClick(View view) {
                                        Utils.showSnackBar(file.isFavorite()
                                                ? R.string.remove_of_favorite_folder
                                                : R.string.add_to_favorite_folder);
                                        return true;
                                    }
                                });
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public static void freeCache() {
        if (thumbnailsCache != null)
            thumbnailsCache.evictAll();
    }

    public DownloadImageTask(ImageView rico, ImageView rtype, boolean isMultiColumn) {
        this.mResIcon = rico;
        this.isMultiColumn = isMultiColumn;
    }

    public interface OnSetImageIconUIThread
    {
        void setImage(ImageView imageView, Bitmap bitmap);
        void setBackground(ImageView icon, Bitmap bitmap, int res);
        void setImage(ImageView imageView, Drawable drawable);
        void setImage(ImageView imageView, int idRes);
        void setImage(ImageView mImage, ImageView ivState, Bitmap bitmap);
        void setVisibility(ProgressBar pbLoading, int visibility);
        void runOnUserInterfaceThread(Runnable runnable);
    }
}
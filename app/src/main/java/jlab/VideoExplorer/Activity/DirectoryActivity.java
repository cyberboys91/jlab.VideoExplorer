package jlab.VideoExplorer.Activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.view.Menu;
import android.view.View;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import jlab.VideoExplorer.*;
import android.widget.Toast;
import android.view.Surface;
import android.view.MenuItem;
import android.text.Selection;
import android.content.Intent;
import android.view.ViewGroup;
import jlab.VideoExplorer.View.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.util.DisplayMetrics;
import android.app.DownloadManager;
import jlab.VideoExplorer.Resource.*;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import android.content.DialogInterface;
import android.content.res.Configuration;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.graphics.drawable.Drawable;
import jlab.VideoExplorer.DownloadImageTask;
import android.content.res.ColorStateList;
import jlab.VideoExplorer.db.FavoriteDetails;
import jlab.VideoExplorer.db.ServerDbManager;
import android.text.SpannableStringBuilder;
import jlab.VideoExplorer.Activity.Fragment.*;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import android.view.animation.AnimationUtils;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.text.style.BackgroundColorSpan;
import android.graphics.drawable.BitmapDrawable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.AppBarLayout;
import jlab.VideoExplorer.Service.MediaPlayerService;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import jlab.VideoExplorer.Service.BackMediaPlayerReceiver;
import static jlab.VideoExplorer.Utils.specialDirectories;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class DirectoryActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, Interfaces.ILoadThumbnailForFile,
        Interfaces.IRemoteResourceClickListener, ResourceDetailsAdapter.OnGetSetViewListener,
        DownloadImageTask.OnSetImageIconUIThread, Interfaces.IGetDirectoryListener, Interfaces.ICopyRefresh,
        ActivityCompat.OnRequestPermissionsResultCallback, Interfaces.IElementRefreshListener,
        Interfaces.ICloseListener, Interfaces.IRefreshListener {

    private int PERMISSION_REQUEST_CODE = 2901;
    private FloatingActionButton mfbSearch, fbPlayOrPause;
    private TextView mtvEmptyFolder,
            mtvMediaResName, mtvMediaResComment;
    private ImageView mivMediaResThumb;
    private static Interfaces.IListContent mlcResourcesDir;
    private LayoutInflater mlinflater;
    private DownloadManager mdMgr;
    private NavigationView mnavMenuExplorer;
    private SwipeRefreshLayout msrlRefresh;
    private DrawerLayout mdrawer;
    private LinearLayout llDirectory;
    private AppBarLayout mpFragment;
    private ProgressBar pbSeekMedia;
    private boolean isRemoteDirectory = false;
    private Toolbar toolbar;
    private SearchView msvSearch;
    private int iconSize, id, swipeColor = R.color.accent;
    private boolean isPortrait;
    private boolean isMoving = false;
    private static Interfaces.IRefreshListener copyFragmentFinish;
    private static Interfaces.IRefreshListener deleteFragmentFinish;
    private static SeekListener seekListener;
    private static final short TIME_WAIT_SEEK = 1000;
    private static final int TIME_WAIT_FBUTTON_ANIM = 300;
    private static final String STACK_VARS_KEY = "STACK_VARS_KEY";
    private static final String NAME_DOWNLOAD_DIR_KEY = "NAME_DOWNLOAD_DIR_KEY";
    private static final String LOST_CONNECTION_KEY = "LOST_CONNECTION_KEY";
    private static final String SHOW_HIDDEN_FILES_KEY = "SHOW_HIDDEN_FILES_KEY";
    public static Uri treeUri;
    private Semaphore mutexLoadDataSpecial = new Semaphore(1);
    private Semaphore mutexLoadDirectory = new Semaphore(1);

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            if (msg.what != Utils.SCROLLER_PATH)
            {
                mtvEmptyFolder.setVisibility(View.INVISIBLE);
                switch (msg.what) {
                    case Utils.LOADING_INVISIBLE:
                        loadingInvisible();
                        Utils.freeUnusedMemory();
                        break;
                    case Utils.LOADING_VISIBLE:
                        if (!msrlRefresh.isRefreshing()) {
                            msrlRefresh.setRefreshing(true);
                            msrlRefresh.setVisibility(View.VISIBLE);
                            invalidateOptionsMenu();
                        }
                        break;
                    case Utils.LOST_CONNECTION:
                        Utils.freeUnusedMemory();
                        lostConnection();
                        break;
                }
            }
        }
    };

    public boolean requestPermission() {
        boolean request = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> requestPermissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                request = true;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                request = true;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(Manifest.permission.INTERNET);
                request = true;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(Manifest.permission.WAKE_LOCK);
                request = true;
            }
            if (request)
                requestAllPermission(requestPermissions);
        }
        return request;
    }

    private void requestAllPermission(ArrayList<String> requestPermissions) {
        String[] permission = new String[requestPermissions.size()];
        for (int i = 0; i < permission.length; i++)
            permission[i] = requestPermissions.get(i);
        ActivityCompat.requestPermissions(this, permission, PERMISSION_REQUEST_CODE);
    }

    public static void setCopyFragmentFinish(Interfaces.IRefreshListener copyFragmentFinish) {
        DirectoryActivity.copyFragmentFinish = copyFragmentFinish;
    }

    public static void setDeleteFragmentFinish(Interfaces.IRefreshListener deleteFragmentFinish) {
        DirectoryActivity.deleteFragmentFinish = deleteFragmentFinish;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.currentActivity = this;
        Bundle extras = savedInstanceState != null ? savedInstanceState : getIntent().getExtras();
        boolean haveExtras = extras != null && extras.containsKey(Utils.SERVER_DATA_KEY);
        if (haveExtras)
            this.isRemoteDirectory = extras.getBoolean(Utils.IS_REMOTE_DIRECTORY);
        loadFromBundle(savedInstanceState);
        setContentView(R.layout.activity_directory);
        DownloadImageTask.monSetImageIcon = this;
        loadViews();
        if (haveExtras)
            mlcResourcesDir.setRelUrlDirectoryRoot(this.isRemoteDirectory ?
                    Utils.NAME_REMOTE_DIRECTORY_ROOT : "", extras.getString(Utils.RELATIVE_URL_DIRECTORY_ROOT));
        setOnListeners();
        this.mdMgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        this.mlinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        reloadSpecialDir();
        requestPermission();
    }

    private void reloadSpecialDir() {
        specialDirectories = new LocalStorageDirectories();
        specialDirectories.openSynchronic(null);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            treeUri = data.getData();
            grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
    }

    @Override
    public Directory getDirectory(String name, String relUrl) {
        int size = Utils.stackVars.size();
        Directory dir;
        if (name.equals(Utils.NAME_SEARCH)) {
            showOrHideSearchFButton(false);
            showOrHideSearchView(true);
            dir = new SearchDirectory(relUrl, isRemoteDirectory);
            reloadDir(dir);
            if (msvSearch.getQuery().toString().equals(""))
                msvSearch.setQuery(((SearchDirectory) dir).getPattern(), false);
            ((SearchDirectory) dir).setAddListener(new Interfaces.IAddResourceListener() {

                @Override
                public void add(String name, String path, String comment, String album, long size, long modification) {

                }

                @Override
                public void add(final Resource resource) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mlcResourcesDir.getResourceDetailsAdapter().add(resource);
                        }
                    });
                }

                @Override
                public void clear() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mlcResourcesDir.getResourceDetailsAdapter().clear();
                        }
                    });
                }
            });
            return dir;
        } else if (!isRemoteDirectory && size == 1 && relUrl.equals(Utils.RELURL_SPECIAL_DIR)) {
            showOrHideSearchFButton(true);
            showOrHideSearchView(false);
            dir = getSpecialDirectory(name);
            reloadDir(dir);
            return dir;
        }
        showOrHideSearchView(false);
        if (isRemoteDirectory)
            return new RemoteDirectory(name, relUrl, null);

        showOrHideSearchFButton(true);
        dir = new AlbumDirectory(name, relUrl, null, 0, false);
        this.toolbar.setTitle(name);
        reloadDir(dir);
        return dir;
    }

    private void showOrHideSearchFButton(boolean show) {
        if (!show && this.mfbSearch.getVisibility() == View.VISIBLE) {
            mfbSearch.setVisibility(View.INVISIBLE);
        }
        else if (show && this.mfbSearch.getVisibility() == View.INVISIBLE) {
            mfbSearch.setVisibility(View.VISIBLE);
        }
    }

    private void startAnimation(View view, int anim) {
        view.startAnimation(AnimationUtils.loadAnimation(this, anim));
    }

    @Override
    public void setImage(final ImageView imageView, final Bitmap bitmap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bitmap != null)
                    imageView.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    public void setBackground(final ImageView icon, final Bitmap bitmap, final int res) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utils.setBackground(icon, bitmap, res);
            }
        });
    }

    @Override
    public void setImage(final ImageView imageView, final Drawable drawable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (drawable != null)
                    imageView.setImageDrawable(drawable);
            }
        });
    }

    @Override
    public void setImage(final ImageView imageView, final int idRes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageResource(idRes);
            }
        });
    }

    @Override
    public void setImage(final ImageView mImage, final ImageView ivState, final Bitmap bitmap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ivState != null)
                    ivState.setImageResource(R.drawable.img_server_run);
                if (mImage != null && bitmap != null) {
                    mImage.setImageResource(R.color.transparent);
                    mImage.setImageDrawable(new BitmapDrawable(bitmap));
                    mImage.setScaleType(ImageView.ScaleType.FIT_XY);
                }
            }
        });
    }

    @Override
    public void setVisibility(final ProgressBar pbLoading, final int visibility) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pbLoading != null)
                    pbLoading.setVisibility(visibility);
            }
        });
    }

    @Override
    public void runOnUserInterfaceThread(Runnable runnable) {
        runOnUiThread(runnable);
    }

    protected void loadViews() {
        this.toolbar = (Toolbar) findViewById(R.id.toolbar);
        this.toolbar.setTitleTextAppearance(this, R.style.ToolBarApparence);
        this.toolbar.setTitle(R.string.all_video);
        setSupportActionBar(toolbar);
        this.mdrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, this.mdrawer, this.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        this.mdrawer.addDrawerListener(toggle);
        this.mdrawer.addDrawerListener(new NavigationBarChangeListener());
        toggle.syncState();
        this.mfbSearch = (FloatingActionButton) findViewById(R.id.fbSearch);
        this.mfbSearch.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Utils.showSnackBar(R.string.search);
                return true;
            }
        });
        this.mfbSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showOrHideSearchView(true);
            }
        });
        refreshFloatingButton(false);
        this.llDirectory = (LinearLayout) findViewById(R.id.llDirectory);
        this.mpFragment = (AppBarLayout) findViewById(R.id.blMediaPlayerFragment);
        RelativeLayout rlDetails = (RelativeLayout) mpFragment.findViewById(R.id.rlResourceDetails);
        rlDetails.setPadding(rlDetails.getPaddingLeft(), 0, rlDetails.getPaddingRight(), rlDetails.getPaddingBottom());
        this.mtvMediaResName = (TextView) findViewById(R.id.tvResourceName);
        this.mtvMediaResComment = (TextView) findViewById(R.id.tvResourceComment);
        this.mtvMediaResComment.setTextColor(getResources().getColor(R.color.transparent_black));
        this.mivMediaResThumb = (ImageView) findViewById(R.id.ivResourceIcon);
        this.pbSeekMedia = (ProgressBar) findViewById(R.id.pbSeekMedia);
        this.fbPlayOrPause = (FloatingActionButton) findViewById(R.id.fbPlayOrPauseMedia);
        this.mtvEmptyFolder = (TextView) findViewById(R.id.tvEmptyFolder);
        mlcResourcesDir = (Interfaces.IListContent) findViewById(android.R.id.list);
        this.mnavMenuExplorer = (NavigationView) findViewById(R.id.nvServerDetails);
        this.mnavMenuExplorer.setNavigationItemSelectedListener(this);
        this.msvSearch = (SearchView) findViewById(R.id.svSearch);
        this.msvSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newPattern) {
                if (!newPattern.equals("") || getDirectory() instanceof SearchDirectory) {
                    try {
                        int indexLast = Utils.stackVars.size() - 1;
                        if (getDirectory() instanceof SearchDirectory) {
                            Utils.Variables var1 = Utils.stackVars.get(indexLast - 1),
                                    var2 = Utils.stackVars.get(indexLast);
                            if (var2.NameDirectory.equals(Utils.NAME_SEARCH))
                                var2.RelUrlDirectory = String.format("%s?%s=%s&%s=%s&%s=%s", Utils.RELURL_SEARCH,
                                        Utils.NAME_KEY, var1.NameDirectory, Utils.PATH_KEY,
                                        Resource.strEncode(var1.RelUrlDirectory),
                                        Utils.PATTERN_KEY, Resource.strEncode(newPattern.toLowerCase()));
                            ((SearchDirectory) getDirectory()).resetPattern(newPattern.toLowerCase(), handler);
                        } else {
                            if (indexLast > 0 && Utils.stackVars.get(indexLast).NameDirectory.equals(Utils.NAME_SEARCH)) {
                                Utils.stackVars.remove(indexLast);
                                indexLast--;
                            }
                            Utils.Variables var = Utils.stackVars.get(indexLast);
                            String searchRelUrl = String.format("%s?%s=%s&%s=%s&%s=%s", Utils.RELURL_SEARCH,
                                    Utils.NAME_KEY, var.NameDirectory, Utils.PATH_KEY,
                                    Resource.strEncode(var.RelUrlDirectory), Utils.PATTERN_KEY,
                                    Resource.strEncode(newPattern.toLowerCase()));
                            Utils.stackVars.add(new Utils.Variables(searchRelUrl, Utils.NAME_SEARCH, 0));
                            msrlRefresh.setRefreshing(false);
                            loadDirectory();
                        }
                    } catch (Exception ignored) {
                        ignored.printStackTrace();
                    }
                }
                return true;
            }
        });
        loadSwipe(true);
        loadServerData();
    }

    private void addNewFile() {
        if (getDirectory() instanceof LocalDirectory) {
            AddNewFileFragment rename = new AddNewFileFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Utils.RESOURCE_FOR_DETAILS_KEY, getDirectory());
            bundle.putInt(Utils.FIRST_POSITION, mlcResourcesDir.getFirstVisiblePosition());
            rename.setArguments(bundle);
            rename.show(getFragmentManager(), "jlab.AddNewFile");
        }
    }

    private void addNewFolder() {
        if (getDirectory() instanceof LocalDirectory) {
            AddNewFolderFragment addFragment = new AddNewFolderFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Utils.RESOURCE_FOR_DETAILS_KEY, getDirectory());
            bundle.putInt(Utils.FIRST_POSITION, mlcResourcesDir.getFirstVisiblePosition());
            addFragment.setArguments(bundle);
            addFragment.show(getFragmentManager(), "jlab.AddNewFolder");
        }
    }

    private void showDeleteFragment(Resource resource) {
        DeleteFragment deleteFragment = new DeleteFragment();
        Bundle args = new Bundle();
        args.putSerializable(Utils.RESOURCE_FOR_DELETE, resource);
        deleteFragment.setArguments(args);
        deleteFragment.show(getFragmentManager(), "jlab.Delete");
    }

    private void loadSwipe(boolean find) {
        if (find)
            this.msrlRefresh = (SwipeRefreshLayout) findViewById(R.id.srlRefresh);
        this.msrlRefresh.setColorSchemeResources(swipeColor);
        this.msrlRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reloadSpecialDir();
                msrlRefresh.setRefreshing(true);
                msrlRefresh.setVisibility(View.VISIBLE);
                handler.removeMessages(Utils.LOADING_VISIBLE);
                updateBeginPosition(0);
                loadDirectory();
            }
        });
    }

    private Directory getSpecialDirectory(String name) {
        Directory result = null;
        int theme = R.style.AppDefaultTheme, pathColor = R.color.accent, barColor = R.color.primary,
                progressDrawable = R.drawable.progressbar_primary, statusBarColor = R.color.primary_dark;
        if (Utils.stackVars.size() == 1) {
            if (name.equals(getString(R.string.downloads_folder))) {
                result = specialDirectories.getDownloadDirectory();
                mnavMenuExplorer.setCheckedItem(R.id.navDownloadVideos);
                toolbar.setTitle(R.string.downloads_folder);
            } else if (name.equals(getString(R.string.camera_folder))) {
                result = specialDirectories.getCameraDirectory();
                mnavMenuExplorer.setCheckedItem(R.id.navCameraVideos);
                toolbar.setTitle(R.string.camera_folder);
            }
            else if(name.equals(getString(R.string.favorite_folder)))
            {
                result = specialDirectories.getFavoritesDirectory();
                mnavMenuExplorer.setCheckedItem(R.id.navFavoriteVideos);
                toolbar.setTitle(R.string.favorite_folder);
            }
            else if(name.equals(getString(R.string.albums_folder)))
            {
                result = specialDirectories.getAlbumsDirectory();
                mnavMenuExplorer.setCheckedItem(R.id.navAlbumsVideos);
                toolbar.setTitle(R.string.albums_folder);
            }
            else {
                result = specialDirectories.getVideosDirectory();
                mnavMenuExplorer.setCheckedItem(R.id.navVideos);
                toolbar.setTitle(R.string.all_video);
            }
            theme = R.style.AppVideoTheme;
            barColor = R.color.red;
            pathColor = R.color.red_bright;
            statusBarColor = R.color.red_dark;
            progressDrawable = R.drawable.progressbar_red;
        }
        swipeColor = barColor;
        setTheme(theme);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(getResources().getColor(statusBarColor));
        toolbar.setBackgroundResource(barColor);
        mfbSearch.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(swipeColor)));
        pbSeekMedia.setProgressDrawable(getResources().getDrawable(progressDrawable));
        fbPlayOrPause.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(barColor)));
        msrlRefresh.setColorSchemeResources(swipeColor);
        return result;
    }

    private void loadServerData() {
        //TODO: implementar
    }

    @Override
    public void onFileClick(FileResource res, int position) {
        createOptionActivity(res, position);
    }

    @Override
    public void onDirectoryClick(String name, String relurlDir) {
        handler.sendEmptyMessage(Utils.SCROLLER_PATH);
    }

    private void refreshFloatingButton(boolean wait) {
        int size = Utils.stackVars.size();
        Directory dir = mlcResourcesDir != null ? getDirectory() : null;
        if (Utils.clipboardRes != null)
            toAddMode(false, wait);
        if (Utils.clipboardRes == null)
            toAddMode(true, wait);
    }

    private void toAddMode(final boolean addMode, final boolean wait) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (wait)
                    try {
                        Thread.sleep(TIME_WAIT_FBUTTON_ANIM);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
            }
        }).start();
    }

    @Override
    public boolean onResourceLongClick(final Resource resource, final int position) {
        CharSequence[] items = resource.isDir()
                ? new CharSequence[]{getString(R.string.open), getString(R.string.delete),
                getString(R.string.details)}
                : new CharSequence[]{getString(R.string.open), getString(R.string.share),
                getString(R.string.rename), getString(R.string.delete),
                getString(R.string.details)};
        final Context context = DirectoryActivity.this;
        new AlertDialog.Builder(this).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (resource.isDir() && i > 0)
                    i+=2;
                switch (i) {
                    case 0:
                        //Open
                        mlcResourcesDir.openResource(resource, position);
                        break;
                    case 1:
                        //Share
                        try {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType(((FileResource) resource).getMimeType());
                            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(resource.getAbsUrl()));
                            startActivity(Intent.createChooser(intent, getString(R.string.share)));
                        }catch (Exception ignored) {
                            ignored.printStackTrace();
                        }
                        break;
                    case 2:
                        //Rename
                        try {
                            ActionFragment rename = new RenameFragment();
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(Utils.RESOURCE_FOR_DETAILS_KEY, resource);
                            bundle.putInt(Utils.FIRST_POSITION, mlcResourcesDir.getFirstVisiblePosition());
                            rename.setArguments(bundle);
                            rename.show(getFragmentManager(), "jlab.Rename");
                        } catch (Exception exp) {
                            exp.printStackTrace();
                        }
                        break;
                    case 3:
                        //Delete
                        if (!DeleteFragment.isRunning())
                            new AlertDialog.Builder(context)
                                    .setTitle(R.string.question)
                                    .setMessage(String.format("%s %s: \"%s\"?", getString(R.string.delete_begin_question),
                                            resource.isDir()
                                                    ? getString(R.string.the_folder)
                                                    : getString(R.string.the_file),
                                            resource.getName()))
                                    .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            showDeleteFragment(resource);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    loadDirectory();
                                                }
                                            });
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, null)
                                    .create().show();
                        else
                            Utils.showSnackBar(R.string.wait_deleting);
                        break;
                    case 4:
                        //Details
                        try {
                            DetailsFragment details = new DetailsFragment();
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(Utils.RESOURCE_FOR_DETAILS_KEY, resource);
                            details.setArguments(bundle);
                            details.show(getFragmentManager(), "jlab.Details");
                        } catch (Exception exp) {
                            exp.printStackTrace();
                        }
                        break;
                }
            }
        }).show();
        return true;
    }

    @Override
    public View getView(ViewGroup parent, int position, boolean isDir) {
        View view = mlinflater.inflate(getDirectory().getResource(position) instanceof AlbumDirectory
                ? R.layout.album_details_grid
                : R.layout.resource_details_grid, parent, false);
        ViewGroup.LayoutParams newParams = view.getLayoutParams();
        newParams.height = iconSize;
        newParams.width = iconSize;
        view.setLayoutParams(newParams);
        return view;
    }

    @Override
    public void setView(View view, Resource resource, int position) {
        ImageView icon = (ImageView) view.findViewById(R.id.ivResourceIcon),
                ivfavorite = (ImageView) view.findViewById(R.id.ivFavorite);
        setDefaultView(icon, view, resource, mdMgr);
        if (!mlcResourcesDir.scrolling()) {
            if (resource.isDir()) {
                Directory dir = (Directory) resource;
                if (dir.getCountElements() > 0 && dir.getResource(0) instanceof FileResource)
                    loadThumbnailForFile((FileResource) dir.getResource(0), icon, ivfavorite, true, true);
            } else
                loadThumbnailForFile((FileResource) resource, icon, ivfavorite, true, false);
        }
    }

    private void setOnListeners() {
        DeleteFragment.setRefreshListener(new Interfaces.IRefreshListener() {
            @Override
            public void refresh() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshFloatingButton(false);
                        reloadSpecialDir();
                        updateBeginPosition(mlcResourcesDir.getFirstVisiblePosition());
                        loadDirectory();
                        if (deleteFragmentFinish != null) {
                            deleteFragmentFinish.refresh();
                            deleteFragmentFinish = null;
                        }
                    }
                });
            }
        });
        DeleteFragment.setDeleteRefresh(this);
        ActionFragment.setOnRefreshListener(new Interfaces.IRefreshListener() {
            @Override
            public void refresh() {
                reloadSpecialDir();
                updateBeginPosition(mlcResourcesDir.getFirstVisiblePosition());
                loadDirectory();
            }
        });
        mlcResourcesDir.setHandler(handler);
        mlcResourcesDir.setListeners(this);
        //No quitar
        mlcResourcesDir.loadItemClickListener();
        //.
    }

    private boolean isSpecialDirectory() {
        return !isRemoteDirectory && Utils.stackVars.size() == 1;
    }

    private int getDrawableForFile(FileResource file, boolean isAlbum) {
        if (file.isImage())
            return mlcResourcesDir.getNumColumns() > 1 ? R.drawable.img_image : R.drawable.icon_image;
        if (file.isAudio())
            return R.drawable.icon_audio;
        if (file.isVideo())
            return isAlbum ? R.drawable.img_album
                    : mlcResourcesDir.getNumColumns() > 1
                    ? R.drawable.img_video
                    : R.drawable.icon_video;
        return -1;
    }

    public void loadThumbnailForFile(final FileResource file, final ImageView ivIcon, final ImageView ivfavorite, boolean setBackground, boolean isAlbum) {
        if (!file.thumbLoaded && file.isThumbnailer()) {
            DownloadImageTask downloadImageTask = new DownloadImageTask(ivIcon, ivfavorite,
                    getDrawableForFile(file, isAlbum), setBackground, getDirectory().isMultiColumn());
            downloadImageTask.load(file.thumbUrl(), file.isRemote(), file, isAlbum);
            file.thumbLoaded = true;
        } else if (!isAlbum && !file.getFavoriteStateLoad() && ivfavorite != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final boolean isFavorite = Utils.isFavorite(file);
                    setImage(ivfavorite, isFavorite
                            ? R.drawable.img_favorite_checked
                            : R.drawable.img_favorite_not_checked);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ivfavorite.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (file.isFavorite())
                                        file.setIsFavorite(false, Utils.deleteFavoriteData(file.getIdFavorite()));
                                    else
                                        file.setIsFavorite(true, Utils.saveFavoriteData(new FavoriteDetails(file.getRelUrl(),
                                                file.getComment(), file.getParentName(), file.mSize, file.getModificationDate())));
                                    setImage(ivfavorite, file.isFavorite()
                                            ? R.drawable.img_favorite_checked
                                            : R.drawable.img_favorite_not_checked);
                                }
                            });
                            ivfavorite.setOnLongClickListener(new View.OnLongClickListener() {
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
            }).start();
        }
    }

    private void reloadDir(Directory directory) {
        if (directory.isMultiColumn()) {
            reload(R.layout.grid_view_directory, directory);
            mlcResourcesDir.setNumColumns(isPortrait ? 2 : 4);
        } else if (mlcResourcesDir.getNumColumns() > 1)
            reload(R.layout.list_view_directory, directory);
    }

    private void reload(int layout, Directory directory) {
        View view = View.inflate(this, layout, null);
        this.msrlRefresh = (SwipeRefreshLayout) view.findViewById(R.id.srlRefresh);
        llDirectory.addView(view, 0);
        view = this.msrlRefresh.findViewById(android.R.id.list);
        mlcResourcesDir = (Interfaces.IListContent) view;
        setOnListeners();
        mlcResourcesDir.setDirectory(directory);
        Utils.viewForSnack = view;
        loadSwipe(false);
    }

    private void loadingInvisible() {
        if (!isRemoteDirectory && this.mdrawer.isDrawerOpen(GravityCompat.START))
            updateLocalStorageDescription(false);
        mlcResourcesDir.loadContent();
        this.msrlRefresh.setRefreshing(false);
        int size = Utils.stackVars.size();
        this.handler.removeMessages(Utils.LOADING_VISIBLE);
        int pos = size == 0 ? 0 : Utils.stackVars.get(size - 1).BeginPosition;
        mlcResourcesDir.setSelection(pos);
        if (!Utils.lostConnection && mlcResourcesDir.isEmpty()) {
            this.mtvEmptyFolder.setText(R.string.empty_folder);
            this.mtvEmptyFolder.setVisibility(View.VISIBLE);
        } else if (Utils.lostConnection)
            lostConnection();
        loadServerData();
        invalidateOptionsMenu();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        DownloadImageTask.freeCache();
        Utils.freeUnusedMemory();
    }

    private void createOptionActivity(final FileResource file, final int position) {
        Intent mIntent = new Intent();
        mIntent.setAction(Intent.ACTION_VIEW);
        mIntent.setDataAndType(Uri.parse(file.getAbsUrl()), file.getMimeType());
        mIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mIntent.putExtra(Utils.INDEX_CURRENT_KEY, position);
        mIntent.putExtra(Utils.HOST_SERVER_KEY, Utils.hostServer);
        mIntent.putExtra(Utils.PORT_SERVER_KEY, Utils.portServer);
        startActivity(mIntent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STACK_VARS_KEY, Utils.stackVars);
        outState.putString(Utils.HOST_SERVER_KEY, Utils.hostServer);
        outState.putString(Utils.PORT_SERVER_KEY, Utils.portServer);
        outState.putString(NAME_DOWNLOAD_DIR_KEY, Utils.pathStorageDownload);
        outState.putBoolean(LOST_CONNECTION_KEY, Utils.lostConnection);
        outState.putBoolean(SHOW_HIDDEN_FILES_KEY, Utils.showHiddenFiles);
        outState.putBoolean(Utils.IS_REMOTE_DIRECTORY, this.isRemoteDirectory);
        outState.putString(Utils.RELATIVE_URL_DIRECTORY_ROOT, isRemoteDirectory ? "/" : Utils.STORAGE_DIRECTORY_PHONE);
        outState.putSerializable(Utils.CLIPBOARD_RESOURCE_KEY, Utils.clipboardRes);
        outState.putBoolean(Utils.IS_MOVING_RESOURCE_KEY, isMoving);
    }

    private void loadDirectory() {
        try {
            mutexLoadDirectory.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mlcResourcesDir.loadDirectory();
        Utils.viewForSnack = (View) mlcResourcesDir;
        mutexLoadDirectory.release();
    }

    private void showOrHideSearchView(boolean show) {
        if (show) {
            msvSearch.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    , ViewGroup.LayoutParams.WRAP_CONTENT));
            msvSearch.startAnimation(AnimationUtils.loadAnimation(this, R.anim.up_in));
            msvSearch.onActionViewExpanded();
        } else {
            msvSearch.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
            msvSearch.onActionViewCollapsed();
            if (!msvSearch.getQuery().equals(""))
                msvSearch.setQuery("", false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MediaPlayerActivity.isClosed() && MediaPlayerService.running()) {
            this.id = BackMediaPlayerReceiver.setCloseAndRefreshListener(this, this);
            MediaPlayerService.setRefreshListener(this);
            refresh(MediaPlayerService.getCurrentPlaying(), -1, MediaPlayerService.isPlaying());
        }
        Utils.viewForSnack = (View) mlcResourcesDir;
        Utils.currentActivity = this;
        refreshConfiguration();
        loadDirectory();
    }

    private void refreshConfiguration() {
        DisplayMetrics displayMetrics = Utils.getDimensionScreen();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        isPortrait = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180;
        this.iconSize = (displayMetrics.widthPixels / (isPortrait ? 2 : 4));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refreshConfiguration();
        if (getDirectory().isMultiColumn()) {
            mlcResourcesDir.setSelection(mlcResourcesDir.getFirstVisiblePosition());
            mlcResourcesDir.setNumColumns(isPortrait ? 2 : 4);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateBeginPosition(mlcResourcesDir.getFirstVisiblePosition());
    }

    @Override
    public void onBackPressed() {
        if (this.mdrawer.isDrawerOpen(GravityCompat.START))
            this.mdrawer.closeDrawer(GravityCompat.START);
        else {
            if (Utils.stackVars.size() == 1)
                super.onBackPressed();
            else {
                if (!Utils.stackVars.isEmpty())
                    handler.sendEmptyMessage(Utils.SCROLLER_PATH);
                mlcResourcesDir.loadParentDirectory();
            }
        }
    }

    private void updateBeginPosition(int position) {
        if (!Utils.stackVars.isEmpty())
            Utils.stackVars.get(Utils.stackVars.size() - 1).BeginPosition = position;
    }

    private void lostConnection() {
        this.msrlRefresh.setRefreshing(false);
        mtvEmptyFolder.setText(R.string.lost_connection);
        mtvEmptyFolder.setVisibility(View.VISIBLE);
    }

    private void loadFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(STACK_VARS_KEY)) {
            Utils.stackVars = savedInstanceState.getParcelableArrayList(STACK_VARS_KEY);
            Utils.hostServer = savedInstanceState.getString(Utils.HOST_SERVER_KEY);
            Utils.portServer = savedInstanceState.getString(Utils.PORT_SERVER_KEY);
            Utils.urlServer = String.format("http://%s:%s", Utils.hostServer, Utils.portServer);
            Utils.pathStorageDownload = savedInstanceState.getString(NAME_DOWNLOAD_DIR_KEY);
            Utils.lostConnection = savedInstanceState.getBoolean(LOST_CONNECTION_KEY);
            Utils.showHiddenFiles = savedInstanceState.getBoolean(SHOW_HIDDEN_FILES_KEY);
            Utils.clipboardRes = (Resource) savedInstanceState.getSerializable(Utils.CLIPBOARD_RESOURCE_KEY);
            isMoving = savedInstanceState.getBoolean(Utils.IS_MOVING_RESOURCE_KEY);
        }
    }

    private void setDefaultView(ImageView rico, View view, final Resource resource, DownloadManager dMgr) {
        final TextView rname = (TextView) view.findViewById(R.id.tvResourceName),
                rsize = (TextView) view.findViewById(R.id.tvResourceComment),
                mcomment = (TextView) view.findViewById(R.id.tvContentComment);
        ImageDownload btdown = (ImageDownload) view.findViewById(R.id.ivDownload);
        final ImageView ivfavorite = (ImageView) view.findViewById(R.id.ivFavorite);
        if (!resource.isDir()) {
            final FileResource file = (FileResource) resource;
            if (file.getFavoriteStateLoad()) {
                ivfavorite.setImageResource(file.isFavorite() ? R.drawable.img_favorite_checked : R.drawable.img_favorite_not_checked);
                ivfavorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (file.isFavorite())
                            file.setIsFavorite(false, Utils.deleteFavoriteData(file.getIdFavorite()));
                        else
                            file.setIsFavorite(true, Utils.saveFavoriteData(new FavoriteDetails(file.getRelUrl(),
                                    file.getComment(), file.getParentName(), file.mSize, file.getModificationDate())));
                        ivfavorite.setImageResource(file.isFavorite()
                                ? R.drawable.img_favorite_checked
                                : R.drawable.img_favorite_not_checked);
                    }
                });
                ivfavorite.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Utils.showSnackBar(file.isFavorite()
                                ? R.string.remove_of_favorite_folder
                                : R.string.add_to_favorite_folder);
                        return true;
                    }
                });
            }
        }
        if (resource.getComment() != null && mcomment != null)
            mcomment.setText(resource.getComment());
        else if (mcomment != null)
            mcomment.setVisibility(View.INVISIBLE);
        if (getDirectory() instanceof SearchDirectory) {
            BackgroundColorSpan colorSpan = new BackgroundColorSpan(getResources().getColor(R.color.red_bright));
            String pattern = ((SearchDirectory) getDirectory()).getPattern();
            SpannableStringBuilder textBd = new SpannableStringBuilder(resource.getName());
            textBd.setSpan(colorSpan, resource.getIndexPattern(), resource.getIndexPattern() + pattern.length(), 0);
            Selection.selectAll(textBd);
            rname.setText(textBd);
        } else
            rname.setText(resource.getName());
        if (!resource.isDir()) {
            if (rsize != null)
                rsize.setText(((FileResource) resource).sizeToString());
            if (btdown != null && isRemoteDirectory) {
                btdown.dMgr = dMgr;
                btdown.resource = (RemoteFile) resource;
            }
        } else if (rsize != null)
            rsize.setText(R.string.folder);
        setImageThumbnail(resource, rico);
    }

    public void setImageThumbnail(Resource res, ImageView rico) {
        if (res.isDir()) {
            Directory directory = (Directory) res;
            if (directory.getCountElements() > 0 && !directory.getResource(0).isDir()) {
                Bitmap bm = DownloadImageTask.get(((FileResource) directory.getResource(0)).thumbUrl());
                if (bm != null) {
                    rico.setImageBitmap(bm);
                    ((FileResource) directory.getResource(0)).thumbLoaded = true;
                }
                else {
                    rico.setImageResource(R.drawable.img_album);
                    ((FileResource) directory.getResource(0)).thumbLoaded = false;
                }
            } else
                rico.setImageResource(R.drawable.img_album);
        } else {
            FileResource fres = (FileResource) res;
            fres.thumbLoaded = false;
            String ext = fres.getExtension();
            switch (ext) {
                case "jpg":
                case "png":
                case "bmp":
                case "jpeg":
                case "ico":
                case "jpe":
                case "jfi":
                case "jfif":
                case "dib":
                case "jif":
                case "apng":
                case "gif":
                    Bitmap bm = DownloadImageTask.get(fres.thumbUrl());
                    if (bm != null) {
                        rico.setImageBitmap(bm);
                        fres.thumbLoaded = true;
                    } else if (mlcResourcesDir.getNumColumns() > 1)
                        rico.setImageResource(R.drawable.img_image);
                    else
                        rico.setImageResource(R.drawable.icon_image);
                    break;
                case "mpg":
                case "avi":
                case "mkv":
                case "rmvb":
                case "wmv":
                case "mp4":
                case "ogm":
                case "vob":
                case "mov":
                case "mpa":
                case "mpe":
                case "mpeg":
                case "flv":
                case "3gp":
                case "webm":
                    bm = DownloadImageTask.get(fres.thumbUrl());
                    if (bm != null) {
                        fres.thumbLoaded = true;
                        Utils.setBackground(rico, bm, mlcResourcesDir.getNumColumns() > 1
                                ? R.drawable.img_very_small_play_video
                                : R.drawable.img_small_play_video);
                    } else if (mlcResourcesDir.getNumColumns() > 1)
                        rico.setImageResource(R.drawable.img_video);
                    else
                        rico.setImageResource(R.drawable.icon_video);
                    break;
                case "mp3":
                case "ogg":
                case "wma":
                case "mp2":
                case "m4a":
                case "wav":
                    bm = DownloadImageTask.get(fres.thumbUrl());
                    if (bm != null) {
                        fres.thumbLoaded = true;
                        Utils.setBackground(rico, bm, mlcResourcesDir.getNumColumns() > 1
                                ? R.drawable.img_very_small_audio
                                : R.drawable.img_small_audio);
                    } else
                        rico.setImageResource(R.drawable.icon_audio);
                    break;
                default:
                    break;
            }
        }
        if (!isRemoteDirectory && res.isHidden())
            rico.setAlpha(Utils.ALPHA_HIDDEN_FILES);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        String nameDirSpecial = "";
        if (id == R.id.navVideos)
            nameDirSpecial = getString(R.string.all_video);

        isRemoteDirectory = false;
        Utils.stackVars.clear();
        if (id == R.id.navVideos) {
            Utils.stackVars.add(new Utils.Variables(Utils.RELURL_SPECIAL_DIR, nameDirSpecial, 0));
            this.toolbar.setTitle(R.string.all_video);
        }
        else if(id == R.id.navCameraVideos) {
            Utils.stackVars.add(new Utils.Variables(Utils.RELURL_SPECIAL_DIR, getString(R.string.camera_folder), 0));
            this.toolbar.setTitle(R.string.camera_folder);
        }
        else if(id == R.id.navDownloadVideos) {
            Utils.stackVars.add(new Utils.Variables(Utils.RELURL_SPECIAL_DIR, getString(R.string.downloads_folder), 0));
            this.toolbar.setTitle(R.string.downloads_folder);
        }
        else if(id == R.id.navFavoriteVideos)
        {
            Utils.stackVars.add(new Utils.Variables(Utils.RELURL_SPECIAL_DIR, getString(R.string.favorite_folder), 0));
            this.toolbar.setTitle(R.string.favorite_folder);
        }
        else if(id == R.id.navAlbumsVideos)
        {
            Utils.stackVars.add(new Utils.Variables(Utils.RELURL_SPECIAL_DIR, getString(R.string.albums_folder), 0));
            this.toolbar.setTitle(R.string.albums_folder);
        }
        this.mdrawer.closeDrawer(GravityCompat.START);
        invalidateOptionsMenu();
        ServerDbManager manager = new ServerDbManager(getApplicationContext());
        loadServerData();
        manager.close();
        msrlRefresh.setRefreshing(false);
        loadDirectory();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.getItem(0).setTitle(Utils.showHiddenFiles ? R.string.hidden_hidden_files : R.string.show_hidden_files);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnShowHiddenFiles:
                Utils.showHiddenFiles = !Utils.showHiddenFiles;
                item.setTitle(Utils.showHiddenFiles ? R.string.hidden_hidden_files : R.string.show_hidden_files);
                updateBeginPosition(0);
                if (this.mdrawer.isDrawerOpen(GravityCompat.START))
                    updateLocalStorageDescription(true);
                else {
                    reloadSpecialDir();
                    loadDirectory();
                }
                break;
            case R.id.mnRateApp:
                try {
                    Utils.rateApp();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
                break;
            case R.id.mnAbout:
                Utils.showAboutDialog();
                break;
            case R.id.mnClose:
                finish();
                break;
        }
        return true;
    }

    public static Directory getDirectory() {
        return mlcResourcesDir.getDirectory();
    }

    @Override
    public void refresh(Runnable run) {
        runOnUiThread(run);
    }

    private void updateLocalStorageDescription(final boolean loadDir) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mutexLoadDataSpecial.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                reloadSpecialDir();
                specialDirectories.loadData();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final Menu menu = mnavMenuExplorer.getMenu();
                        for (int i = 0; i < menu.size(); i++) {
                            MenuItem elem = menu.getItem(i);
                            TextView tv = (TextView) elem.getActionView().findViewById(R.id.tvMenuCount);
                            switch (elem.getItemId()) {
                                case R.id.navVideos:
                                    tv.setText(String.valueOf(specialDirectories.getVideosDirDetails().getCountElements()));
                                    break;
                                case R.id.navAlbumsVideos:
                                    tv.setText(String.valueOf(specialDirectories.getAlbumsDirDetails().getCountElements()));
                                    break;
                                case R.id.navFavoriteVideos:
                                    tv.setText(String.valueOf(specialDirectories.getFavoritesDirDetails().getCountElements()));
                                    break;
                                case R.id.navCameraVideos:
                                    tv.setText(String.valueOf(specialDirectories.getCameraDirDetails().getCountElements()));
                                    break;
                                case R.id.navDownloadVideos:
                                    tv.setText(String.valueOf(specialDirectories.getDownloadDirDetails().getCountElements()));
                                    break;
                            }
                        }
                        if(loadDir)
                            loadDirectory();
                    }
                });
                mutexLoadDataSpecial.release();
            }
        }).start();
    }

    @Override
    public void refresh(final FileResource resource, final int position, final boolean isplaying) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mpFragment.getLayoutParams().height == 0) {
                    mpFragment.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                            , (int)getResources().getDimension(R.dimen.tool_bar_audio_height)));
                    mpFragment.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.bottom_in));
                    mpFragment.post(new Runnable() {
                        @Override
                        public void run() {
                            seekUp(1);
                        }
                    });
                    mpFragment.findViewById(R.id.ivDownload).setVisibility(View.INVISIBLE);
                    mpFragment.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                startActivity(MediaPlayerService.getMediaIntent());
                            } catch (Exception ignored) {
                                ignored.printStackTrace();
                            }
                        }
                    });
                    fbPlayOrPause.setOnClickListener(new View.OnClickListener() {
                        private Toast toast;
                        @Override
                        public void onClick(View view) {
                            if (MediaPlayerService.isError()) {
                                fbPlayOrPause.setImageResource(android.R.drawable.ic_media_play);
                                if (toast == null)
                                    toast = Toast.makeText(getBaseContext(), R.string.dont_start, Toast.LENGTH_LONG);
                                toast.show();
                            } else {
                                if (toast != null)
                                    toast.cancel();
                                boolean play = MediaPlayerService.isPlaying();
                                if (play && MediaPlayerService.pause())
                                    fbPlayOrPause.setImageResource(android.R.drawable.ic_media_play);
                                else if (!play && MediaPlayerService.play())
                                    fbPlayOrPause.setImageResource(android.R.drawable.ic_media_pause);
                            }
                        }
                    });
                    if (seekListener != null)
                        seekListener.setFinish();
                    seekListener = new SeekListener();
                }
                fbPlayOrPause.setImageResource(isplaying ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play);
                mtvMediaResName.setText(resource.getName());
                Bitmap bmAlbumThumbnail = MediaPlayerService.getThumbBack();
                if (bmAlbumThumbnail != null)
                    Utils.setBackground(mivMediaResThumb, bmAlbumThumbnail, MediaPlayerService.getThumbFront());
                else
                    Utils.setBackground(mivMediaResThumb, R.color.white, resource.isAudio()
                            ? R.drawable.icon_audio : R.drawable.icon_video);
            }
        });
    }

    private void seekUp(int mult) {
        int seek = mult * mpFragment.getHeight();
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mfbSearch.getLayoutParams();
        params.bottomMargin += seek;
        mfbSearch.setLayoutParams(params);
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void close() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mpFragment != null && mpFragment.getHeight() != 0) {
                    mpFragment.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
                    seekUp(-1);
                }
                if (seekListener != null)
                    seekListener.setFinish();
            }
        });
    }

    @Override
    public void loaded(FileResource fsFile) {
        refresh(fsFile, 0, MediaPlayerService.isPlaying());
    }

    @Override
    public void refresh() {
        refresh(MediaPlayerService.getCurrentPlaying(), 0, MediaPlayerService.isPlaying());
    }

    class NavigationBarChangeListener implements DrawerLayout.DrawerListener {
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {

        }

        @Override
        public void onDrawerOpened(View drawerView) {
            updateLocalStorageDescription(false);
        }

        @Override
        public void onDrawerClosed(View drawerView) {

        }

        @Override
        public void onDrawerStateChanged(int newState) {

        }
    }

    private class SeekListener {
        private boolean finish = false;

        private SeekListener() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!finish && MediaPlayerService.running()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int duration = MediaPlayerService.getDuration(),
                                        current = MediaPlayerService.getCurrentPosition();
                                pbSeekMedia.setMax(duration);
                                pbSeekMedia.setProgress(current);
                                mtvMediaResComment.setText(String.format("%s / %s",
                                        Utils.getDurationString(current), Utils.getDurationString(duration)));
                            }
                        });
                        try {
                            Thread.sleep(TIME_WAIT_SEEK);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        public void setFinish() {
            this.finish = true;
        }
    }
}
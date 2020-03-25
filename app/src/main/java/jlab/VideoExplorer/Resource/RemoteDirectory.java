package jlab.VideoExplorer.Resource;

import java.net.URL;
import android.os.Handler;
import org.json.JSONObject;
import java.io.IOException;
import org.json.JSONException;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import jlab.VideoExplorer.Utils;
import java.util.concurrent.Semaphore;
import jlab.VideoExplorer.Service.DeleteResourceService;

public class RemoteDirectory extends Directory {
    private static Semaphore semaphore = new Semaphore(1);

    public RemoteDirectory(String name, String relUrl, String comment) {
        super(name, relUrl, comment, 0, false);
        this.isRemote = true;
    }

    public void openSynchronic(final Handler handler) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            if (!loaded) {
                final URL mUrl = new URL(getAbsUrl());
                final URLConnection conn = mUrl.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                fillContent(reader);
            }
            Utils.lostConnection = false;
        } catch (Exception e) {
            Utils.lostConnection = true;
            e.printStackTrace();
        } finally {
            synchronized (monitor) {
                loaded = true;
                if (handler != null)
                    handler.sendEmptyMessage(Utils.LOADING_INVISIBLE);
            }
        }
        semaphore.release();
    }

    private void fillContent(BufferedReader reader) throws IOException, JSONException {
        String line;
        clear();
        while ((line = reader.readLine()) != null) {
            JSONObject jsonRes = new JSONObject(line);
            String name = jsonRes.getString("n"),
                    relUrl = this.getRelUrl() + name;

            addResource(jsonRes.getBoolean("d") ?
                    new RemoteDirectory(name, relUrl + '/', null) :
                    new RemoteFile(name, relUrl, jsonRes.getLong("s")));
        }
    }

    @Override
    public boolean delete(DeleteResourceService.OnDeleteListener deleteListener) {
        return false;
    }

    @Override
    public boolean renameTo(String newName) {
        return false;
    }

    @Override
    public boolean newFile(String name, boolean addToContent) {
        return false;
    }

    @Override
    public boolean newFolder(String name) {
        return false;
    }
}

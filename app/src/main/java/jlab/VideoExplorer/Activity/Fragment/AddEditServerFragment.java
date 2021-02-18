package jlab.VideoExplorer.Activity.Fragment;

import android.os.Bundle;
import android.view.View;
import java.util.ArrayList;
import android.widget.Button;
import android.widget.Spinner;
import jlab.VideoExplorer.R;
import android.content.Context;
import android.widget.EditText;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import jlab.VideoExplorer.Utils;
import android.content.DialogInterface;
import jlab.VideoExplorer.Interfaces;
import androidx.appcompat.app.AlertDialog;
import jlab.VideoExplorer.db.ServerDetails;
import jlab.VideoExplorer.db.ServerDbManager;

public class AddEditServerFragment extends DialogFragment {

    private boolean isNewServer;
    private EditText ethost;
    private EditText etport;
    private Spinner btDownloadPath;
    private EditText etcomment;
    private LayoutInflater inflater;
    private static final String IS_NEW_SERVER_KEY = "IS_NEW_SERVER_KEY";
    public static Interfaces.IRefreshListener monRefreshListener = new Interfaces.IRefreshListener() {
        @Override
        public void refresh() {

        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setCancelable(false);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    public void setIsNewServer(boolean isNewServer) {
        this.isNewServer = isNewServer;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_NEW_SERVER_KEY, this.isNewServer);
    }

    @Override
    public AlertDialog onCreateDialog(Bundle saveInstance) {
        if (saveInstance != null && saveInstance.containsKey(IS_NEW_SERVER_KEY))
            this.isNewServer = saveInstance.getBoolean(IS_NEW_SERVER_KEY);
        final ServerDbManager serverDbManager = new ServerDbManager(getActivity());
        Bundle args = getArguments();
        final ServerDetails serverDetails = args.getParcelable(Utils.SERVER_DATA_KEY);


        inflater = LayoutInflater.from(getActivity().getBaseContext());
        final View fragmentView = inflater.inflate(R.layout.add_edit_server, null, false);

        ethost = (EditText) fragmentView.findViewById(R.id.etHostServer);
        btDownloadPath = (Spinner) fragmentView.findViewById(R.id.ibDownloadPath);
        //etport = (EditText) fragmentView.findViewById(R.id.etPortServer);
        etcomment = (EditText) fragmentView.findViewById(R.id.etComment);

        final Context context = inflater.getContext();
        ArrayList<String> storagePaths = Utils.getStoragesPath();
        storagePaths = getArrayListItems(storagePaths);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.text_view);
        adapter.addAll(storagePaths);
        btDownloadPath.setAdapter(adapter);

        if (serverDetails != null) {
            ethost.setText(serverDetails.getHost());
            //etport.setText(serverDetails.getPort());
            btDownloadPath.setSelection(getIndexItems(storagePaths, serverDetails.getPathDownload()), true);
            etcomment.setText(serverDetails.getComment());
        }

        AlertDialog.Builder adbuilder = new AlertDialog.Builder(inflater.getContext())
                .setView(fragmentView)
                .setNegativeButton(R.string.bt_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                        monRefreshListener.refresh();
                    }
                }).setPositiveButton(R.string.bt_connect, null);
        final AlertDialog result = adbuilder.create();
        result.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button save = result.getButton(AlertDialog.BUTTON_POSITIVE);
                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (serverDetails != null && validate(serverDbManager, serverDetails.getId())) {
                            serverDetails.setHost(ethost.getText().toString());
                            //serverDetails.setPort(etport.getText().toString());
                            serverDetails.setPort(Utils.portServer);
                            serverDetails.setPathDownload(btDownloadPath.getSelectedItem().toString().trim());
                            serverDetails.setComment(etcomment.getText().toString().trim()
                                    .replaceAll("(\\s)+", " ").replaceAll("(\n)+", " "));
                            if (isNewServer)
                                serverDbManager.saveServerData(serverDetails);
                            else
                                serverDbManager.updateServerData(serverDetails.getId(), serverDetails);

                            dismiss();
                            monRefreshListener.refresh();
                        }
                    }
                });
            }
        });
        return result;
    }

    private int getIndexItems(ArrayList<String> storagePaths, String pathDownload) {
        for (int i = 0; i < storagePaths.size(); i++)
            if (pathDownload.equals(storagePaths.get(i)))
                return i;
        return 0;
    }

    private ArrayList<String> getArrayListItems(ArrayList<String> storagePaths) {
        for (int i = 0; i < storagePaths.size(); i++) {
            if (!Utils.existAndMountDir(storagePaths.get(i))) {
                storagePaths.remove(i);
                i--;
            }
        }
        return storagePaths;
    }

    private boolean validate(ServerDbManager serverDbManager, int id) {
        ethost.setText(ethost.getText().toString().trim());
        //etport.setText(etport.getText().toString().trim());
        etcomment.setText(etcomment.getText().toString().trim());

        boolean result = true;

//        if(etport.length() == 0) {
//            etport.setError(getString(R.string.port_can_not_empty));
//            etport.requestFocus();
//            result = false;
//        }
//
//        if(etport.length() == 0) {
//            etport.setError(getString(R.string.port_can_not_empty));
//            etport.requestFocus();
//            result = false;
//        }
//        else if(Integer.parseInt(etport.getText().toString()) > Utils.MAX_PORT_VALUE)
//        {
//            etport.setError(getString(R.string.port_interval));
//            etport.requestFocus();
//            result = false;
//        }

        String host = ethost.getText().toString();
        if (host.length() == 0) {
            ethost.setError(getString(R.string.ip_url_can_not_empty));
            ethost.requestFocus();
            result = false;
        } else {
            ArrayList<ServerDetails> servers = serverDbManager.getServersData();
            for (int i = 0; i < servers.size(); i++) {
                ServerDetails current = servers.get(i);
                if (!isNewServer && current.getId() == id)
                    continue;
                if (current.getHost().equals(host)) {
                    ethost.setError(getString(R.string.exist_server_dialog));
                    ethost.requestFocus();
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
}
package jlab.VideoExplorer.Activity.Fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.text.Spanned;
import android.widget.Button;
import android.content.Context;
import android.widget.EditText;
import jlab.VideoExplorer.R;
import android.text.InputFilter;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import jlab.VideoExplorer.Utils;
import android.content.DialogInterface;
import jlab.VideoExplorer.Interfaces;
import androidx.appcompat.app.AlertDialog;
import jlab.VideoExplorer.Resource.Resource;


public abstract class ActionFragment extends DialogFragment {

    private int firstVisiblePosition;
    protected Resource resource;
    protected String title;
    protected String hint;

    private static Interfaces.IRefreshListener monRefreshListener = new Interfaces.IRefreshListener() {
        @Override
        public void refresh() {

        }
    };

    public static void setOnRefreshListener(Interfaces.IRefreshListener newListener) {
        monRefreshListener = newListener;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setCancelable(false);
        this.firstVisiblePosition = getArguments().getInt(Utils.FIRST_POSITION);
        this.resource = (Resource) getArguments().getSerializable(Utils.RESOURCE_FOR_DETAILS_KEY);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public AlertDialog onCreateDialog(Bundle saveInstance) {
        Context context = getActivity().getBaseContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.name_resource_dialog, null, false);
        final EditText editText = (EditText) view.findViewById(R.id.etResourceNewNameDialog);
        editText.setText(this.hint);
        editText.selectAll();
        final AlertDialog result = new AlertDialog.Builder(inflater.getContext())
                .setTitle(this.title)
                .setCancelable(false)
                .setPositiveButton(R.string.accept, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(view)
                .create();
        result.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                editText.getEditableText().setFilters(new InputFilter[]{new InputFilter() {
                    public Toast toast = Toast.makeText(inflater.getContext(),
                            String.format("%s (? | * \\ / < > : \")", getString(R.string.invalid_chars_error))
                            , Toast.LENGTH_LONG);

                    @Override
                    public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) {
                        if (!Utils.isValidFileName(charSequence.toString())) {
                            toast.show();
                            return "";
                        }
                        return null;
                    }
                }});
                Button posButton = result.getButton(AlertDialog.BUTTON_POSITIVE);
                posButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String name = editText.getText().toString();
                        if(!name.equals("")) {
                            applyAction(name);
                            Utils.stackVars.get(Utils.stackVars.size() - 1).BeginPosition = firstVisiblePosition;
                            dismiss();
                            monRefreshListener.refresh();
                        }
                    }
                });
            }
        });
        return result;
    }

    protected abstract void applyAction(String name);
}
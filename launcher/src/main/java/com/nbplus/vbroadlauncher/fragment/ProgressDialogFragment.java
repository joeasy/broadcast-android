package com.nbplus.vbroadlauncher.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.widget.ProgressDialog;

/**
 * Created by basagee on 2015. 6. 23..
 */
public class ProgressDialogFragment extends DialogFragment {

    public static ProgressDialogFragment newInstance() {
        ProgressDialogFragment frag = new ProgressDialogFragment ();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setContentView(R.layout.dialogfragment_progress);
        //dialog.setIndeterminate(true);
        dialog.setCancelable(false);

        this.setCancelable(false);

        // Disable the back button
        DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode,
                                 KeyEvent event) {

                if( keyCode == KeyEvent.KEYCODE_BACK){
                    return true;
                }
                return false;
            }


        };
        dialog.setOnKeyListener(keyListener);
        return dialog;
    }

}

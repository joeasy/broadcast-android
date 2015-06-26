package com.nbplus.vbroadlistener;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by basagee on 2015. 6. 24..
 */
public class BaseActivity extends AppCompatActivity {

    public void showNetworkConnectionAlertDialog() {
        new AlertDialog.Builder(this).setMessage(R.string.alert_network_message)
                //.setTitle(R.string.alert_network_title)
                .setCancelable(true)
                .setPositiveButton(R.string.alert_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                finish();;
                            }
                        })
                .show();
    }
}

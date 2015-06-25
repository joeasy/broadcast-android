package com.nbplus.vbroadlauncher;

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
                .setNegativeButton(R.string.alert_network_btn_check_wifi,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                            }
                        })
                .setPositiveButton(R.string.alert_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                            }
                        })
                .show();
    }

    public void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }
}

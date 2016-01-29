package com.nbplus.vbroadcreator;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    BroadcastWebViewClient mWebViewClient;

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (mWebViewClient != null && mWebViewClient.getWebView() != null) {
//            mWebViewClient.onBackPressed();
            if (mWebViewClient.getWebView().canGoBack()) {
                mWebViewClient.getWebView().goBack();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Constant.USE_WEBVIEW) {
            setContentView(R.layout.activity_main);
            Log.d(TAG, "WebViewActivity onCreate()");
            WebView webView = (WebView)findViewById(R.id.webview);
            mWebViewClient = new BroadcastWebViewClient(this, webView);

            mWebViewClient.loadWebUrl(Constant.BROADCAST_CREATOR_SELECT_SERVER);
        } else {
            //setContentView(R.layout.activity_main);
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setPackage("com.android.chrome")           // open only chrome
                        .setData(Uri.parse(Constant.BROADCAST_CREATOR_SELECT_SERVER));
                startActivity(intent);
                finish();
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
                alert.setNegativeButton(R.string.alert_market, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=com.android.chrome"));
                        startActivity(intent);
                        finish();
                    }
                });
                alert.setMessage(R.string.activity_not_found_exception);
                alert.show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}

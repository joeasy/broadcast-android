package com.nbplus.iotgateway;

import android.content.Intent;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.nbplus.progress.ProgressDialogFragment;

/**
 * Created by basagee on 2015. 6. 24..
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = BaseActivity.class.getSimpleName();

    ProgressDialogFragment mProgressDialogFragment;

    public void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    public void showProgressDialog() {
        dismissProgressDialog();
        mProgressDialogFragment = ProgressDialogFragment.newInstance();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(mProgressDialogFragment, "iot_gw_progress_dialog");
        transaction.commitAllowingStateLoss();
    }
    public void dismissProgressDialog() {
        if (mProgressDialogFragment != null) {
            mProgressDialogFragment.dismiss();
            mProgressDialogFragment = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        dismissProgressDialog();
    }
}

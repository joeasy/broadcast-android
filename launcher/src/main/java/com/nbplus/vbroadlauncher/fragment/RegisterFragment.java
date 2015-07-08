package com.nbplus.vbroadlauncher.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.nbplus.vbroadlauncher.HomeLauncherActivity;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.hybrid.RegisterWebViewClient;

import org.basdroid.common.DeviceUtils;
import org.basdroid.common.StringUtils;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link RegisterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RegisterFragment extends Fragment implements OnActivityInteractionListener {
    private static final String TAG = RegisterFragment.class.getSimpleName();

    RegisterWebViewClient mWebViewClient;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RegisterFragment.
     */
    public static RegisterFragment newInstance(String param1, String param2) {
        RegisterFragment fragment = new RegisterFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getActivity().setTitle("RegisterFragment");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.activity_broadcast_webview, container, false);

        WebView webView = (WebView)v.findViewById(R.id.webview);
        mWebViewClient = new RegisterWebViewClient(getActivity(), webView);
        mWebViewClient.setBackgroundTransparent();

        String url = LauncherSettings.getInstance(getActivity()).getRegisterAddress();
        if (url.indexOf("?") > 0) {
            url += ("&UUID=" + DeviceUtils.getDeviceIdByMacAddress(getActivity()));
            url += ("&APPID=" + getActivity().getApplicationContext().getPackageName());
        } else {
            url += ("?UUID=" + DeviceUtils.getDeviceIdByMacAddress(getActivity()));
            url += ("&APPID=" + getActivity().getApplicationContext().getPackageName());
        }
        mWebViewClient.loadUrl(url);

        // test view
        final EditText editText = (EditText)v.findViewById(R.id.et_test_url);
        editText.setText(url);
        Button button = (Button)v.findViewById(R.id.btn_test_load);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = editText.getText().toString();
                if (StringUtils.isEmptyString(url)) {
                    return;
                }
                if (url.indexOf("?") > 0) {
                    if (!url.contains("UUID=")) {
                        url += ("&UUID=" + DeviceUtils.getDeviceIdByMacAddress(getActivity()));
                    }
                    if (!url.contains("APPID=")) {
                        url += ("&APPID=" + getActivity().getApplicationContext().getPackageName());
                    }
                } else {
                    if (!url.contains("UUID=")) {
                        url += ("?UUID=" + DeviceUtils.getDeviceIdByMacAddress(getActivity()));
                    }
                    if (!url.contains("APPID=")) {
                        url += ("&APPID=" + getActivity().getApplicationContext().getPackageName());
                    }
                }
                mWebViewClient.loadUrl(url);
            }
        });
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            ((HomeLauncherActivity)getActivity()).registerActivityInteractionListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        ((HomeLauncherActivity)getActivity()).unRegisterActivityInteractionListener(this);
        super.onDetach();
    }

    @Override
    public void onBackPressed() {
        if (this.mWebViewClient != null) {
            mWebViewClient.onBackPressed();
        }
    }

    @Override
    public void onDataChanged() {

    }

    @Override
    public void onPushReceived(Intent intent) {

    }
}

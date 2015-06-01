package com.nbplus.vbroadlauncher.fragment;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
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
import com.nbplus.vbroadlauncher.callback.OnFragmentInteractionListener;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.hybrid.RegisterWebViewClient;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RegisterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RegisterFragment extends Fragment implements OnActivityInteractionListener {
    private static final String TAG = RegisterFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;

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
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getActivity().setTitle("RegisterFragment");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_register, container, false);

        WebView webView = (WebView)v.findViewById(R.id.webview);
        mWebViewClient = new RegisterWebViewClient(getActivity(), webView);
        mWebViewClient.setBackgroundTransparent();

        mWebViewClient.loadUrl(LauncherSettings.getInstance(getActivity()).getRegisterAddress());

        // test view
        final EditText editText = (EditText)v.findViewById(R.id.et_test_url);
        Button button = (Button)v.findViewById(R.id.btn_test_load);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = editText.getText().toString();
                mWebViewClient.loadUrl(url);
            }
        });
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
            ((HomeLauncherActivity)activity).setOnActivityInteractionListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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

}

package com.nbplus.vbroadlauncher.fragment;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.nbplus.vbroadlauncher.BaseActivity;
import com.nbplus.vbroadlauncher.HomeLauncherActivity;
import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.callback.OnLauncherFragmentInteractionListener;
import com.nbplus.vbroadlauncher.data.Constants;
import com.nbplus.vbroadlauncher.data.LauncherSettings;
import com.nbplus.vbroadlauncher.data.PushPayloadData;
import com.nbplus.vbroadlauncher.hybrid.BroadcastWebViewClient;
import com.nbplus.vbroadlauncher.hybrid.RegisterWebViewClient;
import com.nbplus.vbroadlauncher.hybrid.TextToSpeechHandler;

import org.basdroid.common.DeviceUtils;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link LauncherBroadcastFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LauncherBroadcastFragment extends Fragment implements TextToSpeechHandler.OnUtteranceProgressListener {
    private static final String TAG = LauncherBroadcastFragment.class.getSimpleName();

    PushPayloadData mBroadcastData;
    OnLauncherFragmentInteractionListener mActivityInteractionListener;

    // for audio broadcast
    WebView mWebView;
    BroadcastWebViewClient mWebViewClient;

    // for text broadcast
    TextView mTextView;
    TextToSpeechHandler mText2SpeechHandler;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param data Parameter 1.
     * @return A new instance of fragment RegisterFragment.
     */
    public static LauncherBroadcastFragment newInstance(PushPayloadData data) {
        LauncherBroadcastFragment fragment = new LauncherBroadcastFragment();
        Bundle args = new Bundle();
        args.putParcelable(Constants.EXTRA_BROADCAST_PAYLOAD_DATA, data);
        fragment.setArguments(args);
        return fragment;
    }

    public LauncherBroadcastFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBroadcastData = getArguments().getParcelable(Constants.EXTRA_BROADCAST_PAYLOAD_DATA);
        }
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getActivity().setTitle("RegisterFragment");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = null;

        if (mBroadcastData == null) {
            popStack();
            return null;
        }

        if (Constants.PUSH_PAYLOAD_TYPE_TEXT_BROADCAST.equals(mBroadcastData.getServiceType())) {
            // 문자방송
            v = inflater.inflate(R.layout.fragment_text_broadcast, container, false);
            mTextView = (TextView) v.findViewById(R.id.broadcast_text);
            mTextView.setText(mBroadcastData.getMessage());
            mTextView.setVerticalScrollBarEnabled(true);
            mTextView.setHorizontalScrollBarEnabled(false);
            mTextView.setMovementMethod(new ScrollingMovementMethod());

            mText2SpeechHandler = new TextToSpeechHandler(getActivity(), this);
            ((BaseActivity)getActivity()).showProgressDialog();
            ((BaseActivity)getActivity()).getText2SpeechObject(new BaseActivity.OnText2SpeechListener() {
                @Override
                public void onCheckResult(TextToSpeech tts) {
                    if (tts != null && tts instanceof TextToSpeech) {
                        mText2SpeechHandler.setTextToSpeechObject(tts);
                        mText2SpeechHandler.play(mBroadcastData.getMessage());
                    }
                }
            });
        } else {
            // 실시간, 일반음성방송
            v = inflater.inflate(R.layout.fragment_audio_broadcast, container, false);
            mWebView = (WebView)v.findViewById(R.id.webview);
            mWebViewClient = new BroadcastWebViewClient(getActivity(), mWebView);
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
        }

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mActivityInteractionListener = (OnLauncherFragmentInteractionListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        mActivityInteractionListener = null;
        super.onDetach();
    }

    // TTS
    @Override
    public void onStart(String s) {
        Log.d(TAG, "TTS onStart()");
        ((BaseActivity)getActivity()).dismissProgressDialog();
    }

    @Override
    public void onDone(String s) {
        Log.d(TAG, "TTS onDone()");
        popStack();
    }

    @Override
    public void onError(String utteranceId, int errorCode) {
        Log.d(TAG, "TTS onError()");
        ((BaseActivity)getActivity()).dismissProgressDialog();
        Toast.makeText(getActivity(), R.string.toast_tts_error, Toast.LENGTH_SHORT);
        popStack();
    }

    private void popStack() {
        mActivityInteractionListener.popStackBroadcastFragment();
        getActivity().getFragmentManager().popBackStack();
    }
}

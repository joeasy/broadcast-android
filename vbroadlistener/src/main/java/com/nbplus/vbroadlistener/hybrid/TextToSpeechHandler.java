/*
 * Copyright (c) 2015. NB Plus (www.nbplus.co.kr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nbplus.vbroadlistener.hybrid;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by basagee on 2015. 7. 2..
 */
public class TextToSpeechHandler extends UtteranceProgressListener {
    private static final String TAG = TextToSpeechHandler.class.getSimpleName();

    private TextToSpeech mText2Speech;
    private OnUtteranceProgressListener mProgressListener;

    private AudioManager mAudioManager;
    private int mPreviousVolume;
    private boolean mPreviousSpeakOn = false;
    private Context mContext;

    public interface OnUtteranceProgressListener {
        void onStart(String s);
        void onDone(String s);
        void onError(String utteranceId, int errorCode);
    }

    public TextToSpeechHandler(Context context, OnUtteranceProgressListener l) {
        mContext = context;
        this.mProgressListener = l;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public void setTextToSpeechObject(TextToSpeech tts) {
        if (tts != null) {
            mText2Speech = tts;
            mText2Speech.setOnUtteranceProgressListener(this);
        }
    }

    public TextToSpeech getTextToSpeechObject() {
        return this.mText2Speech;
    }

    public void play(String text) {
        if (mText2Speech == null) {
            return;
        }
        mPreviousSpeakOn = mAudioManager.isSpeakerphoneOn();
        if (!mPreviousSpeakOn) {
            mAudioManager.setSpeakerphoneOn(true);
        }
        mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);

        mText2Speech.setSpeechRate(0.8f);
        mText2Speech.setPitch(1.0f);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "REALTIMEBROADCAST_" + System.currentTimeMillis());
            int setListenerResult = mText2Speech.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    if (mProgressListener != null) {
                        mProgressListener.onDone(utteranceId);
                    }
                }
            });
            mText2Speech.speak(text,
                    TextToSpeech.QUEUE_FLUSH,  // Drop all pending entries in the playback queue.
                    map);
        } else {
            /**
             * The new API prefers a Bundle so replace the HashMap with a Bundle

             * > Bundle params = new Bundle();
             * > params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
             *
             * then when you make the speak call
             * > tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "UniqueID");
             *
             * The key is to use the ID in the speak call. You can put it in the Bundle,
             * but it will do nothing more for you.
             * It has to be in the speak call to trigger the listener.
             */
            Bundle params = new Bundle();
            long sysTime = System.currentTimeMillis();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "VILLAGE_BROADCAST_TTS_UTTERANCE_ID" + sysTime);
            mText2Speech.speak(text,
                    TextToSpeech.QUEUE_FLUSH,  // Drop all pending entries in the playback queue.
                    params,
                    "VILLAGE_BROADCAST_TTS_UTTERANCE_ID" + sysTime);
        }
    }

    public void stop() {
        if (mText2Speech != null) {
            mText2Speech.stop();
        }
        if (!mPreviousSpeakOn) {
            mAudioManager.setSpeakerphoneOn(false);
        }
        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxStreamVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (mPreviousVolume != maxStreamVolume && maxStreamVolume == streamVolume) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mPreviousVolume, 0);
        }
    }

    public void finalize() {
        stop();
        if (mText2Speech != null) {
            mText2Speech.shutdown();
        }
        mText2Speech = null;
        mProgressListener = null;
        Log.d(TAG, ">> TextToSpeechHandler finalize() called !!");
    }

    @Override
    public void onStart(String s) {
        if (mProgressListener != null) {
            mProgressListener.onStart(s);
        }
    }

    @Override
    public void onDone(String s) {
        if (mProgressListener != null) {
            mProgressListener.onDone(s);
        }
    }

    /**
     * @param s
     * @deprecated
     */
    @Override
    public void onError(String s) {
        // deprecated
        if (mProgressListener != null) {
            mProgressListener.onError(s, -1);
        }
    }

    @Override
    public void onError(String s, int errorCode) {
        if (mProgressListener != null) {
            mProgressListener.onError(s, errorCode);
        }
    }
}

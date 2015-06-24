package com.nbplus.vbroadlauncher.callback;

/**
 * Created by basagee on 2015. 5. 19..
 */

import android.net.Uri;

import com.nbplus.vbroadlauncher.data.RadioChannelInfo;

/**
 * This interface must be implemented by activities that contain this
 * fragment to allow an interaction in this fragment to be communicated
 * to the activity and potentially other fragments contained in that
 * activity.
 * <p/>
 * See the Android Training lesson <a href=
 * "http://developer.android.com/training/basics/fragments/communicating.html"
 * >Communicating with Other Fragments</a> for more information.
 */
public interface OnRadioFragmentInteractionListener {
    public void onPlayRadioRequest(RadioChannelInfo.RadioChannel channel);
}

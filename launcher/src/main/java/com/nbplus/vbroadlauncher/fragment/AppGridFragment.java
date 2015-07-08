package com.nbplus.vbroadlauncher.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.nbplus.vbroadlauncher.R;
import com.nbplus.vbroadlauncher.ShowApplicationActivity;
import com.nbplus.vbroadlauncher.adapter.AppGridViewAdapter;
import com.nbplus.vbroadlauncher.callback.OnActivityInteractionListener;
import com.nbplus.vbroadlauncher.data.ShowAllLaunchAppsInfo;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link AppGridFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AppGridFragment extends Fragment implements AdapterView.OnItemClickListener,
        OnActivityInteractionListener, View.OnClickListener {

    private static final String TAG = AppGridFragment.class.getSimpleName();

    private static final String ARGS_KEY_PAGE_POSITION = "key_page_position";

    private GridView mGridLayout;
    AppGridViewAdapter mAdapter;
    ArrayList<ApplicationInfo> mAppsList;
    int mViewPagePosition = -1;

    int mMaxIconView;
    boolean mCreated = false;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param page view pager position.
     * @return A new instance of fragment AppGridFragment.
     */
    public static AppGridFragment newInstance(Integer page) {
        AppGridFragment fragment = new AppGridFragment();
        Bundle args = new Bundle();
        args.putInt(ARGS_KEY_PAGE_POSITION, page);
        fragment.setArguments(args);
        return fragment;
    }

    public AppGridFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mViewPagePosition = getArguments().getInt(ARGS_KEY_PAGE_POSITION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_app_grid, container, false);
        mGridLayout = (GridView)v.findViewById(R.id.grid_layout);
        mCreated = true;
        updateGridLayout();
        return v;
    }

    public void updateGridLayout() {
        mMaxIconView = ShowAllLaunchAppsInfo.getMaxPageItemSize(getActivity());
        if (mViewPagePosition >= 0) {
            mAppsList = ShowAllLaunchAppsInfo.getInstance().getSubList(mViewPagePosition * mMaxIconView, (mViewPagePosition * mMaxIconView) + mMaxIconView);
        }

        /**
         * 새롭게 갱신해주지 않으면 swipe 시에 화면이 제대로 갱신되지 않는다.
         * mAdapter.setApplicationList(mAppsList)에서 notifyDataSetChanged()를 호출해주지만..
         * 제대로 되지 않는듯하다.
         */
        //if (mAdapter == null) {
            mAdapter = new AppGridViewAdapter(getActivity(), mAppsList, this);
            mGridLayout.setAdapter(mAdapter);
            mGridLayout.setOnItemClickListener(this);
        //} else {
        //    mAdapter.setApplicationList(mAppsList);
        //}
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            ((ShowApplicationActivity)activity).setOnActivityInteractionListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((ShowApplicationActivity)getActivity()).removeOnActivityInteractionListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mCreated) {
            updateGridLayout();
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onDataChanged() {
        Log.d(TAG, "onDataChanged() update grid layout");
        if (mCreated) {
            updateGridLayout();
        }
    }

    @Override
    public void onClick(View view) {
        AppGridViewAdapter.AppViewHolder viewHolder = (AppGridViewAdapter.AppViewHolder)view.getTag();

        String packageName = viewHolder.appInfo.packageName;
        Intent startIntent = getActivity().getPackageManager().getLaunchIntentForPackage(packageName);

        if(startIntent != null){
            startActivity(startIntent);
        }
    }
}

package com.waid.activity.main;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.waids.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class WhatAmIdoingFragment extends Fragment {

    private static final String TAG = "WhatAmIdoingFragment";

    public WhatAmIdoingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_what_am_idoing, container, false);
    }


}

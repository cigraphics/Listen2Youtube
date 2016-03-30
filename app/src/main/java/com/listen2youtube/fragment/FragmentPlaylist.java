package com.listen2youtube.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.listen2youtube.R;

/**
 * Created by khang on 27/03/2016.
 * Email: khang.neon.1997@gmail.com
 */
public class FragmentPlaylist extends Fragment{
    private static final String TAG = "FragmentPlaylist";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }
}

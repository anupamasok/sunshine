package com.example.android.sunshine.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Intent intent=getActivity().getIntent();

        View rootview= inflater.inflate(R.layout.fragment_detail, container, false);

        if(intent!=null && intent.hasExtra(Intent.EXTRA_TEXT)) {

          String forecastStr=intent.getStringExtra(Intent.EXTRA_TEXT);
          TextView text=(TextView) rootview.findViewById(R.id.detail_text);
          text.setText(forecastStr);

        }
        return rootview;
    }
}

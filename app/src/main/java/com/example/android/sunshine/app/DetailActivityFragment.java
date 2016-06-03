package com.example.android.sunshine.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    private final static String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    private final static String sunshine_tag=" # Sunshine! ";
    private static String mforecaststr;

    public DetailActivityFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.detailfragment, menu);

        MenuItem menuitem=menu.findItem(R.id.action_share);

        ShareActionProvider mShareActionProvider=(ShareActionProvider)MenuItemCompat.getActionProvider(menuitem);

        if(mShareActionProvider!=null)
        {
            mShareActionProvider.setShareIntent(createShareIntent());
        }
        else
        {
            Log.d(LOG_TAG," Share Action provider is null ");
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Intent intent=getActivity().getIntent();

        View rootview= inflater.inflate(R.layout.fragment_detail, container, false);

        if(intent!=null && intent.hasExtra(Intent.EXTRA_TEXT)) {

          mforecaststr=intent.getStringExtra(Intent.EXTRA_TEXT);
          TextView text=(TextView) rootview.findViewById(R.id.detail_text);
          text.setText(mforecaststr);

        }
        return rootview;
    }

    private Intent createShareIntent()
    {
        Intent shareIntent=new Intent(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mforecaststr + sunshine_tag);
        return shareIntent;
    }

}

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

public class MainActivity extends AppCompatActivity {


    private String mLocation;
    private ShowToastReceiver mShowToastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mLocation = Utility.getPreferredLocation(this);
        Log.d("my error", " mlocation " + mLocation);
        setContentView(R.layout.activity_main);
        SunshineSyncAdapter.initializeSyncAdapter(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        if (id == R.id.action_settings) {
            startActivity(new Intent(this,SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {

        super.onResume();
        registerToastReceiver();
        String location = Utility.getPreferredLocation(this);
        String dLocation = ForecastAdapter.getDisplayLocation();

        Log.e("My Error","location : " + location + " dlocation : " + dLocation);

        if(location != null && !location.equals(dLocation) && dLocation != null)
        {
            Log.e("My ERROR","Location has changed");
            mLocation = location;
            ForecastFragment ff = (ForecastFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
            ff.onLocationChanged();

        }
    }

    public class ShowToastReceiver extends BroadcastReceiver
    {
        public static final String SHOW_TOAST = "SHOW_TOAST";

        String message = "Oops! Couldn't connect to server. Check your network settings.";
        int duration = Toast.LENGTH_SHORT;

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == SHOW_TOAST)
                Toast.makeText(context,message,duration).show();
        }
    }

    private void registerToastReceiver()
    {
        IntentFilter filter = new IntentFilter(ShowToastReceiver.SHOW_TOAST);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        mShowToastReceiver = new ShowToastReceiver();
        registerReceiver(mShowToastReceiver,filter);

    }

    @Override
    protected void onPause() {
        unregisterReceiver(mShowToastReceiver);
        super.onPause();
    }
}

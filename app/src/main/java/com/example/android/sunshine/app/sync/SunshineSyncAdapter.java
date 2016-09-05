package com.example.android.sunshine.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

import com.example.android.sunshine.app.MainActivity;
import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();

    public static final int SYNC_INTERVAL = 60*30; // 3 hours in seconds
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3; // 1 hour of flex time
    private static final long DAY_IN_MILLIS = 24*60*60*1000;
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;



    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Sync has started...");


        String locationQuery = Utility.getPreferredLocation(getContext());

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query

            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "id";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM ="APPID";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, locationQuery)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM,"6188b00c976e66c17548289dd3a0c08c")
                    .build();

            URL url = new URL(builtUri.toString());

            Log.d(LOG_TAG,"Connecting to URL : " + builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {

                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            forecastJsonStr = buffer.toString();
            getWeatherDataFromJson(forecastJsonStr, locationQuery);

        }
        catch (IOException e)
        {
            Log.e(LOG_TAG, "Error  ", e);

        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG, "Error  ", e);
            e.printStackTrace();
        }
        finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }


        // This will only happen if there was an error getting or parsing the forecast.
        return;

    }


    private void getWeatherDataFromJson(String forecastJsonStr,
                                        String locationSetting)
            throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";


        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_MAIN_DESCRIPTION = "main";
        final String OWM_DESCRIPTION = "description";
        final String OWM_WEATHER_ID = "id";
        final String OWM_WIND = "wind";
        final String OWM_TEMP = "temp";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            for(int i = 0; i < weatherArray.length(); i++) {
                // These are the values that will be collected.
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                String mainDescription;
                int weatherId;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);

                JSONObject tempNode = dayForecast.getJSONObject(OWM_TEMP);

                high = tempNode.getDouble(OWM_MAX);
                low = tempNode.getDouble(OWM_MIN);

                humidity = dayForecast.getInt(OWM_HUMIDITY);
                pressure = dayForecast.getDouble(OWM_PRESSURE);


                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                // Description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject =
                        dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                mainDescription = weatherObject.getString(OWM_MAIN_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);


                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
            }

            int inserted = 0;
            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                inserted = getContext().getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
            }


            Log.d(LOG_TAG, "SunshineService Complete. " + inserted + " Inserted");


            // delete weather data for previois days
            getContext().getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,

                    WeatherContract.WeatherEntry.COLUMN_DATE + " <= ?",

                    new String[] {Long.toString(dayTime.setJulianDay(julianStartDay-1))});


            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

            String displayNotificationsKey = getContext().getString(R.string.pref_enable_notifications_key);

            boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,

                    Boolean.parseBoolean(getContext().getString(R.string.pref_enable_notifications_default)));

            if(displayNotifications)
              notifyWeather();

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

    }

    long addLocation(String locationSetting, String cityName, double lat, double lon) {

        long locationid;

        Cursor cursor = getContext().getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ",
                new String[]{locationSetting},
                null);

        if(cursor.moveToFirst())
        {
            int columnIndex = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationid = cursor.getLong(columnIndex);
        }
        else
        {
            ContentValues values = new ContentValues();

            values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME,cityName);
            values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,locationSetting);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT,lat);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG,lon);

            Uri insertedUri = getContext().getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI,values);

            locationid = ContentUris.parseId(insertedUri);


        }

        return locationid;

    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount,context);
        }
        return newAccount;
    }

    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    private static void onAccountCreated(Account newAccount, Context context) {

        Log.d("SunshineSyncadapter","Inside On account created");
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    private void notifyWeather() {

        Log.d(LOG_TAG,"Inside OnNotifyWeather");

        Context context = getContext();

        //checking the last update and notify if it' the first of the day

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String lastNotificationKey = context.getString(R.string.pref_last_notification);

        long lastSync = prefs.getLong(lastNotificationKey, 0);

        Log.d(LOG_TAG,"lastsync Value " + lastSync);



        if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {

            // Last sync was more than 1 day ago, let's send a notification with the weather.

            String locationQuery = Utility.getPreferredLocation(context);



            Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());



            // we'll query our contentProvider, as always

            Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);



            if (cursor.moveToFirst()) {

                int weatherId = cursor.getInt(INDEX_WEATHER_ID);

                double high = cursor.getDouble(INDEX_MAX_TEMP);

                double low = cursor.getDouble(INDEX_MIN_TEMP);

                String desc = cursor.getString(INDEX_SHORT_DESC);



                int iconId = Utility.getArtResourceForWeatherCondition(weatherId);

                String title = context.getString(R.string.app_name);



                // Define the text of the forecast.

                String contentText = String.format(context.getString(R.string.format_notification),

                        desc,

                        Utility.formatTemperature(context, high, Utility.isMetric(context)),

                        Utility.formatTemperature(context, low, Utility.isMetric(context)));


                NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                                                       .setSmallIcon(iconId)
                                                       .setContentText(contentText)
                                                       .setContentTitle("Sunshine");

                Intent intent = new Intent(context, MainActivity.class);

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(intent);

                PendingIntent pi = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(pi);

                NotificationManager manager = (NotificationManager)context.getSystemService(context.NOTIFICATION_SERVICE);
                manager.notify(WEATHER_NOTIFICATION_ID,builder.build());


                //refreshing last sync

                SharedPreferences.Editor editor = prefs.edit();

                editor.putLong(lastNotificationKey, System.currentTimeMillis());

                editor.commit();

            }

        }



    }

}
package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ForecastAdapter extends CursorAdapter {

    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;
    private static String mdisplayLocation = null;

    public ForecastAdapter(Context context, Cursor c, int flags)
    {
        super(context, c, flags);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        int viewType = getItemViewType(cursor.getPosition());
        int layoutId;

        if(viewType == VIEW_TYPE_TODAY)
            layoutId = R.layout.list_item_forecast_today;
        else
            layoutId = R.layout.list_item_forecast;

         View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
         return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        String displayLocation = cursor.getString(ForecastFragment.COL_LOCATION_SETTING);
        mdisplayLocation = displayLocation;

        int viewType = getItemViewType(cursor.getPosition());
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        if(viewType == VIEW_TYPE_TODAY)
            viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
        else
            viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherId));



        long dateInMillis = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateInMillis));

        String description = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        viewHolder.descriptionView.setText(description);

        viewHolder.iconView.setContentDescription(description);

        boolean isMetric = Utility.isMetric(context);


        double high = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        viewHolder.highTempView.setText(Utility.formatTemperature(context,high, isMetric));

        double low = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        viewHolder.lowTempView.setText(Utility.formatTemperature(context,low, isMetric));
    }

    public static String getDisplayLocation()
    {
        return mdisplayLocation;
    }
}
package com.example.ajoy3.mylocation;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class CheckedInPlacesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checked_in_places);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //scrollable textview to display checked in places
        TextView checkedInPlaces;
        checkedInPlaces = (TextView)findViewById(R.id.checkinTextView);

        LocationSQLHelper mDbHelper = new LocationSQLHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] columns = {
                LocationSQLHelper.COLUMN_ID,
                LocationSQLHelper.COLUMN_NAME_TIMESTAMP,
                LocationSQLHelper.COLUMN_NAME_ADDRESS,
                LocationSQLHelper.COLUMN_NAME_LATITUDE,
                LocationSQLHelper.COLUMN_NAME_LONGITUDE
        };

        Cursor c = db.query(
                LocationSQLHelper.TABLE_NAME,  // The table to query
                columns,                               // The columns to return
                null,
                null,
                null,
                null,
                null
        );

        int col_id_index = c.getColumnIndexOrThrow(LocationSQLHelper.COLUMN_ID);
        int col_time_index = c.getColumnIndexOrThrow(LocationSQLHelper.COLUMN_NAME_TIMESTAMP);
        int col_lat_index = c.getColumnIndexOrThrow(LocationSQLHelper.COLUMN_NAME_LATITUDE);
        int col_lon_index = c.getColumnIndexOrThrow(LocationSQLHelper.COLUMN_NAME_LONGITUDE);
        int col_addrs_index = c.getColumnIndexOrThrow(LocationSQLHelper.COLUMN_NAME_ADDRESS);

        //read recent check in first which will be the last in the table
        c.moveToLast();
        int id = c.getInt(col_id_index);
        String latitude = c.getString(col_lat_index);
        String longitude = c.getString(col_lon_index);
        String addrs = c.getString(col_addrs_index);
        String time = c.getString(col_time_index);
        String date = DateFormat.getDateTimeInstance().format(new Date(new Long(time)));
        checkedInPlaces.append(id + "\n" +
                "You were at\n" + addrs + "\n" +
                "on " + date + "\n" +
                "Latitude: " + latitude + "\n" +
                "Longitude: " + longitude + "\n");
        //read every checked in places.
        while(c.moveToPrevious()) {
            id = c.getInt(col_id_index);
            latitude = c.getString(col_lat_index);
            longitude = c.getString(col_lon_index);
            addrs = c.getString(col_addrs_index);
            time = c.getString(col_time_index);
            date = DateFormat.getDateTimeInstance().format(new Date(new Long(time)));
            checkedInPlaces.append(id+"\n"+
                                    "You were at\n"+addrs+"\n"+
                                    "on "+date+"\n"+
                                    "Latitude: "+latitude+"\n"+
                                    "Longitude: "+longitude+"\n");
        }
        c.close();
        db.close();
    }

}

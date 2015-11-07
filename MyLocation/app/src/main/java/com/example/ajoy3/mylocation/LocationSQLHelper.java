package com.example.ajoy3.mylocation;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by ajoy3 on 10/11/2015.
 */
public class LocationSQLHelper extends SQLiteOpenHelper {
    //helper class that defines the schema for SQLite DB
    public static final String TABLE_NAME = "checkedin";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME_LATITUDE = "latitude";
    public static final String COLUMN_NAME_LONGITUDE = "longitude";
    public static final String COLUMN_NAME_ADDRESS = "address";
    public static final String COLUMN_NAME_TIMESTAMP = "timestamp";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Locations.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " +TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NAME_TIMESTAMP + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_LATITUDE + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_LONGITUDE + TEXT_TYPE + COMMA_SEP+
                    COLUMN_NAME_ADDRESS + TEXT_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;


    public LocationSQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LocationSQLHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}

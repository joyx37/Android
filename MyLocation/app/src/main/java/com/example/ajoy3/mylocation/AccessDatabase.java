package com.example.ajoy3.mylocation;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * Created by ajoy3 on 10/11/2015.
 */
public class AccessDatabase extends AsyncTask<String, Void, Long> {

    private Context mContext;
    public AccessDatabase(Context context){
        mContext = context;
    }
    @Override
    protected Long doInBackground(String... params) {
        //async task to perform write to database operation
        LocationSQLHelper mDbHelper = new LocationSQLHelper(mContext);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(LocationSQLHelper.COLUMN_NAME_TIMESTAMP,params[0]);
        values.put(LocationSQLHelper.COLUMN_NAME_LATITUDE,params[1]);
        values.put(LocationSQLHelper.COLUMN_NAME_LONGITUDE,params[2]);
        values.put(LocationSQLHelper.COLUMN_NAME_ADDRESS,params[3]);

        long newRowId;
        newRowId = db.insert(LocationSQLHelper.TABLE_NAME,null,values);
        db.close();
        return newRowId;
    }

    @Override
    protected void onPostExecute(Long rowId){
        if(rowId > 0) {
            Toast.makeText(mContext, "Checked In Successfully", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(mContext, "Checking In Unsuccessfull", Toast.LENGTH_SHORT).show();
        }
    }
}

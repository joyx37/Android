package com.example.ajoy3.mycamera;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by ajoy3 on 11/20/2015.
 */
public class FetchAddressIntentService extends IntentService {
    //ResultReceiver class allows you to send a numeric result code and a message containing
    //result data
    protected ResultReceiver mReceiver;

    public FetchAddressIntentService() {

        super("FetchAddressIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String errorMessage = "";
        //Geocoder class for reverse geocoding....get address from lat/long
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        if (intent != null) {
            mReceiver = intent.getParcelableExtra(Constants.RECEIVER);
            // Get the location passed to this service through an extra.
            Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);
            Log.i("RevGeoLatLng",location.toString());
            List<Address> addressList = null;
            try{
                addressList = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),2);
            }catch(IOException ioException){
                // Catch network or other I/O problems.
                Log.e("Geocoder IO Exception", errorMessage, ioException);
            }catch (IllegalArgumentException illegalArgumentException){
                // Catch invalid latitude or longitude values.
                Log.e("Lat/Lon Invalid", errorMessage + ". " +
                        "Latitude = " + location.getLatitude() +
                        ", Longitude = " + location.getLongitude(), illegalArgumentException);
            }
            // Handle case where no address was found.
            if(addressList == null || addressList.size() == 0){
                if(errorMessage.isEmpty()){
                    Log.e("Empty Address", errorMessage);
                }
                deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
            } else {
                Address address1 = addressList.get(0);
                ArrayList<String> addressFragments1 = new ArrayList<>();

                // Fetch the address lines using getAddressLine,
                // join them, and send them to the thread.
                for(int i = 0; i < address1.getMaxAddressLineIndex(); i++) {
                    addressFragments1.add(address1.getAddressLine(i));
                }
                deliverResultToReceiver(Constants.SUCCESS_RESULT,
                        TextUtils.join(System.getProperty("line.separator"), addressFragments1));
            }
        }
    }

    private void deliverResultToReceiver(int resultCode, String address1){
        Bundle bundle = new Bundle();
        //put results in bundle and send to main UI thread.
        Log.i("RevGeo",address1);
        bundle.putString(Constants.RESULT_DATA_KEY_1,address1);
        mReceiver.send(resultCode,bundle);
    }
}


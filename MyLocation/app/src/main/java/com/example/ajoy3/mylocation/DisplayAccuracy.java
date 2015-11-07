package com.example.ajoy3.mylocation;

import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class DisplayAccuracy extends AppCompatActivity implements
        ConnectionCallbacks, LocationListener {

    private static GoogleApiClient mGoogleAPIClient;
    protected static Location mCurrentLocation_old;
    protected static Location mCurrentLocation_new;
    private static LocationRequest mLocationRequest;
    private  TextView textView;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static boolean stop = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_accuracy);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textView = (TextView)findViewById(R.id.accuracyTextView);

        buildGoogleApiClient();
    }

    @Override
    protected  void onStart(){
        super.onStart();
        //Connect to google API to start receiving location
        mGoogleAPIClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stopLocationUpdates when App goes to Pause state to save battery
        stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        //If app is resumed and google API is connected to, then connect
        if (mGoogleAPIClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected  void onStop(){
        super.onStop();
        //Disconnect from google API. This will stop all location updates when ap is stopped
        mGoogleAPIClient.disconnect();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleAPIClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(LocationServices.API)//check if required API version is available before connecting
                .addConnectionCallbacks(this)//add connection callbacks for onConnected etc...
                .addApi(LocationServices.API)//I am using only location services
                .build();
        createLocationRequest();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mCurrentLocation_old = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPIClient);
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        //Get location updates at intervals as defined.
        //interval 10s. fastest = 5s
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleAPIClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleAPIClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //if connection suspended try connecting again
        mGoogleAPIClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        //when location changes (updates) read to current location
        mCurrentLocation_new = location;
        //getAccuracy
        getAccuracy(mCurrentLocation_new,mCurrentLocation_old);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(20000);//regular interval for updates.
        mLocationRequest.setFastestInterval(20000);//fastest interval. will not receive location updates faster than this!!
        mLocationRequest.setSmallestDisplacement(5);//for improved reliability, performance, and to account for constant changes in GPS,
        // only accept updates if displacement minimum 5 meters
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//get very accurate locatino. tested... really very accurate!!
    }

    private void getAccuracy(Location newLoc, Location oldLoc){
        textView.setText("");
        if (oldLoc == null && !stop) {
            stop = true;
            // A new location is always better than no location
            textView.append("New Location is better because no old location\n");
            Log.i("Accuracy", "New Location is better because no old location");
        }
        else {

            long timeDelta = newLoc.getTime() - oldLoc.getTime();
            boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
            boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
            boolean isNewer = timeDelta > 0;

            if (isSignificantlyNewer && !stop) {
                stop = true;
                textView.append("New Location is significantly newer than old location\n");
                Log.i("Accuracy", "New Location is better because no old location");

            } else if (isSignificantlyOlder && !stop) {
                stop = true;
                textView.append("New Location is significantly older than old location\n");
                Log.i("Accuracy", "New Location is better because no old location");
            }

            int accuracyDelta = (int) (newLoc.getAccuracy() - oldLoc.getAccuracy());
            boolean isLessAccurate = accuracyDelta > 0;
            boolean isMoreAccurate = accuracyDelta < 0;
            boolean isSignificantlyLessAccurate = accuracyDelta > 200;

            boolean isFromSameProvider;
            if (newLoc.getProvider() == null) {
                isFromSameProvider = oldLoc.getProvider() == null;
            } else {
                isFromSameProvider = newLoc.getProvider().equals(oldLoc.getProvider());
                Log.i("Provider","New Loc Provider: "+newLoc.getProvider()+"\nOld Loc Provider: "+oldLoc.getProvider());
            }
            // Determine location quality using a combination of timeliness and accuracy
            if (isMoreAccurate && !stop) {
                stop = true;
                textView.append("Old Location is more accurate than New location\n"+"Old Location Accuracy: " + oldLoc.getAccuracy()+"\n"+
                                "New Location accuracy: " + newLoc.getAccuracy() + "\n");
                Log.i("Accuracy", "Old Location is more accurate than New location\n"+"Old Location Accuracy: " + oldLoc.getAccuracy()+"\n"+
                        "New Location accuracy: " + newLoc.getAccuracy() + "\n");
            } else if (isNewer && !isLessAccurate && !stop) {
                stop = true;
                textView.append("New Location is newer and better than old location\n" + "New Location accuracy: " + newLoc.getAccuracy() +
                        "\nOld Location Accuracy: " + oldLoc.getAccuracy()+"\n");
                Log.i("Accuracy", "New Location is and better than old location\n" + "New Location accuracy: " + newLoc.getAccuracy() +
                        "\nOld Location Accuracy: " + oldLoc.getAccuracy());
                mCurrentLocation_old = newLoc;
            } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider && !stop) {
                stop = true;
                textView.append("New Location is significantly accurate than old location and it is from same provider\n" + "New Location accuracy: " + newLoc.getAccuracy() + "\n" +
                        "Old Location Accuracy: " + oldLoc.getAccuracy()+"\n");
                Log.i("Accuracy", "New Location is significantly accurate than old location and it is from same provider\n" + "New Location accuracy: " + newLoc.getAccuracy() + "\n" +
                        "Old Location Accuracy: " + oldLoc.getAccuracy());
                mCurrentLocation_old = newLoc;
            }
            else {
                textView.append("Old Location is more accurate than New location\n" + "Old Location Accuracy: " + oldLoc.getAccuracy() + "\n" +
                        "New Location accuracy: " + newLoc.getAccuracy() + "\n");
                Log.i("Accuracy", "Old Location is more accurate than New location");
            }
        }
        stop = false;
    }

}

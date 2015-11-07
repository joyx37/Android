package com.example.ajoy3.mylocation;

import android.content.Intent;
import android.location.Location;
import android.os.*;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import java.util.Date;

public class MainActivity1 extends AppCompatActivity
        implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    //used for saving Bundle Instance
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    private static GoogleApiClient mGoogleAPIClient;
    private static TextView displayLatitude,displayLongitude;
    private static ListView addressList;
    protected static Location mCurrentLocation;
    private static LocationRequest mLocationRequest;
    private static String mLastUpdateTime;
    private static AddressResultReceiver mResultReceiver;
    private static String[] addresses;
    private static ArrayAdapter<String> listviewAdapter;
    private static String checkInAddress;
    private static String latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mResultReceiver = new AddressResultReceiver(new Handler());
        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();

        displayLatitude = (TextView) findViewById(R.id.latitudeTextView);
        displayLongitude = (TextView) findViewById(R.id.longitudeTextView);
        addressList = (ListView) findViewById(R.id.listView);

        Log.i("Thread ID UI",Integer.toString(android.os.Process.myTid()));
    }

    @Override
    protected  void onStart(){
        super.onStart();
        //Connect to google API to start receiving location
        mGoogleAPIClient.connect();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        //save current location and last update time to Bundle
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocation is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
                updateLatLong();
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }
        }
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
        else {
            mGoogleAPIClient.connect();
        }
    }

    @Override
    protected  void onStop(){
        super.onStop();
        //Disconnect from google API. This will stop all location updates when ap is stopped
        mGoogleAPIClient.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity1, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleAPIClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(LocationServices.API)//check if required API version is available before connecting
                .addConnectionCallbacks(this)//add connection callbacks for onConnected etc...
                .addApi(LocationServices.API)//I am using only location services
                .addOnConnectionFailedListener(this)//to check if connection failed.
                .build();
        createLocationRequest();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i("Thread ID onConnected", Integer.toString(android.os.Process.myTid()));
        //after connection to google API get last known (cached) location
        //use this as current location.
        //This is usually approximate and close to true location.
        //Helps for smooth functioning without delay in getting location.
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPIClient);
        mLastUpdateTime = Long.toString(new Date().getTime());
        if(mCurrentLocation != null) {
            Log.i("Connected", "Now");
            //display latitude and longitude on UI
            updateLatLong();
        }
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
        Log.i("Location Changed","Now");
        mCurrentLocation = location;
        mLastUpdateTime = Long.toString(new Date().getTime());
        Log.i("Update Time",mLastUpdateTime);
        //display lat long to UI
        updateLatLong();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("ThreadID onConFailed",Integer.toString(android.os.Process.myTid()));
    }

    protected void createLocationRequest() {
        Log.i("Requested","Now");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);//regular interval for updates.
        mLocationRequest.setFastestInterval(5000);//fastest interval. will not receive location updates faster than this!!
        mLocationRequest.setSmallestDisplacement(5);//for improved reliability, performance, and to account for constant changes in GPS,
        // only accept updates if displacement minimum 5 meters
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//get very accurate locatino. tested... really very accurate!!
    }

    private void updateLatLong(){
        //method to display lat long to UI
        latitude = String.valueOf(mCurrentLocation.getLatitude());
        displayLatitude.setText(latitude);
        longitude = String.valueOf(mCurrentLocation.getLongitude());
        displayLongitude.setText(longitude);
        //Now we have location, so start intent service to fetch address
        startIntentService();
    }

    private void updateAddressList(){
        Log.i("Thread ID updateAddrs",Integer.toString(android.os.Process.myTid()));
        listviewAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,addresses);
        //display received address
        addressList.setAdapter(listviewAdapter);
        //listen for address list click
        addressList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                checkInAddress = addresses[position];
                displayToast("Address Selected \nClick Check-in Button");
            }
        });
    }

    private void displayToast(String message) {
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        //used to get back result from intent service because it runs on different thread
        intent.putExtra(Constants.RECEIVER,mResultReceiver);
        //send current lat long to get possible addresses for it
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mCurrentLocation);
        startService(intent);
    }

    public void onViewMapClick(View view) {
        //start map activity
        Intent intent = new Intent(this,MapsActivity.class);
        startActivity(intent);
    }

    public void onCheckInClick(View view) {
        if(checkInAddress != null) {
            displayToast("Checking In");
            //store time,lat,long,address in database
            //Async task is used here to ensure uninterrupted UI
            new AccessDatabase(getApplicationContext()).execute(mLastUpdateTime, latitude, longitude, checkInAddress);
        }
        else{
            displayToast("Address is Empty. Try Again");
        }
    }

    public void onCheckInPlacesClick(View view) {
        //start activity to display checked in places as scrollable text view
        Intent intent = new Intent(this,CheckedInPlacesActivity.class);
        startActivity(intent);
    }

    public void onViewAccuracyClick(View view) {
        //start activity to display accuracy
        Intent intent = new Intent(this,DisplayAccuracy.class);
        startActivity(intent);
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            Log.i("Thread ID AddrsReceive",Integer.toString(android.os.Process.myTid()));
            //called when address result is obtained
            addresses = new String[2];
            addresses[0] = resultData.getString(Constants.RESULT_DATA_KEY_1);
            addresses[1] = resultData.getString(Constants.RESULT_DATA_KEY_2);
            //third address not used. not so accurate
            //addresses[2] = resultData.getString(Constants.RESULT_DATA_KEY_3);

            //default address is first on list. use this if user only clicks check-in without selecting address
            checkInAddress = addresses[0];
            updateAddressList();
        }
    }
}

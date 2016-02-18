package com.example.ajoy3.mycamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.util.FloatMath;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


public class MainActivity extends AppCompatActivity
        implements SensorEventListener,ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final int START_CAMERA = 0;
    ImageView imageView1;
    TextView textView3;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private static final int Y_THRESHOLD = 4; // Y > 3
    private static final int SPEED_THRESHOLD = 300;
    private static long mLastTime = 0;
    private float mLastX=0.0f, mLastY=0.0f, mLastZ=0.0f;

    private static String checkInAddress;
    protected final static String LOCATION_KEY = "location-key";
    private static GoogleApiClient mGoogleAPIClient;
    protected static Location mCurrentLocation;
    private static LocationRequest mLocationRequest;
    private static AddressResultReceiver mResultReceiver;
    private static String[] addresses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView1 =(ImageView)findViewById(R.id.imageView);
        textView3 = (TextView)findViewById(R.id.textView3);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mResultReceiver = new AddressResultReceiver(new Handler());
        buildGoogleApiClient();
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
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        senSensorManager.unregisterListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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
        //after connection to google API get last known (cached) location
        //use this as current location.
        //This is usually approximate and close to true location.
        //Helps for smooth functioning without delay in getting location.
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPIClient);
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
        //display lat long to UI
        updateLatLong();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("ThreadID onConFailed", Integer.toString(android.os.Process.myTid()));
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
        //Now we have location, so start intent service to fetch address
        Log.i("Lat/Lng",mCurrentLocation.toString());
        startIntentService();
    }

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        //used to get back result from intent service because it runs on different thread
        intent.putExtra(Constants.RECEIVER,mResultReceiver);
        //send current lat long to get possible addresses for it
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mCurrentLocation);
        startService(intent);
    }

    @SuppressLint("ParcelCreator")
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            //called when address result is obtained
            addresses = new String[2];
            addresses[0] = resultData.getString(Constants.RESULT_DATA_KEY_1);
            addresses[1] = resultData.getString(Constants.RESULT_DATA_KEY_2);
            //third address not used. not so accurate
            //addresses[2] = resultData.getString(Constants.RESULT_DATA_KEY_3);

            //default address is first on list. use this if user only clicks check-in without selecting address
            checkInAddress = addresses[0];
            Log.i("address",checkInAddress);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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

    public void StartCamera(View view) {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, START_CAMERA);
    }


    public void StartVideo() {
        Intent intent = new Intent(this, AutoVideoRecord.class);
        intent.putExtra(ExifInterface.TAG_GPS_LATITUDE,String.valueOf(mCurrentLocation.getLatitude()));
        intent.putExtra(ExifInterface.TAG_GPS_LONGITUDE, String.valueOf(mCurrentLocation.getLongitude()));
        if(checkInAddress!=null){
            intent.putExtra("UserComment", checkInAddress);
        }
        else{
            intent.putExtra("UserComment", "");
        }
        startActivity(intent);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == START_CAMERA) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            String imageName = Long.toString(new Date().getTime()) + ".jpg";
            String state = Environment.getExternalStorageState();
            File file = null;
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File directory = new File(Environment.getExternalStorageDirectory()+"/MyCamera/");
                directory.mkdirs();
                file = new File(directory, imageName);
                try {
                    file.createNewFile();
                    FileOutputStream out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();

                    MediaScannerConnection.scanFile(getApplicationContext(),
                            new String[]{file.toString()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i("ExternalStorage", "Scanned " + path + ":");
                                    Log.i("ExternalStorage", "-> uri=" + uri);
                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            imageView1.setImageBitmap(BitmapFactory.decodeFile(file.toString()));
            if(checkInAddress != null) {
                String mString = checkInAddress;
                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(file.toString());
                    exif.setAttribute("UserComment", mString);
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,String.valueOf(mCurrentLocation.getLatitude()));
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,String.valueOf(mCurrentLocation.getLongitude()));
                    exif.saveAttributes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    exif = new ExifInterface(file.toString());
                    textView3.setText(exif.getAttribute("UserComment"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float yAbs = Math.abs(y);
            float zAbs = Math.abs(z);

            long timeNow = System.currentTimeMillis();
            long diff = timeNow - mLastTime;
            mLastTime = timeNow;
            float speed = Math.abs(x+y+z - mLastX - mLastY - mLastZ) / diff * 10000;
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            Log.i("speed",Float.toString(speed));
            if ((yAbs>Y_THRESHOLD) && (speed > SPEED_THRESHOLD)) {
                StartVideo();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}

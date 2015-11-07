package com.example.ajoy3.mylocation;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,LocationListener,
        OnMapReadyCallback,GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private static GoogleApiClient mGoogleAPIClient;
    private static LatLng mCurrentLocationLatLng;
    private static Location mCurrentLocation;
    private static LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        buildGoogleApiClient();
    }

    @Override
    protected  void onStart(){
        super.onStart();

        mGoogleAPIClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mGoogleAPIClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected  void onStop(){
        super.onStop();
        stopLocationUpdates();
        mGoogleAPIClient.disconnect();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleAPIClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(LocationServices.API)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(this)
                .build();
        createLocationRequest();
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleAPIClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleAPIClient, this);
    }

    protected void createLocationRequest() {
        Log.i("Requested","Now");
        mLocationRequest = new LocationRequest();
        //faster interval becasue user might be moving and want to see current location as we move
        mLocationRequest.setInterval(2000);//get location every 2 second
        mLocationRequest.setFastestInterval(1000);//get location as fast as 1 second
        mLocationRequest.setSmallestDisplacement(5);//accept location only if displacement is minimum 5 meters
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPIClient);
        if(mCurrentLocation != null) {
            Log.i("Connected", "Now");
            updateMapCamera();
        }
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleAPIClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
    @Override
    public void onLocationChanged(Location location) {
        Log.i("Location Changed", "Now");
        mCurrentLocation = location;
        //update current location on map
        updateMapCamera();
    }

    private void updateMapCamera(){
        //get lat lng for current location and mpve camera
        mCurrentLocationLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLocationLatLng));//move camera to current location
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add  markers to Rutgers Buildings
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);//shows current location as small blue circle
        mMap.getUiSettings().setCompassEnabled(true);//compass enable. but shows only when tilted
        mMap.getUiSettings().setZoomControlsEnabled(true);//zoom buttons
        mMap.getUiSettings().setAllGesturesEnabled(true);//enable all gestures for better usability

        //read locations from database
        LocationSQLHelper mDbHelper = new LocationSQLHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] columns = {
                LocationSQLHelper.COLUMN_NAME_ADDRESS,
                LocationSQLHelper.COLUMN_NAME_LATITUDE,
                LocationSQLHelper.COLUMN_NAME_LONGITUDE
        };

        Cursor c = db.query(
                LocationSQLHelper.TABLE_NAME,  // The table to query
                columns,                       // The columns to return
                null,
                null,
                null,
                null,
                null
        );

        //check if null in database table
        //usually when user doesn't check-in and on first use
        if(c.getCount()>0) {
            int col_lat_index = c.getColumnIndexOrThrow(LocationSQLHelper.COLUMN_NAME_LATITUDE);
            int col_lon_index = c.getColumnIndexOrThrow(LocationSQLHelper.COLUMN_NAME_LONGITUDE);
            int col_addrs_index = c.getColumnIndexOrThrow(LocationSQLHelper.COLUMN_NAME_ADDRESS);

            //get recent check-in location to show on map
            c.moveToLast();
            float latitude = Float.valueOf(c.getString(col_lat_index));
            float longitude = Float.valueOf(c.getString(col_lon_index));
            String addressMarker = c.getString(col_addrs_index);
            LatLng lastCheckedInLocation = new LatLng(latitude, longitude);
            //add marker to recently checked in location
            mMap.addMarker(new MarkerOptions().position(lastCheckedInLocation).title(addressMarker));
        }

        //Rutgers Building Locations and markers
        //Busch Campus Center 40.523128,-74.458797
        LatLng BuschCampusCenter = new LatLng(40.523128,-74.458797);
        mMap.addMarker(new MarkerOptions().position(BuschCampusCenter).title("Busch Campus Center"));

        //HighPoint Solutions Stadium 40.513817,-74.464844
        LatLng HighPointSolutions = new LatLng(40.513817,-74.464844);
        mMap.addMarker(new MarkerOptions().position(HighPointSolutions).title("HighPoint Solutions Stadium"));

        //Electrical Engineering Building 40.521663,-74.460665
        LatLng ElectricalEngineeringBuilding = new LatLng(40.513817,-74.464844);
        mMap.addMarker(new MarkerOptions().position(ElectricalEngineeringBuilding).title("Electrical Engineering Building"));

        //Rutgers Student Center 40.502661,-74.451771
        LatLng RutgersStudentCenter = new LatLng(40.502661,-74.451771);
        mMap.addMarker(new MarkerOptions().position(RutgersStudentCenter).title("Rutgers Student Center"));

        //Old Queens 40.498720,-74.446229
        LatLng OldQueens = new LatLng(40.498720,-74.446229);
        mMap.addMarker(new MarkerOptions().position(OldQueens).title("Old Queens"));

        //Old Queens 40.498720,-74.446229
        LatLng Core = new LatLng(40.521428,-74.461398);
        mMap.addMarker(new MarkerOptions().position(Core).title("CORE"));

        //polyline used for testing
        /*
        mMap.addPolyline(new PolylineOptions().geodesic(true)).setPoints(Arrays.asList(mCurrentLocationLatLng, BuschCampusCenter));
        mMap.addPolyline(new PolylineOptions().geodesic(true)).setPoints(Arrays.asList(mCurrentLocationLatLng, HighPointSolutions));
        mMap.addPolyline(new PolylineOptions().geodesic(true)).setPoints(Arrays.asList(mCurrentLocationLatLng, ElectricalEngineeringBuilding));
        mMap.addPolyline(new PolylineOptions().geodesic(true)).setPoints(Arrays.asList(mCurrentLocationLatLng, RutgersStudentCenter));
        mMap.addPolyline(new PolylineOptions().geodesic(true)).setPoints(Arrays.asList(mCurrentLocationLatLng, OldQueens));
        */
        //listen for marker clicks
        googleMap.setOnMarkerClickListener(this);
        addHeatMap();
        c.close();
        db.close();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        float[] distance = new float[3];
        String units = "m";
        Location.distanceBetween(mCurrentLocationLatLng.latitude, mCurrentLocationLatLng.longitude,
                marker.getPosition().latitude, marker.getPosition().longitude, distance);
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(2);
        //if distance is greater than 1000 m, display in km
        if(distance[0] > 1000){
            distance[0] = distance[0] / 1000f;
            units = "km";
        }
        marker.setSnippet("You are " + nf.format(distance[0]) + " " + units + " from here");
        return false;
    }

    private void addHeatMap() {
        List<LatLng> list = new ArrayList<>();

        LocationSQLHelper mDbHelper = new LocationSQLHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // The columns to return
        String[] columns = {
                LocationSQLHelper.COLUMN_NAME_LATITUDE,
                LocationSQLHelper.COLUMN_NAME_LONGITUDE
        };

        Cursor c = db.query(
                LocationSQLHelper.TABLE_NAME,  // The table to query
                columns,
                null,
                null,
                null,
                null,
                null
        );

        // Get the data: latitude/longitude positions to display heatmap
        while (c.moveToNext()) {
            int col_lat_index = c.getColumnIndexOrThrow(LocationSQLHelper.COLUMN_NAME_LATITUDE);
            int col_lon_index = c.getColumnIndexOrThrow(LocationSQLHelper.COLUMN_NAME_LONGITUDE);
            Double latitude = Double.parseDouble(c.getString(col_lat_index));
            Double longitude = Double.parseDouble(c.getString(col_lon_index));
            list.add(new LatLng(latitude,longitude));
        }
        c.close();
        db.close();

        // Create a heat map tile provider, passing it the list of lat long
        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder().data(list).build();

        // Add a tile overlay to the map, using the heat map tile provider.
        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }

}

package com.example.ajoy3.downloader;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private EditText urlText;
    private long fileDownloadId = -1L;
    //Rutgers Wi-Fi SSIDs
    private static final String SSID[]={"RUWireless","RUWireless_Secure","LAWN","ECE"};
    //flags to indicate chosen radio button
    private static boolean flagAnyNetwork = true;
    private static boolean flagAnyWiFi = false;
    private static boolean flagRUWiFi = false;
    private static boolean flagRUWiFiClick = false;
    //flag to execute onDownloadComplete method just once
    private static boolean flagDownloadSuccess = false;
    private static String stringUrl;
    private static String fileName;
    private static String startTime;
    private static String endTime;
    private static String preferredNetwork = "Any Network";
    private static String duration;
    private static Date downloadStartDate;
    private static Date downloadEndDate;
    public static final String logFileName = "logFile.txt";
    DownloadManager downloadMgr;
    DownloadManager.Request request;
    File file;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        file = new File(getFilesDir(),logFileName);
        //check if log file exists. This is done so that log file will be created when app is just installed and started.
        // If it doesn't then create one.
        if(!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        //Register Broadcast Receivers for Download Manager when a file download completes
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));//Broadcast intent when download completes
        //Register Broadcast Receivers for Wi-Fi Manager whenever any network state changes.
        registerReceiver(onWiFiChange, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));//Broadcast intent when network state changes
        registerReceiver(onWiFiChange, new IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION));//Broadcast intent When connection to supplicant changes.
        registerReceiver(onWiFiChange, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));//Broadcast intent when supplicant state changes.
        registerReceiver(onWiFiChange, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));//Broadcast intent when Wi-Fi connectivity state changes
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //When app is destroyed unregister the broadcast receivers.
        unregisterReceiver(onDownloadComplete);
        unregisterReceiver(onWiFiChange);
    }

    @Override
    public void onStart(){
        super.onStart();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    //Thos method is called when "Download" button is pressed
    public void downloadNow(View view) {
        urlText = (EditText)findViewById(R.id.url_input);//Read the URL from EditText box
        stringUrl = urlText.getText().toString();//Parse the URL to String
        fileName = URLUtil.guessFileName(stringUrl, null, null);//Get(Guess) the file name from URL
        uri=Uri.parse(stringUrl);//conver the URL to Uri to be used in Downlaod Manager

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);//Used to monitor network connectivity
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();//Network Info is used to get network connectivity type
        downloadMgr = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);//Download Manager is used for downloading the Uri
        //Set this flag to false. This is used in onDownloadComplete() method to ensure that the block in that method is
        //executed just once. This was found during debugging/running that onDownloadComplete() was geting called twice.
        flagDownloadSuccess = false;
        request = new DownloadManager.Request(uri);//Request Download Manager for the Uri
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();//Get the sdcard Downloads directory
        request
                .setAllowedOverRoaming(true)//cellular data will be used on roaming, to ensure seamless download capability
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)//To show download progress and on complete in notification bar
                .setVisibleInDownloadsUi(true);//To show the downloaded file in system Downloads App

        //Get start time of download operation
        downloadStartDate = new Date();
        startTime = downloadStartDate.toString();

        //Do this if network connectivity is available and if any network option is chosen.
        if (networkInfo != null && networkInfo.isConnected() && (networkInfo.getType() == connMgr.TYPE_WIFI | networkInfo.getType() == connMgr.TYPE_MOBILE) && flagAnyNetwork) {
            Context context = getApplicationContext();
            CharSequence text = "Download Will Begin on Preferred Network";//Toast message
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);//request download manager for both cellular and Wi-Fi
            fileDownloadId = downloadMgr.enqueue(request);//enqueue to start downloading
        }
        //Do this network connectivity is available and if any wifi option is chosen
        else if ((networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == connMgr.TYPE_WIFI && flagAnyWiFi)) {
            Context context = getApplicationContext();
            CharSequence text = "Download Will Begin on Preferred Network";//Toast message
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);//request download manager for Wi-Fi
            fileDownloadId = downloadMgr.enqueue(request);//enqueue to start downloading
        }
        //Schedule to download on any network when connectivity becomes available
        else if(flagAnyNetwork){
            Context context = getApplicationContext();
            CharSequence text = "Download Scheduled To Begin on Preferred Network";//Toast message
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            fileDownloadId = downloadMgr.enqueue(request);//enqueue to start downloading when network becomes available
        }
        //Schedule to download on any wifi when connectivity becomes available
        else if(flagAnyWiFi){
            Context context = getApplicationContext();
            CharSequence text = "Download Scheduled To Begin on Preferred Network";//Toast message
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            fileDownloadId = downloadMgr.enqueue(request);//enqueue to start downloading when network becomes available
        }
        //Schedule to start download on RU Wi-Fi
        else if(flagRUWiFi){
            //Set this flag to true to indicate that the block inside the onWiFiChange (broadcast) method gets executed just once.
            flagRUWiFiClick = true;
            //check if wifi connectivity is available and has Rutgers SSID
            //if yes then download file right away without waiting for Wi-Fi change broadcast
            if (flagRUWiFiClick && networkInfo != null && networkInfo.isConnected() && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI)) {
                WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
                String wifiSSID = wifiMgr.getConnectionInfo().getSSID();// Get Wi-Fi SSID

                //if SSID is not null use this regular expression to replace the starting and ending double quotes (") in obtained SSID.
                //This is done because in some mobile getSSID() method returns SSID with double quotes ad in some it does not.
                if (wifiSSID != null) {
                    wifiSSID = wifiSSID.replaceAll("^\"|\"$", "");
                    //Check if SSID matches Rutgers Wi-Fi
                    if (wifiSSID.equals(SSID[0]) | wifiSSID.equals(SSID[1]) | wifiSSID.equals(SSID[2]) | wifiSSID.equals(SSID[3])) {
                        //set this flag to flase so that this isn't executed again on another Wi-Fi state change
                        flagRUWiFiClick = false;
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);//request download manager on Wi-Fi
                        downloadStartDate = new Date();
                        startTime = downloadStartDate.toString();
                        fileDownloadId = downloadMgr.enqueue(request);//enqueue download to start on RUWiFi
                    }
                }
            }
            Context context = getApplicationContext();
            CharSequence text = "Download Scheduled To Begin on Preferred Network";//Toast message
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    //This method gets called when Radio Button is clicked
    public void chooseNetwork(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked and set the corresponding flags
        switch(view.getId()) {
            case R.id.radioButton1:
                if(checked) {
                    //Any Network
                    flagAnyNetwork = true;
                    flagAnyWiFi = false;
                    flagRUWiFi = false;
                    preferredNetwork = "Any Network";
                    break;
                }

            case R.id.radioButton2:
                if(checked) {
                    //Any WiFi only
                    flagAnyNetwork = false;
                    flagAnyWiFi = true;
                    flagRUWiFi = false;
                    preferredNetwork = "Any WiFi";
                    break;
                }

            case R.id.radioButton3:
                if(checked) {
                    //RU WiFi
                    flagAnyNetwork = false;
                    flagAnyWiFi = false;
                    flagRUWiFi = true;
                    preferredNetwork = "Rutgers WiFi";
                    break;
                }
        }
    }

    //Broadcast receiver method when downloading completes
    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DownloadManager.Query query = new DownloadManager.Query();
            Bundle extras = intent.getExtras();
            query.setFilterById(extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID));
            Cursor c = downloadMgr.query(query);//Query the downlaod manager for downloaded file information

            //Execute the following just once(flagDownloadSuccess) and if download status is successfull
            if(!flagDownloadSuccess && c.moveToFirst() && c.getInt(c.getColumnIndex(downloadMgr.COLUMN_STATUS)) == downloadMgr.STATUS_SUCCESSFUL) {
                //Call method to display download complete toast message
                showDownloadComplete(context);
                downloadEndDate = new Date();
                endTime = downloadEndDate.toString();
                long downloadTime = (downloadEndDate.getTime() - downloadStartDate.getTime());
                duration = Long.toString(downloadTime/new Long(1000));//get duration in seconds
                String throughput = Float.toString(((c.getFloat(c.getColumnIndex(downloadMgr.COLUMN_TOTAL_SIZE_BYTES))) * new Float(1000)) / new Float(downloadTime));

                WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
                String downloadSSID = wifiMgr.getConnectionInfo().getSSID();
                String toFile;
                //Use this string to write to file. This contains SSID information if Any Wi-Fi or RU Wi-Fi is chosen
                if(flagRUWiFi|flagAnyWiFi){
                    toFile = "\nFile:" + fileName + "\n" + "Preferred Network:" + preferredNetwork +": "+downloadSSID + "\n" + "Downloaded Started On:" + startTime + "\n" +
                            "Download Completed On:" + endTime + "\n" + "Download Duration:" + duration + "s" + "\n" + "Throughput:" + throughput + " bytes/s" + "\n";
                }
                //Use this string to write to file if Any Network is chosen
                else {
                    toFile = "\nFile:" + fileName + "\n" + "Preferred Network:" + preferredNetwork + "\n" + "Downloaded Started On:" + startTime + "\n" +
                            "Download Completed On:" + endTime + "\n" + "Download Duration:" + duration + "s" + "\n" + "Throughput:" + throughput + " bytes/s" + "\n";
                }
                try {
                    //Append the log data to logfile
                    FileOutputStream fout = openFileOutput(file.getName(), MODE_APPEND);
                    OutputStreamWriter osw = new OutputStreamWriter(fout);
                    osw.append(toFile);
                    osw.close();
                    fout.close();
                } catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }
                //set the flag to ensure this block is executed just once
                flagDownloadSuccess = true;
            }
            c.close();
        }
    };

    private BroadcastReceiver onWiFiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            //on any changes to Wi-Fi state check if Rutgers Wi-Fi option was chosen and clicked and also if network connectivity type is wifi
            //flagRUWiFiClick ensures this block executed just once
            if (flagRUWiFiClick && networkInfo != null && networkInfo.isConnected() && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI)) {
                WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
                String wifiSSID = wifiMgr.getConnectionInfo().getSSID();// Get Wi-Fi SSID

                //if SSID is not null use this regular expression to replace the starting and ending double quotes (") in obtained SSID.
                //This is done because in some mobile getSSID() method returns SSID with double quotes ad in some it does not.
                if (wifiSSID != null) {
                    wifiSSID = wifiSSID.replaceAll("^\"|\"$", "");
                    //Check if SSID matches Rutgers Wi-Fi
                    if (wifiSSID.equals(SSID[0]) | wifiSSID.equals(SSID[1]) | wifiSSID.equals(SSID[2]) | wifiSSID.equals(SSID[3])) {
                        //set this flag to flase so that this isn't executed again on another Wi-Fi state change
                        flagRUWiFiClick = false;
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);//request download manager on Wi-Fi
                        downloadStartDate = new Date();
                        startTime = downloadStartDate.toString();
                        fileDownloadId = downloadMgr.enqueue(request);//enqueue download to start on RUWiFi
                    }
                }
            }
        }
    };

    //Method called to display "Downlaod Complete" toast message
    private void showDownloadComplete(Context context){
        CharSequence text = "Download Complete";
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    //This method is called when "View Log" button is clicked
    public void viewLog(View view) {
        Intent intent = new Intent(this, Main2Activity.class);
        //Start another activity to display logs
        startActivity(intent);
    }
}

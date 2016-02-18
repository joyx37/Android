package com.example.ajoy3.mycamera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class AutoVideoRecord extends AppCompatActivity implements SensorEventListener {

    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mediaRecorder;
    private Context myContext;
    private LinearLayout cameraPreview;
    private static boolean cameraFront = false;
    private boolean recording = false;
    private static File file;
    private static int cameraId = -1;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private static final int Y_THRESHOLD = 7;
    private static final int SPEED_THRESHOLD = 15;
    private static long mLastTime = 0;
    private float mLastX=0.0f, mLastY=0.0f, mLastZ=0.0f;
    ImageView imageView2;
    TextView textView, textView2;
    Camera.PictureCallback mPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_video_record);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = getApplicationContext();
        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);
        imageView2 =(ImageView)findViewById(R.id.imageView2);
        textView = (TextView)findViewById(R.id.textView);
        textView2 = (TextView)findViewById(R.id.textView2);

        mCamera = Camera.open(findBackFacingCamera());
        mPreview = new CameraPreview(myContext, mCamera);
        mPreview.refreshCamera(mCamera);
        cameraPreview.addView(mPreview);


        mediaRecorder = new MediaRecorder();
        mPicture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (bitmap == null){
                    Log.d("bitmap", "Bitmap is null");
                    return;
                }
                Toast.makeText(myContext, "Image captured!", Toast.LENGTH_SHORT).show();
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
                String mString = getIntent().getStringExtra("UserComment");
                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(file.toString());
                    exif.setAttribute("UserComment", mString);
                    exif.saveAttributes();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                imageView2.setImageBitmap(BitmapFactory.decodeFile(file.toString()));

                try {
                    exif = new ExifInterface(file.toString());
                    textView.setText(exif.getAttribute("UserComment"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    mediaRecorder.stop();
                    releaseMediaRecorder(); // release the MediaRecorder object
                    Toast.makeText(myContext, "Video captured!", Toast.LENGTH_SHORT).show();
                    Log.d("Info", "Video CAptured");

                    mCamera.setDisplayOrientation(90);
                    mCamera.takePicture(null, null, mPicture);

                    MediaScannerConnection.scanFile(myContext,
                            new String[]{file.toString()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i("ExternalStorage", "Scanned " + path + ":");
                                    Log.i("ExternalStorage", "-> uri=" + uri);
                                }
                            });
                }
            }
        });

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this,senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // when on Pause, release camera in order to be used from other
        // applications
        releaseCamera();
        senSensorManager.unregisterListener(this);
    }

    @Override
    protected void onStart(){
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        if (!myContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCamera == null) {
            mCamera = Camera.open(findBackFacingCamera());
            mPreview.refreshCamera(mCamera);
        }
    }

    //call when sensor detetcts
    public void StartRecording(){
        try {
            File directory = new File(Environment.getExternalStorageDirectory() + "/MyCamera/");
            directory.mkdirs();
            String videoName = Long.toString(new Date().getTime()) + ".mp4";
            file = new File(directory, videoName);

            //mCamera.stopPreview();
            mCamera.lock();
            mCamera.unlock();

            mediaRecorder.setCamera(mCamera);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setProfile(CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P));
            mediaRecorder.setOutputFile(file.toString());
            mediaRecorder.setMaxDuration(10000); // Set max duration 10 sec.
            mediaRecorder.prepare();
            mediaRecorder.start();
            Toast.makeText(myContext, "Video capture started!", Toast.LENGTH_LONG).show();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
        } catch (IOException e) {
            releaseMediaRecorder();
        } catch (final Exception ex) {
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            //mCamera.lock(); // lock camera for later use
        }
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private int findBackFacingCamera() {
        // Search for the back facing camera
        // get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        // for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            textView2.setText("X:"+x+"\nY:"+y+"\nZ:"+z);

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
            if ((yAbs>Y_THRESHOLD)&& (speed > SPEED_THRESHOLD) && (!recording)) {
                StartRecording();
                recording = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

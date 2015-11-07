package com.example.ajoy3.gestures;

import android.app.DownloadManager;
import android.app.UiModeManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int NumberOfMultiPoint = 5;
    private FrameLayout mFrameLayout;
    //gesture colors
    private int paintColor[] = {0xFFFF0000,0xFF000CFA,0xFF00EA5A,0xFFDCF70F,0xFF42D3FF};

    //Draw Gesture objects to draw cirle and gesture
    private static DrawGesture[] mCurrentTouch = new DrawGesture[NumberOfMultiPoint];

    //text view to display X and Y
    private static TextView[] touchInfo = new TextView[NumberOfMultiPoint];

    //To store gesture path information
    private static Path[] gesturePath = new Path[NumberOfMultiPoint];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFrameLayout = (FrameLayout)findViewById(R.id.frameLayout);

        //text view to display pointer ID, X and Y
        touchInfo[0] = (TextView)findViewById(R.id.textView1);
        touchInfo[1] = (TextView)findViewById(R.id.textView2);
        touchInfo[2] = (TextView)findViewById(R.id.textView3);
        touchInfo[3] = (TextView)findViewById(R.id.textView4);
        touchInfo[4] = (TextView)findViewById(R.id.textView5);

        //create gesture path objects and their corresponding paint color attributes.
        for(int i=0;i<NumberOfMultiPoint;i++) {
            gesturePath[i] = new Path();
        }

        mFrameLayout.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int pointerIndex = event.getActionIndex();
                int pointerID = event.getPointerId(pointerIndex);

                float x = event.getX(pointerIndex);
                float y = event.getY(pointerIndex);

                switch (event.getActionMasked()) {
                    //detect touch
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN: {

                        mCurrentTouch[pointerID] = new DrawGesture(getApplicationContext(), pointerID);
                        mCurrentTouch[pointerID].setLoc(x, y);
                        mFrameLayout.addView(mCurrentTouch[pointerID]);
                        //move the gesture source point to the point of touch detection.
                        gesturePath[pointerID].moveTo(x, y);
                        //update textview
                        touchInfo[pointerID].setText("Pointer ID = " + pointerID + "  X = " + x + "  Y = " + y);
                        break;
                    }

                    //detect finger/touch lifted up from display
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP: {
                        if (mCurrentTouch[pointerID] != null) {
                            //call invalidate to execute the onDraw() method
                            mCurrentTouch[pointerID].invalidate();
                        }

                        break;
                    }

                    //Detect gesture move
                    case MotionEvent.ACTION_MOVE: {
                        for (int i = 0; i < event.getPointerCount(); i++) {
                            int ID = event.getPointerId(i);

                            if (mCurrentTouch[ID] != null) {
                                //draw the gesture path
                                //gestures are approximated to be lines connecting points
                                gesturePath[ID].lineTo(event.getX(i), event.getY(i));

                                mCurrentTouch[ID].setLoc(event.getX(i), event.getY(i));
                                //call invalidate to execute the onDraw() method
                                mCurrentTouch[ID].invalidate();
                                //update textview
                                touchInfo[ID].setText("Pointer ID = " + ID + "  X = " + event.getX(i) + "  Y = " + event.getY(i));
                            }
                        }
                        break;
                    }
                }

                return true;
            }
        });

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

    private class DrawGesture extends View {
        private float xLoc,yLoc;
        //get dots per inch for the device display
        int dpi = getApplicationContext().getResources().getDisplayMetrics().densityDpi;
        //radius of circle in inches
        float x = 0.3f;
        //get number of dots for a x inch radius
        private final float touchRadius = x*(new Float(dpi));
        //Paint objects for different paint color for every touch circle
        private final Paint touchPaint = new Paint();
        //Paint objects for different paint color for every touch gesture path
        private final Paint drawPaint = new Paint();
        private int ID;

        public DrawGesture(Context context, int pointerID) {
            super(context);

            //pointer ID of the touch
            ID = pointerID;
            //paint attributes for circle
            touchPaint.setStyle(Paint.Style.FILL);
            touchPaint.setColor(paintColor[ID]);
            touchPaint.setAntiAlias(true);
            touchPaint.setDither(true);
            touchPaint.setAlpha(100);

            //paint attributes for gesture path
            drawPaint.setColor(paintColor[ID]);
            drawPaint.setAntiAlias(true);
            drawPaint.setStrokeWidth(10);
            drawPaint.setStyle(Paint.Style.STROKE);
            drawPaint.setStrokeJoin(Paint.Join.ROUND);
            drawPaint.setStrokeCap(Paint.Cap.ROUND);
            drawPaint.setDither(true);

        }

        public void setLoc(float x, float y){
            xLoc = x;
            yLoc = y;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            //draw circle with center as the touch point as given by xLoc and yLoc.
            canvas.drawCircle(xLoc, yLoc, touchRadius, touchPaint);
            //draw the gesture path
            canvas.drawPath(gesturePath[ID], drawPaint);
        }

    }

}

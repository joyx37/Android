package com.example.ajoy3.steganography;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class Decrypt extends AppCompatActivity {

    private ImageView mImageView;
    private TextView mTextView;
    private static Boolean success = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypt);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mImageView = (ImageView)findViewById(R.id.imageView);
        mTextView = (TextView)findViewById(R.id.textView);
    }

    public void Decrypt(View view) {
        File directory = new File(Environment.getExternalStorageDirectory()+"/Steganos/Images/Sent/UserName/");
        directory.mkdirs();
        String imageName = "msg";
        String msgImage = imageName+"_msg"+".png";
        String decodedImage = imageName+"_decoded"+".png";

        Bitmap receivedBitmap = BitmapFactory.decodeFile((new File(directory, msgImage)).toString());
        mImageView.setImageBitmap(receivedBitmap);

        int encodedWidth = receivedBitmap.getWidth();
        int encodedHeight = receivedBitmap.getHeight();
        int code = getHiddenInt(receivedBitmap.getPixel(encodedWidth - 1, encodedHeight - 1));

        success = false;
        if(code == Constants.TEXT_IN_IMAGE) {
            int pixelInt;
            int msgSize;
            char msgCharArray[] = null;
            for (int i = 0; (i < encodedHeight) && (!success); i++) {
                for (int j = 0; (j < encodedWidth) && (!success); j++) {
                    int pixel = receivedBitmap.getPixel(j, i);
                    pixelInt = getHiddenInt(pixel);
                    if ((i == 0 && j == 0)) {
                        msgSize = pixelInt;
                        msgCharArray = new char[msgSize];
                    } else if ((pixelInt == Constants.TEXT_IN_IMAGE)) {
                        String msg = new String(msgCharArray);
                        String key = "F38B6106BC8DFA9BE07735F86932EA8A";
                        try {
                            mTextView.setText(AES.decrypt(key,msg));
                        } catch (Exception e) {
                            Toast.makeText(this,"Error in Decryption Key",Toast.LENGTH_LONG);
                            e.printStackTrace();
                        }
                        success = true;
                    } else {
                        msgCharArray[(i * encodedWidth) + j - 1] = (char)pixelInt;
                    }
                }
            }
        }
        else if(code == Constants.IMAGE_IN_IMAGE){
            Decode imageInImage = new Decode();
            imageInImage.Decode(getApplicationContext());

            Toast.makeText(getApplicationContext(),"Image Decoding Started",Toast.LENGTH_SHORT).show();
            imageInImage.execute((new File(directory, msgImage)).toString(), (new File(directory, decodedImage)).toString());
        }
    }

    public static int getHiddenInt(int pixelColor){
        int[] digits = new int[4];
        digits[0] = intToDigits(Color.red(pixelColor))[2];
        digits[1] = intToDigits(Color.green(pixelColor))[2];
        digits[2] = intToDigits(Color.blue(pixelColor))[2];

        return digitsToInt(digits);
    }

    public static int digitsToInt(int[] digits){
        int a = (digits[0] * 100) + (digits[1] * 10) + digits[2];
        return (digits[0] * 100) + (digits[1] * 10) + digits[2];

    }

    public static int[] intToDigits(int aNumber){
        int[] digits = new int[3];
        digits[0] = aNumber / 100;
        digits[1] = (aNumber % 100) / 10;
        digits[2] = aNumber % 10;
        return digits;

    }
}

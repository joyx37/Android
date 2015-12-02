package com.example.ajoy3.steganography;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;

/**
 * Created by ajoy3 on 11/29/2015.
 */
public class Decode extends AsyncTask<String,Void,Void> {

    private static Boolean success = false;
    private static Context mContext;
    private static String encoded, writeTo;

    public void Decode(Context context){
        mContext = context;
    }
    @Override
    protected Void doInBackground(String... params) {
        encoded = params[0];
        writeTo = params[1];

        decodeImage(encoded, writeTo);

        return null;
    }

    @Override
    protected void onPostExecute(Void v){
        if(success) {
            Toast.makeText(mContext, "Image Decoding Complete", Toast.LENGTH_SHORT).show();
        }
    }

    public static Bitmap decodeImage(String encoded,String writeTo){

        Bitmap encodedImage = BitmapFactory.decodeFile(encoded);

        int encodedWidth = encodedImage.getWidth();
        int encodedHeight = encodedImage.getHeight();

        int[] hiddenPixels = null;
        int hiddenWidth = 0;
        int hiddenHeight = 0;
        int pixelInt;

        for (int i = 0; i < encodedHeight; i++){
            for (int j = 0; j < encodedWidth; j++){
                int pixel = encodedImage.getPixel(j,i);
                pixelInt = getHiddenInt(pixel);
                if((i==0 && j==0)){
                    hiddenWidth = getHiddenInt(encodedImage.getPixel(j,i));
                }
                else if((i==0 && j==1)){
                    hiddenHeight = getHiddenInt(encodedImage.getPixel(j,i));
                    hiddenPixels = new int[4 * hiddenWidth * hiddenHeight];
                }
                else if ((pixelInt == Constants.IMAGE_IN_IMAGE)){
                    return rebuildImage(hiddenPixels,hiddenWidth,hiddenHeight,writeTo);
                }
                else{
                        hiddenPixels[(i * encodedWidth) + j - 2] = pixelInt;
                }
            }
        }
        return null;
    }

    public static Bitmap rebuildImage(int[] pixels,int width,int height,String writeTo){
        int[] color  = new int[pixels.length/4];
        for (int i = 0; i < width*height; i++){
            color[i] = Color.argb(pixels[(4 * i)], pixels[(4 * i) + 1], pixels[(4 * i) + 2], pixels[(4*i)+3]);
        }

        Bitmap hiddenImage = Bitmap.createScaledBitmap(Bitmap.createBitmap(color, width, height, Bitmap.Config.ARGB_8888),
                                                         (int) (width / Constants.SECRET_DOWN_SCALE), (int)(height/Constants.SECRET_DOWN_SCALE),true);
        try{
            FileOutputStream output = new FileOutputStream(writeTo);
            hiddenImage.compress(Bitmap.CompressFormat.PNG,100,output);
            output.close();
            success = true;
        }
        catch(Exception e){
            System.out.println(e);
        }
        return hiddenImage;

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

package com.example.ajoy3.steganography;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * Created by ajoy3 on 11/28/2015.
 */
public class Encode extends AsyncTask<String,Void,Void> {

    private static Boolean success = false;
    private static Context mContext;
    private static byte[] key;
    private static Bitmap imageToHide,carrierImage;
    private static String hideThis,hideIn,writeTo;

    public void Encode(Context context){
        mContext = context;
    }
    @Override
    protected Void doInBackground(String... params) {
        hideThis = params[0];
        hideIn = params[1];
        writeTo = params[2];

        encodePicture(hideThis, hideIn, writeTo);

        return null;
    }

    @Override
    protected void onPostExecute(Void v){
        if(success) {
            Toast.makeText(mContext, "Stegnos Complete", Toast.LENGTH_SHORT).show();
        }
    }

    public static void encodePicture(String hideThis,String hideIn,String writeTo){
        //Bitmap imageToHide,placeToHide;

        imageToHide = BitmapFactory.decodeFile(hideThis);
        imageToHide = imageToHide.copy(imageToHide.getConfig(), true);

        carrierImage = BitmapFactory.decodeFile(hideIn);
        carrierImage = carrierImage.copy(carrierImage.getConfig(),true);

        int imageWidth = imageToHide.getWidth();
        int imageHeight = imageToHide.getHeight();
        int[] imagePixels = new int[4 * imageWidth * imageHeight];

        for (int i = 0; i < imageHeight; i++){
            for (int j = 0; j < imageWidth; j++){
                imagePixels[4 * ((i * imageWidth) + j)] = Color.alpha(imageToHide.getPixel(j, i));
                imagePixels[4 * ((i * imageWidth) + j) + 1] = Color.red(imageToHide.getPixel(j, i));
                imagePixels[4 * ((i * imageWidth) + j) + 2] = Color.green(imageToHide.getPixel(j, i));
                imagePixels[4 * ((i * imageWidth) + j) + 3] = Color.blue(imageToHide.getPixel(j, i));
            }
        }

        int carrierWidth = carrierImage.getWidth();
        int carrierHeight = carrierImage.getHeight();
        int color;

        //store code for image in image in last pixel
        color = changeColor(carrierImage.getPixel(carrierWidth-1,carrierHeight-1),Constants.IMAGE_IN_IMAGE);
        carrierImage.setPixel(carrierWidth-1,carrierHeight-1,color);

        for(int i = 0; i < carrierHeight; i++){
            for (int j = 0; j < carrierWidth; j++){
                if((i == 0) && (j == 0)){
                    color = changeColor(carrierImage.getPixel(j,i),imageWidth);
                    carrierImage.setPixel(0,0,color);
                }
                else if((i == 0) && (j == 1)){
                    color = changeColor(carrierImage.getPixel(j,i),imageHeight);
                    carrierImage.setPixel(i,j,color);
                }
                else if(((i * carrierWidth) + j) == (imagePixels.length + 2)){
                    color = changeColor(carrierImage.getPixel(j,i),Constants.IMAGE_IN_IMAGE);
                    carrierImage.setPixel(j, i,color);//carrierImage.getPixel(j,i)&0xFFF0F0F0)|0xFFF4F2F3
                    try{
                        FileOutputStream output = new FileOutputStream(writeTo);
                        carrierImage.compress(Bitmap.CompressFormat.PNG,100,output);
                        output.close();
                        success = true;
                        return;
                    }
                    catch(Exception e){
                        System.out.println(e);
                        return;
                    }
                }
                else{
                    carrierImage.setPixel(j,i, changeColor(carrierImage.getPixel(j,i), imagePixels[(i * carrierWidth) + j - 2]));
                }
            }
        }
    }

    public static int changeColor(int currentColor,int inputNumber){

        int[] number = intToDigits(inputNumber);

        int alpha = Color.alpha(currentColor);

        int[] red = intToDigits(Color.red(currentColor));
        red[2] = number[0];

        int[] green = intToDigits(Color.green(currentColor));
        green[2] = number[1];

        int[] blue = intToDigits(Color.blue(currentColor));
        blue[2] = number[2];

        if(number[2] > 5){
            if(blue[0]==2 && blue[1]==5){
                blue[1] = 4;
            }
        }
        if(number[1] > 5){
            if (red[0] == 2 && red[1] == 5) {
                red[1] = 4;
            }
        }
        if(number[0] > 5){
            if(green[0]==2 && green[1]==5){
                green[1] = 4;
            }
        }

        return Color.argb(alpha, digitsToInt(red),digitsToInt(green),digitsToInt(blue));

    }

    public static int[] intToDigits(int aNumber){

        int[] digits = new int[3];
        digits[0] = aNumber / 100;
        digits[1] = (aNumber % 100) / 10;
        digits[2] = aNumber % 10;
        return digits;

    }

    public static int digitsToInt(int[] digits){

        return (digits[0] * 100) + (digits[1] * 10) + digits[2];

    }

}

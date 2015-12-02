package com.example.ajoy3.steganography;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class ChooseTextorImage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_textor_image);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    public void TextInImageStegnos(View view) {

        Intent intent = new Intent(this,TextInImageStegnos.class);
        startActivity(intent);
    }

    public void ImageInImageStegnos(View view) {
        Intent intent = new Intent(this,ImageInImageStegnos.class);
        startActivity(intent);
    }
}

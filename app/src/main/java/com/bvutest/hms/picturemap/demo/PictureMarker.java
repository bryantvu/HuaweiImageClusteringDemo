package com.bvutest.hms.picturemap.demo;

import android.graphics.Bitmap;

//contains image and GPS coords for marker
public class PictureMarker {

    public Bitmap image;
    public double lati;
    public double longi;

    PictureMarker(Bitmap image, double lati, double longi){
        this.image = image;
        this.lati = lati;
        this.longi = longi;
    }

}

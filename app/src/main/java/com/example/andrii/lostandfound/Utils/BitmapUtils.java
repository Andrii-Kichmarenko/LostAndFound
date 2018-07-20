package com.example.andrii.lostandfound.Utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;

public class BitmapUtils {

    public static Bitmap rotateBitmap(Bitmap source, float rotation) {
        if (rotation == 0)
            return source;
        int w = source.getWidth();
        int h = source.getHeight();
        Matrix m = new Matrix();
        m.postRotate(rotation);
        Bitmap bitmap = Bitmap.createBitmap(source, 0, 0, w, h, m, true);

        return bitmap;
    }

    public static byte[] getBytesFromBitmap(Bitmap bitmap, int quality){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    public static Bitmap cropCenterBitmap(Bitmap srcBitmap){
        Bitmap dstBitmap;
        if (srcBitmap.getWidth() >= srcBitmap.getHeight()){

            dstBitmap = Bitmap.createBitmap(
                    srcBitmap,
                    srcBitmap.getWidth()/2 - srcBitmap.getHeight()/2,
                    0,
                    srcBitmap.getHeight(),
                    srcBitmap.getHeight()
            );

        }else{

            dstBitmap = Bitmap.createBitmap(
                    srcBitmap,
                    0,
                    srcBitmap.getHeight()/2 - srcBitmap.getWidth()/2,
                    srcBitmap.getWidth(),
                    srcBitmap.getWidth()
            );
        }
        return dstBitmap;
    }
}

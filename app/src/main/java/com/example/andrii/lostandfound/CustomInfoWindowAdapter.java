package com.example.andrii.lostandfound;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private static final String TAG = "CustomInfoWindowAdapter";

    private View mWindow;
    private Context mContext;

    private ItemInformation itemInfo;
    private Bitmap bitmap;

    public CustomInfoWindowAdapter(Context context) {
        mContext = context;
        mWindow = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null);

    }

    private void rendowWindowText(Marker marker, View view) {
        itemInfo = MapsActivity.getItemInfotmation(marker);
        bitmap = BitmapFactory.decodeFile(itemInfo.getImageFile().getAbsolutePath());


        ImageView image = view.findViewById(R.id.iv_image);
        image.setImageBitmap(bitmap);

        String title = marker.getTitle();
        TextView tvTitle = view.findViewById(R.id.tv_title);

        if (!title.equals("")) {
            tvTitle.setText(title);
        }

        String snippet = marker.getSnippet();
        TextView tvDescription = view.findViewById(R.id.tv_desctiption);

        if (!snippet.equals("")) {
            tvDescription.setText(snippet);
        }
    }

    @Override
    public View getInfoWindow(Marker marker) {
        rendowWindowText(marker, mWindow);
        return mWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        rendowWindowText(marker, mWindow);
        return mWindow;
    }

}

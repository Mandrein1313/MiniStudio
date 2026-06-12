package com.dev.ministudio;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.GridView;

public class IconAdapter extends BaseAdapter {
    private Context context;
    private int[] iconList;

    public IconAdapter(Context context, int[] iconList) {
        this.context = context;
        this.iconList = iconList;
    }

    @Override
    public int getCount() { return iconList.length; }

    @Override
    public Object getItem(int position) { return iconList[position]; }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(150, 150));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }
        imageView.setImageResource(iconList[position]);
        return imageView;
    }
}

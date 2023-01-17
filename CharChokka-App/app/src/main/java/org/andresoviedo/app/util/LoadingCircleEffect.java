package org.andresoviedo.app.util;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class LoadingCircleEffect {
    private Context context;
    private FrameLayout frameLayout;

    private ImageView iv;

    public LoadingCircleEffect(int resid, Context context, FrameLayout frameLayout){
        this.context = context;
        this.frameLayout = frameLayout;

        iv = new ImageView(context);

        iv.setBackgroundResource(resid);

        final AnimationDrawable ad = (AnimationDrawable)iv.getBackground();

        frameLayout.addView(iv);

        ad.start();
    }

    public void stop(){
        frameLayout.removeView(iv);
    }
}

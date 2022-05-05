package com.example.screentimeusabilitydemo;

import static android.content.Context.WINDOW_SERVICE;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

public class Window {
    private Context context;
    private View view;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private LayoutInflater layoutInflater;
    private static final char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f'};

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Window(Context context) {
        this.context = context;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSPARENT);
        params.gravity = Gravity.CENTER;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = layoutInflater.inflate(R.layout.overlay_window, null);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void open() {
//        try {
            if (view.getWindowToken() == null && view.getParent() == null) {
//                ColorMatrix cm = new ColorMatrix();
//                cm.setSaturation(0);
//                Paint greyscalePaint = new Paint();
//                greyscalePaint.setColorFilter(new ColorMatrixColorFilter(cm));
//                view.setLayerType(View.LAYER_TYPE_HARDWARE, greyscalePaint);
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    windowManager.addView(view, params);
                }, 100);
                transition(0, 1);
            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private void transition(int i, int j) {
        if (i == 10) {
            return;
        }
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            String color = "#" + hex[i] + hex[j] + "ffffff";
            view.setBackgroundColor(Color.parseColor(color));
            try {
                windowManager.updateViewLayout(view, params);
            } catch (IllegalArgumentException e) {
                return;
            }
            Log.d("Update color", color);
            if (j < 15) {
                transition(i, j+1);
            } else {
                transition(i+1, 0);
            }
        }, 100);

    }
    
    public void close() {
        // remove the view from the window
        windowManager.removeView(view);
        // invalidate the view
        view.invalidate();
        // remove all views
        ViewGroup viewGroup = (ViewGroup) view.getParent();
        if (viewGroup != null) {
            viewGroup.removeAllViews();
        }
    }
}

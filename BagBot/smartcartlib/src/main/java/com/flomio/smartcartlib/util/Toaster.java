package com.flomio.smartcartlib.util;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class Toaster {
    private Toast toast;
    private Context context;
    int gravity = Gravity.CENTER;

    public Toaster(Context context) {
        this.context = context;
    }
    public Toast toast(String text) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this.context, text,
                Toast.LENGTH_SHORT);
        toast.setGravity(gravity, 0, 0);
        toast.show();
        return toast;
    }
}

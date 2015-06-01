package com.nbplus.vbroadlauncher.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nbplus.vbroadlauncher.R;

/**
 * Created by basagee on 2015. 5. 27..
 */
public class IButton extends LinearLayout {

    private LinearLayout layout;
    private ImageView image;
    private TextView text;
    private Object tag;

    public IButton(Context context) {
        this(context, null);
    }

    public IButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.btn_image, this, true);

        layout = (LinearLayout) view.findViewById(R.id.btn_layout);

        image = (ImageView) view.findViewById(R.id.btn_icon);
        text = (TextView) view.findViewById(R.id.btn_text);

        if (attrs != null) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ImageButtonStyle);

            Drawable drawable = attributes.getDrawable(R.styleable.ImageButtonStyle_button_icon);
            if (drawable != null) {
                image.setImageDrawable(drawable);
            }

            String str = attributes.getString(R.styleable.ImageButtonStyle_button_text);
            int textSize = attributes.getDimensionPixelSize(R.styleable.ImageButtonStyle_button_textSize, -1);
            if (textSize > 0) {
                text.setTextSize(textSize);
            }
            ColorStateList textColor = attributes.getColorStateList(R.styleable.ImageButtonStyle_button_textColor);
            if (textColor != null) {
                text.setTextColor(textColor);
            }

            text.setText(str);

            attributes.recycle();
        }

    }

    @Override
    public void setOnClickListener(final OnClickListener l) {
        super.setOnClickListener(l);
        layout.setOnClickListener(l);
    }

    public void setDrawable(int resId) {
        image.setImageResource(resId);
    }

    public void setText(String text) {
        this.text.setText(text);
    }

    public void setBackground(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            layout.setBackground(getResources().getDrawable(resId, null));
        } else {
            layout.setBackground(getResources().getDrawable(resId));
        }
    }

    public void setTag(Object obj) {
        layout.setTag(obj);
    }
}

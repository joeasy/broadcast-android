package com.nbplus.vbroadlauncher.widget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.nbplus.vbroadlauncher.R;

/**
 * Created by basagee on 2015. 6. 23..
 */
public class ProgressDialog extends Dialog {
    private TextView mText;

    public ProgressDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 지저분한(?) 다이얼 로그 제목을 날림
        setContentView(R.layout.dialogfragment_progress); // 다이얼로그에 박을 레이아웃

        mText = (TextView) findViewById(R.id.progress_label);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 0.8f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        getWindow().setAttributes(lp);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
    }

    public void setText(String str) {
        if (this.mText != null) {
            this.mText.setText(str);
        }
    }
}

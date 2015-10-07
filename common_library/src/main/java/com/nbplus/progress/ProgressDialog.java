/*
 * Copyright (c) 2015. Basagee Yun. (www.basagee.tk)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nbplus.progress;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.basdroid.common.R;

/**
 * Created by basagee on 2015. 6. 23..
 */
public class ProgressDialog extends Dialog {
    private TextView mText;

    public ProgressDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        // fullscreen without statusbar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.dialogfragment_progress); // 다이얼로그에 박을 레이아웃

        mText = (TextView) findViewById(R.id.progress_label);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 0.3f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        getWindow().setAttributes(lp);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
    }

    public void setText(String str) {
        if (this.mText != null) {
            this.mText.setText(str);
        }
    }
}

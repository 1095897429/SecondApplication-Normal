package com.ngbj.browser2.activity;

import android.widget.TextView;

import com.ngbj.browser2.R;
import com.ngbj.browser2.base.BaseActivity;

import butterknife.BindView;
import butterknife.OnClick;

public abstract class CommonHeadActivity extends BaseActivity {

    @BindView(R.id.center_title)
    TextView center_title;

   @OnClick(R.id.back)
    public void back(){
       finish();
   }
}

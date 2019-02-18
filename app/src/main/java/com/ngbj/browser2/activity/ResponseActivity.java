package com.ngbj.browser2.activity;


import com.ngbj.browser2.R;

public  class ResponseActivity extends CommonHeadActivity {



    @Override
    protected int getLayoutId() {
        return R.layout.activity_response;
    }

    @Override
    protected void initDatas() {
        center_title.setText("免责条款");
    }



}

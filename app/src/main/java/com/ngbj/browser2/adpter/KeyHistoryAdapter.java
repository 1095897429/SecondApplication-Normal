package com.ngbj.browser2.adpter;


import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.ngbj.browser2.R;
import com.ngbj.browser2.bean.KeyBean;

import java.util.List;


public class KeyHistoryAdapter extends BaseQuickAdapter<KeyBean,BaseViewHolder> {

    public KeyHistoryAdapter(List<KeyBean> data) {
        super(R.layout.activity_history_key_item,data);
    }

    @Override
    protected void convert(BaseViewHolder helper, KeyBean item) {

        helper.setText(R.id.title, item.getKeyName());


    }
}
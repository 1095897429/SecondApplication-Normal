package com.ngbj.browser2.mvp.contract.app;


import com.ngbj.browser2.base.BaseContract;

/**
 * Date:2018/7/18
 * author:zl
 * 备注：闪屏界面具体的契约类
 */
public interface SplashContract {

    //界面展示逻辑
    interface View extends BaseContract.BaseView{
        void showOrder();
    }

    interface Presenter<T> extends BaseContract.BasePresenter<T>{
        void getOrder();
    }
}

package com.ngbj.browser2.mvp.presenter.app;


import com.ngbj.browser2.base.RxPresenter;
import com.ngbj.browser2.mvp.contract.app.SplashContract;

/**
 * Date:2018/7/18
 * author:zl
 * 备注：闪屏页的具体逻辑
 *       RxPresenter<T> 代表的是同等级下的View接口，面向接口编程
 */
public class SplashPresenter extends RxPresenter<SplashContract.View>
                implements SplashContract.Presenter<SplashContract.View>{

    @Override
    public void getOrder() {
        mView.showOrder();
    }
}

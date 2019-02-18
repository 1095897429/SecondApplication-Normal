package com.ngbj.browser2.mvp.contract.app;


import com.ngbj.browser2.base.BaseContract;
import com.ngbj.browser2.bean.LoginResult;

/**
 * Date:2018/7/18
 * author:zl
 * 备注：
 */
public interface LoginContract {

    interface View extends BaseContract.BaseView{
            void showLoginSuccess(LoginResult loginResult);
    }


    interface Presenter<T> extends BaseContract.BasePresenter<T>{
            void getLoginSuccess();
    }
}

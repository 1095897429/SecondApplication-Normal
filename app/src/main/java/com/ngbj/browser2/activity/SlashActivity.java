package com.ngbj.browser2.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ngbj.browser2.R;
import com.ngbj.browser2.base.BaseActivity;
import com.ngbj.browser2.bean.OriData;
import com.ngbj.browser2.bean.SlashAdBean;
import com.ngbj.browser2.db.DBManager;
import com.ngbj.browser2.network.retrofit.helper.RetrofitHelper;
import com.ngbj.browser2.network.retrofit.response.BaseObjectSubscriber;
import com.ngbj.browser2.util.DeviceIdHepler;
import com.ngbj.browser2.util.SPHelper;
import com.ngbj.browser2.util.ScreenHelper;
import com.ngbj.browser2.util.StringUtils;
import com.socks.library.KLog;
import com.umeng.analytics.AnalyticsConfig;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.commonsdk.statistics.common.DeviceConfig;

import org.w3c.dom.Text;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import cn.jpush.android.api.JPushInterface;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/***
 * 闪屏界面
 * 1.首先在引导页授权下，不利用第三方库，采用默认的
 */

public class SlashActivity extends BaseActivity {


    @BindView(R.id.splashImage)
    ImageView imageView;

    @BindView(R.id.start_skip_count_down)
    TextView mCountDownTextView;


    SlashAdBean slashAdBean;
    MyCountDownTimer mCountDownTimer;
    Runnable myRunnable;


    @SuppressLint("HandlerLeak")
    Handler tmpHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 1){

            }else if(msg.what == 0){
                imageView.setImageResource(R.mipmap.welcome);
                imageView.setScaleType(ImageView.ScaleType.CENTER);
                Intent intent = new Intent(SlashActivity.this, MainActivityNew.class);
                startActivity(intent);
                finish();
            }
        }
    };


    class MyCountDownTimer extends CountDownTimer {
        /**
         * @param millisInFuture
         *      表示以「 毫秒 」为单位倒计时的总数
         *      例如 millisInFuture = 1000 表示1秒
         *
         * @param countDownInterval
         *      表示 间隔 多少微秒 调用一次 onTick()
         *      例如: countDownInterval = 1000 ; 表示每 1000 毫秒调用一次 onTick()
         *
         */

        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }


        public void onFinish() {
            mCountDownTextView.setText("0s 跳过");
        }

        public void onTick(long millisUntilFinished) {
            mCountDownTextView.setText( millisUntilFinished / 1000 + "s 跳过");
        }

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_slash;
    }

    @Override
    protected void initWidget() {
        if (!isTaskRoot()) {
            finish();
            return;
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        myRunnable = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SlashActivity.this, MainActivityNew.class);
                startActivity(intent);
                finish();
            }
        };


        //TODO 判断时间戳，设置登录状态
        DBManager dbManager = DBManager.getInstance(this);
        if(dbManager.queryUserInfo()!= null &&  dbManager.queryUserInfo().size() != 0){
            int expireTime = dbManager.queryUserInfo().get(0).getExpire_time();
            int currentTiem = (int) (System.currentTimeMillis()/1000);//获取系统时间的10位的时间戳
            KLog.d("currentTiem: " + currentTiem);
            if(currentTiem > expireTime){//已失效
                DBManager.getInstance(this).deleteUserInfo();
                SPHelper.put(this,"isLogin",false);
                SPHelper.put(this,"token","");
            }
        }

        //TODO 为空的时候 进入都获取当日最后的时间
        String str2 = (String) SPHelper.get(this,"last_today_time","");
        if(TextUtils.isEmpty(str2)){
            String result = StringUtils.getYear_Month_Day();
            SPHelper.put(this,"last_today_time",result);
        }

        //初始化接口
        getData();

//        imageView.setImageResource(R.mipmap.welcome);
        mCountDownTextView.setVisibility(View.GONE);
        tmpHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SlashActivity.this, MainActivityNew.class);
                startActivity(intent);
                finish();
            }
        },1000);

        //第一次进入就默认为false
//        boolean isNetWork = (boolean) SPHelper.get(this,"is_network",false);
//        if (isNetWork) {
//            getAdData();
//        } else {
//            imageView.setImageResource(R.mipmap.welcome);
//            mCountDownTextView.setVisibility(View.GONE);
//            tmpHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    Intent intent = new Intent(SlashActivity.this, MainActivityNew.class);
//                    startActivity(intent);
//                    finish();
//                }
//            },1000);
//        }
    }



    @Override
    protected void initDatas() {
        //添加次数
        int count = (int) SPHelper.get(this,"count",0);
        ++count;
        SPHelper.put(this,"count",count);

    }

    @Override
    protected void onDestroy() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        super.onDestroy();
    }




    private void getAdData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int sw = ScreenHelper.getScreenWidth(SlashActivity.this);
                int sh = ScreenHelper.getScreenHeight(SlashActivity.this);
                String osver = StringUtils.getVersionCode(SlashActivity.this);
                String url = " http://sdk.cferw.com/api.php?z=22219&appkey=f423aa0f1a6669e87faf7a346288767e" +
                        "&" + "deviceId=" + "imei" +
                        "&" + "sw=" + sw + ""+
                        "&" + "sh=" + sh +"" +
                        "&" + "osver=" + osver;
                OkHttpClient okHttpClient = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Message message = Message.obtain();
                        message.what = 0;
                        tmpHandler.sendMessage(message);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseStr = response.body().string();
//                        KLog.d(responseStr);
                        if(null != responseStr && !TextUtils.isEmpty(responseStr)){
                            Gson gson = new Gson();
                            slashAdBean = gson.fromJson(responseStr,SlashAdBean.class);
                            if(slashAdBean.getSucc() == 1){//成功

                                //TODO 发送展示成功请求
                                sendAdShowOk();

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Glide.with(mContext)
                                                .load(slashAdBean.getImgurl())
                                                .placeholder(R.mipmap.welcome)
                                                .centerCrop()
                                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                .into(imageView);
                                        mCountDownTextView.setVisibility(View.VISIBLE);
                                        mCountDownTextView.setText("4s 跳过");
                                        //创建倒计时类
                                        mCountDownTimer = new MyCountDownTimer(4000, 1000);
                                        mCountDownTimer.start();
                                        //这是一个 Handler 里面的逻辑是从 Splash 界面跳转到 Main 界面，这里的逻辑每个公司基本上一致
                                        tmpHandler.postDelayed(myRunnable,  4000);
                                    }
                                });
                            }else{
                                Message message = Message.obtain();
                                message.what = 0;
                                tmpHandler.sendMessage(message);
                            }
                        }
                    }
                });
            }
        }).start();

    }

    private void sendAdShowOk() {
        String url = slashAdBean.getCount_url();
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}


            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //{"succ":1,"code":"succ"}
//                String responseStr = response.body().string();
//                KLog.d(responseStr);
//                if(null != responseStr && !TextUtils.isEmpty(responseStr)){
//                    Gson gson = new Gson();
//                    SplashAdShowBean splashAdShowBean = gson.fromJson(responseStr,SplashAdShowBean.class);
//                }
            }
        });
    }

    private void sendAdClickOk() {
        String url = slashAdBean.getClick_url();
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //{"succ":1,"code":"views_n"}
//                String responseStr = response.body().string();
//                KLog.d(responseStr);
//                if(null != responseStr && !TextUtils.isEmpty(responseStr)){
//
//                }
            }
        });
    }




    private void getData() {
        Map<String,String> map = new HashMap<>();
        map.put("device_id", DeviceIdHepler.getUniquePsuedoID());
        map.put("app_version", StringUtils.getVersionCode(SlashActivity.this) + "");
        String channel_name = AnalyticsConfig.getChannel(this);
        map.put("from_plat", channel_name);
        Gson gson = new Gson();
        String jsonString = gson.toJson(map);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),jsonString.toString());

        //初始化
        RetrofitHelper.getAppService()
                .initOriData(requestBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new BaseObjectSubscriber<OriData>(){
                    @Override
                    public void onSuccess(OriData oriData) {
                        //TODO 11.12 如果数据为空的，给个默认的值
                        if(!TextUtils.isEmpty(oriData.getCity())){
                            SPHelper.put(SlashActivity.this,"location",oriData.getCity());
                        }else
                            SPHelper.put(SlashActivity.this,"location","上海市");

                        if(!TextUtils.isEmpty(oriData.getLatest_version())){
                            SPHelper.put(SlashActivity.this,"lastVersion",oriData.getLatest_version());
                        }else
                            SPHelper.put(SlashActivity.this,"lastVersion","1.0");

                        if(!TextUtils.isEmpty(oriData.getDownload_url())){
                            SPHelper.put(SlashActivity.this,"downlink",oriData.getDownload_url());
                        }
                    }
                });
    }





    protected void goToNextPage() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SlashActivity.this, MainActivityNew.class);
                startActivity(intent);
                finish();
            }
        }, 500);
    }

    @OnClick(R.id.splashImage)
    public void goUrl(){
        tmpHandler.removeCallbacks(myRunnable);
        Intent intent = new Intent(SlashActivity.this, MainActivityNew.class);
        startActivity(intent);
        finish();
        if(!TextUtils.isEmpty(slashAdBean.getGotourl())){
            Intent intent2 = new Intent(SlashActivity.this, WebViewHao123Activity.class);
            String url = slashAdBean.getGotourl();
            intent2.putExtra("url",url);
            startActivity(intent2);
        }

        sendAdClickOk();
    }

    @OnClick(R.id.start_skip_count_down)
    public void startSkipCountDown(){
        tmpHandler.removeCallbacks(myRunnable);
        Intent intent = new Intent(SlashActivity.this, MainActivityNew.class);
        startActivity(intent);
        finish();
    }
}

package com.ngbj.browser2;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.support.multidex.MultiDexApplication;


import com.allfree.security.SecurityManager;
import com.ngbj.browser2.bean.CountData;
import com.ngbj.browser2.db.DBManager;
import com.ngbj.browser2.util.AppUtil;
import com.ngbj.browser2.util.SDCardHelper;
import com.ngbj.browser2.util.SPHelper;
import com.socks.library.KLog;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.socialize.PlatformConfig;
import com.umeng.socialize.UMShareAPI;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cn.jpush.android.api.JPushInterface;


/**
 * Date:2018/7/18
 * author:zl
 * 备注：单例  + 管理Activity[需优化] + 实例化全局的Context
 */
public class MyApplication extends MultiDexApplication {

    private static  MyApplication myApplication;
    private Set<Activity> allActivities;


    /** 获取Application */
    public static MyApplication getInstance(){
        return myApplication;
    }

    //各个平台的配置 -- 这里就相当于在友盟后台配置了对应相应的第三方平台了！！
    {
        PlatformConfig.setWeixin("wxc84367f2df18bcfe", "5fe01b48f1307e72e8ccfb3aa364d073");
        PlatformConfig.setSinaWeibo("272803955", "4b8947ec27e19e7f77ce92a73153f261","http://www.birdbrowser.info");
        PlatformConfig.setQQZone("1107938202", "LbeOwX2w7j4k17WL");
    }


    @Override
    public void onCreate() {
        super.onCreate();
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            return;
//        }
//        LeakCanary.install(this);

       SecurityManager.getInstance().init(this);
        myApplication = this;
        AppUtil.init(this);

        String channel_name2 = getChannelFromApk(this,"channel");
        UMConfigure.init(this,"5bd81c75f1f556c25e000022",channel_name2 ,UMConfigure.DEVICE_TYPE_PHONE,"");
        UMShareAPI.get(this);//初始化sdk
        UMConfigure.setLogEnabled(true);

        JPushInterface.setDebugMode(true);
        JPushInterface.init(this);

//        JPushInterface.setDebugMode(true);
//        JPushInterface.init(this);
//       String registrationId = JPushInterface.getRegistrationID(this);
//        KLog.d("1099", "run:--------->registrationId： "+registrationId );
    }

    private static String getChannelFromApk(Context context, String channelKey) {
        //从apk包中获取
        ApplicationInfo appinfo = context.getApplicationInfo();
        String sourceDir = appinfo.sourceDir;
        //默认放在meta-inf/里， 所以需要再拼接一下
        String key = "META-INF/" + channelKey;
        String ret = "";
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(sourceDir);
            Enumeration<?> entries = zipfile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                String entryName = entry.getName();
                if (entryName.startsWith(key)) {
                    ret = entryName;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipfile != null) {
                try {
                    zipfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        String[] split = ret.split("_");
        String channel = "";
        if (split.length >= 2) {
            channel = ret.substring(split[0].length() + 1);
        }
        return channel;
    }




    /** 增加Activity */
    public void addActivity(Activity activity){
        if(allActivities == null){
            allActivities = new HashSet<>();
        }
        allActivities.add(activity);
    }

    /** 移除Activity */
    public void removeActivity(Activity activity){
        if(allActivities != null){
            allActivities.remove(activity);
        }
    }

    /** 退出应用 */
    public void exitApp(){
        if(allActivities != null){
            for (Activity act : allActivities) {
                act.finish();
            }
        }
        //TODO 清除一些信息
        SPHelper.put(this,"click_user_num",0);
        SPHelper.put(this,"history_type","1");
        //TODO 发送请求，判断是否清空点击表
        boolean isNetWork = (boolean) SPHelper.get(this,"is_network",false);
        if(isNetWork){
            DBManager.getInstance(this).deleteAllCountData();
            DBManager.getInstance(this).deleteAllBigModelCountData();
        }else{
            //TOO 保存
        }

        //删除文件
        SDCardHelper.removeFileFromSDCard(SDCardHelper.getSDCardPrivateCacheDir(this));

        //友盟保存数据
        MobclickAgent.onKillProcess(this);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

}

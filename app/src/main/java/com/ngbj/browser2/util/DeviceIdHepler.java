package com.ngbj.browser2.util;

import android.os.Build;

import com.socks.library.KLog;

import java.util.UUID;

/***
 * 参考
 *
 *    https://blog.csdn.net/a360940265a/article/details/79907844
 *
 *     https://www.jianshu.com/p/471df87af749 -- 加密下
 */

public class DeviceIdHepler {

//获得独一无二的Psuedo ID
public static String getUniquePsuedoID() {
    String serial ;
    String m_szDevIDShort = "35" +
            Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +

            Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +

            Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +

            Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +

            Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +

            Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +

            Build.USER.length() % 10; //13 位

    try {
        serial = android.os.Build.class.getField("SERIAL").get(null).toString();
        //API>=9 使用serial号
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    } catch (Exception exception) {
        //serial需要一个初始化
        serial = "serial"; // 随便一个初始化
    }
    //使用硬件信息拼凑出来的15位号码
    KLog.d("设备Id :" +  new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString());
    return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();

}

}
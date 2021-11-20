package org.easydarwin.easypusher;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.res.AssetManager;
import android.os.Build;

import com.basenetlib.util.NetWorkUtil;
import com.juntai.wisdom.basecomponent.app.BaseApplication;
import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.orhanobut.hawk.Hawk;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.socialize.PlatformConfig;

import org.easydarwin.easypusher.push.StreamActivity;
import org.easydarwin.easypusher.util.PublicUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.annotations.Beta;

import static org.easydarwin.easyrtmp.push.EasyRTMP.getActiveDays;

public class MyApp extends BaseApplication {

    public static final String CHANNEL_CAMERA = "camera";

    private static MyApp mApplication;
    public static int activeDays = 9999;

    public static MyApp getEasyApplication() {
        return mApplication;
    }

    @Override
    public void appBackground(boolean isBackground, Activity activity) {

    }

    @Override
    public String getTinkerId() {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mApplication = this;
//
//        Bugly.init(getApplicationContext(), "9a829a728a", false);
//        setBuglyInit();

        NetWorkUtil.initContext(this);
        File youyuan = getFileStreamPath("SIMYOU.ttf");
        if (!youyuan.exists()) {
            AssetManager am = getAssets();

            try {
                InputStream is = am.open("zk/SIMYOU.ttf");
                FileOutputStream os = openFileOutput("SIMYOU.ttf", MODE_PRIVATE);
                byte[] buffer = new byte[1024];
                int len;

                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }

                os.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        activeDays = getActiveDays(this, Hawk.get(HawkProperty.APP_KEY));

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.camera);

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_CAMERA, name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        //初始化组件化基础库, 统计SDK/推送SDK/分享SDK都必须调用此初始化接口
        UMConfigure.init(this, "6033ba79668f9e17b8b7bd80",
                "", UMConfigure.DEVICE_TYPE_PHONE, "");
        {
            PlatformConfig.setWeixin("wx7e72389cc05d7a6d", "19105a1f99174464c211fd6384d7ebe5");
        }
    }

//    public void setBuglyInit(){
//        // 添加可显示弹窗的Activity
//        Beta.canShowUpgradeActs.add(StreamActivity.class);
//
////        例如，只允许在MainActivity上显示更新弹窗，其他activity上不显示弹窗; 如果不设置默认所有activity都可以显示弹窗。
////        设置是否显示消息通知
//        Beta.enableNotification = true;
//
////        如果你不想在通知栏显示下载进度，你可以将这个接口设置为false，默认值为true。
////        设置Wifi下自动下载
//        Beta.autoDownloadOnWifi = false;
//
////        如果你想在Wifi网络下自动下载，可以将这个接口设置为true，默认值为false。
////        设置是否显示弹窗中的apk信息
//        Beta.canShowApkInfo = true;
//
////        如果你使用我们默认弹窗是会显示apk信息的，如果你不想显示可以将这个接口设置为false。
////        关闭热更新能力
//        Beta.enableHotfix = true;
//
//        Beta.largeIconId = R.mipmap.ic_launcher_foreground;
//        Beta.smallIconId = R.mipmap.ic_launcher_foreground;
//
//        // 设置是否显示消息通知
//        Beta.enableNotification = true;
//    }
}

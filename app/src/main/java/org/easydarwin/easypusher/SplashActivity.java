package org.easydarwin.easypusher;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.orhanobut.hawk.Hawk;

import org.easydarwin.easypusher.push.StreamActivity;

public class SplashActivity extends BaseProjectActivity {

    @Override
    public void onUvcCameraConnected() {

    }

    @Override
    public void onUvcCameraAttached() {

    }

    @Override
    public void onUvcCameraDisConnected() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        boolean isLogin = Hawk.get(HawkProperty.LOGIN_SUCCESS, false);
        if (isLogin) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(mContext, StreamActivity.class));
                    finish();
                }
            },1000);

        }else {
            startActivity(new Intent(mContext, LoginActivity.class));
            finish();
        }


    }
}

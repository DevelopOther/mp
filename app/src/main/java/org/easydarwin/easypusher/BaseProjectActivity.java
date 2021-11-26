package org.easydarwin.easypusher;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import androidx.versionedparcelable.ParcelUtils;

import com.juntai.wisdom.basecomponent.base.BaseActivity;
import com.juntai.wisdom.basecomponent.base.BaseMvpActivity;
import com.juntai.wisdom.basecomponent.mvp.BasePresenter;
import com.juntai.wisdom.basecomponent.utils.ActivityManagerTool;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import org.easydarwin.easypusher.push.StreamActivity;
import org.easydarwin.easypusher.util.PublicUtil;
import org.easydarwin.easypusher.util.SPUtil;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @Author: tobato
 * @Description: 作用描述
 * @CreateDate: 2020/5/5 20:47
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/5/5 20:47
 */
public abstract class BaseProjectActivity<P extends BasePresenter> extends BaseMvpActivity<P> {

    protected boolean isPushingStream = false;//是否正在推流
    protected boolean isPushingFirstStream = false;//是否正在推流
    protected boolean isPushingSecendStream = false;//是否正在推流
    protected boolean isPushingThirdStream = false;//是否正在推流
    protected boolean isPushingFourthStream = false;//是否正在推流

    public abstract void onUvcCameraConnected();

    public abstract void onUvcCameraAttached();

    public abstract void onUvcCameraDisConnected();

    protected Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        ActivityManagerTool.getInstance().addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mContext = null;
        EventBus.getDefault().unregister(this);
        ActivityManagerTool.getInstance().removeActivity(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receivedStringMsg(String msg) {
        switch (msg) {
            case "onAttach":
//                Toast.makeText(getApplicationContext(),"Attached",Toast.LENGTH_SHORT).show();
                onUvcCameraAttached();
                break;
            case "onConnect":
//                Toast.makeText(getApplicationContext(),"connect",Toast.LENGTH_SHORT).show();
                SPUtil.setBitrateKbps(this,5000000);
                onUvcCameraConnected();
                break;
            case "onDisconnect":
                SPUtil.setBitrateKbps(this,SPUtil.BITRATEKBPS);
//                Toast.makeText(getApplicationContext(),"disconnect",Toast.LENGTH_SHORT).show();

                onUvcCameraDisConnected();
                break;
            case "onPremissionConfirm":
                if (PublicUtil.isRunBackground(this)) {
                    startActivity(new Intent(this, StreamActivity.class));
                }

                break;
            default:
                break;
        }
    }

    /**
     * 隐藏控件  Invisible  gone
     *
     * @param isGone gone
     * @param views
     */
    protected void setViewsInvisible(boolean isGone, View... views) {
        if (views != null && views.length > 0) {
            for (View view : views) {
                if (view != null) {
                    if (isGone) {
                        view.setVisibility(View.GONE);
                    } else {
                        view.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }
    }

    /**
     * 显示控件  Invisible  gone
     *
     * @param views
     */
    protected void setViewsVisible(View... views) {
        if (views != null && views.length > 0) {
            for (View view : views) {
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        }
    }


}

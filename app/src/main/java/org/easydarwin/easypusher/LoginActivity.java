/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/
package org.easydarwin.easypusher;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.basenetlib.RequestStatus;
import com.basenetlib.util.NetWorkUtil;
import com.gyf.barlibrary.ImmersionBar;
import com.juntai.wisdom.basecomponent.utils.ActivityManagerTool;
import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.juntai.wisdom.basecomponent.utils.LogUtil;
import com.juntai.wisdom.basecomponent.utils.ToastUtils;
import com.mobile.auth.gatewayauth.AuthUIConfig;
import com.mobile.auth.gatewayauth.PhoneNumberAuthHelper;
import com.mobile.auth.gatewayauth.ResultCode;
import com.mobile.auth.gatewayauth.TokenResultListener;
import com.mobile.auth.gatewayauth.model.TokenRet;
import com.orhanobut.hawk.Hawk;
import com.regmode.RegLatestContact;
import com.regmode.RegLatestPresent;
import com.regmode.Utils.RegOperateManager;
import com.regmode.bean.AppInfoBean;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;
import com.umeng.socialize.UMAuthListener;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.bean.SHARE_MEDIA;

import org.easydarwin.easypusher.auth.BaseUIConfig;
import org.easydarwin.easypusher.auth.Constant;
import org.easydarwin.easypusher.auth.ExecutorManager;
import org.easydarwin.easypusher.auth.MockRequest;
import org.easydarwin.easypusher.bean.LiveBean;
import org.easydarwin.easypusher.mine.SettingActivity;
import org.easydarwin.easypusher.push.StreamActivity;
import org.easydarwin.easypusher.util.PublicUtil;
import org.easydarwin.easypusher.util.SPUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Consumer;

/**
 * 启动页
 */
public class LoginActivity extends BaseProjectActivity<RegLatestPresent> implements RequestStatus,
        View.OnClickListener, RegLatestContact.IRegView {
    /**
     * 登录
     */
    private TextView mLogin;
    private LinearLayout mLoginByMobileLl;
    private PhoneNumberAuthHelper mPhoneNumberAuthHelper;
    private TokenResultListener mTokenResultListener;
    private ProgressDialog mProgressDialog;
    private BaseUIConfig mUIConfig;
    String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
    };
    @Override
    public void onUvcCameraConnected() {
        //        Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUvcCameraAttached() {
        //        Toast.makeText(getApplicationContext(),"Attached888",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUvcCameraDisConnected() {

    }

    @Override
    protected RegLatestPresent createPresenter() {
        return new RegLatestPresent();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



    }
    @Override
    protected void onResume() {
        super.onResume();
        mUIConfig.onResume();
    }
    /**
     * 阿里一键登录
     */
    private void aliAuth() {
        sdkInit(BuildConfig.AUTH_SECRET);
        mUIConfig = BaseUIConfig.init(Constant.FULL_PORT, this, mPhoneNumberAuthHelper);
    }

    /**
     * 初始化平台数据
     */
    private void initPlatform() {
        List<LiveBean> arrays = Hawk.get(HawkProperty.PLATFORMS);
        if (arrays == null) {
            arrays = new ArrayList<>();
            arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_BILI, R.mipmap.bilibili_off, true, 0)
                    .setUrlHead(getString(R.string.biliurl)));
            arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_HUYA, R.mipmap.huya_off, true, 0)
                    .setUrlHead(getString(R.string.huyaurl)));
            if (PublicUtil.isMoreThanTheAndroid10()) {
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_DOUYU, R.mipmap.douyu_live_off, true, 0)
                        .setUrlHead(getString(R.string.douyuurl)));
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_XIGUA, R.mipmap.xigua_live_off, true, 0)
                        .setUrlHead(getString(R.string.xiguaurl)));
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_YI, R.mipmap.yi_live_off, true, 0)
                        .setUrlHead(getString(R.string.yiurl)));
            } else {
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_DOUYU, R.mipmap.douyu_live_off, false, 0)
                        .setUrlHead(getString(R.string.douyuurl)));
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_XIGUA, R.mipmap.xigua_live_off, false, 0)
                        .setUrlHead(getString(R.string.xiguaurl)));
                arrays.add(new LiveBean().config(SettingActivity.LIVE_TYPE_YI, R.mipmap.yi_live_off, false, 0)
                        .setUrlHead(getString(R.string.yiurl)));
            }
            Hawk.put(HawkProperty.PLATFORMS, arrays);
        }
    }

    @Override
    public void onStart(String tag) {

    }

    @Override
    public void onSuccess(Object o, String tag) {
        //获取key
        AppInfoBean appInfoBean = (AppInfoBean) o;
        if (appInfoBean != null) {
            if (appInfoBean.getModel() != null && appInfoBean.getModel().size() > 0) {
                AppInfoBean.ModelBean dataBean = appInfoBean.getModel().get(0);
                String key = dataBean.getSoftDescription();
                if (key != null) {
                    Hawk.put(HawkProperty.APP_KEY, key);
                } else {
                    ToastUtils.toast(this, "参数初始化失败");
                }
            }
        }
    }

    @Override
    public void onError(String tag) {

    }


    @Override
    public int getLayoutView() {
        return R.layout.login_activity;
    }

    @Override
    public void initView() {
        mLogin = (TextView) findViewById(R.id.login);
        mLogin.setOnClickListener(this);
        mLoginByMobileLl = (LinearLayout) findViewById(R.id.login_by_mobile_ll);
        mLoginByMobileLl.setOnClickListener(this);
        //获取软件的key
        mPresenter.getAppVersionInfoAndKeyFromService(RegLatestContact.GET_KEY, LoginActivity.this);
    }


    @Override
    public void initData() {
        initToolbarAndStatusBar(false);
        Hawk.put(HawkProperty.HIDE_FLOAT_VIEWS, false);
        initPlatform();
        SPUtil.setBitrateKbps(this, SPUtil.BITRATEKBPS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //隐藏状态栏
        if (!NetWorkUtil.isNetworkAvailable()) {
            new AlertDialog.Builder(mContext)
                    .setCancelable(false)
                    .setMessage("网络连接异常，请检查手机网络或系统时间！")
                    .setPositiveButton("知道了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityManagerTool.getInstance().finishApp();
                        }
                    }).show();
            return;
        }
        new RxPermissions(this)
                .request(permissions)
                .delay(1, TimeUnit.SECONDS)
                .compose(this.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
//                        if (aBoolean) {
//                           //全部同意
//                        } else {
//                            //有一个权限没通过
//                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                    }
                });


        aliAuth();
    }

    UMAuthListener authListener = new UMAuthListener() {
        @Override
        public void onStart(SHARE_MEDIA platform) {
//            Toast.makeText(mContext, "开始", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onComplete(SHARE_MEDIA platform, int action, Map<String, String> data) {
//            Toast.makeText(mContext, "成功了", Toast.LENGTH_LONG).show();
            boolean isAgree = Hawk.get(HawkProperty.AGREE_PROTOCAL, false);
            if (!isAgree) {
                showAgreementAlter();
            } else {
                jumpToHomePage(LoginActivity.this);
            }
        }

        @Override
        public void onError(SHARE_MEDIA platform, int action, Throwable t) {
//            Toast.makeText(mContext, "失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCancel(SHARE_MEDIA platform, int action) {
//            Toast.makeText(mContext, "取消了", Toast.LENGTH_LONG).show();
        }
    };

    private void showAgreementAlter() {
        Intent intentAgreement = new Intent(this, UserAgreementActivity.class);
        SpannableStringBuilder spannable = new SpannableStringBuilder(getString(R.string.agreement_xieyi_tag));
        // 在设置点击事件、同时设置字体颜色
        ClickableSpan clickableSpanOne = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                LogUtil.e("isGo", "点击了用户协议");
                intentAgreement.putExtra("url", getString(R.string.user_xieyi_url));
                startActivity(intentAgreement);
            }

            @Override
            public void updateDrawState(TextPaint paint) {
                paint.setColor(getResources().getColor(R.color.colorTheme));
                // 设置下划线 true显示、false不显示
                paint.setUnderlineText(false);
            }
        };
        ClickableSpan clickableSpanTwo = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                intentAgreement.putExtra("url", getString(R.string.secret_xieyi_url));
                startActivity(intentAgreement);
            }

            @Override
            public void updateDrawState(TextPaint paint) {
                paint.setColor(getResources().getColor(R.color.colorTheme));
                // 设置下划线 true显示、false不显示
                paint.setUnderlineText(false);
            }
        };
        spannable.setSpan(clickableSpanOne, 50, 56, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(clickableSpanTwo, 57, 63, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);


        AgreementDialog agreementDialog = new AgreementDialog(this).builder();
        agreementDialog.getContentTextView().setMovementMethod(LinkMovementMethod.getInstance());
        agreementDialog.setCanceledOnTouchOutside(false)
                .setTitle("服务协议和隐私政策")
                .setContent(spannable)
                .setCancelButton("暂不使用", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                })
                .setOkButton("同意并进入", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Hawk.put(HawkProperty.AGREE_PROTOCAL, true);
                        jumpToHomePage(mContext);
                    }
                }).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.login:
                UMShareAPI.get(this).doOauthVerify(this, SHARE_MEDIA.WEIXIN, authListener);
                break;
            case R.id.login_by_mobile_ll:
                oneKeyLogin();
                break;
        }
    }
    public void sdkInit(String secretInfo) {
        mTokenResultListener = new TokenResultListener() {
            @Override
            public void onTokenSuccess(String s) {
                hideLoadingDialog();
                TokenRet tokenRet = null;
                try {
                    tokenRet = TokenRet.fromJson(s);
                    if (ResultCode.CODE_START_AUTHPAGE_SUCCESS.equals(tokenRet.getCode())) {
                        Log.i("TAG", "唤起授权页成功：" + s);
                    }

                    if (ResultCode.CODE_SUCCESS.equals(tokenRet.getCode())) {
                        Log.i("TAG", "获取token成功：" + s);
                        getResultWithToken(tokenRet.getToken());
                        mPhoneNumberAuthHelper.setAuthListener(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onTokenFailed(String s) {
                hideLoadingDialog();
                TokenRet tokenRet = null;
                try {
                    tokenRet = TokenRet.fromJson(s);
                    if (ResultCode.CODE_ERROR_USER_CANCEL.equals(tokenRet.getCode())) {
                        //模拟的是必须登录 否则直接退出app的场景
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "一键登录失败,请确保开启移动网络", Toast.LENGTH_SHORT).show();
                        //                        Intent pIntent = new Intent(OneKeyLoginActivity.this,
                        //                        MessageActivity.class);
                        //                        startActivityForResult(pIntent, 1002);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mPhoneNumberAuthHelper.setAuthListener(null);
            }
        };
        mPhoneNumberAuthHelper = PhoneNumberAuthHelper.getInstance(this, mTokenResultListener);
        mPhoneNumberAuthHelper.getReporter().setLoggerEnable(true);
        mPhoneNumberAuthHelper.setAuthSDKInfo(secretInfo);
    }

    /**
     * 进入app就需要登录的场景使用
     */
    private void oneKeyLogin() {
        mPhoneNumberAuthHelper = PhoneNumberAuthHelper.getInstance(getApplicationContext(), mTokenResultListener);
        mUIConfig.configAuthPage();
        getLoginToken(5000);
    }

    /**
     * 拉起授权页
     *
     * @param timeout 超时时间
     */
    public void getLoginToken(int timeout) {
        mPhoneNumberAuthHelper.setAuthUIConfig(new AuthUIConfig.Builder()
                .setAppPrivacyOne("《一见多播用户隐私协议》", getString(R.string.secret_xieyi_url))
                .setAppPrivacyTwo("《一见多播APP使用协议》", getString(R.string.user_xieyi_url))
//                .setSwitchAccHidden(true)
//                .create());
//        mPhoneNumberAuthHelper.setAuthUIConfig(new AuthUIConfig.Builder()
//                .setAppPrivacyOne("《自定义隐私协议》", "https://www.baidu.com")
//                .setAppPrivacyColor(Color.GRAY, Color.parseColor("#002E00"))
                .setPrivacyState(false)
                .setCheckboxHidden(true)
                .setStatusBarColor(Color.parseColor("#5E90FF"))
                .setStatusBarUIFlag(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                .setLightColor(true)
                .setSwitchAccHidden(true)
//                .setVendorPrivacyPrefix("《")
//                .setVendorPrivacySuffix("》")
//                .setLogoImgPath("mytel_app_launcher")
//                .setScreenOrientation(authPageOrientation)
                .create());
        mPhoneNumberAuthHelper.getLoginToken(this, timeout);
        showLoadingDialog("正在唤起授权页");
    }


    public void getResultWithToken(final String token) {
        ExecutorManager.run(new Runnable() {
            @Override
            public void run() {
                final String result = MockRequest.getPhoneNumber(token);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPhoneNumberAuthHelper.quitLoginPage();
                        boolean isAgree = Hawk.get(HawkProperty.AGREE_PROTOCAL, false);
                        if (!isAgree) {
                            showAgreementAlter();
                        } else {
                            jumpToHomePage(mContext);
                        }
                    }
                });
            }
        });
    }

    /**
     * 成功后跳转
     * @param mContext
     */
    private void jumpToHomePage(Context mContext) {
        Hawk.put(HawkProperty.LOGIN_SUCCESS,true);
        startActivity(new Intent(mContext, StreamActivity.class));
        finish();
    }


    public void showLoadingDialog(String hint) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        mProgressDialog.setMessage(hint);
        mProgressDialog.setCancelable(true);
        mProgressDialog.show();
    }

    public void hideLoadingDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }


    @Override
    public void onSuccess(String tag, Object o) {

    }
}

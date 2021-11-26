/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/

package org.easydarwin.easypusher.mine;

import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.juntai.wisdom.basecomponent.utils.ActivityManagerTool;
import com.juntai.wisdom.basecomponent.utils.ToastUtils;
import com.orhanobut.hawk.Hawk;

import org.easydarwin.easypusher.BaseProjectActivity;
import org.easydarwin.easypusher.SplashActivity;
import org.easydarwin.easypusher.bean.LiveBean;
import org.easydarwin.easypusher.record.MediaFilesActivity;
import org.easydarwin.easypusher.R;
import org.easydarwin.easypusher.databinding.ActivitySettingBinding;

import com.juntai.wisdom.basecomponent.utils.HawkProperty;

import org.easydarwin.easypusher.util.SPUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置页
 */
public class SettingActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener,
        View.OnClickListener {

    public static final int REQUEST_OVERLAY_PERMISSION = 1004;  // 悬浮框
    private static final int REQUEST_SCAN_TEXT_URL_BILI = 1003;      // 扫描二维码bili
    private static final int REQUEST_SCAN_TEXT_URL_HUYA = 1005;      // 扫描二维码huya
    private static final int REQUEST_SCAN_TEXT_URL_YI = 1006;      // 扫描二维码yi
    private static final int REQUEST_SCAN_TEXT_URL_NOW = 1007;      // 扫描二维码now
    public static final String LIVE_TYPE_BILI = "哔哩哔哩";
    public static final String LIVE_TYPE_HUYA = "虎牙直播";
    public static final String LIVE_TYPE_YI = "一直播";
    public static final String LIVE_TYPE_DOUYU = "斗鱼直播";
    public static final String LIVE_TYPE_XIGUA = "西瓜视频";
    public static final String LIVE_TYPE_CUSTOM = "ADDPLATE";
    private ActivitySettingBinding binding;
    private MyLivesAdapter adapter;
    private AlertDialog alertDialog;
    ;


    //    EditText url;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);
        //        binding.registCodeKey.setText(String.format("%s%s","注册码:",Hawk.get(HawkProperty.REG_CODE)));
        setSupportActionBar(binding.mainToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.mainToolbar.setOnMenuItemClickListener(this);
        binding.openRecordLocalBt.setOnClickListener(this);
        binding.quitAppBt.setOnClickListener(this);
        binding.recordDurationCl.setOnClickListener(this);
        binding.recordDurationDesTv.setText(String.format(getString(R.string.record_nuration),
                String.valueOf(Hawk.get(HawkProperty.RECORD_DURACTION, 5))));
        adapter = new MyLivesAdapter(R.layout.my_lives_item);
        GridLayoutManager manager = new GridLayoutManager(this, 3);
        binding.livePlatformRv.setAdapter(adapter);
        binding.livePlatformRv.setLayoutManager(manager);

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                LiveBean liveBean = (LiveBean) adapter.getData().get(position);
                if (liveBean.isPushing()) {
                    ToastUtils.toast(SettingActivity.this, "正在推流,请先停止推流后再重试");
                    return;
                }
                int selectedSize = getSelectedAmount(adapter);
                startActivity(new Intent(SettingActivity.this, EditLivePlatActivity.class).putExtra(EditLivePlatActivity.PLATE,
                        liveBean)
                        .putExtra(EditLivePlatActivity.PLATE_LIVE_SIZE, selectedSize));

            }
        });
        // 使能摄像头后台采集
        onPushBackground();
        //        onEncodeType();
        // 推送内容
        onRadioGroupCheckedStatus();
    }

    /**
     * 获取选中的个数
     *
     * @param adapter
     * @return
     */
    private int getSelectedAmount(BaseQuickAdapter adapter) {
        List<LiveBean> arrays = adapter.getData();
        List<LiveBean> a = new ArrayList<>();
        for (LiveBean array : arrays) {
            if (array.isSelect()) {
                a.add(array);
            }
        }
        return a.size();
    }

    private List<LiveBean> getAdapterData() {
        boolean hasAddTag = false;//是否有添加平台的标识
        List<LiveBean> arrays = Hawk.get(HawkProperty.PLATFORMS);
        for (LiveBean array : arrays) {
            if (1 == array.getItemType()) {
                hasAddTag = true;
                break;
            }
        }
        if (!hasAddTag) {
            arrays.add(new LiveBean().config(LIVE_TYPE_CUSTOM, 0, false, 1));

        }
        return arrays;
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.setNewData(getAdapterData());
    }


    /**
     * radiogroup的选中状态
     */
    private void onRadioGroupCheckedStatus() {
        boolean videoEnable = SPUtil.getEnableVideo(this);
        if (videoEnable) {
            boolean audioEnable = SPUtil.getEnableAudio(this);

            if (audioEnable) {
                RadioButton push_av = findViewById(R.id.push_av);
                push_av.setChecked(true);
            } else {
                RadioButton push_v = findViewById(R.id.push_v);
                push_v.setChecked(true);
            }
        } else {
            RadioButton push_a = findViewById(R.id.push_a);
            push_a.setChecked(true);
        }
        binding.pushContentRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.push_av) {
                    SPUtil.setEnableVideo(SettingActivity.this, true);
                    SPUtil.setEnableAudio(SettingActivity.this, true);
                } else if (checkedId == R.id.push_a) {
                    SPUtil.setEnableVideo(SettingActivity.this, false);
                    SPUtil.setEnableAudio(SettingActivity.this, true);
                } else if (checkedId == R.id.push_v) {
                    SPUtil.setEnableVideo(SettingActivity.this, true);
                    SPUtil.setEnableAudio(SettingActivity.this, false);
                }
            }
        });
    }

    //    /**
    //     *
    //     */
    //    private void onEncodeType() {
    //        // 是否使用软编码
    //        CheckBox x264enc = findViewById(R.id.use_x264_encode);
    //        x264enc.setChecked(Hawk.get(HawkProperty.KEY_SW_CODEC, false));
    //        x264enc.setOnCheckedChangeListener((buttonView, isChecked) -> Hawk.put(HawkProperty.KEY_SW_CODEC,
    //        isChecked));
    //
    //        //        // 使能H.265编码
    //        //        CheckBox enable_hevc_cb = findViewById(R.id.enable_hevc);
    //        //        enable_hevc_cb.setChecked(SPUtil.getHevcCodec(this));
    //        //        enable_hevc_cb.setOnCheckedChangeListener(
    //        //                (buttonView, isChecked) -> SPUtil.setHevcCodec(this, isChecked)
    //        //        );
    //
    //        //        // 叠加水印
    //        //        CheckBox enable_video_overlay = findViewById(R.id.enable_video_overlay);
    //        //        enable_video_overlay.setChecked(SPUtil.getEnableVideoOverlay(this));
    //        //        enable_video_overlay.setOnCheckedChangeListener(
    //        //                (buttonView, isChecked) -> SPUtil.setEnableVideoOverlay(this, isChecked)
    //        //        );
    //    }

    /**
     * 后台采集
     */
    private void onPushBackground() {
        CheckBox backgroundPushing = (CheckBox) findViewById(R.id.enable_background_camera_pushing);
        backgroundPushing.setChecked(SPUtil.getEnableBackgroundCamera(this));
        backgroundPushing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(SettingActivity.this)) {
                            SPUtil.setEnableBackgroundCamera(SettingActivity.this, true);
                        } else {
                            new AlertDialog.Builder(SettingActivity.this).setTitle("后台直播").setMessage(getResources().getString(R.string.live_bg_notice)).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // 在Android 6.0后，Android需要动态获取权限，若没有权限，提示获取.
                                    final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:org.chuangchi.yjdb"));
                                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                                }
                            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SPUtil.setEnableBackgroundCamera(SettingActivity.this, false);
                                    buttonView.toggle();
                                }
                            }).setCancelable(false).show();
                        }
                    } else {
                        SPUtil.setEnableBackgroundCamera(SettingActivity.this, true);
                    }
                } else {
                    SPUtil.setEnableBackgroundCamera(SettingActivity.this, false);
                }
            }
        });
    }

    //    /**
    //     * 码率
    //     */
    //    private void initBitrateData() {
    //        SeekBar sb = findViewById(R.id.bitrate_seekbar);
    //        final TextView bitrateValue = findViewById(R.id.bitrate_value);
    //
    //        int bitrate_added_kbps = SPUtil.getBitrateKbps(this);
    //        int kbps = 72000 + bitrate_added_kbps;
    //        bitrateValue.setText(kbps/1000 + "kbps");
    //
    //        sb.setMax(5000000);
    //        sb.setProgress(bitrate_added_kbps);
    //        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
    //            @Override
    //            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    //                int kbps = 72000 + progress;
    //                bitrateValue.setText(kbps/1000 + "kbps");
    //            }
    //
    //            @Override
    //            public void onStartTrackingTouch(SeekBar seekBar) {
    //
    //            }
    //
    //            @Override
    //            public void onStopTrackingTouch(SeekBar seekBar) {
    //                SPUtil.setBitrateKbps(SettingActivity.this, seekBar.getProgress());
    //            }
    //        });
    //    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onBackPressed() {
        //        if (!isPushingStream) {
        //            String text = binding.pushServerIpEt.getText().toString().trim();
        //            if (text.contains("//")) {
        //                text = text.substring(text.indexOf("//") + 2, text.length());
        //            }
        //            Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_IP, text);
        //            String textPort = binding.pushServerPortEt.getText().toString().trim();
        //            Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_PORT, textPort);
        //            String tag = binding.liveTagEt.getText().toString().trim();
        //            Hawk.put(HawkProperty.KEY_SCREEN_PUSHING_TAG, tag);
        //        } else {
        //            ToastUtils.toast(mContext, "正在推流，无法更改推流地址");
        //        }
        //        if (!isPushingFirstStream) {
        //            String bilibili = binding.firstLiveValueEt.getText().toString().trim();
        //            Hawk.put(HawkProperty.KEY_FIRST_URL, bilibili);
        //
        //        }
        //        if (!isPushingSecendStream) {
        //            String huya = binding.secendLiveValueEt.getText().toString().trim();
        //            Hawk.put(HawkProperty.KEY_SECEND_URL, huya);
        //        }
        //        if (!isPushingThirdStream) {
        //            String url = binding.thirdLiveValueEt.getText().toString().trim();
        //            Hawk.put(HawkProperty.KEY_THIRD_URL, url);
        //
        //        }
        //        if (!isPushingFourthStream) {
        //            String url = binding.fourthLiveValueEt.getText().toString().trim();
        //            Hawk.put(HawkProperty.KEY_FOURTH_URL, url);
        //
        //        }
        //
        //        String registCode = binding.registCodeValue.getText().toString().trim();
        //        Hawk.put(HawkProperty.KEY_REGIST_CODE, registCode);
        super.onBackPressed();
    }


    //    /*
    //    * 二维码扫码
    //    * */
    //    public void onScanQRCode(View view) {
    //        Intent intent = new Intent(this, ScanQRActivity.class);
    //        startActivityForResult(intent, REQUEST_SCAN_TEXT_URL);
    //        overridePendingTransition(R.anim.slide_bottom_in, R.anim.slide_top_out);
    //    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean canDraw = Settings.canDrawOverlays(this);
                SPUtil.setEnableBackgroundCamera(SettingActivity.this, canDraw);

                if (!canDraw) {
                    CheckBox backgroundPushing = (CheckBox) findViewById(R.id.enable_background_camera_pushing);
                    backgroundPushing.setChecked(false);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return false;
    }

    // 返回的功能
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.open_record_local_bt:
                Intent intent = new Intent(this, MediaFilesActivity.class);
                startActivityForResult(intent, 0);
                //                overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
                break;
            case R.id.quit_app_bt:
                View view = LayoutInflater.from(this).inflate(R.layout.quite_app, null);
                view.findViewById(R.id.quit_app_tv).setOnClickListener(this);
                view.findViewById(R.id.logout_app_tv).setOnClickListener(this);
                AlertDialog alertDialog = new AlertDialog.Builder(this).setCancelable(false)
                        .setView(view)
                        .show();
                view.findViewById(R.id.cancel_tv).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (alertDialog != null && alertDialog.isShowing()) {
                            alertDialog.dismiss();
                        }

                    }
                });
                break;
            case R.id.quit_app_tv:
                ActivityManagerTool.getInstance().finishApp();
                break;
            case R.id.logout_app_tv:
                ActivityManagerTool.getInstance().finishApp();
                Hawk.delete(HawkProperty.LOGIN_SUCCESS);
                startActivity(new Intent(this, SplashActivity.class));
                break;

            case R.id.record_duration_cl:
                View durationView = LayoutInflater.from(SettingActivity.this).inflate(R.layout.record_duration, null);
                EditText durationEt = durationView.findViewById(R.id.duration_et);
                AlertDialog dialog = new AlertDialog.Builder(SettingActivity.this).setView(durationView).show();
                durationView.findViewById(R.id.confirm_tv).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String duration = durationEt.getText().toString().trim();
                        if (!TextUtils.isEmpty(duration)) {
                            Hawk.put(HawkProperty.RECORD_DURACTION, Integer.parseInt(duration));
                        }
                        binding.recordDurationDesTv.setText(String.format(getString(R.string.record_nuration),
                                duration));
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    }
                });


                break;
            default:
                break;
        }
    }


}

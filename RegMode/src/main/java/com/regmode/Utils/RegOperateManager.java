package com.regmode.Utils;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.basenetlib.RequestStatus;
import com.juntai.wisdom.basecomponent.utils.FileUtils;
import com.juntai.wisdom.basecomponent.utils.HawkProperty;
import com.juntai.wisdom.basecomponent.utils.ToastUtils;
import com.orhanobut.hawk.Hawk;
import com.regmode.AppHttpUrl;
import com.regmode.R;
import com.regmode.RegLatestContact;
import com.regmode.RegLatestPresent;
import com.regmode.adapter.CommonProgressDialog;
import com.regmode.bean.AppInfoBean;
import com.regmode.bean.RegCodeBean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import static android.content.Context.MODE_PRIVATE;

/**
 * ?????????????????????
 * Created by Administrator on 2017/3/30.
 */

public class RegOperateManager extends BaseReg implements RequestStatus {
    public static String APP_MARK = "YJDB";//????????????
    private CommonProgressDialog mProgressDialog;
    private String nearestVersion;
    private Context context;
    public static String username;//?????????
    private AMapLocationClient locationClient = null;
    private String Lat;
    private String Lng;
    private String Addr;
    private Dialog dialog_Reg;
    private ProgressDialog progressDialog;
    private RegLatestContact.CancelCallBack cancelCallBack;
    private boolean firstCite = true;//??????????????????
    public static RegOperateManager regOperateUtil;
    private RegLatestPresent present;
    private String input;


    public RegOperateManager(Context context) {
        this.context = context;
        present = new RegLatestPresent();
        if (firstCite) {
            firstCite = false;
            initReg(context);
        }

    }

    public static RegOperateManager getInstance(Context context) {

        if (regOperateUtil == null) {
            synchronized (RegOperateManager.class) {
                return new RegOperateManager(context);
            }
        } else {
            return regOperateUtil;
        }
    }

    /**
     * ?????????
     *
     * @param context
     */
    private void initReg(Context context) {
        String strreg = Hawk.get(HawkProperty.REG_CODE);

        initLocation();
        if (strreg == null || TextUtils.isEmpty(strreg)) {
            showRegDialog();
        } else {
            checkRegStatus();
        }
    }


    //    /**
    //     * ?????????????????????
    //     *
    //     * @param str ????????????
    //     */
    //    private void SaveRegStatus(String str) {
    //        SharedPreferences.Editor editor = sp.edit();
    //        editor.putString("REGSTATUS", str);
    //        editor.commit();
    //    }

    //    /**
    //     * ??????????????????????????????????????????
    //     */
    //    public void CheckUnMinusedRegSizeToMinus() {
    //        if (isTheRegStatusOkNoToast(context)) {
    //            if (sp.getInt("UNMINUSEDSIZE", 0) > 0) {
    //                if (isNumberLimit) {
    //                    present.setRegisCodeNumber(strreg, sp.getInt("UNMINUSEDSIZE", 0), this);
    //                    sp.edit().putInt("UNMINUSEDSIZE", 0).commit();
    //                }
    //
    //            }
    //        }
    //
    //    }

    //??????????????????????????????
    public void UnMinusedRegSizeToCommit() {
        //        if (REGSIZE > 1) {
        //            SharedPreferences.Editor et = sp.edit();
        //            et.putInt("UNMINUSEDSIZE", REGSIZE - 1);
        //            et.commit();
        //        }
    }

    /**
     * ?????????????????????????????????
     *
     * @return
     */
    public boolean isTheRegStatusOk(String reg_status) {
        //  "?????????????????????"
        if (reg_status.contains("?????????")) {
            warnRegStatus("???????????????????????????????????????", "");
            return false;
        } else if (reg_status.contains("????????????")) {
            warnRegStatus("?????????????????????????????????????????????", "");
            return false;
        } else if (reg_status.contains("?????????")) {
            warnRegStatus("???????????????????????????????????????", "");
            return false;
        } else if (reg_status.contains("?????????")) {
            warnRegStatus("???????????????????????????????????????", "");
            return false;
        } else {
            warnRegStatus(reg_status, "");
            return true;
        }
    }


    /**
     * ?????????????????????????????????
     *
     * @return
     */
    public boolean isTheRegStatusOkNoToast(Context context) {
        SharedPreferences sp = context.getSharedPreferences("REG", MODE_PRIVATE);
        String reg_status = sp.getString("REGSTATUS", "???????????????");
        //  "?????????????????????"
        if (reg_status.equals("??????????????????")) {
            return false;
        } else if (reg_status.equals("????????????????????????")) {
            return false;
        } else if (reg_status.equals("??????????????????")) {
            return false;
        } else if (reg_status.equals("??????????????????")) {
            return false;
        } else {
            return true;
        }
    }


    /**
     * ????????????????????????
     */
    @Override
    public void checkRegStatus() {
        if (!RegPubUtils.isConnected(context.getApplicationContext())) {
            Toast.makeText(context, "????????????????????????????????????", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        present.checkReg((String) Hawk.get(HawkProperty.REG_CODE), APP_MARK, RegLatestContact.CHECK_REG_EVERYTIME,
                RegOperateManager.this);
    }

    /**
     * ???????????????????????????
     *
     * @param status
     */
    private void resetNextWarnTime(String status) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("NEXTWARNTIME", MODE_PRIVATE);
        SharedPreferences.Editor et = sharedPreferences.edit();
        et.putString("nextRegStatusTime" + status, "");
        et.commit();
    }


    /**
     * ?????????????????????????????????
     */
    private boolean IsTheRegStatusTime(String status) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("NEXTWARNTIME", MODE_PRIVATE);
        final String time = sharedPreferences.getString("nextRegStatusTime" + status, "");
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        String time2 = new SimpleDateFormat("yyyy-MM-dd").format(date);
        if (TextUtils.isEmpty(time)) {
            return true;
        } else {
            if (RegPubUtils.compareTime(time2, time)) {
                return true;
            } else {
                return false;
            }
        }

    }


    /**
     * ????????????????????????
     *
     * @param text
     * @param status
     */
    private void warnRegStatus(final String text, final String status) {

        View v = LayoutInflater.from(context).inflate(R.layout.warn_reg_layout
                , null);
        final Dialog dialog_toWarn = new Dialog(context, R.style.DialogStyle);
        dialog_toWarn.setCanceledOnTouchOutside(false);
        dialog_toWarn.setCancelable(false);
        dialog_toWarn.show();
        Window window = dialog_toWarn.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        window.setAttributes(lp);
        window.setContentView(v);
        final TextView nfs_set_no_tv = (TextView) v.findViewById(R.id.warn_reg_tv);
        final TextView warn_reg_textViewreg = (TextView) v.findViewById(R.id.warn_reg_textViewreg);
        nfs_set_no_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (text != null && !TextUtils.isEmpty(text)) {
                    if (text.contains("?????????") || text.contains("?????????") || text.contains("?????????") || text.contains("?????????") || text.contains("?????????")) {
                        if (cancelCallBack != null) {
                            cancelCallBack.toFinishActivity();
                        }
                    } else {
                        String nextTime = GetNextWarnTime(1);
                        SharedPreferences sharedPreferences = context.getSharedPreferences("NEXTWARNTIME",
                                MODE_PRIVATE);
                        SharedPreferences.Editor et = sharedPreferences.edit();
                        et.putString("nextRegStatusTime" + status, nextTime);
                        et.commit();
                        dialog_toWarn.dismiss();
                    }

                }
            }

        });
        warn_reg_textViewreg.setText(text);
    }

    /**
     * ???????????????????????????,day??????
     */
    private String GetNextWarnTime(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, day);
        Date date = calendar.getTime();
        String time = new SimpleDateFormat("yyyy-MM-dd").format(date);
        return time;
    }


    /**
     * ??????????????????apk???
     *
     * @param url
     */
    private void downAPKfromService(String url) {

        if (RegPubUtils.isConnected(context)) {
            mProgressDialog = new CommonProgressDialog(context);
            mProgressDialog.setMessage("????????????");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            final DownloadTask downloadTask = new DownloadTask(context);
            downloadTask.execute(url);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    downloadTask.cancel(true);
                }
            });
            //downFile(url);
        }

    }

    @Override
    public void onStart(String tag) {

    }

    @Override
    public void onSuccess(Object o, String tag) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        switch (tag) {
            case RegLatestContact.CHECK_REG:
                //???????????????
                RegCodeBean regCodeBean = (RegCodeBean) o;
                if (regCodeBean != null) {
                    String resultTag = regCodeBean.getResult();
                    if ("ok".equals(resultTag)) {
                        if (regCodeBean.getModel() != null && regCodeBean.getModel().size() > 0) {
                            RegCodeBean.ModelBean modelBean = regCodeBean.getModel().get(0);
                            String regStatus = modelBean.getRegisCodeState();

                            if ("??????".equals(regStatus)) {
                                //???????????????
                                Hawk.put(HawkProperty.REG_CODE, input);
                                if (!RegPubUtils.PUBLIC_REGCODE.equals(input)) {
                                    //??????????????????????????? ??????????????????
                                    if (!checkImei(modelBean)) {
                                        return;
                                    }
                                }

                                //                                String mac = modelBean.getMAC();
                                //                                if (mac != null && !TextUtils.isEmpty(mac)) {
                                //                                    //??????mac??????
                                //                                    Hawk.put(HawkProperty.MAC_CODE, mac);
                                //                                }
                                ToastUtils.toast(context, "?????????????????????");
                                if (dialog_Reg != null && dialog_Reg.isShowing()) {
                                    dialog_Reg.dismiss();
                                }
                            } else {
                                isTheRegStatusOk(regStatus);
                            }
                        } else {
                            isTheRegStatusOk("??????????????????");
                        }

                    } else {
                        ToastUtils.toast(context, "???????????????");
                    }
                }
                break;
            case RegLatestContact.CHECK_REG_EVERYTIME:
                //?????????????????????????????????
                //???????????????
                RegCodeBean regBean = (RegCodeBean) o;
                if (regBean != null) {
                    String resultTag = regBean.getResult();
                    if ("ok".equals(resultTag)) {
                        if (regBean.getModel() != null && regBean.getModel().size() > 0) {
                            RegCodeBean.ModelBean modelBean = regBean.getModel().get(0);
                            String regStatus = modelBean.getRegisCodeState();

                            if ("??????".equals(regStatus)) {
                                if (!RegPubUtils.PUBLIC_REGCODE.equals(Hawk.get(HawkProperty.REG_CODE))) {
                                    //??????????????????????????? ??????????????????
                                    if (!checkImei(modelBean)) {
                                        return;
                                    }
                                }
                                //????????????
                                String isAutoUpdate = modelBean.getIsAutoUpdate();
                                if (isAutoUpdate != null && !TextUtils.isEmpty(isAutoUpdate)) {
                                    if (isAutoUpdate.equals("1")) {//??????????????????
                                        getNearestVersionFromService();
                                        return;
                                    }
                                }
                                //???????????????
                                String isValid = modelBean.getIsValid();
                                if (isValid != null && !TextUtils.isEmpty(isValid)) {
                                    if (isValid.equals("0")) {//?????????????????????
                                        String ValidEnd = modelBean.getValidEnd();
                                        String time = ValidEnd.split(" ")[0];
                                        if (RegPubUtils.TheDayToNextDay(time) > 0 && RegPubUtils.TheDayToNextDay(time) < 8) {

                                            if (IsTheRegStatusTime("isValid")) {
                                                warnRegStatus("????????????????????????" + RegPubUtils.TheDayToNextDay(time) +
                                                        "????????????????????????", "isValid");
                                            }

                                        } else {//???????????????????????????
                                            resetNextWarnTime("isValid");
                                        }
                                    }
                                }
                                //????????????
                                String isNumber = modelBean.getIsNumber();
                                if (isNumber != null && !TextUtils.isEmpty(isNumber)) {
                                    if (isNumber.equals("0")) {//????????????????????????
                                        String NumberTotal = modelBean.getNumber();
                                        String NumberUsed = modelBean.getNumberNow();
                                        int NumberNow = Integer.parseInt(NumberTotal) - Integer.parseInt(NumberUsed);
                                        if (NumberNow < 100) {
                                            if (IsTheRegStatusTime("isNumber")) {
                                                warnRegStatus("?????????????????????" + NumberNow + "????????????????????????", "isNumber");
                                            }

                                        } else {//???????????????????????????
                                            resetNextWarnTime("isNumber");
                                        }
                                    }
                                }
                            } else {
                                isTheRegStatusOk(regStatus);
                            }
                        } else {
                            isTheRegStatusOk("??????????????????");

                        }

                    } else {
                        ToastUtils.toast(context, "???????????????");
                    }
                }
                break;
            case RegLatestContact.SET_IMEI:

                break;
            case RegLatestContact.GET_APP_VERSION_INFO:
                AppInfoBean appInfoBean = (AppInfoBean) o;
                if (appInfoBean != null) {
                    if (appInfoBean.getModel() != null && appInfoBean.getModel().size() > 0) {
                        AppInfoBean.ModelBean dataBean = appInfoBean.getModel().get(0);
                        String nearestVersion = dataBean.getSoftwareVersion();
                        String down_url = dataBean.getSoftDownloadUrl();
                        if (updateableSoftVersion(getAPPVersion(), nearestVersion)) {
                            if (IsTheTime()) {
                                warnUpgradeDialog(AppHttpUrl.BASE_URL + down_url);
                            }

                        }
                        //                        else {//???
                        //                            SharedPreferences sharedPreferences = context
                        //                            .getSharedPreferences("NEXTWARNTIME", MODE_PRIVATE);
                        //                            SharedPreferences.Editor et = sharedPreferences.edit();
                        //                            et.putString("nextTime", "");
                        //                            et.commit();
                        //                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * ??????imei
     *
     * @param modelBean
     */
    private boolean checkImei(RegCodeBean.ModelBean modelBean) {
        // ????????????????????????????????????????????????
        String imei = modelBean.getImei();
        if (imei != null && !TextUtils.isEmpty(imei)) {
            //??????
            String localImei = FileUtils.getFileContent("property.txt");
            if (localImei != null && !TextUtils.isEmpty(localImei)) {
                if (localImei.equals(imei)) {
                    return true;
                } else {
                    warnRegStatus("???????????????IMEI??????????????????????????????", "");
                    return false;
                }
            } else {
                //?????????????????? ??????????????????????????????
                warnRegStatus("???????????????IMEI??????????????????????????????", "");
                return false;
            }
        } else {
            String isImei = modelBean.getIsImei();
            if ("1".equals(isImei)) {
                //???????????????md5?????????????????????
                FileUtils.writeToTxtFile((String) Hawk.get(HawkProperty.REG_CODE), "property.txt");
                //??????????????????????????????????????????
                present.setImei((String) Hawk.get(HawkProperty.REG_CODE), FileUtils.getFileContent("property.txt"),
                        RegLatestContact.SET_IMEI, this);
            }
            return true;
        }
    }

    @Override
    public void onError(String tag) {
        Toast.makeText(context, tag, Toast.LENGTH_SHORT).show();
        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    @Override
    public void setRegistCodeNumber(int size) {
        present.setRegisCodeNumber((String) Hawk.get(HawkProperty.REG_CODE), size, this);
    }

    /**
     * ?????????????????????????????????
     */
    private void getNearestVersionFromService() {

        if (isConnected(context)) {
            //???????????????key
            present.getAppVersionInfoAndKeyFromService(RegLatestContact.GET_APP_VERSION_INFO, this);
        }

    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param localVersionName  ???????????????????????????
     * @param serverVersionName ??????????????????????????????
     * @return
     */
    private boolean updateableSoftVersion(String localVersionName, String serverVersionName) {
        if (TextUtils.isEmpty(localVersionName) || TextUtils.isEmpty(serverVersionName)) {
            return false;
        }
        String local3 = "0";
        String server3 = "0";
        String[] localVersion = localVersionName.split("\\.");
        String[] serverVersion = serverVersionName.split("\\.");
        String local1 = localVersion[0];
        String local2 = localVersion[1];
        if (localVersion.length == 3) {
            local3 = localVersion[2];
        }
        String server1 = serverVersion[0];
        String server2 = serverVersion[1];
        if (serverVersion.length == 3) {
            server3 = serverVersion[2];
        }
        if (Integer.parseInt(server1) > Integer.parseInt(local1)) {
            return true;
        }
        if (Integer.parseInt(server2) > Integer.parseInt(local2)) {
            return true;
        }
        if (Integer.parseInt(server3) > Integer.parseInt(local3)) {
            return true;
        }
        return false;
    }

    /**
     * ????????????????????????
     */
    private boolean IsTheTime() {
        final String time = Hawk.get(HawkProperty.NEXT_WARN_UPDATE);
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        String time2 = new SimpleDateFormat("yyyy-MM-dd").format(date);
        if (TextUtils.isEmpty(time)) {
            return true;
        } else {
            if (compareTime(time2, time)) {
                return true;
            } else {
                return false;
            }
        }


    }

    /**
     * ??????????????????????????????
     *
     * @param startTime ????????????
     * @param endTime   ????????????
     * @return
     */
    public static boolean compareTime(String startTime, String endTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Long a = sdf.parse(startTime).getTime();
            Long b = sdf.parse(endTime).getTime();
            if (a > b || a == b) {
                return true;
            } else {
                return false;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ????????????????????????
     *
     * @param url
     */
    private void warnUpgradeDialog(final String url) {
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("?????????????????????????????????????????????")
                .setPositiveButton("????????????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        downAPKfromService(url);
                    }
                }).setNegativeButton("????????????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        String nextTime = GetNextWarnTime(7);
                        Hawk.put(HawkProperty.NEXT_WARN_UPDATE, nextTime);
                    }
                }).show();
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;

        DownloadTask(Context context) {
            this.context = context;
        }

        //?????????????????????doInBackground???????????????????????????ui???????????????
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //???????????? ????????????????????????
            mProgressDialog.show();
            mProgressDialog.setProgress(0);
        }

        @Override
        protected String doInBackground(String... params) {
            int i = 0;
            String uri = params[0];
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(uri);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file


                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();
                mProgressDialog.setMax(fileLength);
                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(GetAPKPath());
                byte data[] = new byte[4096];
                int total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    //					if (fileLength > 0) // only if total length is known
                    //					{
                    //						i = (int) (total * 100 / fileLength);
                    //					}
                    mProgressDialog.setProgress(total);
                    //					publishProgress(i);
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }
                if (connection != null)
                    connection.disconnect();
            }
            return "right";
        }

        //???ui??????????????? ????????????ui
        @Override
        protected void onPostExecute(String string) {
            // TODO Auto-generated method stub
            super.onPostExecute(string);
            //???????????? ????????????????????????
            if (string.equals("right")) {
                mProgressDialog.cancel();
                Toast.makeText(context, "????????????", Toast.LENGTH_SHORT).show();
                installApk();
            } else {
                mProgressDialog.cancel();
                Toast.makeText(context, "????????????", Toast.LENGTH_LONG).show();
            }


        }

        /*
         * ???doInBackground?????????????????????publishProgress?????? ??????????????????????????????
         * ?????????????????? ????????????????????????
         * */
        @Override
        protected void onProgressUpdate(Integer... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);

        }
    }

    /**
     * ??????APK
     */
    private void installApk() {
        File file = new File(GetAPKPath());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    /**
     * ???????????????????????????
     */
    private String GetAPKPath() {
        File file = new File("/mnt/sdcard/.toInstallPG");
        if (!file.exists()) {
            file.mkdir();
        }
        String path = "/mnt/sdcard/.toInstallPG" + "/" + getAPPName() + nearestVersion + ".apk";
        return path;
    }


    /**
     * ??????????????????
     */
    public String getAPPName() {
        String appName = "";
        PackageManager pm = context.getPackageManager();//??????PackageManager??????
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(context.getPackageName(), 0);
            appName = (String) pm.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appName;
    }


    /**
     * ??????????????????
     */
    public void showRegDialog() {

        View v = LayoutInflater.from(context).inflate(R.layout.reg_dialog, null);
        dialog_Reg = new Dialog(context, R.style.DialogStyle);
        dialog_Reg.setCanceledOnTouchOutside(false);
        dialog_Reg.show();
        dialog_Reg.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog_Reg.dismiss();
                //????????????
                if (cancelCallBack != null) {
                    cancelCallBack.toFinishActivity();
                }
            }
        });
        Window window = dialog_Reg.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        lp.width = RegPubUtils.dip2px(context, 290); // ??????
        lp.height = RegPubUtils.dip2px(context, 200); // ??????
        lp.alpha = 0.7f; // ?????????
        window.setAttributes(lp);
        window.setContentView(v);
        final TextView reg = (TextView) v.findViewById(R.id.editTextReg);
        ImageButton ib = (ImageButton) v.findViewById(R.id.imageButtonReg);
        ImageButton.OnClickListener listener = new ImageButton.OnClickListener() {

            public void onClick(View v) {
                if (!RegPubUtils.isConnected(context.getApplicationContext())) {
                    Toast.makeText(context, "????????????????????????????????????", Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                input = reg.getText().toString().trim();
                if (input == null || TextUtils.isEmpty(input)) {
                    Toast.makeText(context, "??????????????????",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // ???????????????
                progressDialog = ProgressDialog.show(context, "?????????",
                        "?????????????????????????????????????????????", true);
                progressDialog.setCancelable(true);
                present.checkReg(input, APP_MARK, RegLatestContact.CHECK_REG, RegOperateManager.this);

            }
        };

        ib.setOnClickListener(listener);

    }


    /**
     * ???????????????
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void initLocation() {
        //?????????client
        locationClient = new AMapLocationClient(context.getApplicationContext());
        // ??????????????????
        locationClient.setLocationListener(locationListener);
        startLocation();
    }

    /**
     * ?????????????????????
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private AMapLocationClientOption getDefaultOption() {
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????
        mOption.setGpsFirst(false);//?????????????????????gps??????????????????????????????????????????????????????
        mOption.setHttpTimeOut(30000);//???????????????????????????????????????????????????30?????????????????????????????????
        mOption.setInterval(2000);//???????????????????????????????????????2???
        mOption.setNeedAddress(true);//????????????????????????????????????????????????????????????true
        mOption.setOnceLocation(true);//?????????????????????????????????????????????false
        //		mOption.setOnceLocationLatest(false);//???????????????????????????wifi??????????????????false.???????????????true,?????????????????????????????????????????????????????????
        //		AMapLocationClientOption.setLocationProtocol(AMapLocationProtocol.HTTP);//?????????
        //		????????????????????????????????????HTTP??????HTTPS????????????HTTP
        //		mOption.setSensorEnable(false);//????????????????????????????????????????????????false
        return mOption;
    }

    /**
     * ????????????
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void startLocation() {
        //		//????????????????????????????????????????????????
        //		resetOption();
        // ??????????????????
        locationClient.setLocationOption(getDefaultOption());
        // ????????????
        locationClient.startLocation();
    }

    /**
     * ????????????
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void stopLocation() {
        // ????????????
        locationClient.stopLocation();
    }

    /**
     * ????????????
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void destroyLocation() {
        if (null != locationClient) {
            /**
             * ??????AMapLocationClient????????????Activity???????????????
             * ???Activity???onDestroy??????????????????AMapLocationClient???onDestroy
             */
            stopLocation();
            locationClient.onDestroy();
            locationClient = null;
        }
    }

    /**
     * ????????????
     */
    AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation loc) {
            if (null != loc) {
                //??????????????????
                Lat = loc.getLatitude() + "";
                Lng = loc.getLongitude() + "";
                Addr = loc.getAddress();
                checkSavedVersion();
            }
        }
    };

    //    /**
    //     * ???????????????sim??????ICCID???
    //     *
    //     * @return ICCID???
    //     */
    //    public String getRegImeiIccid() {
    //        String msg = "";
    //        TelephonyManager tm = (TelephonyManager) context
    //                .getSystemService(Context.TELEPHONY_SERVICE);
    //        if (TextUtils.isEmpty(tm.getDeviceId())) {
    //            // ??????sim??????ICCID???
    //            return tm.getSimSerialNumber();
    //        } else {
    //            return tm.getDeviceId();
    //        }
    //
    //    }

    private String getPhoneMessage() {
        String lac = "";
        String cid = "";
        String nid = "";
        String imei = "";
        String phoneNo = "";
        String imsi = "";
        String mac = "";
        StringBuffer phoneInfo_sb = new StringBuffer();
        TelephonyManager mTManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTManager != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return "";
            }
            imei = mTManager.getDeviceId();
            phoneNo = mTManager.getLine1Number();
            imsi = mTManager.getSubscriberId();
        }
        mac = macAddress();
        phoneInfo_sb.append("PhoneNo:" + phoneNo + "," + "Imei:" + imei + "," + "Imsi:" + imsi + "," + "Mac:" + mac + ",");

        if (mTManager != null) {
            int phonetype = mTManager.getPhoneType();
            if (phonetype == TelephonyManager.PHONE_TYPE_GSM) {
                GsmCellLocation gcl = (GsmCellLocation) mTManager.getCellLocation();
                cid = gcl.getCid() + "";
                lac = gcl.getLac() + "";
            } else if (phonetype == TelephonyManager.PHONE_TYPE_CDMA) {
                CdmaCellLocation gcl = (CdmaCellLocation) mTManager
                        .getCellLocation();
                if (gcl != null) {
                    nid = gcl.getNetworkId() + "";// nid
                    cid = gcl.getBaseStationId() + "";// cellid
                    lac = gcl.getSystemId() + ""; // sid
                }

            }
            phoneInfo_sb.append("Lat:" + Lat + "," + "Log:" + Lng + "," + "Lac:" + lac + "," + "Cid:" + cid + "," +
                    "Nid:" + nid + "," + "Addr:" + Addr);
        }
        return phoneInfo_sb.toString();
    }

    /**
     * ?????????????????????
     */
    private String getAPPVersion() {
        PackageManager pm = context.getPackageManager();//??????PackageManager??????
        String version_app = "";
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);//??????PackageInfo???????????????????????????????????????????????????
            version_app = pi.versionName;//?????????????????????versionCode????????????
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version_app;
    }


    public String macAddress() {
        String address = null;
        try {
            // ???????????????????????????????????????????????? Enumeration?????????
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface netWork = interfaces.nextElement();
                // ????????????????????????????????????????????????????????????????????????????????????????????????????????? MAC??????
                byte[] by = netWork.getHardwareAddress();
                if (by == null || by.length == 0) {
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                for (byte b : by) {
                    builder.append(String.format("%02X:", b));
                }
                if (builder.length() > 0) {
                    builder.deleteCharAt(builder.length() - 1);
                }
                String mac = builder.toString();
                // ??????????????????????????????MAC?????????????????????????????????Wifi??? name ??? wlan0
                if (netWork.getName().equals("wlan0")) {
                    address = mac;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return address;
    }


    //    /**
    //     * ????????????????????????
    //     */
    //    private void RegSuccess(String input, String guestName, String isToolTip, String isNumber) {
    //        SharedPreferences.Editor editor = sp.edit();
    //        if (input != null && guestName != null) {
    //            Toast.makeText(context, "?????????????????????",
    //                    Toast.LENGTH_LONG).show();
    //            strreg = input;
    //            editor.putString("OBJREG", input);
    //            editor.putString("GUESTNAME", guestName);
    //        }
    //
    //        if (isToolTip.equals("0")) {//0???????????????
    //            istoolTip = false;
    //            editor.putBoolean("ISTOOLTIP", false);
    //        } else {
    //            istoolTip = true;
    //            editor.putBoolean("ISTOOLTIP", true);
    //        }
    //        if (isNumber.equals("0")) {//0?????????????????????
    //            isNumberLimit = true;
    //            editor.putBoolean("ISNUMBER", true);
    //        } else {
    //            isNumberLimit = false;
    //            editor.putBoolean("ISNUMBER", false);
    //        }
    //        editor.commit();
    //        if (dialog_Reg != null && dialog_Reg.isShowing()) {
    //            dialog_Reg.dismiss();
    //        }
    //
    //        if (cancelCallBack != null) {
    //            cancelCallBack.toDoNext();
    //        }
    //
    //    }


    /**
     * ??????????????????????????????
     */
    private void checkSavedVersion() {
        String savedVersion = Hawk.get(HawkProperty.APP_SAVED_VERSION, "");
        //        String savedVersion = "1.0";
        String nowVersion = getAPPVersion();
        if (savedVersion.equals("")) {
            Hawk.put(HawkProperty.APP_SAVED_VERSION, nowVersion);
            //??????????????????
            String info = getInfoWhenVersionChanged(savedVersion, nowVersion);
            present.uploadVersionInfo((String) Hawk.get(HawkProperty.REG_CODE), info, this);
        } else {
            if (!savedVersion.equals(nowVersion) && Double.parseDouble(nowVersion) > Double.parseDouble(savedVersion)) {
                //??????????????????
                String info = getInfoWhenVersionChanged(savedVersion, nowVersion);
                present.uploadVersionInfo((String) Hawk.get(HawkProperty.REG_CODE), info, this);
            }

        }

    }

    private String getInfoWhenVersionChanged(String originalVersion, String newestVersion) {
        String PhoneNo = "";
        String Imei = "";
        String time = "";
        time = RegPubUtils.getDateToString(System.currentTimeMillis());
        StringBuffer Info_sb = new StringBuffer();
        TelephonyManager mTManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTManager != null) {
            Imei = mTManager.getDeviceId();
            PhoneNo = mTManager.getLine1Number();
        }
        Info_sb.append("SoftName:" + getAPPName() + "," + "GuestName:" + Hawk.get(HawkProperty.APP_GUEST_NAME, "") +
                "," + "RegCode:" + Hawk.get(HawkProperty.REG_CODE) + "," + "PhoneNo:" + PhoneNo + "," + "Imei:" + Imei + "," + "Mac:" + macAddress() + "," + "Lat:" + Lat + "," + "Lng:" + Lng + "," + "Addr:" + Addr + "," + "OriginalVersion:" + originalVersion + "," + "NewestVersion:" + newestVersion + "," + "Time:" + time);

        return Info_sb.toString();
    }


    public void setCancelCallBack(RegLatestContact.CancelCallBack callBack) {
        this.cancelCallBack = callBack;
    }
    // ????????????????????????

    public static boolean isConnected(Context context) {
        boolean isOk = true;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobNetInfo = connectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifiNetInfo = connectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (wifiNetInfo != null && !wifiNetInfo.isConnectedOrConnecting()) {
                if (mobNetInfo != null && !mobNetInfo.isConnectedOrConnecting()) {
                    NetworkInfo info = connectivityManager
                            .getActiveNetworkInfo();
                    if (info == null) {
                        isOk = false;
                    }
                }
            }
            mobNetInfo = null;
            wifiNetInfo = null;
            connectivityManager = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isOk;
    }

}

package org.easydarwin.easypusher.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: tobato
 * @Description: 作用描述
 * @CreateDate: 2020/4/26 11:53
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/4/26 11:53
 */
public class PublicUtil {

    private static MediaScannerConnection mMediaScanner;

    public static boolean isIP(String addr) {
        if (addr.length() < 7 || addr.length() > 15 || "".equals(addr)) {
            return false;
        }
        /**
         * 判断IP格式和范围
         */
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

        Pattern pat = Pattern.compile(rexp);

        Matcher mat = pat.matcher(addr);

        boolean ipAddress = mat.find();

        //============对之前的ip判断的bug在进行判断
        if (ipAddress == true) {
            String ips[] = addr.split("\\.");

            if (ips.length == 4) {
                try {
                    for (String ip : ips) {
                        if (Integer.parseInt(ip) < 0 || Integer.parseInt(ip) > 255) {
                            return false;
                        }

                    }
                } catch (Exception e) {
                    return false;
                }

                return true;
            } else {
                return false;
            }
        }

        return ipAddress;
    }

    /**
     * 系统10.0以上
     *
     * @return
     */
    public static boolean isMoreThanTheAndroid10() {
        return Build.VERSION.SDK_INT > 28;
    }

    /**
     * 通知系统相册更新图库
     *
     * @param context
     * @param imagePath
     */
    public static void sendBroadcastToAlbum(Context context, String imagePath) {
        if (context != null && imagePath != null && imagePath.length() > 0) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri uri = Uri.fromFile(imageFile);
                if (uri != null && context != null) {
                    intent.setData(uri);
                    context.sendBroadcast(intent);
                }
            }
        }
    }

    /**
     * 刷新图库
     * @param mContext
     * @param fileAbsolutePath
     * @param isVideo
     */
    public static void refreshAlbum(Context mContext,String fileAbsolutePath, boolean isVideo) {

        mMediaScanner = new MediaScannerConnection(mContext, new MediaScannerConnection.MediaScannerConnectionClient() {

            @Override

            public void onMediaScannerConnected() {

                if (mMediaScanner.isConnected()) {


                    if (isVideo) {

                        mMediaScanner.scanFile(fileAbsolutePath, "video/mp4");

                    } else {

                        mMediaScanner.scanFile(fileAbsolutePath, "image/jpeg");

                    }

                } else {


                }

            }

            @Override

            public void onScanCompleted(String path, Uri uri) {


            }

        });

        mMediaScanner.connect();

    }


    /**
     * 保存视频
     * @param context
     * @param file
     */
    public static void saveVideo(Context context, File file) {
        //是否添加到相册
        ContentResolver localContentResolver = context.getContentResolver();
        ContentValues localContentValues = getVideoContentValues(context, file, System.currentTimeMillis());
        Uri localUri = localContentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, localContentValues);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri));
    }

    public static ContentValues getVideoContentValues(Context paramContext, File paramFile, long paramLong) {
        ContentValues localContentValues = new ContentValues();
        localContentValues.put("title", paramFile.getName());
        localContentValues.put("_display_name", paramFile.getName());
        localContentValues.put("mime_type", "video/mp4");
        localContentValues.put("datetaken", Long.valueOf(paramLong));
        localContentValues.put("date_modified", Long.valueOf(paramLong));
        localContentValues.put("date_added", Long.valueOf(paramLong));
        localContentValues.put("_data", paramFile.getAbsolutePath());
        localContentValues.put("_size", Long.valueOf(paramFile.length()));
        return localContentValues;
    }

}

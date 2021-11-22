package org.easydarwin.easypusher.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

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
//        return Build.VERSION.SDK_INT > 28;
        return false;
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
                refreshGrally(mContext,path);
                mMediaScanner.disconnect();
            }
        });
        mMediaScanner.connect();

    }


    /**
     * 保存视频
     * @param context
     */



    public static void saveVideo(Context context, String filePath) {
        ContentResolver localContentResolver = context.getContentResolver();
        ContentValues localContentValues = getVideoContentValues(new File(filePath), System.currentTimeMillis());
        Uri localUri = localContentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, localContentValues);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri));
    }
    public static ContentValues getVideoContentValues(File paramFile, long paramLong) {
        ContentValues localContentValues = new ContentValues();
        localContentValues.put(MediaStore.Video.Media.TITLE, paramFile.getName());
        localContentValues.put(MediaStore.Video.Media.DISPLAY_NAME, paramFile.getName());
        localContentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        localContentValues.put(MediaStore.Video.Media.DATE_TAKEN, Long.valueOf(paramLong));
        localContentValues.put(MediaStore.Video.Media.DATE_MODIFIED, Long.valueOf(paramLong));
        localContentValues.put(MediaStore.Video.Media.DATE_ADDED, Long.valueOf(paramLong));
        localContentValues.put(MediaStore.Video.Media.DATA, paramFile.getAbsolutePath());
        localContentValues.put(MediaStore.Video.Media.SIZE, Long.valueOf(paramFile.length()));
        return localContentValues;
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

    public static void refreshGrally(Context mContext,String filePath){
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= 24) {//7.0 Android N
            //com.xxx.xxx.fileprovider为上述manifest中provider所配置相同
            uri = FileProvider.getUriForFile(mContext, "org.chuangchi.yjdb.fileProvider", new File(filePath));
            // 读取权限，安装完毕以后，系统会自动收回权限，该过程没有用户交互
        } else {//7.0以下
            uri = Uri.fromFile(new File(filePath));
        }
        Intent localIntent =new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,uri);
        mContext.sendBroadcast(localIntent);

    }

    /**
     * 刷新相册
     * @param path
     */
    public static void refreshAlbumByMediaScannerConnectionMP4(Context context, String path) {
        String[] paths = {path};
        String[] mimeTypes = {"video/mp4"};
        MediaScannerConnection.scanFile(context, paths, mimeTypes,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        refreshGrally(context,path);
                    }
                });
    }


    /**
     * 刷新视频到相册方法一 API通用方法包括API29及以上高版本方法
     *
     * @param activity     当前页面
     * @param fileNamePath 文件路径
     */
    public static void refreshApi29(Activity activity, String fileNamePath) {
        //刷新相册，mineTypes为null的话让系统自己根据文件后缀判断文件类型
        MediaScannerConnection.scanFile(activity, new String[]{fileNamePath}, null, (path, uri) -> Log.e("资源刷新成功路径为", path));
        //代表只刷新视频格式为mp4类型其它格式视频文件不刷新
//                MediaScannerConnection.scanFile(activity, new String[]{fileNamePath}, new String[]{"video/mp4"}, (path, uri) -> Log.e("资源刷新成功路径为", path));
        //代表刷新视频文件，只要是视频都刷新根据当前Android系统支持哪些视频格式进行刷新
//                MediaScannerConnection.scanFile(activity, new String[]{fileNamePath}, new String[]{"video/*"}, (path, uri) -> Log.e("资源刷新成功路径为", path));
        //代表只刷新图片格式为jpg的文件到相册中
//                MediaScannerConnection.scanFile(activity, new String[]{fileNamePath}, new String[]{"image/jpg"}, (path, uri) -> Log.e("资源刷新成功路径为", path));
        //代表刷新图片到相册只要是图片就会刷新
//                MediaScannerConnection.scanFile(activity, new String[]{fileNamePath}, new String[]{"image/*"}, (path, uri) -> Log.e("资源刷新成功路径为", path));
    }

}

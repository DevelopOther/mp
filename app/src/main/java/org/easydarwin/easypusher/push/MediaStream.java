package org.easydarwin.easypusher.push;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.orhanobut.hawk.Hawk;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.UVCCamera;

import org.easydarwin.bus.SupportResolution;
import org.easydarwin.easypusher.MyApp;
import org.easydarwin.easypusher.util.Config;
import org.easydarwin.easypusher.util.HawkProperty;
import org.easydarwin.easypusher.util.SPUtil;
import org.easydarwin.easyrtmp.push.EasyRTMP;
import org.easydarwin.encode.AudioStream;
import org.easydarwin.encode.ClippableVideoConsumer;
import org.easydarwin.encode.HWConsumer;
import org.easydarwin.encode.SWConsumer;
import org.easydarwin.encode.VideoConsumer;
import org.easydarwin.muxer.EasyMuxer;
import org.easydarwin.muxer.RecordVideoConsumer;
import org.easydarwin.push.InitCallback;
import org.easydarwin.push.Pusher;
import org.easydarwin.sw.JNIUtil;
import org.easydarwin.util.BUSUtil;
import org.easydarwin.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar;
import static org.easydarwin.easypusher.BuildConfig.RTMP_KEY;

/**
 * 摄像头实时数据采集，并调用相关编码器
 */
public class MediaStream {
    private static final String TAG = MediaStream.class.getSimpleName();
    private static final int SWITCH_CAMERA = 11;

    private final boolean enableVideo;
    private boolean mSWCodec, mHevc;    // mSWCodec是否软编码, mHevc是否H265

    private String recordPath;          // 录像地址
    protected boolean isPushStream = false;       // 是否要推送数据
    protected boolean isBiliPushStream = false;       // 是否要推送bili数据
    protected boolean isHuyaPushStream = false;       // 是否要推送huya数据
    protected boolean isYiPushStream = false;       // 是否要推送huya数据
    protected boolean isNowPushStream = false;       // 是否要推送huya数据
    private int displayRotationDegree;  // 旋转角度

    private Context context;
    WeakReference<SurfaceTexture> mSurfaceHolderRef;

    private VideoConsumer mVC, mRecordVC, mFirstVC, mSecendVC, mThirdVC, mFourthVC;
    private AudioStream audioStream;
    private EasyMuxer mMuxer;
    private Pusher mEasyPusher;
    private Pusher mFirstEasyPusher;//第一个
    private Pusher mSecendEasyPusher;//第二个
    private Pusher mThirdEasyPusher;//yi
    private Pusher mFourthEasyPusher;//now

    private final HandlerThread mCameraThread;
    private final Handler mCameraHandler;
    /*
     * 默认后置摄像头
     *   Camera.CameraInfo.CAMERA_FACING_BACK
     *   Camera.CameraInfo.CAMERA_FACING_FRONT
     *   CAMERA_FACING_BACK_UVC
     * */ int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    public static final int CAMERA_FACING_BACK = 0;//后置
    public static final int CAMERA_FACING_FRONT = 1;
    public static final int CAMERA_FACING_BACK_UVC = 2;
    public static final int CAMERA_FACING_BACK_LOOP = -1;
    int defaultWidth = 1920, defaultHeight = 1080;
    int nativeWidth, nativeHeight;//原生camera的宽高
    int uvcWidth, uvcHeight;//uvcCamera的宽高
    private int mTargetCameraId;
    private int frameWidth;
    private int frameHeight;
    private int pushType = -1;//0代表正常推流 1代表bili 2 代表 虎牙 3 代表 一直播 4代表now直播

    /**
     * 初始化MediaStream
     */
    public MediaStream(Context context, SurfaceTexture texture, boolean enableVideo) {
        this.context = context;
        audioStream = AudioStream.getInstance(context);
        mSurfaceHolderRef = new WeakReference(texture);

        mCameraThread = new HandlerThread("CAMERA") {
            public void run() {
                try {
                    super.run();
                } catch (Throwable e) {
                    e.printStackTrace();
                    Intent intent = new Intent(context, BackgroundCameraService.class);
                    context.stopService(intent);
                } finally {
                    for (int i = 0; i < 5; i++) {
                        stopPusherStream(i);
                    }
                    stopPreview();
                    destroyCamera();
                }
            }
        };

        mCameraThread.start();

        mCameraHandler = new Handler(mCameraThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.what == SWITCH_CAMERA) {
                    switchCameraTask.run();
                }
            }
        };

        this.enableVideo = enableVideo;
    }

    /// 初始化摄像头
    public void createCamera(int mCameraId) {
        this.mCameraId = mCameraId;
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                createCamera(mCameraId);
            });

            return;
        }

        mSWCodec = Hawk.get(HawkProperty.KEY_SW_CODEC, true);
        mHevc = SPUtil.getHevcCodec(context);
        mEasyPusher = new EasyRTMP(mHevc ? EasyRTMP.VIDEO_CODEC_H265 : EasyRTMP.VIDEO_CODEC_H264, RTMP_KEY);
        mFirstEasyPusher = new EasyRTMP(mHevc ? EasyRTMP.VIDEO_CODEC_H265 : EasyRTMP.VIDEO_CODEC_H264, RTMP_KEY);
        mSecendEasyPusher = new EasyRTMP(mHevc ? EasyRTMP.VIDEO_CODEC_H265 : EasyRTMP.VIDEO_CODEC_H264, RTMP_KEY);
        mThirdEasyPusher = new EasyRTMP(mHevc ? EasyRTMP.VIDEO_CODEC_H265 : EasyRTMP.VIDEO_CODEC_H264, RTMP_KEY);
        mFourthEasyPusher = new EasyRTMP(mHevc ? EasyRTMP.VIDEO_CODEC_H265 : EasyRTMP.VIDEO_CODEC_H264, RTMP_KEY);

        if (!enableVideo) {
            return;
        }

        if (mCameraId == CAMERA_FACING_BACK_UVC) {
            createUvcCamera();
        } else {
            createNativeCamera();
        }
    }

    private void createNativeCamera() {
        try {
            mCamera = Camera.open(mCameraId);// 初始化创建Camera实例对象
            mCamera.setErrorCallback((i, camera) -> {
                throw new IllegalStateException("Camera Error:" + i);
            });
            Log.i(TAG, "open Camera");

            parameters = mCamera.getParameters();

            if (Util.getSupportResolution(context).size() == 0) {
                StringBuilder stringBuilder = new StringBuilder();

                // 查看支持的预览尺寸
                List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();

                for (Camera.Size str : supportedPreviewSizes) {
                    stringBuilder.append(str.width + "x" + str.height).append(";");
                }

                Util.saveSupportResolution(context, stringBuilder.toString());
            }

            BUSUtil.BUS.post(new SupportResolution());

            camInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, camInfo);
            int cameraRotationOffset = camInfo.orientation;

            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT)
                cameraRotationOffset += 180;

            int rotate = (360 + cameraRotationOffset - displayRotationDegree) % 360;
            parameters.setRotation(rotate); // 设置Camera预览方向
            //            parameters.setRecordingHint(true);

            ArrayList<CodecInfo> infos = listEncoders(mHevc ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC);

            if (!infos.isEmpty()) {
                CodecInfo ci = infos.get(0);
                info.mName = ci.mName;
                info.mColorFormat = ci.mColorFormat;
            } else {
                mSWCodec = true;
            }
            nativeWidth = Hawk.get(HawkProperty.KEY_NATIVE_WIDTH, defaultWidth);
            nativeHeight = Hawk.get(HawkProperty.KEY_NATIVE_HEIGHT, defaultHeight);
            //            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            parameters.setPreviewSize(nativeWidth, nativeHeight);// 设置预览尺寸

            int[] ints = determineMaximumSupportedFramerate(parameters);
            parameters.setPreviewFpsRange(ints[0], ints[1]);

            List<String> supportedFocusModes = parameters.getSupportedFocusModes();

            if (supportedFocusModes == null)
                supportedFocusModes = new ArrayList<>();

            // 自动对焦
            if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }


            mCamera.setParameters(parameters);
            Log.i(TAG, "setParameters");

            int displayRotation;
            displayRotation = (cameraRotationOffset - displayRotationDegree + 360) % 360;
            mCamera.setDisplayOrientation(displayRotation);

            Log.i(TAG, "setDisplayOrientation");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            //            String stack = sw.toString();
            destroyCamera();
            e.printStackTrace();
        }
    }

    /**
     * uvc 第一步是创建camera
     */
    private void createUvcCamera() {
        //        int previewWidth = 640;
        //        int previewHeight = 480;
        ArrayList<CodecInfo> infos = listEncoders(mHevc ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC);

        if (!infos.isEmpty()) {
            CodecInfo ci = infos.get(0);
            info.mName = ci.mName;
            info.mColorFormat = ci.mColorFormat;
        } else {
            mSWCodec = true;
        }
        frameWidth = nativeWidth;
        frameHeight = nativeHeight;
        //        uvcWidth = Hawk.get(HawkProperty.KEY_UVC_WIDTH, defaultWidth);
        //        uvcHeight = Hawk.get(HawkProperty.KEY_UVC_HEIGHT, defaultHeight);
        uvcCamera = UVCCameraService.liveData.getValue();
        if (uvcCamera != null) {
            uvcCamera.setPreviewSize(frameWidth,
                    frameHeight,
                    1,
                    30,
                    UVCCamera.PIXEL_FORMAT_YUV420SP,1.0f);
            //            uvcCamera.setPreviewSize(uvcWidth,uvcHeight,1,30,UVCCamera.FRAME_FORMAT_MJPEG, 1.0f);
//            try {
//                uvcCamera.setPreviewSize(frameWidth, frameHeight, 1, 30, UVCCamera.FRAME_FORMAT_MJPEG, 1.0f);
//            } catch (final IllegalArgumentException e) {
//                try {
//                    // fallback to YUV mode
//                    uvcCamera.setPreviewSize(frameWidth, frameHeight, 1, 30, UVCCamera.DEFAULT_PREVIEW_MODE, 1.0f);
//                } catch (final IllegalArgumentException e1) {
//                    if (uvcCamera != null) {
//                        uvcCamera.destroy();
//                        uvcCamera = null;
//                    }
//                }
//            }
        }

        if (uvcCamera == null) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            createNativeCamera();
        }
    }


    /// 开启预览
    public synchronized void startPreview() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(() -> startPreview());
            return;
        }
        if (uvcCamera != null) {

            startUvcPreview();
            initConsumer(uvcWidth, uvcHeight);
        } else if (mCamera != null) {

            startCameraPreview();
            initConsumer(frameWidth, frameHeight);
        }
        audioStream.setEnableAudio(SPUtil.getEnableAudio(context));
        audioStream.addPusher(mEasyPusher);
        audioStream.addPusher(mFirstEasyPusher);
        audioStream.addPusher(mSecendEasyPusher);
        audioStream.addPusher(mThirdEasyPusher);
        audioStream.addPusher(mFourthEasyPusher);
    }

    private void initConsumer(int width, int height) {
        if (mSWCodec) {
            SWConsumer sw = new SWConsumer(context, mEasyPusher, SPUtil.getBitrateKbps(context));
            mVC = new ClippableVideoConsumer(context, sw, width, height, SPUtil.getEnableVideoOverlay(context));
            SWConsumer swBili = new SWConsumer(context, mFirstEasyPusher, SPUtil.getBitrateKbps(context));
            mFirstVC = new ClippableVideoConsumer(context, swBili, width, height, SPUtil.getEnableVideoOverlay(context));
            SWConsumer swHuya = new SWConsumer(context, mSecendEasyPusher, SPUtil.getBitrateKbps(context));
            mSecendVC = new ClippableVideoConsumer(context, swHuya, width, height, SPUtil.getEnableVideoOverlay(context));
            SWConsumer swYi = new SWConsumer(context, mThirdEasyPusher, SPUtil.getBitrateKbps(context));
            mThirdVC = new ClippableVideoConsumer(context, swYi, width, height, SPUtil.getEnableVideoOverlay(context));
            SWConsumer swNow = new SWConsumer(context, mFourthEasyPusher, SPUtil.getBitrateKbps(context));
            mFourthVC = new ClippableVideoConsumer(context, swNow, width, height, SPUtil.getEnableVideoOverlay(context));
        } else {
            HWConsumer hw = new HWConsumer(context, mHevc ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC, mEasyPusher, SPUtil.getBitrateKbps(context), info.mName, info.mColorFormat);
            HWConsumer hwBili = new HWConsumer(context, mHevc ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC, mFirstEasyPusher, SPUtil.getBitrateKbps(context), info.mName, info.mColorFormat);
            HWConsumer hwHuya = new HWConsumer(context, mHevc ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC, mSecendEasyPusher, SPUtil.getBitrateKbps(context), info.mName, info.mColorFormat);
            HWConsumer hwYi = new HWConsumer(context, mHevc ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC, mThirdEasyPusher, SPUtil.getBitrateKbps(context), info.mName, info.mColorFormat);
            HWConsumer hwNow = new HWConsumer(context, mHevc ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC, mFourthEasyPusher, SPUtil.getBitrateKbps(context), info.mName, info.mColorFormat);
            mVC = new ClippableVideoConsumer(context, hw, width, height, SPUtil.getEnableVideoOverlay(context));
            mFirstVC = new ClippableVideoConsumer(context, hwBili, width, height, SPUtil.getEnableVideoOverlay(context));
            mSecendVC = new ClippableVideoConsumer(context, hwHuya, width, height, SPUtil.getEnableVideoOverlay(context));
            mThirdVC = new ClippableVideoConsumer(context, hwYi, width, height, SPUtil.getEnableVideoOverlay(context));
            mFourthVC = new ClippableVideoConsumer(context, hwNow, width, height, SPUtil.getEnableVideoOverlay(context));
        }
        mVC.onVideoStart(width, height);
        mFirstVC.onVideoStart(width, height);
        mSecendVC.onVideoStart(width, height);
        mThirdVC.onVideoStart(width, height);
        mFourthVC.onVideoStart(width, height);
    }

    /**
     * uvc 第二步 开始预览
     */
    private void startUvcPreview() {
        SurfaceTexture holder = mSurfaceHolderRef.get();
        if (holder != null) {
            uvcCamera.setPreviewTexture(holder);
        }

        try {
            uvcCamera.setFrameCallback(uvcFrameCallback, UVCCamera.PIXEL_FORMAT_YUV420SP/*UVCCamera.PIXEL_FORMAT_NV21*/);
            uvcCamera.startPreview();
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private void startCameraPreview() {
        int previewFormat = parameters.getPreviewFormat();

        Camera.Size previewSize = parameters.getPreviewSize();
        int size = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(previewFormat) / 8;
        mCamera.addCallbackBuffer(new byte[size]);
        mCamera.addCallbackBuffer(new byte[size]);
        mCamera.setPreviewCallbackWithBuffer(previewCallback);

        Log.i(TAG, "setPreviewCallbackWithBuffer");

        try {
            // TextureView的
            SurfaceTexture holder = mSurfaceHolderRef.get();

            // SurfaceView传入上面创建的Camera对象
            if (holder != null) {
                mCamera.setPreviewTexture(holder);
                Log.i(TAG, "setPreviewTexture");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.startPreview();
        boolean frameRotate;
        int result;

        if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (camInfo.orientation + displayRotationDegree) % 360;
        } else {  // back-facing
            result = (camInfo.orientation - displayRotationDegree + 360) % 360;
        }

        frameRotate = result % 180 != 0;

        frameWidth = frameRotate ? nativeHeight : nativeWidth;
        frameHeight = frameRotate ? nativeWidth : nativeHeight;
    }

    /// 停止预览
    public synchronized void stopPreview() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(() -> stopPreview());
            return;
        }

        if (uvcCamera != null) {
            uvcCamera.stopPreview();
        }

        //        mCameraHandler.removeCallbacks(dequeueRunnable);

        // 关闭摄像头
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
        }

        // 关闭音频采集和音频编码器
        if (audioStream != null) {
            audioStream.removePusher(mEasyPusher);
            audioStream.removePusher(mFirstEasyPusher);
            audioStream.removePusher(mSecendEasyPusher);
            audioStream.removePusher(mThirdEasyPusher);
            audioStream.removePusher(mFourthEasyPusher);
            audioStream.setMuxer(null);
            Log.i(TAG, "Stop AudioStream");
        }
        for (int i = 0; i < 5; i++) {
            stopVcVedio(i);
        }

        // 关闭录像的编码器
        if (mRecordVC != null) {
            mRecordVC.onVideoStop();
        }

        // 关闭音视频合成器
        if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
        }
    }

    /**
     * 关闭视频编码器
     *  private int pushType = -1;//0代表正常推流 1代表bili 2 代表 虎牙 3 代表 一直播 4代表now直播
     * @param type
     */
    private void stopVcVedio(int type) {
        VideoConsumer videoConsumer = null;
        switch (type) {
            case 0:
                videoConsumer = mVC;
                break;
            case 1:
                videoConsumer = mFirstVC;
                break;
            case 2:
                videoConsumer = mSecendVC;
                break;
            case 3:
                videoConsumer = mThirdVC;
                break;
            case 4:
                videoConsumer = mFourthVC;
                break;
            default:
                break;
        }
        // 关闭视频编码器
        if (videoConsumer != null) {
            videoConsumer.onVideoStop();
        }

    }


    /// 开始推流
    // private int pushType = -1;//0代表正常推流 1代表bili 2 代表 虎牙 3 代表 一直播 4代表now直播
    public void startPushStream(int pushType, InitCallback callback) throws IOException {
        Pusher pusher = null;
        String url = null;
        switch (pushType) {
            case 0:
                pusher = mEasyPusher;
                url = Config.getServerURL();
                isPushStream = true;
                break;
            case 1:
                pusher = mFirstEasyPusher;
                url = Hawk.get(HawkProperty.KEY_BILIBILI_URL);
                isBiliPushStream = true;
                break;
            case 2:
                pusher = mSecendEasyPusher;
                url = Hawk.get(HawkProperty.KEY_HU_YA_URL);
                isHuyaPushStream = true;
                break;
            case 3:
                pusher = mThirdEasyPusher;
                url = Hawk.get(HawkProperty.KEY_YI_URL);
                isYiPushStream = true;
                break;
            case 4:
                pusher = mFourthEasyPusher;
                url = Hawk.get(HawkProperty.KEY_NOW_URL);
                isNowPushStream = true;
                break;
            default:
                break;
        }
        try {
            if (SPUtil.getEnableVideo(MyApp.getEasyApplication())) {
                if (!TextUtils.isEmpty(url)) {
                    pusher.initPush(url, context, callback);
                }
            } else {
                if (!TextUtils.isEmpty(url)) {
                    pusher.initPush(url, context, callback, ~0);
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }


    /// 停止推流
    public void stopPusherStream(int pushType) {
        Pusher pusher = null;
        switch (pushType) {
            case 0:
                pusher = mEasyPusher;
                isPushStream = false;
                break;
            case 1:
                pusher = mFirstEasyPusher;
                isBiliPushStream = false;
                break;
            case 2:
                pusher = mSecendEasyPusher;
                isHuyaPushStream = false;
                break;
            case 3:
                pusher = mThirdEasyPusher;
                isYiPushStream = false;
                break;
            case 4:
                pusher = mFourthEasyPusher;
                isNowPushStream = false;
                break;
            default:
                break;
        }
        if (pusher != null) {
            pusher.stop();
        }
    }

    /// 开始录像
    public synchronized void startRecord() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(() -> startRecord());
            return;
        }

        if (mCamera == null && uvcCamera == null) {
            return;
        }

        // 默认录像时间300000毫秒
        mMuxer = new EasyMuxer(new File(recordPath, new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date())).toString(), 300000);

        mRecordVC = new RecordVideoConsumer(context, mHevc ? MediaFormat.MIMETYPE_VIDEO_HEVC : MediaFormat.MIMETYPE_VIDEO_AVC, mMuxer, SPUtil.getEnableVideoOverlay(context), SPUtil.getBitrateKbps(context), info.mName, info.mColorFormat);
        if (uvcCamera != null) {
            mRecordVC.onVideoStart(uvcWidth, uvcHeight);
        } else {
            boolean frameRotate;
            int result;

            if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (camInfo.orientation + displayRotationDegree) % 360;
            } else {  // back-facing
                result = (camInfo.orientation - displayRotationDegree + 360) % 360;
            }
            frameRotate = result % 180 != 0;
            mRecordVC.onVideoStart(frameRotate ? nativeHeight : nativeWidth, frameRotate ? nativeWidth : nativeHeight);
        }
        if (audioStream != null) {
            audioStream.setMuxer(mMuxer);
        }
    }

    /// 停止录像
    public synchronized void stopRecord() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(() -> stopRecord());
            return;
        }

        if (mRecordVC == null || audioStream == null) {
            //            nothing
        } else {
            audioStream.setMuxer(null);
            mRecordVC.onVideoStop();
            mRecordVC = null;
        }

        if (mMuxer != null)
            mMuxer.release();

        mMuxer = null;
    }

    /// 更新分辨率
    public void updateResolution() {
        if (mCamera == null && uvcCamera == null)
            return;

        stopPreview();
        destroyCamera();
        //
        //        mCameraHandler.post(() -> {
        //            frameWidth = w;
        //            frameHeight = h;
        //        });

        createCamera(mCameraId);
        startPreview();
    }

    /* ============================== Camera ============================== */


    /**
     * 切换前后摄像头
     * CAMERA_FACING_BACK_LOOP                 循环切换摄像头
     * Camera.CameraInfo.CAMERA_FACING_BACK    后置摄像头
     * Camera.CameraInfo.CAMERA_FACING_FRONT   前置摄像头
     * CAMERA_FACING_BACK_UVC                  UVC摄像头
     */
    public void switchCamera(int cameraId) {
        mCameraId = cameraId;
        if (mCameraHandler.hasMessages(SWITCH_CAMERA)) {
            return;
        } else {
            mCameraHandler.sendEmptyMessage(SWITCH_CAMERA);
        }
    }


    /// 切换摄像头的线程
    private Runnable switchCameraTask = new Runnable() {
        @Override
        public void run() {
            if (!enableVideo)
                return;

            try {
                if (mCameraId == CAMERA_FACING_BACK_UVC) {
                    if (uvcCamera != null) {
                        return;
                    }
                }

                stopPreview();
                destroyCamera();
                createCamera(mCameraId);
                startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

            }
        }
    };

    /* ============================== Native Camera ============================== */

    Camera mCamera;
    private Camera.CameraInfo camInfo;
    private Camera.Parameters parameters;
    private byte[] i420_buffer;

    // 摄像头预览的视频流数
    Camera.PreviewCallback previewCallback = (data, camera) -> {
        if (data == null)
            return;

        int result;
        if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (camInfo.orientation + displayRotationDegree) % 360;
        } else {  // back-facing
            result = (camInfo.orientation - displayRotationDegree + 360) % 360;
        }

        if (i420_buffer == null || i420_buffer.length != data.length) {
            i420_buffer = new byte[data.length];
        }

        JNIUtil.ConvertToI420(data, i420_buffer, nativeWidth, nativeHeight, 0, 0, nativeWidth, nativeHeight, result % 360, 2);
        System.arraycopy(i420_buffer, 0, data, 0, data.length);

        if (mRecordVC != null) {
            mRecordVC.onVideo(i420_buffer, 0);
        }

        mVC.onVideo(data, 0);
        mFirstVC.onVideo(data, 0);
        mSecendVC.onVideo(data, 0);
        mThirdVC.onVideo(data, 0);
        mFourthVC.onVideo(data, 0);
        mCamera.addCallbackBuffer(data);
    };

    /* ============================== UVC Camera ============================== */

    private UVCCamera uvcCamera;

    BlockingQueue<byte[]> cache = new ArrayBlockingQueue<byte[]>(100);

    final IFrameCallback uvcFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(ByteBuffer frame) {
            if (uvcCamera == null)
                return;

            Thread.currentThread().setName("UVCCamera");
            frame.clear();

            byte[] data = cache.poll();
            if (data == null) {
                data = new byte[frame.capacity()];
            }

            frame.get(data);

            //            bufferQueue.offer(data);
            //            mCameraHandler.post(dequeueRunnable);

            onUvcCameraPreviewFrame(data, uvcCamera);
        }
    };

    public void onUvcCameraPreviewFrame(byte[] data, Object camera) {
        if (data == null)
            return;

        if (i420_buffer == null || i420_buffer.length != data.length) {
            i420_buffer = new byte[data.length];
        }

        JNIUtil.ConvertToI420(data, i420_buffer, uvcWidth, uvcHeight, 0, 0, uvcWidth, uvcHeight, 0, 2);
        System.arraycopy(i420_buffer, 0, data, 0, data.length);

        if (mRecordVC != null) {
            mRecordVC.onVideo(i420_buffer, 0);
        }

        mVC.onVideo(data, 0);
    }

    /* ============================== CodecInfo ============================== */

    public static CodecInfo info = new CodecInfo();

    public static class CodecInfo {
        public String mName;
        public int mColorFormat;
    }

    public static ArrayList<CodecInfo> listEncoders(String mime) {
        // 可能有多个编码库，都获取一下
        ArrayList<CodecInfo> codecInfoList = new ArrayList<>();
        int numCodecs = MediaCodecList.getCodecCount();

        // int colorFormat = 0;
        // String name = null;
        for (int i1 = 0; i1 < numCodecs; i1++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i1);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            if (codecMatch(mime, codecInfo)) {
                String name = codecInfo.getName();
                int colorFormat = getColorFormat(codecInfo, mime);

                if (colorFormat != 0) {
                    CodecInfo ci = new CodecInfo();
                    ci.mName = name;
                    ci.mColorFormat = colorFormat;
                    codecInfoList.add(ci);
                }
            }
        }

        return codecInfoList;
    }

    /* ============================== private method ============================== */

    private static boolean codecMatch(String mimeType, MediaCodecInfo codecInfo) {
        String[] types = codecInfo.getSupportedTypes();

        for (String type : types) {
            if (type.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }

        return false;
    }

    private static int getColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        // 在ByteBuffer模式下，视频缓冲区根据其颜色格式进行布局。
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        int[] cf = new int[capabilities.colorFormats.length];
        System.arraycopy(capabilities.colorFormats, 0, cf, 0, cf.length);
        List<Integer> sets = new ArrayList<>();

        for (int i = 0; i < cf.length; i++) {
            sets.add(cf[i]);
        }

        if (sets.contains(COLOR_FormatYUV420SemiPlanar)) {
            return COLOR_FormatYUV420SemiPlanar;
        } else if (sets.contains(COLOR_FormatYUV420Planar)) {
            return COLOR_FormatYUV420Planar;
        } else if (sets.contains(COLOR_FormatYUV420PackedPlanar)) {
            return COLOR_FormatYUV420PackedPlanar;
        } else if (sets.contains(COLOR_TI_FormatYUV420PackedSemiPlanar)) {
            return COLOR_TI_FormatYUV420PackedSemiPlanar;
        }

        return 0;
    }

    private static int[] determineMaximumSupportedFramerate(Camera.Parameters parameters) {
        int[] maxFps = new int[]{0, 0};
        List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();

        for (Iterator<int[]> it = supportedFpsRanges.iterator(); it.hasNext(); ) {
            int[] interval = it.next();

            if (interval[1] > maxFps[1] || (interval[0] > maxFps[0] && interval[1] == maxFps[1])) {
                maxFps = interval;
            }
        }

        return maxFps;
    }

    /* ============================== get/set ============================== */

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    public boolean isRecording() {
        return mMuxer != null;
    }

    public void setSurfaceTexture(SurfaceTexture texture) {
        mSurfaceHolderRef = new WeakReference<SurfaceTexture>(texture);
    }


    public Camera getCamera() {
        return mCamera;
    }

    public int getDisplayRotationDegree() {
        return displayRotationDegree;
    }

    public void setDisplayRotationDegree(int degree) {
        displayRotationDegree = degree;
    }

    /**
     * 旋转YUV格式数据
     *
     * @param src    YUV数据
     * @param format 0，420P；1，420SP
     * @param width  宽度
     * @param height 高度
     * @param degree 旋转度数
     */
    private static void yuvRotate(byte[] src, int format, int width, int height, int degree) {
        int offset = 0;
        if (format == 0) {
            JNIUtil.rotateMatrix(src, offset, width, height, degree);
            offset += (width * height);
            JNIUtil.rotateMatrix(src, offset, width / 2, height / 2, degree);
            offset += width * height / 4;
            JNIUtil.rotateMatrix(src, offset, width / 2, height / 2, degree);
        } else if (format == 1) {
            JNIUtil.rotateMatrix(src, offset, width, height, degree);
            offset += width * height;
            JNIUtil.rotateShortMatrix(src, offset, width / 2, height / 2, degree);
        }
    }

    /// 销毁Camera
    public synchronized void destroyCamera() {
        if (Thread.currentThread() != mCameraThread) {
            mCameraHandler.post(() -> destroyCamera());
            return;
        }

        if (uvcCamera != null) {
            uvcCamera.destroy();
            uvcCamera = null;
        }

        if (mCamera != null) {
            mCamera.stopPreview();

            try {
                mCamera.release();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.i(TAG, "release Camera");

            mCamera = null;
        }

        if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
        }
    }

    /// 回收线程
    public void release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mCameraThread.quitSafely();
        } else {
            if (!mCameraHandler.post(() -> mCameraThread.quit())) {
                mCameraThread.quit();
            }
        }

        try {
            mCameraThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
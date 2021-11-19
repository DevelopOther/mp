package org.easydarwin.bus;

/**
 * 停止录像的通知
 *
 * Created by apple on 2017/7/21.
 */
public class StopRecord {

    public StopRecord(String videoPath) {
        this.videoPath = videoPath;
    }

    private  String  videoPath;

    public String getVideoPath() {
        return videoPath == null ? "" : videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath == null ? "" : videoPath;
    }
}

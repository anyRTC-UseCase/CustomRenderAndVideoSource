package org.ar.sample_custom_video;

import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;


import org.ar.rtc.IRtcEngineEventHandler;
import org.ar.rtc.RtcEngine;
import org.ar.rtc.video.ARVideoFrame;
import org.ar.rtc.video.VideoCanvas;

import androidx.appcompat.app.AppCompatActivity;


public class VideoActivity extends AppCompatActivity implements CameraPresenter.CameraCallBack{


    private static final String TAG = "VideoActivity";

    private ImageButton btnJoin,ibtnAudio,ibtnVideo;

    private RelativeLayout rlRemoteGroup;

    private RtcEngine rtcEngine;

    private CameraPresenter cameraPresenter;

    private SurfaceView sv_local;

    private boolean isJoin = false;

    private int width = 640;

    private int height = 480;

    private int fps = 20;

    private int bitrate = 90000;



    private int timespan = 90000 / fps;

    private long time;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        btnJoin = findViewById(R.id.ibtn_join);
        sv_local = findViewById(R.id.sv_local);
        ibtnAudio=findViewById(R.id.ibtn_audio);
        ibtnVideo=findViewById(R.id.ibtn_video);
        rlRemoteGroup = findViewById(R.id.rl_remote);
        initSDK();


    }


    private void initSDK(){
        try {
            cameraPresenter = new CameraPresenter(VideoActivity.this,sv_local);
            cameraPresenter.setFrontOrBack(Camera.CameraInfo.CAMERA_FACING_FRONT);
            cameraPresenter.setCameraCallBack(VideoActivity.this::onPreviewFrame);
            rtcEngine = RtcEngine.create(this, BuildConfig.APPID,engineEventHandler);
            rtcEngine.enableVideo();
            rtcEngine.setExternalVideoSource(true,true,true);//设置外部视频源
        } catch (Exception e) {
            e.printStackTrace();
        }
       }

    private void joinChannel(){
        rtcEngine.joinChannel("","9999","","");
    }

    private void leaveChannel(){
        rtcEngine.leaveChannel();
        finish();
    }




    IRtcEngineEventHandler engineEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, String uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setTitle("加入成功");
                }
            });
        }

        @Override
        public void onLocalVideoStats(LocalVideoStats stats) {
            super.onLocalVideoStats(stats);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        @Override
        public void onUserJoined(String uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
        }

        @Override
        public void onUserOffline(String uid, int reason) {
            super.onUserOffline(uid, reason);
            VideoActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    rlRemoteGroup.removeAllViews();
                    rtcEngine.setRemoteVideoRenderer(uid,null);//释放自定义渲染器
                }
            });
        }

        @Override
        public void onFirstRemoteVideoDecoded(String uid, int width, int height, int elapsed) {
            super.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
            VideoActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextureView arTextureView =RtcEngine.CreateRendererView(VideoActivity.this);
                    rlRemoteGroup.removeAllViews();
                    rlRemoteGroup.addView(arTextureView);
                    rtcEngine.setupRemoteVideo(new VideoCanvas(arTextureView,1,uid));
                }
            });
        }
    };



    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isJoin){
            ARVideoFrame arVideoFrame = new ARVideoFrame();
            arVideoFrame.format=ARVideoFrame.FORMAT_NV12;
            arVideoFrame.timeStamp=System.currentTimeMillis();
            arVideoFrame.stride=640;
            arVideoFrame.height=480;
            arVideoFrame.rotation=270;
            arVideoFrame.buf=data;
            arVideoFrame.bufType=ARVideoFrame.BUFFER_TYPE_ARRAY;
            rtcEngine.pushExternalVideoFrame(arVideoFrame);
        }
    }


    public void join(View view) {
        if (!isJoin){
            joinChannel();
            btnJoin.setImageResource(R.drawable.leave);
        }else {
            setTitle("未加入");
            btnJoin.setImageResource(R.drawable.join);
            leaveChannel();
        }
        isJoin=!isJoin;
    }

    public void MuteLocalAudio(View view) {
        ibtnAudio.setSelected(!ibtnAudio.isSelected());
        rtcEngine.muteLocalAudioStream(ibtnAudio.isSelected());

    }

    public void MuteLocalVideo(View view) {
        ibtnVideo.setSelected(!ibtnVideo.isSelected());
        rtcEngine.enableLocalVideo(!ibtnVideo.isSelected());//不会停止采集enableLocalVideo才会
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        RtcEngine.destroy();
    }
}
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


import org.ar.rtc.Constants;
import org.ar.rtc.IRtcEngineEventHandler;
import org.ar.rtc.RtcEngine;
import org.ar.rtc.mediaio.ARSurfaceView;
import org.ar.rtc.mediaio.ARTextureView;
import org.ar.rtc.mediaio.IVideoFrameConsumer;
import org.ar.rtc.mediaio.IVideoSource;
import org.ar.rtc.mediaio.MediaIO;
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

    private IVideoFrameConsumer mConsumer;


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
            rtcEngine = RtcEngine.create(this, BuildConfig.APPID,engineEventHandler);
            rtcEngine.setExternalVideoSource(true,true,true);//设置外部视频源
            rtcEngine.setVideoSource(new VideoSource());//设置外部视频源
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
                    ARTextureView arTextureView =new ARTextureView(VideoActivity.this);
                    arTextureView.init(null);
                    arTextureView.setBufferType(MediaIO.BufferType.BYTE_ARRAY);
                    arTextureView.setPixelFormat(MediaIO.PixelFormat.I420);
                    arTextureView.setMirror(true);
                    rlRemoteGroup.removeAllViews();
                    rlRemoteGroup.addView(arTextureView);
                    rtcEngine.setRemoteVideoRenderer(uid,arTextureView);
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
        if (mConsumer!=null&&isJoin){//将NV21数据塞进SDK
           // byte[] data, int format, int width, int height, int rotation, long timestamp
            mConsumer.consumeByteArrayFrame(data, ARVideoFrame.FORMAT_NV21,cameraPresenter.getPreviewSize().width,cameraPresenter.getPreviewSize().height,270,System.currentTimeMillis());
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


    public class VideoSource implements IVideoSource {

        //初始化成功后 会返回一个IVideoFrameConsumer对象 通过这个将视频数据塞进SDK
        //可以在这个回调中做一些准备工作，例如打开摄像头
        @Override
        public boolean onInitialize(IVideoFrameConsumer consumer) {
            Log.d("VideoSource","onInitialize");
            //开启相机
            mConsumer = consumer;
            cameraPresenter = new CameraPresenter(VideoActivity.this,sv_local);
            cameraPresenter.setFrontOrBack(Camera.CameraInfo.CAMERA_FACING_FRONT);
            cameraPresenter.setCameraCallBack(VideoActivity.this::onPreviewFrame);
            return true;
        }


        @Override
        public boolean onStart() {
            Log.d("VideoSource","onStart");
            return true;
        }

        @Override
        public void onStop() {
            Log.d("VideoSource","onStop");
        }

        @Override
        public void onDispose() {//关闭相机
            Log.d("VideoSource","onDispose");
            cameraPresenter.releaseCamera();
            mConsumer=null;
        }

        @Override
        public int getBufferType() {
            return MediaIO.BufferType.BYTE_ARRAY.intValue();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RtcEngine.destroy();
    }
}
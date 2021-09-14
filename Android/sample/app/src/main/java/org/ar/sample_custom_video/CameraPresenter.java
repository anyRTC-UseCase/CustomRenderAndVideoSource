package org.ar.sample_custom_video;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class CameraPresenter implements Camera.PreviewCallback {


    //相机对象
    private Camera mCamera;
    //相机对象参数设置
    private Camera.Parameters mParameters;
    //自定义照相机页面
    private AppCompatActivity mAppCompatActivity;
    //surfaceView 用于预览对象
    private SurfaceView mSurfaceView;
    //SurfaceHolder对象
    private SurfaceHolder mSurfaceHolder;

    //摄像头Id 默认后置 0,前置的值是1
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    //预览旋转的角度
    private int orientation;

    //自定义回调
    private CameraCallBack mCameraCallBack;
    //手机宽和高
    private int screenWidth, screenHeight;

    private  Camera.Size previewSize;

    public Camera.Size getPreviewSize() {
        return previewSize;
    }

    private boolean isFull =false;

    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean full) {
        isFull = full;
    }



    //自定义回调
    public interface CameraCallBack {
        //预览帧回调
        void onPreviewFrame(byte[] data, Camera camera);
    }

    public void setCameraCallBack(CameraCallBack mCameraCallBack) {
        this.mCameraCallBack = mCameraCallBack;

    }

    public CameraPresenter(AppCompatActivity mAppCompatActivity, SurfaceView mSurfaceView) {
        this.mAppCompatActivity = mAppCompatActivity;
        this.mSurfaceView = mSurfaceView;
        //  mSurfaceView.getHolder().setKeepScreenOn(true);
        mSurfaceHolder = mSurfaceView.getHolder();
        DisplayMetrics dm = new DisplayMetrics();
        mAppCompatActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        //获取宽高像素
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        Log.d("sssd-手机宽高尺寸:",screenWidth +"*"+screenHeight);
        init();
    }




    /**
     * 设置前置还是后置
     *
     * @param mCameraId 前置还是后置
     */
    public void setFrontOrBack(int mCameraId) {
        this.mCameraId = mCameraId;

    }


    /**
     * 初始化增加回调
     */
    public void init() {
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d("sssd-宽",mSurfaceView.getMeasuredWidth() + "*" +mSurfaceView.getMeasuredHeight());
                //surface创建时执行
                if (mCamera == null) {
                    openCamera(mCameraId);
                }
                //并设置预览
                startPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                //surface绘制时执行
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //surface销毁时执行
                releaseCamera();
            }
        });
    }

    /**
     * 打开相机 并且判断是否支持该摄像头
     *
     * @param FaceOrBack 前置还是后置
     * @return
     */
    private boolean openCamera(int FaceOrBack) {
        //是否支持前后摄像头
        boolean isSupportCamera = isSupport(FaceOrBack);
        //如果支持
        if (isSupportCamera) {
            try {
                mCamera = Camera.open(FaceOrBack);
                initParameters(mCamera);
                //设置预览回调
                if (mCamera != null) {
                    mCamera.setPreviewCallback(this);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }

        return isSupportCamera;
    }


    /**
     * 设置相机参数
     *
     * @param camera
     */
    private void initParameters(Camera camera) {
        try {
            //获取Parameters对象
            mParameters = camera.getParameters();
            //设置预览格式
            mParameters.setPreviewFormat(ImageFormat.NV21);

            if(isFull){
                setPreviewSize(screenWidth,screenHeight);
            } else {
                setPreviewSize(mSurfaceView.getMeasuredWidth(),mSurfaceView.getMeasuredHeight());
            }

            //给相机设置参数
            mCamera.setParameters(mParameters);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("camera","初始化相机失败");
        }


    }




    /**
     * 设置预览界面尺寸
     */
    public void setPreviewSize(int width,int height) {
        //获取系统支持预览大小
        List<Camera.Size> localSizes = mParameters.getSupportedPreviewSizes();
        Camera.Size biggestSize = null;//最大分辨率
        Camera.Size fitSize = null;// 优先选屏幕分辨率
        Camera.Size targetSize = null;// 没有屏幕分辨率就取跟屏幕分辨率相近(大)的size
        Camera.Size targetSiz2 = null;// 没有屏幕分辨率就取跟屏幕分辨率相近(小)的size
        if (localSizes != null) {
            int cameraSizeLength = localSizes.size();

            if(Float.valueOf(width) / height == 3.0f / 4){
                for (int n = 0; n < cameraSizeLength; n++) {
                    Camera.Size size = localSizes.get(n);
                    //  Log.d("sssd-系统支持的尺寸size.width:",size.width + "*" +size.height);
                    //  Log.d("sssd-系统",1440f / 1080+"");
                    //  Log.d("sssd-系统支持的尺寸比:",Double.valueOf(size.width) / size.height+"");
                    if(Float.valueOf(size.width) / size.height == 4.0f / 3){
                        Log.d("sssd-系统支持的尺寸:","进入");
                        mParameters.setPreviewSize(size.width,size.height);
                        previewSize=size;
                        break;
                    }


                }
            } else {
                for (int n = 0; n < cameraSizeLength; n++) {
                    Camera.Size size = localSizes.get(n);
                    Log.d("sssd-系统支持的尺寸:",size.width + "*" +size.height);
                    if (biggestSize == null ||
                            (size.width >= biggestSize.width && size.height >= biggestSize.height)) {
                        biggestSize = size;
                    }

                    //如果支持的比例都等于所获取到的宽高
                    if (size.width == height
                            && size.height == width) {
                        fitSize = size;
                        //如果任一宽或者高等于所支持的尺寸
                    } else if (size.width == height
                            || size.height == width) {
                        if (targetSize == null) {
                            targetSize = size;
                            //如果上面条件都不成立 如果任一宽高小于所支持的尺寸
                        } else if (size.width < height
                                || size.height < width) {
                            targetSiz2 = size;
                        }
                    }
                }

                if (fitSize == null) {
                    fitSize = targetSize;
                }

                if (fitSize == null) {
                    fitSize = targetSiz2;
                }

                if (fitSize == null) {
                    fitSize = biggestSize;
                }
                Log.d("sssd-最佳预览尺寸:",fitSize.width + "*" + fitSize.height);

                //mParameters.setPreviewSize(640,480);
                mParameters.setPreviewSize(fitSize.width, fitSize.height);
                previewSize=fitSize;
            }

        }
    }




    /**
     * 解决预览变形问题
     *
     *
     */
    private void getOpyimalPreviewSize(){
        List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();
        int w = screenWidth;
        int h = screenHeight;
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;
        for(Camera.Size size : sizes){
            double ratio = (double) size.width / size.height;
            if(Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;;
            if(Math.abs(size.height - targetHeight) < minDiff){
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }

        }


        if(optimalSize == null){
            minDiff = Double.MAX_VALUE;
            for(Camera.Size size : sizes){
                if(Math.abs(size.height - targetHeight) < minDiff){
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }

        }

        mParameters.setPreviewSize(optimalSize.width,optimalSize.height);
    }









    /**
     * 判断是否支持某个相机
     *
     * @param faceOrBack 前置还是后置
     * @return
     */
    private boolean isSupport(int faceOrBack) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            //返回相机信息
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == faceOrBack) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mCameraCallBack != null) {
            mCameraCallBack.onPreviewFrame(data, camera);
        }
    }

    /**
     * 开始预览
     */
    private void startPreview() {
        try {
            //根据所传入的SurfaceHolder对象来设置实时预览
            mCamera.setPreviewDisplay(mSurfaceHolder);
            //调整预览角度
            setCameraDisplayOrientation(mAppCompatActivity,mCameraId,mCamera);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     * 保证预览方向正确
     *
     * @param appCompatActivity Activity
     * @param cameraId          相机Id
     * @param camera            相机
     */
    private void setCameraDisplayOrientation(AppCompatActivity appCompatActivity, int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        //rotation是预览Window的旋转方向，对于手机而言，当在清单文件设置Activity的screenOrientation="portait"时，
        //rotation=0，这时候没有旋转，当screenOrientation="landScape"时，rotation=1。
        int rotation = appCompatActivity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        //计算图像所要旋转的角度
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        orientation = result;
        //调整预览图像旋转角度
        camera.setDisplayOrientation(result);

    }

    /**
     * 前后摄像切换
     */
    public void switchCamera() {
        //先释放资源
        releaseCamera();
        //在Android P之前 Android设备仍然最多只有前后两个摄像头，在Android p后支持多个摄像头 用户想打开哪个就打开哪个
        mCameraId = (mCameraId + 1) % Camera.getNumberOfCameras();
        //打开摄像头
        openCamera(mCameraId);
        //切换摄像头之后开启预览
        startPreview();
    }


//    这里可以找准前后摄像头的id
//    int frontIndex = -1;
//    int backIndex = -1;
//    int cameraCount = Camera.getNumberOfCameras();
//    Camera.CameraInfo info = new Camera.CameraInfo();
//        for(int cameraIndex = 0;cameraIndex < cameraCount;cameraIndex ++){
//        Camera.getCameraInfo(cameraIndex,info);
//        if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
//            frontIndex = cameraIndex;
//        }else if(info.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
//            backIndex = cameraIndex;
//        }
//
//    }
//
//    //跟据传入的type来判断
//        if(type == FRONT && frontIndex != -1){
//
//        openCamera(frontIndex);
//    } else if(type == BACK && backIndex != -1){
//        openCamera(backIndex);
//
//    }


    /**
     * 释放相机资源
     */
    public void releaseCamera() {
        if (mCamera != null) {
            //停止预览
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }

    }










}

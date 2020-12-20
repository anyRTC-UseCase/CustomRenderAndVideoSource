

## 说明

1. 如果你还没有注册开发者账号，请先注册[开发者账号](https://console.anyrtc.io/signin)
2. 注册开发者账号后，请在后台创建一个 **AppID**
3. 将 AppId 填入本项目中的App/build.gradle文件中即可
4. 本项目最低支持 Android 4.4（API 19）CPU架构支持armeabi-v7a，arm64-v8a
5. 请使用真机运行该项目



## 自定义视频采集

**本示例演示了mediaIO 方式的视频采集**

通过 MediaIO 提供 `IVideoSource` 接口和 `IVideoFrameConsumer` 类，你可以通过该类设置采集的视频数据格式，并控制视频的采集过程。

参考如下步骤，在你的项目中使用 MediaIO 方式实现自定义视频源功能：

1. 实现 `IVideoSource` 接口。通过 `IVideoSource` 接口下的各回调设置视频数据格式，并控制采集过程：

   - 收到 `getBufferType` 回调后，在该回调的返回值中指定想要采集的视频数据格式。

   - 收到 `onInitialize` 回调后，保存该回调中的 `IVideoFrameConsumer` 对象。通过 `IVideoFrameConsumer` 对象发送和接收自定义的视频数据。

   - 收到 `onStart` 回调后，通过 `IVideoFrameConsumer` 对象中的 `consumeByteBufferFrame`，`consumeByteArrayFrame`，或 `consumeTextureFrame` 方法向 SDK 发送视频帧。

     为满足实际使用需求，你可以在将视频帧发送回 SDK 前，修改 `IVideoFrameConsumer` 中视频帧参数，如 `rotation`。

   - 收到 `onStop` 回调后，停止使用 `IVideoFrameConsumer` 对象向 SDK 发送视频帧。

   - 收到 `onDispose` 回调后，释放 `IVideoFrameConsumer` 对象。

2. 继承实现的 `IVideoSource` 类，构建一个自定义的视频源对象。

3. 调用 `setVideoSource` 方法，将自定义的视频源对象设置给 `RtcEngine`。

4. 根据场景需要，调用 `startPreview`、`joinChannel` 等方法预览或发送自定义采集的视频数据。



## 自定义视频渲染

你可以通过  `IVideoSink` 接口实现自定义渲染功能。

参考如下步骤，在你的项目中使用 MediaIO 方式实现自定义渲染模块功能：

1. 实现 `IVideoSink` 接口。 通过 `IVideoSink` 接口下的各回调设置视频数据格式，并控制渲染过程：
   - 收到 `getBufferType` 和 `getPixelFormat` 回调后，在对应回调的返回值中设置你想要渲染的数据类型。
   - 根据收到的 `onInitialize`、`onStart`、`onStop`、`onDispose`、`getEglContextHandle` 回调，控制视频数据的渲染过程。
   - 实现一个对应渲染数据类型的 `IVideoFrameConsumer` 类，以获取视频数据。
2. 继承实现的 `IVideoSink` 类，构建一个自定义的渲染模块。
3. 调用 `setLocalVideoRenderer` 或 `setRemoteVideoRenderer`，用于渲染本地用户或远端用户的视频。
4. 根据场景需要，调用 `startPreview`、`joinChannel` 等方法预览或发送自定义渲染的视频数据。



本示例演示了使用anyRTC提供的自定义组件渲染



#### 方法一：使用  SDK 提供的自定义组件

本地用户加入频道后，导入并实现 `ARTextureView` 类并设置远端视频渲染。SDK 提供的 `ARTextureView` 类继承了 `textureView` 同时实现了 `IVideoSink` 类，而且内嵌 `BaseVideoRenderer` 对象作为渲染模块。因此你无需自行实现 `IVideoSink` 类和自定义渲染模块。`BaseVideoRenderer` 对象使用 OpenGL 渲染，也创建了 EGLContext，可以共享 EGLContext 的 Handle 给 Media Engine。



```
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
```



#### 方法二：使用 IVideoSink 接口

你可以自行实现 `IVideoSink` 接口，并继承实现的类，构建一个自定义的渲染模块。



```
// 先创建一个实现 IVideoSink 接口的实例
IVideoSink sink = new IVideoSink() {
    @Override
    // 初始化渲染器。你可以在该方法中对渲染器进行初始化，也可以提前初始化好。将返回值设为 true，表示已完成初始化
    public boolean onInitialize () {
        return true;
    }

    @Override
    // 启动渲染器
    public boolean onStart() {
        return true;
    }

    @Override
    // 停止渲染器
    public void onStop() {

    }

    @Override
    // 释放渲染器
    public void onDispose() {

    }

    @Override
    public long getEGLContextHandle() {
        // 构造你的 EGL context
        // 返回 0 代表渲染器中并没有创建 EGL context
        return 0;
    }

    // 返回当前渲染器需要的数据 Buffer 类型
    // 若切换 VideoSink 的类型，必须重新创建另一个实例
    // 有三种类型：BYTE_BUFFER(1)；BYTE_ARRAY(2)；TEXTURE(3)
    @Override
    public int getBufferType() {
        return BufferType.BYTE_ARRAY;
    }

    // 返回当前渲染器需要的 Pixel 格式
    @Override
    public int getPixelFormat() {
        return PixelFormat.NV21;
    }

   // SDK 调用该方法将获取到的视频帧传给渲染器
   // 根据获取到的视频帧的格式，选择相应的回调
   @Override
   public void consumeByteArrayFrame(byte[] data, int format, int width, int height, int rotation, long timestamp) {

   // 渲染器在此渲染
   }
   public void consumeByteBufferFrame(ByteBuffer buffer, int format, int width, int height, int rotation, long timestamp) {


   // 渲染器在此渲染
   }
   public void consumeTextureFrame(int textureId, int format, int width, int height, int rotation, long timestamp, float[] matrix) {

   // 渲染器在此渲染
   }

}

rtcEngine.setLocalVideoRenderer(sink);
```



## 联系我们

- 如需阅读完整的文档和 API 注释，你可以访问[anyRTC开发者中心](https://docs.anyrtc.io/)。
- 如果在集成中遇到问题，你可以到[anyRTC开发者社区](https://bbs.anyrtc.io)提问。
- 如果有售前咨询或售后技术问题，你可以拨打 021-65650071，或加入官方Q群 580477436 提问。
- 如果发现了示例代码的 bug，欢迎提交 [issue](https://github.com/anyRTC-UseCase/ARCall/issues)
- 项目交流微信群,请扫描下方二维码进群
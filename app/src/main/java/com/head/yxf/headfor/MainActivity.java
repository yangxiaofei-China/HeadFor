package com.head.yxf.headfor;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String HEAD_NAME = "/head.jpg";
    private CircleImageView headCiv;
    /**
     * 头像获取方式选择PopupWindow
     */
    private PopupWindow mPopWindow;
    /**
     * 头像
     */
    private Bitmap headBit;
    private final static int HEAD_CAMERA_RESULT = 1;
    private final static int HEAD_MEDIA_RESULT = 2;
    private final static int HEAD_CROP_RESULT = 3;

    private final static int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 6;
    private final static int PERMISSIONS_REQUEST_CAMERA = 7;
    private String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,};
    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权
    private List<String> mPermissionList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        headCiv = (CircleImageView) findViewById(R.id.head_civ);
        headCiv.setOnClickListener(this);

    }
    /**
     * 添加点击头像弹出popupWindow视图
     */
    private void showPopupWindow() {
        //设置contentView
        View contentView = LayoutInflater.from(this).inflate(R.layout.popupwindow_setting, null);
        mPopWindow = new PopupWindow(contentView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);
        //设置各个空间的点击事件
        Button photographBtn = (Button) contentView.findViewById(R.id.setting_popuwindow_photograph_btn);
        Button selectBtn = (Button) contentView.findViewById(R.id.setting_popuwindow_mobile_album_select_btn);
        Button cancelBtn = (Button) contentView.findViewById(R.id.setting_popuwindow_cancel_btn);
        photographBtn.setOnClickListener(this);
        selectBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        //显示PopupWindow
        View mainView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        mPopWindow.setAnimationStyle(R.style.popupWindowAnim);
        mPopWindow.showAtLocation(mainView, Gravity.BOTTOM, 0, 0);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == HEAD_CAMERA_RESULT && resultCode == Activity.RESULT_OK
                && null != data) {
            String sdState = Environment.getExternalStorageState();
            if (!sdState.equals(Environment.MEDIA_MOUNTED)) {
                return;
            }
            Uri uri = getUri(data);
            startCrop(uri, 200, 200);
        }
        if (requestCode == HEAD_MEDIA_RESULT && resultCode == Activity.RESULT_OK
                && null != data) {
            Uri uri = getUri(data);
            startCrop(uri, 200, 200);
        }
        if (requestCode == HEAD_CROP_RESULT && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                setImageAndSave(data);
            }
        }
    }
    /**
     * 根据onActivityResult返回的data获取uri
     *
     * @param data
     * @return
     */
    private Uri getUri(Intent data) {
        Uri uri = data.getData();
        if (uri == null) {
            Bundle extras = data.getExtras();
            uri = Uri.parse(MediaStore.Images.Media.insertImage(this.getContentResolver(), (Bitmap) extras.getParcelable("data"), null, null));
        }
        return uri;
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.head_civ:
                showPopupWindow();
                break;
            case R.id.setting_popuwindow_photograph_btn:
                /**在android6.0以后要动态添加权限*/
                mPermissionList.clear();
                for (int i = 0; i < permissions.length; i++) {
                    if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                        mPermissionList.add(permissions[i]);
                    }
                }
                if (mPermissionList.isEmpty()) {//未授予的权限为空，表示都授予了
                    startCamera();
                } else {//请求权限方法
                    String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
                    ActivityCompat.requestPermissions(this,permissions, PERMISSIONS_REQUEST_CAMERA);
                }
                break;
            case R.id.setting_popuwindow_mobile_album_select_btn:
                /**在android6.0以后要动态添加权限*/
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
//                  在Fragment中要使用下边这种方法
//                  requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                } else {
                    startMedia();
                }
                break;
            case R.id.setting_popuwindow_cancel_btn:
                mPopWindow.dismiss();
                break;
        }
    }
    /**
     * 打开媒体文件
     */
    private void startMedia() {
        Intent picture = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(picture, HEAD_MEDIA_RESULT);
        mPopWindow.dismiss();
    }

    /**
     * 打开相机
     */
    private void startCamera() {
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camera, HEAD_CAMERA_RESULT);
        mPopWindow.dismiss();
    }
    /**
     * 开始裁剪
     * 附加选项	   数据类型	    描述
     * crop	        String	    发送裁剪信号
     * aspectX	    int	        X方向上的比例
     * aspectY	    int	        Y方向上的比例
     * outputX	    int	        裁剪区的宽
     * outputY	    int	        裁剪区的高
     * scale	    boolean	    是否保留比例
     * return-data	boolean	    是否将数据保留在Bitmap中返回
     * data	        Parcelable	相应的Bitmap数据
     * circleCrop	String	    圆形裁剪区域？
     * MediaStore.EXTRA_OUTPUT ("output")	URI	将URI指向相应的file:///...
     *
     * @param uri uri
     */
    private void startCrop(Uri uri, int outputX, int outputY) {

        Intent intent = new Intent("com.android.camera.action.CROP"); //调用Android系统自带的一个图片剪裁页面
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");//进行修剪
        // aspectX aspectY 是宽高的比例
        if (outputX == outputY) {
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
        } else {
            intent.putExtra("scale", true);
        }
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", outputX);
        intent.putExtra("outputY", outputY);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, HEAD_CROP_RESULT);
    }

    /**
     * 设置显示图片
     *
     * @param data
     */
    public void setImageAndSave(Intent data) {
        Bundle extras = data.getExtras();
        if (extras == null || !extras.containsKey("data")) {
            Uri uri = data.getData();
            if (uri != null) {
                headBit = BitmapFactory.decodeFile(uri.getPath());
                headCiv.setImageBitmap(headBit);
                saveBitmapFile(headBit, HEAD_NAME);
                return;
            }
        }
        headBit = extras.getParcelable("data");
        headCiv.setImageBitmap(headBit);
        saveBitmapFile(headBit, HEAD_NAME);
    }

    /**
     * 保存bitmap到path路经下
     *
     * @param bitmap
     * @param path
     */
    public void saveBitmapFile(Bitmap bitmap, String path) {
        File file = new File(getExternalCacheDir(this), path);//将要保存图片的路径
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (headBit != null && !headBit.isRecycled()) {
            // 回收并且置为null
            headBit.recycle();
            headBit = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera();
                }
                break;
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startMedia();
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    /**
     * 获取sdCard缓存路径 /Android/data/$packageName$/cache
     *
     * @param context
     * @return
     */
    public static File getExternalCacheDir( Context context ) {
        File path = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            path = context.getExternalCacheDir();
        }
        if (null == path) {
            final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
            path = new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
        }
        return path;
    }
}

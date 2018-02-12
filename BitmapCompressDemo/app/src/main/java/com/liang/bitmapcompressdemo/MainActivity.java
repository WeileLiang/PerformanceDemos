package com.liang.bitmapcompressdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    String path = Environment
            .getExternalStorageDirectory().getAbsolutePath()
            + "/aphotos/thunder.jpg";

    private String[] premits = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE = 1000;

    private TextView tv_compress;
    private TextView tv_origin;
    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, premits, REQUEST_CODE);

        iv = findViewById(R.id.iv);
        tv_origin = findViewById(R.id.tv_origin);
        tv_compress = findViewById(R.id.tv_compress);

        findViewById(R.id.btn_quality).setOnClickListener(this);
        findViewById(R.id.btn_scale).setOnClickListener(this);
        findViewById(R.id.btn_sample).setOnClickListener(this);
        findViewById(R.id.btn_rgb565).setOnClickListener(this);
    }

    /**
     * 质量压缩
     *
     * @param bmp        要压缩的图片
     * @param targetSize 最终要压缩的大小（KB）
     * @return
     */
    private Bitmap qualityCompress(Bitmap bmp, int targetSize) {

        //压缩后的图片输出到baos中
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        // 压缩程度，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > targetSize) {
            // 循环判断如果压缩后图片是否大于target,大于继续压缩
            baos.reset();// 重置baos即清空baos
            bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
            // 逐渐减少压缩率，把压缩后的数据存放到baos中
            options -= 10;// 每次都减少10
            if (options <= 0) {
                break;
            }
        }
//        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        byte[] bytes = baos.toByteArray();
        // 得到最终质量压缩后的Bitmap
        Bitmap targetBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        //输出相关信息
        StringBuilder sb = new StringBuilder();
        sb.append("压缩后图片以bitmap形式存在的内存大小：").append(targetBmp.getByteCount() / 1024)
                .append("KB\n")
                .append("像素为：").append(targetBmp.getWidth() + "*" + targetBmp.getHeight())
                .append("\n在SD卡中的文件大小为：").append(bytes.length / 1024).append("KB");


        tv_compress.setText(sb);
//        iv.setImageBitmap(targetBmp);

        return targetBmp;
    }

    /**
     * 尺寸压缩
     *
     * @param bmp
     * @param targetW 目标图片的宽（像素）
     * @param targetH 目标图片的高（像素）
     * @return .
     */
    private Bitmap scaleCompress(Bitmap bmp, int targetW, int targetH) {
        Bitmap targetBmp = Bitmap.createScaledBitmap(bmp, targetW, targetH, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        targetBmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        //输出相关信息
        StringBuilder sb = new StringBuilder();
        sb.append("压缩后图片以bitmap形式存在的内存大小：").append(targetBmp.getByteCount() / 1024)
                .append("KB\n")
                .append("分辨率为：").append(targetBmp.getWidth() + "*" + targetBmp.getHeight())
                .append("\n在SD卡中的文件大小为：").append(baos.toByteArray().length / 1024).append("KB");

        tv_compress.setText(sb);
        iv.setImageBitmap(targetBmp);

        return targetBmp;
    }

    /**
     * @param filePath 图片路径
     * @param targetW 压缩的目标宽度（像素）
     * @param targetH 压缩的目标高度（像素）
     * @return
     */
    private Bitmap sampleCompress(String filePath, int targetW, int targetH) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        //设置为true的时候不会真正加载图片到内存中，但是仍可以获得图片的分辨率
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, opts);

        //设置压缩比例，只能是正数，如果是2表示长宽压缩至原来的1/2，4就是1/4
        opts.inSampleSize = inSampleSizeCalculate(opts, targetW, targetH);
        //这下子是真的加载到内存了，但是加载进来的是已经压缩了的图片了，所以不会OOM
        opts.inJustDecodeBounds = false;
        Bitmap targetBmp = BitmapFactory.decodeFile(filePath, opts);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        targetBmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        StringBuilder sb = new StringBuilder();
        sb.append("图片大小：")
                .append(targetBmp.getByteCount() / 1024 + "KB")
                .append("\n压缩后图片的分辨率为：")
                .append(targetBmp.getWidth() + "*" + targetBmp.getHeight())
                .append("\n图片在SD卡中的大小为：").append(baos.toByteArray().length / 1024).append("KB");

        tv_compress.setText(sb);
        iv.setImageBitmap(targetBmp);

        return targetBmp;
    }

    private int inSampleSizeCalculate(BitmapFactory.Options opts, double targetW, double targetH) {
        //options就是在inJustDecodeBounds设置为false时，计算的图片长宽
        int w = opts.outWidth;
        int h = opts.outHeight;

        //默认不压缩
        int inSampleSize = 1;
        //原始长宽任一大于目的长宽就需要计算压缩率
        if (w > targetW || h > targetH) {
            int ratioW = (int) Math.ceil(w / targetW);
            int ratioH = (int) Math.ceil(h / targetH);

            //取二者中大的
            inSampleSize = Math.max(ratioH, ratioW);
        }

        return inSampleSize;
    }

    /**
     * 改变图片为RGB565
     * @param filePath
     * @return
     */
    private Bitmap change2Rgb565(String filePath){
        BitmapFactory.Options opts=new BitmapFactory.Options();
        opts.inPreferredConfig= Bitmap.Config.RGB_565;

        Bitmap targetBmp=BitmapFactory.decodeFile(filePath,opts);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        targetBmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        StringBuilder sb = new StringBuilder();
        sb.append("图片大小：")
                .append(targetBmp.getByteCount() / 1024 + "KB")
                .append("\n压缩后图片的分辨率为：")
                .append(targetBmp.getWidth() + "*" + targetBmp.getHeight())
                .append("\n图片在SD卡中的大小为：").append(baos.toByteArray().length / 1024).append("KB");

        tv_compress.setText(sb);
//        iv.setImageBitmap(targetBmp);

        return targetBmp;
    }


    @Override
    public void onClick(View view) {
        Bitmap bmp = BitmapFactory.decodeFile(path);
        File file = new File(path);
        StringBuilder sb = new StringBuilder();
        sb.append("压缩前图片以bitmap形式存在的内存大小：").append(bmp.getByteCount() / 1024)
                .append("KB\n")
                .append("像素为：").append(bmp.getWidth() + "*" + bmp.getHeight())
                .append("\n在SD卡中的文件大小为：").append(file.length() / 1024).append("KB\n");
        tv_origin.setText(sb);

        switch (view.getId()) {
            case R.id.btn_quality:
                qualityCompress(bmp, 32);
                break;
            case R.id.btn_scale:
                scaleCompress(bmp, bmp.getWidth() / 2, bmp.getHeight() / 2);
                break;
            case R.id.btn_sample:
                sampleCompress(path, 800, 600);
                break;
            case R.id.btn_rgb565:
                change2Rgb565(path);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            boolean isAllGranted = true;
            for (int grantRes : grantResults) {
                if (grantRes != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }

            if (!isAllGranted)
                Toast.makeText(this, "请同意申请权限！", Toast.LENGTH_SHORT).show();
        }
    }

}

package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Responsible for starting compress and managing active and cached resources.
 */
class Engine {
    private ExifInterface srcExif;
    private String srcImg;
    private File tagImg;
    private int srcWidth;
    private int srcHeight;

    //  srcImg 原图路径
    //  tagImg 缓存图片路径
    Engine(String srcImg, File tagImg) throws IOException {
        if (Checker.isJPG(srcImg)) {
            this.srcExif = new ExifInterface(srcImg);
        }
        this.tagImg = tagImg;
        this.srcImg = srcImg;

        BitmapFactory.Options options = new BitmapFactory.Options();
        // inJustDecodeBounds只生成宽高 bitmap为空
        options.inJustDecodeBounds = true;
        // 默认采样率
        options.inSampleSize = 1;
        BitmapFactory.decodeFile(srcImg, options);
        //获取原图的长宽 以便计算采样率和缩小图片
        this.srcWidth = options.outWidth;
        this.srcHeight = options.outHeight;
    }

    // 采样率计算采样率
    private int computeSize() {
        // 强行变成偶数（不要问我为啥子）
        srcWidth = srcWidth % 2 == 1 ? srcWidth + 1 : srcWidth;
        srcHeight = srcHeight % 2 == 1 ? srcHeight + 1 : srcHeight;

        //得到长边，短边
        int longSide = Math.max(srcWidth, srcHeight);
        int shortSide = Math.min(srcWidth, srcHeight);

        //scale 比例
        float scale = ((float) shortSide / longSide);
        // 这部分看不懂 不知道具体的几个值有什么含义 返回的是采样率 TODO
        if (scale <= 1 && scale > 0.5625) {
            if (longSide < 1664) {
                return 1;
            } else if (longSide >= 1664 && longSide < 4990) {
                return 2;
            } else if (longSide > 4990 && longSide < 10240) {
                return 4;
            } else {
                return longSide / 1280 == 0 ? 1 : longSide / 1280;
            }
        } else if (scale <= 0.5625 && scale > 0.5) {
            return longSide / 1280 == 0 ? 1 : longSide / 1280;
        } else {
            return (int) Math.ceil(longSide / (1280.0 / scale));
        }
    }

    // 其实我是不太懂为什么要旋转图片的
    private Bitmap rotatingImage(Bitmap bitmap) {
        if (srcExif == null) return bitmap;

        Matrix matrix = new Matrix();
        int orientation = srcExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
        }


        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    File compress() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = computeSize();

        // 采样率压缩
        Bitmap tagBitmap = BitmapFactory.decodeFile(srcImg, options);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // 旋转图片
        tagBitmap = rotatingImage(tagBitmap);
        // 质量压缩
        tagBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
        tagBitmap.recycle();

        // 写到缓存的路径下
        FileOutputStream fos = new FileOutputStream(tagImg);
        fos.write(stream.toByteArray());
        fos.flush();
        fos.close();
        stream.close();

        return tagImg;
    }
}
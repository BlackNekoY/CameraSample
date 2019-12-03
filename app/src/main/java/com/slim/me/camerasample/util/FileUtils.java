package com.slim.me.camerasample.util;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileUtils {

    protected static final String TAG = "FileUtils";
    private static final int GLOBLE_BUFFER_SIZE = 512 * 1024;
    static final String ZIP_SUFIIX = ".zip";

    public enum ImageType {
        UNKNOWN,
        JPG,
        PNG,
        GIF
    }
    // gif file magic numbers.
    private final static byte[] GIF87A = new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
    private final static byte[] GIF89A = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61};

    // jpeg file magic numbers
    private final static byte[] JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    // png file magic numbers
    private final static byte[] PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    public static boolean hasSDCardMounted() {
        try {
            return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean renameFile(String oldPath, String newPath) {
        if (TextUtils.isEmpty(oldPath) || TextUtils.isEmpty(newPath)) {
            return false;
        }

        File oldFile = new File(oldPath);
        File newFile = new File(newPath);
        return oldFile.exists() && oldFile.renameTo(newFile);
    }

    /**
     *
     * 检测文件是否存在
     * @param filePath
     * @return
     */
    public static boolean isFileExist(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return file.isFile() && file.exists();
    }

    /**
     * 检测目录是否存在
     * @param filePath
     * @return
     */
    public static boolean isDirectoryExist(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return file.isDirectory() && file.exists();
    }

    /**
     * 检测目录是否存在
     * @param filePath
     * @return
     */
    public static boolean isDirectoryNotEmpty(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        if (file.isDirectory() && file.exists()) {
            String[] files = file.list();
            return files != null && files.length > 0;
        }

        return false;
    }


    public synchronized static void writeFile(String filePath, String content, boolean isAppend) {
        if (filePath == null) {
            return;
        }
        if (content == null) {
            return;
        }
        File file = new File(filePath);
        FileWriter fileWriter = null;
        try {
            if (!file.exists()) {
                makeSureFileExist(file);
            }
            fileWriter = new FileWriter(file, isAppend);
            fileWriter.write(content);
            fileWriter.flush();
        } catch (IOException e) {
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static void makesureParentExist(File file) {
        File parent = file.getParentFile();
        if ((parent != null) && (!parent.exists())) {
            mkdirs(parent);
        }
    }

    /**
     * 确保某文件或文件夹的父文件夹存在
     *
     * @param file
     */
    private static boolean makesureParentDirExist(File file) {

        final File parent = file.getParentFile();
        if (parent == null || parent.exists()) {
            return true;
        }
        return mkdirs(parent);
    }

    /**
     * 验证文件夹创建操作成功有否
     *
     * @param dir
     */
    public static boolean mkdirs(File dir) {
        if (dir == null) {
            return false;
        }
        return dir.mkdirs();
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        try {
            byte[] buffer = new byte[GLOBLE_BUFFER_SIZE];
            int temp = -1;
            input = makeInputBuffered(input);
            output = makeOutputBuffered(output);
            while ((temp = input.read(buffer)) != -1) {
                output.write(buffer, 0, temp);
                output.flush();
            }
        } catch (IOException e) {
            throw e;
        } finally {
            FileUtils.closeStream(input);
            FileUtils.closeStream(output);
        }
    }

    static void copyAssetToStorage(Context context, String src, String des) {
        if (TextUtils.isEmpty(src) || TextUtils.isEmpty(des) || FileUtils.isFileExist(des)) {
            return;
        }

        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = context.getAssets().open(src);
            fos = new FileOutputStream(new File(des));
            byte[] buffer = new byte[1024];
            int byteCount = 0;
            while ((byteCount = is.read(buffer)) != -1) {//循环从输入流读取 buffer字节
                fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.flush();//刷新缓冲区
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void unZipFile(File source, String destDir) throws IOException {
        if (isDirectoryNotEmpty(destDir)) {
            return;
        }
        File outFile = new File(destDir);
        if (!outFile.exists()) {
            outFile.mkdirs();
        }
        ZipFile zipFile = new ZipFile(source);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            final String name = entry.getName();
            File destination = new File(outFile, name);
            if (entry.isDirectory()) {
                destination.mkdirs();
            } else if (name != null && name.contains("../")) {
                // 防止zip压缩文件的漏洞
                continue;
            } else {
                destination.createNewFile();
                FileOutputStream outStream = new FileOutputStream(destination);
                try {
                    copy(zipFile.getInputStream(entry), outStream);
                } catch (Exception e) {
                }
                outStream.close();
            }
        }
        zipFile.close();
    }

    public static void deleteFiles(File file) {
        if ((file.exists()) && (file.isDirectory())) {
            File[] childrenFile = file.listFiles();
            if (childrenFile != null) {
                for (File f : childrenFile) {
                    if (f.isFile()) {
                        delete(f);
                    } else if (f.isDirectory()) {
                        deleteFiles(f);
                    }
                }
            }
            delete(file);
        } else if ((file.exists()) && (file.isFile())) {
            delete(file);
        }
    }

    public static void clearDirectory(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        File file = new File(path);
        if ((file.exists()) && (file.isDirectory())) {
            File[] childrenFile = file.listFiles();
            if (childrenFile != null) {
                for (File f : childrenFile) {
                    if (f.isFile()) {
                        delete(f);
                    } else if (f.isDirectory()) {
                        deleteFiles(f);
                    }
                }
            }
        } else if ((file.exists()) && (file.isFile())) {
            delete(file);
        }
    }

    private static long getFileSize(File file) {
        long length = 0;
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    if (f != null && f.exists() && f.canRead()) {
                        length += f.length();
                    }
                }
            } else if (file.isFile() && file.canRead()) {
                length = file.length();
            }
        }
        return length;
    }

    static long getFileSize(String filepath) {
        return getFileSize(new File(filepath));
    }

    public static boolean doesExisted(String filepath) {
        if (TextUtils.isEmpty(filepath)) {
            return false;
        }
        return doesExisted(new File(filepath));
    }

    public static void delete(File f) {
        if ((f != null) && (f.exists())) {
            f.delete();
        }
    }

    public static void delete(String path) {
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
    }

    public static boolean createNewFile(File file) {
        makesureParentExist(file);
        if (file.exists()) {
            delete(file);
        }
        try {
            return file.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "catch", e);
        }

        return false;
    }

    /**
     * 确保指定文件或者文件夹存在
     *
     * @param file
     * @return
     */
    private static boolean makeSureFileExist(File file) {

        if (makesureParentDirExist(file)) {
            if (file.isFile()) {
                try {
                    return file.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "catch", e);
                }
            } else if (file.isDirectory()) {
                return file.mkdir();
            }
        }

        return false;
    }

    /**
     * 确保指定文件或者文件夹存在
     *
     * @param filePath
     * @return
     */
    public static void makeSureFileExist(String filePath) {
        makeSureFileExist(new File(filePath));
    }

    public static boolean closeStream(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
                return true;
            }
        } catch (IOException e) {
        }

        return false;
    }

    private static InputStream makeInputBuffered(InputStream input) {

        if ((input instanceof BufferedInputStream)) {
            return input;
        }

        return new BufferedInputStream(input, GLOBLE_BUFFER_SIZE);
    }

    private static OutputStream makeOutputBuffered(OutputStream output) {
        if ((output instanceof BufferedOutputStream)) {
            return output;
        }

        return new BufferedOutputStream(output, GLOBLE_BUFFER_SIZE);
    }

    private static boolean doesExisted(File file) {
        return (file != null) && (file.exists());
    }
    /**
     * 2. 存储路径(持久化),视频及图片存储，目录： mnt/sdcard/0/火山星球
     */
    public static File getExternalAppDir() {
        if (!isSdcardAvailable() || !isSdcardWritable()) {
            return null;
        }
        File appDir = new File(Environment.getExternalStorageDirectory().getPath()
                + "/livestream");
        ensureDirExists(appDir);

        return appDir;
    }

    public static boolean isSdcardAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static boolean isSdcardWritable() {
        try {
            return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        } catch (Exception e) {
        }
        return false;
    }
    public static void ensureDirExists(File dir) {
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
    }

    public static String copyAudio(String oldPath,String destPath) {
        if (TextUtils.isEmpty(destPath)) {
            return null;
        }
        InputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            int byteRead;
            File oldFile = new File(oldPath);
            if (oldFile.exists()) { //文件存在时
                if (destPath.equals(oldPath)) {
                    return oldPath;
                }
                inStream = new FileInputStream(oldPath); //读入原文件
                outStream = new FileOutputStream(destPath);
                byte[] buffer = new byte[1024 * 10];
                while ((byteRead = inStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, byteRead);
                }
                return destPath;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException exception) {
            }
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (IOException exception) {
            }
        }
    }
    public static String copyVideo(String oldPath,String destDir) {
        if (TextUtils.isEmpty(destDir)) {
            return null;
        }
        InputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            int byteRead;
            File oldFile = new File(oldPath);
            if (oldFile.exists()) { //文件存在时
                String newPath = destDir + File.separator + oldFile.getName();
                if (newPath.equals(oldPath)) {
                    return oldPath;
                }

                inStream = new FileInputStream(oldPath); //读入原文件
                outStream = new FileOutputStream(newPath);
                byte[] buffer = new byte[1024 * 10];
                while ((byteRead = inStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, byteRead);
                }
                return newPath;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException exception) {
            }
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (IOException exception) {
            }
        }
    }
    /**
     * 保存到视频到本地，并插入MediaStore以保证相册可以查看到,这是更优化的方法，防止读取的视频获取不到宽高
     *
     * @param context    上下文
     * @param filePath   文件路径
     * @param createTime 创建时间 <=0时为当前时间 ms
     * @param duration   视频长度 ms
     * @param width      宽度
     * @param height     高度
     */
    public static void insertVideoToMediaStore(Context context, String filePath, long createTime, int width, int height, long duration) {
        if (getExternalAppDir() == null) {
            return;
        }
        String savePath = copyVideo(filePath, getExternalAppDir().getPath());
        if (!checkFile(savePath)) {
            return;
        }
        createTime = getTimeWrap(createTime);
        ContentValues values = initCommonContentValues(savePath, createTime);
        values.put(MediaStore.Video.VideoColumns.DATE_TAKEN, createTime);
        if (duration > 0) {
            values.put(MediaStore.Video.VideoColumns.DURATION, duration);
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            if (width > 0) {
                values.put(MediaStore.Video.VideoColumns.WIDTH, width);
            }
            if (height > 0) {
                values.put(MediaStore.Video.VideoColumns.HEIGHT, height);
            }
        }
        values.put(MediaStore.MediaColumns.MIME_TYPE, getVideoMimeType(savePath));
        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }
    /**
     * 检测文件存在
     */
    private static boolean checkFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        try {
            File file = new File(filePath);
            return file.exists();
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * 获得转化后的时间
     */
    private static long getTimeWrap(long time) {
        if (time <= 0) {
            return System.currentTimeMillis();
        }
        return time;
    }
    /**
     * 针对非系统文件夹下的文件,使用该方法
     * 插入时初始化公共字段
     *
     * @param filePath 文件
     * @param time     ms
     * @return ContentValues
     */
    private static ContentValues initCommonContentValues(String filePath, long time) {
        ContentValues values = new ContentValues();
        File saveFile = new File(filePath);
        long timeMillis = getTimeWrap(time);
        values.put(MediaStore.MediaColumns.TITLE, saveFile.getName());
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, saveFile.getName());
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, timeMillis);
        values.put(MediaStore.MediaColumns.DATE_ADDED, timeMillis);
        values.put(MediaStore.MediaColumns.DATA, saveFile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.SIZE, saveFile.length());
        return values;
    }

    /**
     * 获取video的mine_type,暂时只支持mp4,3gp
     */
    private static String getVideoMimeType(String path) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith("mp4") || lowerPath.endsWith("mpeg4")) {
            return "video/mp4";
        } else if (lowerPath.endsWith("3gp")) {
            return "video/3gp";
        }
        return "video/mp4";
    }

    /**
     * 判断是否为gif
     */
    public static boolean isGif(Context context, Uri uri) {
        return getImageType(context, uri) == ImageType.GIF;
    }

    public static ImageType getImageType(Context context, Uri uri) {
        if (null == uri) {
            return ImageType.UNKNOWN;
        }
        ImageType type = ImageType.UNKNOWN;
        InputStream fis = null;
        try {
            fis = context.getContentResolver().openInputStream(uri);
            byte[] buffer = new byte[8];
            fis.read(buffer);
            if (checkSignature(buffer, GIF89A) || checkSignature(buffer, GIF87A)) {
                return ImageType.GIF;
            } else if (checkSignature(buffer, JPEG)) {
                return ImageType.JPG;
            } else if (checkSignature(buffer, PNG)) {
                return ImageType.PNG;
            }
        } catch (Exception e) {
        } finally {
            try {
                if (null != fis) {
                    fis.close();
                }
            } catch (Exception e2) {
            }
        }
        return type;
    }

    private static boolean checkSignature(byte[] input, byte[] signature) {
        if (null == input || null == signature) {
            return false;
        }
        boolean success = true;
        for (int ix = 0; ix < signature.length; ix++) {
            if (input[ix] != signature[ix]) {
                success = false;
                break;
            }
        }
        return success;
    }

    public static boolean saveInputStream(InputStream input, String dir,
                                          String name) {
        if (input == null) {
            return false;
        }
        FileOutputStream out = null;
        try {
            File path = new File(dir);
            if (!path.exists()) {
                if (!path.mkdirs()) {
                    return false;
                }
            }
            File f = new File(path, name);
            out = new FileOutputStream(f);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            out.flush();
            out.close();
            out = null;
            buffer = null;
            input.close();
        } catch (Exception e) {
            Log.d(TAG, "save inputstream error: " + e);
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception ex) {
                // ignore
            }
            try {
                if (input != null) {
                    input.close();
                }
            } catch (Exception e2) {
            }
        }
        return true;
    }
}

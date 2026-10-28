package io.github.ihatobatsugui.firefightfont;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * JS 侧调用 AndroidSaver.saveBase64(base64, mime, fileName) 把导出的 PNG / zip 写进系统下载目录。
 * WebView 里 <a download> 对 data:/blob: 无效，所以走这个桥。
 */
public class FileBridge {

    private final Context context;

    FileBridge(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void saveBase64(final String base64, final String mime, final String fileName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                try {
                    byte[] data = Base64.decode(base64, Base64.NO_WRAP);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                        values.put(MediaStore.Downloads.MIME_TYPE, mime);
                        values.put(MediaStore.Downloads.IS_PENDING, 1);
                        Uri uri = context.getContentResolver()
                                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri == null) {
                            throw new IllegalStateException("MediaStore 拒绝写入");
                        }
                        OutputStream out = context.getContentResolver().openOutputStream(uri);
                        if (out == null) {
                            throw new IllegalStateException("无法打开输出流");
                        }
                        out.write(data);
                        out.close();
                        values.clear();
                        values.put(MediaStore.Downloads.IS_PENDING, 0);
                        context.getContentResolver().update(uri, values, null, null);
                        message = "已保存到下载目录：" + fileName;
                    } else {
                        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                        if (dir == null) {
                            dir = context.getFilesDir();
                        }
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        File file = new File(dir, fileName);
                        FileOutputStream out = new FileOutputStream(file);
                        out.write(data);
                        out.close();
                        message = "已保存到：" + file.getAbsolutePath();
                    }
                } catch (Exception error) {
                    message = "保存失败：" + error.getMessage();
                }

                final String text = message;
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}

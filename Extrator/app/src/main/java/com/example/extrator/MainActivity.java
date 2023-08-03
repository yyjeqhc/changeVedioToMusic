package com.example.extrator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.media.MediaExtractor;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.File;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_VIDEO_REQUEST_CODE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonOpenGallery = findViewById(R.id.button_open_gallery);
        buttonOpenGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        Button buttonOpenFilePicker = findViewById(R.id.button_open_file_picker);
        buttonOpenFilePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        // 检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // 如果权限没有被授予，请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission
                            .READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            // 如果权限已经授予，执行相关操作
//            openFilePicker();
        }
    }

    private void openGallery() {
        // 启动相册选择视频的 Intent
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        startActivityForResult(intent, PICK_VIDEO_REQUEST_CODE);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        startActivityForResult(intent, PICK_VIDEO_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了存储权限，执行相关操作
                openGallery();
            } else {
                // 用户拒绝了存储权限，可以采取其他措施或显示相关提示信息
                Toast.makeText(this, "存储权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();
            if (videoUri != null) {
                String videoPath = getPathFromUri(videoUri);
                convertVideoToMp3(videoPath);
            }
        }
    }

    /*
    I/System.out: /document/primary:Amp3/1.mp4
    I/System.out: /external_files/Amp3/1.mp4
    I/System.out: /raw//storage/emulated/0/Amp3/1.mp4
    对于第一种情况需要优先处理，否则就会崩溃
     */
    private String getPathFromUri(Uri uri) {
        String path = uri.getPath();
        System.out.println(path);
        if (path.startsWith("/document/")) {
            // Handle /document/ path
            String[] parts = path.split(":");
            if (parts.length > 1) {
                String type = parts[0];
                String realPath = parts[1];
                path  = Environment.getExternalStorageDirectory() + "/" + realPath;
                return path;
            }
        }
        String[] projection = { MediaStore.Video.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return null;
    }

    private void convertVideoToMp3(String videoPath) {
        // 提取文件名（不包括扩展名）
        String fileName = new File(videoPath).getName();
        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            fileName = fileName.substring(0, pos);
        }

        // 生成输出文件的路径
        String outputFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/" + fileName + ".mp3";

        // 执行FFmpeg命令
        String command = "-i " + videoPath + " -acodec libmp3lame " + outputFilePath;
        int rc = FFmpeg.execute(command);
        Toast.makeText(this, "转换完成，请在Music目录下查看转换结果", Toast.LENGTH_SHORT).show();
    }
}
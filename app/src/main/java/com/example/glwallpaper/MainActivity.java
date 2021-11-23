package com.example.glwallpaper;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.glwallpaper.wallpapers.image.ImageWallpaperMeta;
import com.example.glwallpaper.wallpapers.image.ImageWallpaperService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityResultLauncher<String[]> launcher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                handleUri(result);
            }
        });

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launcher.launch(new String[]{"image/*"});
            }
        });

        findViewById(R.id.three_d).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setThreeD();
            }
        });
    }

    private void setThreeD() {
        File dir = getExternalFilesDir("three-d");
        File l1 = new File(dir, "l1.webp");
        File l2 = new File(dir, "l2.webp");
        File l3 = new File(dir, "l3.png");

        ImageWallpaperMeta bean = new ImageWallpaperMeta();
        bean.addImage(l1.getAbsolutePath());
        bean.addImage(l2.getAbsolutePath());
        bean.addImage(l3.getAbsolutePath());

        bean.addFactor(-0.4f);
        bean.addFactor(10f);
        bean.addFactor(0.9f);

        if (!l1.exists()) {
            try {
                copy("l1.webp", l1);
                copy("l2.webp", l2);
                copy("l3.png", l3);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ImageWallpaperService.setWallpaper(this, bean);
    }

    private void copy(String name, File out) throws IOException {
        InputStream is = getAssets().open(name);
        BufferedSource source = Okio.buffer(Okio.source(is));

        BufferedSink sink = Okio.buffer(Okio.sink(out));
        sink.writeAll(source);
        sink.flush();
        sink.close();
        source.close();
    }

    private void handleUri(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedSource source = Okio.buffer(Okio.source(inputStream));

            File dir = getExternalFilesDir("wallpapers");
            File out = new File(dir, System.currentTimeMillis() + ".jpg");

            BufferedSink sink = Okio.buffer(Okio.sink(out));
            sink.writeAll(source);
            sink.flush();
            sink.close();

            source.close();

            ImageWallpaperMeta bean = new ImageWallpaperMeta();
            bean.addImage(out.getAbsolutePath());

            ImageWallpaperService.setWallpaper(this, bean);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
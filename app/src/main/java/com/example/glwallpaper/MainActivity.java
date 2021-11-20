package com.example.glwallpaper;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.glwallpaper.wallpapers.image.ImageWallpaperService;

import java.io.File;
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

            ImageWallpaperService.setWallpaper(this, out.getAbsolutePath());

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
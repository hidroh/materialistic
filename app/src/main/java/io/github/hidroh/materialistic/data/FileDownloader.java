package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.WorkerThread;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

public class FileDownloader {
    private Call.Factory mCallFactory;
    private final String mCacheDir;

    @Inject
    public FileDownloader(Context context, Call.Factory callFactory) {
        mCacheDir = context.getCacheDir().getPath(); // don't need to keep a reference to context after this
        mCallFactory = callFactory;
    }

    @WorkerThread
    public void downloadFile(String url, String mimeType, FileDownloaderCallback callback) {
        final Request request = new Request.Builder().url(url)
                .addHeader("Content-Type", mimeType)
                .build();

        mCallFactory.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(call, e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    final File outputFile = new File(mCacheDir, new File(url).getName());
                    BufferedSink sink = Okio.buffer(Okio.sink(outputFile));
                    sink.writeAll(response.body().source());
                    sink.close();
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(outputFile.getPath()));
                } catch (IOException e) {
                    this.onFailure(call, e);
                }
            }
        });
    }

    public interface FileDownloaderCallback {
        void onFailure(Call call, IOException e);
        void onSuccess(String filePath);
    }
}

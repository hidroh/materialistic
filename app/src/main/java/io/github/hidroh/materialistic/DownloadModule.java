package io.github.hidroh.materialistic;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import dagger.Module;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okio.*;

import javax.inject.Inject;
import java.io.*;

@Module(library = true, complete = false, includes = NetworkModule.class)
class DownloadModule {
    private Call.Factory mCallFactory;

    @Inject
    DownloadModule(Call.Factory callFactory) {
        this.mCallFactory = callFactory;
    }

    void downloadFile(Context context, String url, String mimeType, DownloadModuleCallback callback) {
        final Request request = new Request.Builder().url(url)
            .addHeader("Content-Type", mimeType)
            .build();

        mCallFactory.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    final File outputFile = new File(context.getCacheDir().getPath(), new File(url).getName());
                    BufferedSink sink = Okio.buffer(Okio.sink(outputFile));
                    sink.writeAll(response.body().source());
                    sink.close();
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(outputFile.getPath()));
                } catch (IOException e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public interface DownloadModuleCallback {
        void onFailure(IOException e);
        void onSuccess(String filePath);
    }
}

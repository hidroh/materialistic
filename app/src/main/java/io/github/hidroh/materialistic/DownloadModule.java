package io.github.hidroh.materialistic;

import android.content.Context;
import dagger.Module;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

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
            public void onResponse(Call call, Response response) throws IOException {
                final InputStream inputStream = response.body().byteStream();
                final File outputFile = new File(context.getCacheDir().getPath(), new File(url).getName());
                final FileOutputStream outputStream = new FileOutputStream(outputFile);
                byte[] data = new byte[1024];
                int count;
                while ((count = inputStream.read(data)) != -1) {
                    outputStream.write(data, 0, count);
                }
                inputStream.close();
                outputStream.close();
                callback.onSuccess(outputFile);
            }
        });
    }

    public interface DownloadModuleCallback {
        void onFailure(IOException e);
        void onSuccess(File file) throws IOException;
    }
}

package io.github.hidroh.materialistic;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.squareup.picasso.Picasso;

import java.io.IOException;

import javax.inject.Inject;

public interface ImageUtils {
    @Nullable Bitmap fromUri(Uri uri);

    class PicassoImpl implements ImageUtils {
        private final Picasso mPicasso;

        @Inject
        public PicassoImpl(Context context) {
            mPicasso = Picasso.with(context);
        }

        @Nullable
        @Override
        public Bitmap fromUri(Uri uri) {
            try {
                return mPicasso.load(uri).get();
            } catch (IOException e) {
                return null;
            }
        }
    }
}

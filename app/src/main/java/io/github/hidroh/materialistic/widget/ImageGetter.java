package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import io.github.hidroh.materialistic.ActivityModule;
import io.github.hidroh.materialistic.Application;
import io.github.hidroh.materialistic.ImageUtils;

public class ImageGetter implements android.text.Html.ImageGetter {
    private final Context mContext;
    private final TextView mTextView;
    @Inject ImageUtils mImageUtils;

    /**
     * Construct an instance of {@link android.text.Html.ImageGetter}
     * @param view      {@link android.widget.TextView} that holds HTML which contains $lt;img&gt; tag to load
     */
    public ImageGetter(TextView view) {
        ((Application) view.getContext().getApplicationContext())
                .getApplicationGraph()
                .plus(new ActivityModule(view.getContext()))
                .inject(this);
        mContext = view.getContext();
        mTextView = view;
    }

    @Override
    public Drawable getDrawable(String source) {
        final Uri uri = Uri.parse(source);
        if (uri.isRelative()) {
            return null;
        }
        final URLDrawable urlDrawable = new URLDrawable(mContext.getResources(), null);
        new LoadFromUriAsyncTask(mImageUtils, mTextView, urlDrawable).execute(uri);
        return urlDrawable;
    }

    private static class URLDrawable extends BitmapDrawable {
        private Drawable mDrawable;

        public URLDrawable(Resources res, Bitmap bitmap) {
            super(res, bitmap);
        }

        @Override
        public void draw(Canvas canvas) {
            if(mDrawable != null) {
                mDrawable.draw(canvas);
            }
        }
    }

    private static class LoadFromUriAsyncTask extends AsyncTask<Uri, Void, Bitmap> {
        private final WeakReference<TextView> mTextViewRef;
        private final URLDrawable mUrlDrawable;
        private final ImageUtils mImageUtils;

        public LoadFromUriAsyncTask(ImageUtils imageUtils, TextView textView, URLDrawable urlDrawable) {
            mImageUtils = imageUtils;
            mTextViewRef = new WeakReference<>(textView);
            mUrlDrawable = urlDrawable;
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            return mImageUtils.fromUri(params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result == null) {
                return;
            }
            if (mTextViewRef.get() == null) {
                return;
            }
            TextView textView = mTextViewRef.get();
            // change the reference of the current mDrawable to the result
            // from the HTTP call
            mUrlDrawable.mDrawable = new BitmapDrawable(textView.getResources(), result);
            // set bound to scale image to fit width and keep aspect ratio
            // according to the result from HTTP call
            int width = textView.getWidth();
            int height = Math.round(1.0f * width *
                    mUrlDrawable.mDrawable.getIntrinsicHeight() /
                    mUrlDrawable.mDrawable.getIntrinsicWidth());
            mUrlDrawable.setBounds(0, 0, width, height);
            mUrlDrawable.mDrawable.setBounds(0, 0, width, height);
            // force redrawing bitmap by setting text
            textView.setText(textView.getText());
        }
    }
}

package android.support.v4.content;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.robolectric.Shadows.shadowOf;

// TODO remove this once https://github.com/robolectric/robolectric/issues/2020 is fixed
@Implements(value = ContentResolverCompatJellybean.class, inheritImplementationMethods = true)
public class ShadowContentResolverCompatJellybean {
    @Implementation
    public static Cursor query(ContentResolver resolver, Uri uri, String[] projection,
                               String selection, String[] selectionArgs, String sortOrder,
                               Object cancellationSignalObj) {
        return shadowOf(RuntimeEnvironment.application.getContentResolver())
                .query(uri, projection, selection, selectionArgs, sortOrder);
    }
}

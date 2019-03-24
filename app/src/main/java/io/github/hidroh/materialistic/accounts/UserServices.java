/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic.accounts;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.StringRes;

import java.io.IOException;

public interface UserServices {
    abstract class Callback {
        public void onDone(boolean successful) {}
        public void onError(Throwable throwable) {}
    }

    class Exception extends IOException {
        public final @StringRes int message;
        public Uri data;

        public Exception(int message) {
            this.message = message;
        }

        Exception(String message) {
            super(message);
            this.message = 0;
        }
    }

    void login(String username, String password, boolean createAccount, Callback callback);

    boolean voteUp(Context context, String itemId, Callback callback);

    void reply(Context context, String parentId, String text, Callback callback);

    void submit(Context context, String title, String content, boolean isUrl, Callback callback);
}

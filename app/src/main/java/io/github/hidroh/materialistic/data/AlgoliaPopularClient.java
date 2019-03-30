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

package io.github.hidroh.materialistic.data;

import android.text.format.DateUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Inject;

import androidx.annotation.StringDef;
import retrofit2.Call;
import rx.Observable;

public class AlgoliaPopularClient extends AlgoliaClient {

    @Inject
    public AlgoliaPopularClient(RestServiceFactory factory) {
        super(factory);
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            LAST_24H,
            PAST_WEEK,
            PAST_MONTH,
            PAST_YEAR
    })
    public @interface Range {}
    public static final String LAST_24H = "last_24h";
    public static final String PAST_WEEK = "past_week";
    public static final String PAST_MONTH = "past_month";
    public static final String PAST_YEAR = "past_year";

    @Override
    protected Observable<AlgoliaHits> searchRx(@Range String filter) {
        return mRestService.searchByMinTimestampRx(MIN_CREATED_AT + toTimestamp(filter) / 1000);
    }

    @Override
    protected Call<AlgoliaHits> search(@Range String filter) {
        return mRestService.searchByMinTimestamp(MIN_CREATED_AT + toTimestamp(filter) / 1000);
    }

    private long toTimestamp(@Range String filter) {
        long timestamp = System.currentTimeMillis();
        switch (filter) {
            case LAST_24H:
            default:
                timestamp -= DateUtils.DAY_IN_MILLIS;
                break;
            case PAST_WEEK:
                timestamp -= DateUtils.WEEK_IN_MILLIS;
                break;
            case PAST_MONTH:
                timestamp -= DateUtils.WEEK_IN_MILLIS * 4;
                break;
            case PAST_YEAR:
                timestamp -= DateUtils.YEAR_IN_MILLIS;
                break;
        }
        return timestamp;
    }
}

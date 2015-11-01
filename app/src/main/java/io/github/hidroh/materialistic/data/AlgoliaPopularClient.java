package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.support.annotation.StringDef;
import android.text.format.DateUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Inject;

import retrofit.Callback;

public class AlgoliaPopularClient extends AlgoliaClient {

    private static final String MIN_CREATED_AT = "created_at_i>";

    @Inject
    public AlgoliaPopularClient(Context context, RestServiceFactory factory) {
        super(context, factory);
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
    protected void search(@Range String filter, Callback<AlgoliaHits> callback) {
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
        mRestService.searchByMinTimestamp(MIN_CREATED_AT + timestamp / 1000, callback);
    }
}

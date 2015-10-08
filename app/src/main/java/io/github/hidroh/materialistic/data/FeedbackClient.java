package io.github.hidroh.materialistic.data;

import javax.inject.Inject;

import io.github.hidroh.materialistic.BuildConfig;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;

public interface FeedbackClient {
    interface Callback {
        void onSent(boolean success);
    }

    void send(String title, String body, Callback callback);

    class Impl implements FeedbackClient {
        private final FeedbackService mFeedbackService;

        @Inject
        public Impl(RestServiceFactory factory) {
            mFeedbackService = factory.create(FeedbackService.GITHUB_API_URL, FeedbackService.class);
        }

        @Override
        public void send(String title, String body, final Callback callback) {
            mFeedbackService.createGithubIssue(new Issue(title, body),
                    new retrofit.Callback<Object>() {
                        @Override
                        public void success(Object object, Response response) {
                            callback.onSent(true);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            callback.onSent(false);
                        }
                    });
        }

        interface FeedbackService {
            String GITHUB_API_URL = "https://api.github.com";

            @POST("/repos/hidroh/materialistic/issues")
            @Headers("Authorization: token " + BuildConfig.GITHUB_TOKEN)
            void createGithubIssue(@Body Issue issue, retrofit.Callback<Object> callback);
        }

        static class Issue {
            private static final String LABEL_FEEDBACK = "feedback";

            private final String title;
            private final String body;
            private final String[] labels;

            private Issue(String title, String body) {
                this.title = title;
                this.body = body;
                this.labels = new String[]{LABEL_FEEDBACK};
            }
        }
    }
}

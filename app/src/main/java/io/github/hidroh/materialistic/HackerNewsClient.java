package io.github.hidroh.materialistic;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Client to retrieve Hacker News content asynchronously
 */
public class HackerNewsClient {
    private static final String BASE_URL = "https://hacker-news.firebaseio.com/v0";
    private static HackerNewsClient mInstance;
    private RestService mRestService;

    /**
     * Gets singleton client instance
     * @return a hacker news client
     */
    public static HackerNewsClient getInstance() {
        if (mInstance == null) {
            mInstance = new HackerNewsClient();
            mInstance.mRestService = new RestAdapter.Builder()
                    .setLogLevel(RestAdapter.LogLevel.BASIC)
                    .setEndpoint(BASE_URL)
                    .build()
                    .create(RestService.class);
        }

        return mInstance;
    }

    /**
     * Gets array of top 100 stories
     * @param listener callback to be notified on response
     */
    public void getTopStories(final ResponseListener<TopStory[]> listener) {
        mRestService.topStories(new Callback<int[]>() {
            @Override
            public void success(int[] ints, Response response) {
                if (listener == null) {
                    return;
                }

                TopStory[] topStories = new TopStory[ints.length];
                for (int i = 0; i < ints.length; i++) {
                    topStories[i] = new TopStory(ints[i]);
                }
                listener.onResponse(topStories);
            }

            @Override
            public void failure(RetrofitError error) {
                if (listener == null) {
                    return;
                }

                listener.onError(error == null ? error.getMessage() : "");
            }
        });
    }

    /**
     * Gets individual item by ID
     * @param itemId    item ID
     * @param listener  callback to be notified on response
     * TODO consider subclassing Item
     */
    public void getItem(String itemId, ResponseListener<Item> listener) {
        mRestService.item(itemId, makeCallback(listener));
    }

    private <T> Callback<T> makeCallback(final ResponseListener<T> listener) {
        return new Callback<T>() {
            @Override
            public void success(T t, Response response) {
                if (listener == null) {
                    return;
                }

                listener.onResponse(t);
            }

            @Override
            public void failure(RetrofitError error) {
                if (listener == null) {
                    return;
                }

                listener.onError(error == null ? error.getMessage() : "");
            }
        };
    }

    public static interface ResponseListener<T> {
        void onResponse(T response);
        void onError(String errorMessage);
    }

    private static interface RestService {
        @GET("/topstories.json")
        void topStories(Callback<int[]> callback);
        @GET("/item/{itemId}.json")
        void item(@Path("itemId") String itemId, Callback<Item> callback);
    }

    public static class Item {
        // The item's unique id. Required.
        private long id;
        // true if the item is deleted.
        private boolean deleted;
        // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
        private String type;
        // The username of the item's author.
        private String by;
        // Creation date of the item, in Unix Time.
        private long time;
        // The comment, Ask HN, or poll text. HTML.
        private String text;
        // true if the item is dead.
        private boolean dead;
        // The item's parent. For comments, either another comment or the relevant story. For pollopts, the relevant poll.
        private long parent;
        // The ids of the item's comments, in ranked display order.
        private long[] kids;
        // The URL of the story.
        private String url;
        // The story's score, or the votes for a pollopt.
        private int score;
        // The title of the story or poll.
        private String title;
        // A list of related pollopts, in display order.
        private long[] parts;

        public String getTitle() {
            return title;
        }
    }

    public static class TopStory {
        private int id;
        private String title;

        private TopStory(int id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public String setTitle() {
            return title;
        }

        public int getId() {
            return id;
        }

        public void populate(Item info) {
            title = info.title;
        }
    }
}

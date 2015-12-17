package io.github.hidroh.materialistic.data;

/**
 * Callback interface for requests
 * @param <T> response type
 */
public interface ResponseListener<T> {
    /**
     * Fired when request is successful
     * @param response result
     */
    void onResponse(T response);

    /**
     * Fired when request is failed
     * @param errorMessage error message or null
     */
    void onError(String errorMessage);
}

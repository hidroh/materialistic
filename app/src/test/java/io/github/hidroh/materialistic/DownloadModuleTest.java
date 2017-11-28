package io.github.hidroh.materialistic;

import okhttp3.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import io.github.hidroh.materialistic.test.TestRunner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static junit.framework.Assert.assertEquals;

@RunWith(TestRunner.class)
public class DownloadModuleTest {
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
    }

    @Test
    public void testSuccessfulFileDownload() throws InterruptedException, ExecutionException, UnsupportedEncodingException {
        final Call.Factory callFactory = getCallFactory(new MockRequest[] {
            new MockRequest("http://example.com/file.pdf", "application/pdf", "woo".getBytes("utf-8"))
        });
        final DownloadModule downloadModule = new DownloadModule(callFactory);
        final CompletableFuture<String> future = new CompletableFuture<>();
        downloadModule.downloadFile("http://example.com/file.pdf", "application/pdf", new DownloadModule.DownloadModuleCallback() {
            @Override
            public void onFailure(IOException e) {}
            @Override
            public void onSuccess(byte[] bytes) throws IOException {
                future.complete(new String(bytes));
            }
        });
        final String result = future.get();
        assertEquals(result, "woo");
    }

    private Call.Factory getCallFactory(MockRequest[] mockRequests) {
        return new OkHttpClient.Builder().addInterceptor(new TestInterceptor(mockRequests)).build();
    }

    class MockRequest {
        private String mUrl;
        private String mContentType;
        private byte[] mBytes;

        MockRequest(String url, String contentType, byte[] bytes) {
            mUrl = url;
            mContentType = contentType;
            mBytes = bytes;
        }

        public String getUrl() {
            return mUrl;
        }

        public String getContentType() {
            return mContentType;
        }

        public byte[] getBytes() {
            return mBytes;
        }
    }

    class TestInterceptor implements Interceptor {
        private MockRequest[] mRequests;

        TestInterceptor(MockRequest[] requests) {
            mRequests = requests;
        }

        @Override public Response intercept(Interceptor.Chain chain) throws IOException {
            final Request request = chain.request();
            MockRequest mockRequest = null;
            for (MockRequest r : mRequests) {
                if (r.getUrl().equals(request.url().toString()) &&
                        r.getContentType().equals(request.header("Content-Type"))) {
                    mockRequest = r;
                }
            }

            if (mockRequest != null) {
                return new Response.Builder()
                        .request(chain.request())
                        .code(200)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create(MediaType.parse(mockRequest.getContentType()), mockRequest.getBytes()))
                        .build();
            } else {
                return new Response.Builder()
                        .request(chain.request())
                        .code(500)
                        .protocol(Protocol.HTTP_1_1)
                        .body(ResponseBody.create(MediaType.parse("text/plain"), "failure".getBytes("utf-8")))
                        .build();
            }
        }
    }
}


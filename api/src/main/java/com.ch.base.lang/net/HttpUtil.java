package com.ch.base.lang.net;

import com.ch.base.lang.serial.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class HttpUtil {
    public static final int ORD_PRICE_FROM_CACHE_TIME_BEFORE = 5 * 1000;//5S
    private static final int TIME_OUT_IN_MILLSECONDS = 10000;
    private static final int LONG_TIME_OUT_IN_MILLSECONDS = 100000; // 100s
    private static final int CONN_TIMEOUT_IN_MILLISECONDS = 3000;
    private static final HttpClient longHttpClient;
    private static final int RETRY_TIMES = 2;
    private static final String URL_TOKEN = "?";
    private static final String PARAM_TOKEN = "&";
    private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    private static Logger log = LoggerFactory.getLogger(HttpUtil.class);

    static {
        HttpParams params = new BasicHttpParams();

        HttpConnectionParams.setConnectionTimeout(params, CONN_TIMEOUT_IN_MILLISECONDS);
        HttpConnectionParams.setSoTimeout(params, LONG_TIME_OUT_IN_MILLSECONDS);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(params, "Chrome/5.0.342.9 Safari/533.2");

        HttpClientParams.setCookiePolicy(params, CookiePolicy.BROWSER_COMPATIBILITY);

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(getHttpsSupportScheme());
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(schemeRegistry);

        cm.setMaxTotal(600);
        cm.setDefaultMaxPerRoute(100);

        longHttpClient = new HttpClient(cm, params);
    }



    public static String getContent(final String url) throws Exception {
        return getContent(url, RETRY_TIMES);
    }

    public static <T> T getObject(final String url, Class<T> clazz) {
        String content = null;
        try {
            content = getContent(url);
            logger.info("{}:{}",url,content);
            if (StringUtils.isEmpty(content)) {
                return null;
            }
            return JsonUtil.getObjectMapperInstance().readValue(content, clazz);
        } catch (Exception e) {
            logger.error("httputil meet error:{}", url, e);
        }
        return null;
    }

    public static String getContent(final String url, final int retryTimes) throws Exception {
        return getContent(url, retryTimes, TIME_OUT_IN_MILLSECONDS);
    }

    public static String getContentWithTimeOut(final String url, final int timeOut) throws Exception {
        return getContent(url, RETRY_TIMES, timeOut);
    }

    public static String getContent(final String url, final int retryTimes, final int timeOut) throws Exception {
        // 检测是不是mock的url
        // String mockContent = MockUtil.getMockRs(url);
        // if (StringUtils.isNotEmpty(mockContent)) {
        // return mockContent;
        // }

        final HttpClient httpClient = getHttpClient(timeOut);
        try {
            FutureTask<String> fu = new FutureTask<String>(new Callable<String>() {
                public String call() throws Exception {
                    String resultString = StringUtils.EMPTY;
                    for (int i = 0; i <= retryTimes; i++) {
                        HttpGet request = new HttpGet(url);
                        HttpResponse response = null;
                        HttpEntity entity = null;
                        try {
                            response = httpClient.execute(request);
                            entity = response.getEntity();
                            StatusLine status = response.getStatusLine();
                            if (status != null && status.getStatusCode() == HttpStatus.SC_OK) {
                                resultString = EntityUtils.toString(entity, "UTF-8");
                                return resultString;
                            } else {
                                throw new HttpException(String.format("请求响应状态异常!状态码=%s,url=%s", status, url));
                            }
                        } catch (Exception e) {
                            request.releaseConnection();
                            log.error("access http error,url={},,errorMsg={}", url, e.getMessage(), e);
                            if (i == retryTimes) {
                                throw e;
                            }
                        } finally {
                            EntityUtils.consume(entity);
                            entity = null;
                        }
                    }
                    return resultString;
                }
            });
            Executors.newSingleThreadExecutor().execute(fu);
            return fu.get(timeOut * retryTimes + 1, TimeUnit.MILLISECONDS);
        } finally {
            httpClient.close();
        }
    }

    @SuppressWarnings("deprecation")
    public static String getContentByLongPost(final int time, final String url, final List<NameValuePair> formParams) throws Exception {
        FutureTask<String> fu = new FutureTask<String>(new Callable<String>() {
            public String call() throws Exception {
                String resultString = StringUtils.EMPTY;
                for (int i = 0; i < RETRY_TIMES; i++) {
                    HttpPost request = new HttpPost(url);
                    request.setEntity(new UrlEncodedFormEntity(formParams, "utf-8"));
                    HttpEntity entity = null;
                    try {
//                        Interceptors.gzip(longHttpClient, true);
                        HttpResponse response = longHttpClient.execute(request);
                        entity = response.getEntity();
                        StatusLine status = response.getStatusLine();
                        if (status != null && status.getStatusCode() == 200) {
                            resultString = EntityUtils.toString(entity, "UTF-8");
                            return resultString;
                        } else {
                            request.abort();
                        }
                    } catch (IOException ex) {
                        if (ex instanceof org.apache.http.conn.ConnectionPoolTimeoutException) {
                            log.error("ConnectionPoolTimeoutException=" + ex.getMessage());
                            request.releaseConnection();
                        }
                        request.releaseConnection();
                        log.error("access " + url + " exception," + ex.getMessage());
                        throw ex;
                    } catch (Exception e) {
                        request.releaseConnection();
                        log.error("access " + url + " exception," + e.getMessage());
                        throw e;
                    } finally {
                        EntityUtils.consume(entity);
                        entity = null;
                    }
                }
                return resultString;
            }
        });
        Executors.newSingleThreadExecutor().execute(fu);
        String content = null;
        try {
            content = fu.get(time, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            throw e;
        }
        return content;
    }

    public static String formatUrl(String host, Map<String, String> params) {
        StringBuilder builder = new StringBuilder(host);

        if (host.contains("?")) {
            builder.append(PARAM_TOKEN);
        } else {
            builder.append(URL_TOKEN);
        }

        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                builder.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                log.warn("URL Encoder Failed:{}", host, e);
                builder.append(entry.getKey()).append("=").append(entry.getValue());
            }
            builder.append(PARAM_TOKEN);
        }

        return builder.toString().substring(0, builder.length() - 1);
    }

    private static HttpClient getHttpClient(int timeout) {
        HttpParams params = new BasicHttpParams();

        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setSoTimeout(params, timeout);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(params, "Chrome/5.0.342.9 Safari/533.2");

        HttpClientParams.setCookiePolicy(params, CookiePolicy.BROWSER_COMPATIBILITY);

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(getHttpsSupportScheme());
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(schemeRegistry);

        cm.setMaxTotal(1200);
        cm.setDefaultMaxPerRoute(300);

        HttpClient httpClient = new HttpClient(cm, params);
        httpClient.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
        return httpClient;
    }

    private static Scheme getHttpsSupportScheme() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            return new Scheme("https", 443, ssf);
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (KeyManagementException e) {
            return null;
        }
    }

    public static String getContentByLongPost(final String url, final String json) throws Exception {
        return getContentByLongPost(url, json, RETRY_TIMES, LONG_TIME_OUT_IN_MILLSECONDS);
    }

    public static String getContentByLongPost(final String url, final String json, final int retryTimes, long timeOutMilliSeconds) throws Exception {
        FutureTask<String> fu = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String resultString = StringUtils.EMPTY;
                final int tryTimes = retryTimes <= 0 ? 1 : retryTimes + 1;
                for (int i = 0; i < tryTimes; i++) {
                    HttpPost post = new HttpPost(url);
                    StringEntity stringEntity = new StringEntity(json, "utf-8");
                    stringEntity.setContentType("application/json");
                    post.setEntity(stringEntity);
                    post.setHeader(new BasicHeader("Accept", "application/json"));

                    HttpEntity entity = null;
                    try {
                        HttpResponse response = longHttpClient.execute(post);
                        entity = response.getEntity();
                        StatusLine status = response.getStatusLine();
                        if (status != null && status.getStatusCode() == 200) {
                            resultString = EntityUtils.toString(entity, "UTF-8");
                            return resultString;
                        } else {
                            post.abort();
                        }
                    } catch (IOException ex) {
                        if (ex instanceof org.apache.http.conn.ConnectionPoolTimeoutException) {
                            log.error("ConnectionPoolTimeoutException=" + ex.getMessage());
                            post.releaseConnection();
                        }
                        post.releaseConnection();
                        log.error("access " + url + " exception," + ex.getMessage());
                        throw ex;
                    } catch (Exception e) {
                        post.releaseConnection();
                        log.error("access " + url + " exception," + e.getMessage());
                        throw e;
                    } finally {
                        EntityUtils.consume(entity);
                        entity = null;
                    }
                }
                return resultString;
            }
        });
        Executors.newSingleThreadExecutor().execute(fu);
        String content;
        try {
            content = fu.get(timeOutMilliSeconds, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            throw e;
        }
        return content;
    }

    /**
     * 根据URL 组装 HTTP POST
     *
     * @param url
     * @param xml
     * @return
     * @throws java.io.UnsupportedEncodingException
     */
    public static HttpPost getPost(String url, String xml) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url);
        StringEntity stringEntity = new StringEntity(xml, "utf-8");
        stringEntity.setContentType("application/xml");
        httpPost.setEntity(stringEntity);
        httpPost.setHeader(new BasicHeader("Accept", "application/xml"));
        return httpPost;

    }

    /**
     * 根据URL 组装 HTTP POST
     *
     * @param url
     * @param json
     * @return
     * @throws UnsupportedEncodingException
     */
    public static HttpPost getPostByJson(String url, String json) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url);
        StringEntity stringEntity = new StringEntity(json, "utf-8");
        stringEntity.setContentType("application/json");
        httpPost.setEntity(stringEntity);
        httpPost.setHeader(new BasicHeader("Accept", "application/json"));
        return httpPost;

    }

    /**
     * 使用post请求获取url内容
     *
     * @param httpPost       请求地址
     * @param connectTimeout 连接超时时间
     * @param socketTimeout  读取超时时间
     */
    public static String getPostContent(HttpPost httpPost, int connectTimeout, int socketTimeout) throws Exception {
        return process(httpPost, connectTimeout, socketTimeout, false);
    }

    private static String process(final HttpRequestBase httpUriRequest, int connectTimeout, int socketTimeout,
                                  boolean isSSL) throws Exception {
        final DefaultHttpClient client = getHttpClient(connectTimeout);
        try {
            FutureTask<String> fu = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String resultString = StringUtils.EMPTY;
                    HttpResponse response = client.execute(httpUriRequest);
                    HttpEntity entity = null;
                    try {
                        entity = response.getEntity();
                        StatusLine status = response.getStatusLine();
                        if (status != null && status.getStatusCode() == 200) {
                            resultString = EntityUtils.toString(entity, "UTF-8");
                            return resultString;
                        } else {
                        }
                    } catch (Exception e) {
                        throw e;
                    } finally {
                        EntityUtils.consume(entity);
                        entity = null;
                    }
                    return resultString;
                }
            });
            Executors.newSingleThreadExecutor().execute(fu);
            return fu.get(connectTimeout, TimeUnit.MILLISECONDS);
        }finally {
            if (client != null){
                client.close();
            }
        }
        // CloseableHttpAsyncClient httpclient = httpAsyncClientBuilder.build();
        // String result = StringUtils.EMPTY;
        // try {
        // httpclient.start();
        // Future<HttpResponse> future = httpclient.execute(httpUriRequest,
        // null);
        // HttpResponse response = future.get();
        // StatusLine status = response.getStatusLine();
        // if (status != null && status.getStatusCode() == 200) {
        // result = EntityUtils.toString(response.getEntity());
        // }
        // } finally {
        // httpclient.close();
        // }
        // return result;
    }

    public static String getContentByPost(final String monitorName, final HttpPost post, final String requestXml)
            throws Exception {
        long start = System.currentTimeMillis();
        FutureTask<String> fu = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String resultString = StringUtils.EMPTY;
                for (int i = 0; i < RETRY_TIMES; i++) {
                    HttpEntity entityReq = new StringEntity(requestXml, "UTF-8");
                    post.setEntity(entityReq);
                    HttpEntity responseEntity = null;
                    try {
//                        Interceptors.gzip(longHttpClient, true);
                        HttpResponse response = longHttpClient.execute(post);
                        responseEntity = response.getEntity();
                        StatusLine status = response.getStatusLine();
                        if (status != null && status.getStatusCode() == 200) {
                            resultString = EntityUtils.toString(responseEntity);
                            return resultString;
                        } else {
                            post.abort();
                        }
                    } catch (IOException ex) {
                        if (ex instanceof org.apache.http.conn.ConnectionPoolTimeoutException) {
                            log.error("ConnectionPoolTimeoutException=" + ex.getMessage(), ex);
                            post.releaseConnection();
                        }
                        post.releaseConnection();
                        log.warn(ex.getMessage());
                        throw ex;
                    } catch (Exception e) {
                        log.warn(e.getMessage());
                        post.releaseConnection();
                        throw e;
                    } finally {
                        EntityUtils.consume(responseEntity);
                        responseEntity = null;
                    }
                }
                return resultString;
            }
        });
        Executors.newSingleThreadExecutor().execute(fu);
        String content = null;
        try {
            content = fu.get(TIME_OUT_IN_MILLSECONDS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("TimeoutException=" + e.getMessage(), e);
            post.releaseConnection();
            throw e;
        }
        return content;
    }

    public static void main(String[] args) {
        String url = "http://54.244.116.90/dplatform-resbooking/action/qunarTtsHotelRate?hotelId=MAR-BJSBC&fromDate=2015-08-09&toDate=2015-08-10&usedFor=ORDER&count=1&roomId=CBXU|BAVB";
        try {
            String content = HttpUtil.getContent(url);
            System.out.println(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

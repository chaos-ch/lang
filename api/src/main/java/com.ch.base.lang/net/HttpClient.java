package com.ch.base.lang.net;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by he.chen on 5/3/16.
 */
public class HttpClient extends DefaultHttpClient{
    static Logger log = LoggerFactory.getLogger(HttpClient.class);

    public HttpClient(ClientConnectionManager conman, HttpParams params) {
        super(conman, params);
    }

    public HttpClient(HttpParams params) {
        this((ClientConnectionManager)null, params);
    }

    public HttpClient() {
        this((HttpParams)null);
    }

    public static HttpClient createDefaultClient() {
        BasicHttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 1000);
        HttpConnectionParams.setSoTimeout(params, 3000);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 6.1; WOW64) Chrome/27.0.1453.94 Safari/537.36 qunarhc/8.0.1");
        HttpClientParams.setCookiePolicy(params, "ignoreCookies");
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry, 1L, TimeUnit.MINUTES);
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(50);
        HttpClient client = new HttpClient(cm, params);
        return client;
    }

    public CloseableHttpResponse execute(HttpHost target, HttpRequest request) throws IOException {
        BasicHttpContext context = new BasicHttpContext();

        try {
            return this.doExecute(target, request, context);
        } catch (IOException var5) {
            this.traceException(context, var5);
            throw var5;
        }
    }

    public CloseableHttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        try {
            return super.execute(target, request, context);
        } catch (IOException var5) {
            this.traceException(context, var5);
            throw var5;
        }
    }

    public CloseableHttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
        Args.notNull(request, "HTTP request");

        try {
            return super.execute(request, context);
        } catch (IOException var4) {
            this.traceException(context, var4);
            throw var4;
        }
    }

    private void traceException(HttpContext context, Exception e) {
        if(context == null) {
            context = new BasicHttpContext();
        }


        try {
            Object obj = ((HttpContext)context).getAttribute("context_scope");
            if(obj != null) {
            }
        } finally {

        }

    }

}

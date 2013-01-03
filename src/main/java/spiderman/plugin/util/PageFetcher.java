/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spiderman.plugin.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParamBean;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.eweb4j.spiderman.fetcher.FetchResult;
import org.eweb4j.spiderman.fetcher.Page;
import org.eweb4j.spiderman.fetcher.Status;

/**
 * @author Yasser Ganjisaffar <lastname at gmail dot com>
 */
public class PageFetcher {

	private ThreadSafeClientConnManager connectionManager;

	private DefaultHttpClient httpClient;

	private final Object mutex = new Object();

	private long lastFetchTime = 0;
	
	private final SpiderConfig config;
	
	private final ConcurrentLinkedQueue<Cookie> cookies;
	
	public PageFetcher(SpiderConfig aconfig, final List<Cookie> cookies) {
		this.config = aconfig;
		this.cookies = new ConcurrentLinkedQueue<Cookie>(cookies);
		HttpParams params = new BasicHttpParams();
		HttpProtocolParamBean paramsBean = new HttpProtocolParamBean(params);
		paramsBean.setVersion(HttpVersion.HTTP_1_1);
		paramsBean.setContentCharset("UTF-8");
		paramsBean.setUseExpectContinue(false);

		params.setParameter(CoreProtocolPNames.USER_AGENT, config.getUserAgentString());
		params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, config.getSocketTimeout());
		params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, config.getConnectionTimeout());

//		params.setBooleanParameter("http.protocol.handle-redirects", true);
		
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));

		if (config.isIncludeHttpsPages()) {
			schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
		}

		connectionManager = new ThreadSafeClientConnManager(schemeRegistry);
		connectionManager.setMaxTotal(config.getMaxTotalConnections());
		connectionManager.setDefaultMaxPerRoute(config.getMaxConnectionsPerHost());
		httpClient = new DefaultHttpClient(connectionManager, params);
//		httpClient.getParams().setIntParameter("http.socket.timeout", 15000);
		for (Cookie cookie : cookies) {
			String name = cookie.name();
			String value = cookie.value();
			BasicClientCookie clientCookie = new BasicClientCookie(name, value);
			clientCookie.setPath(cookie.path());
			clientCookie.setDomain(cookie.domain());
			httpClient.getCookieStore().addCookie(clientCookie);
		}
		httpClient.getParams().setIntParameter("http.socket.timeout", 15000);
		
		if (config.getProxyHost() != null) {

			if (config.getProxyUsername() != null) {
				httpClient.getCredentialsProvider().setCredentials(
						new AuthScope(config.getProxyHost(), config.getProxyPort()),
						new UsernamePasswordCredentials(config.getProxyUsername(), config.getProxyPassword()));
			}

			HttpHost proxy = new HttpHost(config.getProxyHost(), config.getProxyPort());
			httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }

        httpClient.addResponseInterceptor(new HttpResponseInterceptor() {

            public void process(final HttpResponse response, final HttpContext context) throws HttpException,
                    IOException {
                HttpEntity entity = response.getEntity();
                Header contentEncoding = entity.getContentEncoding();
                if (contentEncoding != null) {
                    HeaderElement[] codecs = contentEncoding.getElements();
                    for (HeaderElement codec : codecs) {
                        if (codec.getName().equalsIgnoreCase("gzip")) {
                            response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                            return;
                        }
                    }
                }
            }

        });

	}

	public FetchResult fetchHeader(String webUrl) {
		FetchResult fetchResult = new FetchResult();
		String toFetchURL = webUrl;
		HttpGet get = null;
		HttpEntity entity = null;
		try {
			get = new HttpGet(toFetchURL);
			synchronized (mutex) {
				long now = (new Date()).getTime();
				if (now - lastFetchTime < config.getPolitenessDelay()) {
					Thread.sleep(config.getPolitenessDelay() - (now - lastFetchTime));
				}
				lastFetchTime = (new Date()).getTime();
			}
			get.addHeader("Accept-Encoding", "gzip");
			HttpResponse response = httpClient.execute(get);
			entity = response.getEntity();

			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				if (statusCode != HttpStatus.SC_NOT_FOUND) {
					if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
						Header header = response.getFirstHeader("Location");
						if (header != null) {
							String movedToUrl = header.getValue();
							movedToUrl = URLCanonicalizer.getCanonicalURL(movedToUrl, toFetchURL);
							fetchResult.setMovedToUrl(movedToUrl);
						} 
						fetchResult.setStatusCode(statusCode);
						return fetchResult;
					}
				}
				fetchResult.setStatusCode(response.getStatusLine().getStatusCode());
				return fetchResult;
			}

			fetchResult.setFetchedUrl(toFetchURL);
			String uri = get.getURI().toString();
			if (!uri.equals(toFetchURL)) {
				if (!URLCanonicalizer.getCanonicalURL(uri).equals(toFetchURL)) {
					fetchResult.setFetchedUrl(uri);
				}
			}

			if (entity != null) {
				fetchResult.setStatusCode(HttpStatus.SC_OK);
				Page page = load(entity);
				page.setUrl(fetchResult.getFetchedUrl());
				fetchResult.setPage(page);
				return fetchResult;
			} else {
				get.abort();
			}
		} catch (IOException e) {
			fetchResult.setStatusCode(Status.INTERNAL_SERVER_ERROR.ordinal());
			return fetchResult;
		} catch (IllegalStateException e) {
			// ignoring exceptions that occur because of not registering https
			// and other schemes
		} catch (Exception e) {
			if (e.getMessage() == null) {
			} else {
			}
		} finally {
			try {
				if (entity == null && get != null) {
					get.abort();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		fetchResult.setStatusCode(Status.UNSPECIFIED_ERROR.ordinal());
		return fetchResult;
	}
	
	private Page load(HttpEntity entity) throws Exception {
		Page page = new Page();
		String contentType = null;
		Header type = entity.getContentType();
		if (type != null) {
			contentType = type.getValue();
		}
		page.setContentType(contentType);
		
		String contentEncoding = null;
		Header encoding = entity.getContentEncoding();
		if (encoding != null) {
			contentEncoding = encoding.getValue();
		}
		page.setEncoding(contentEncoding);
		
		String contentCharset = EntityUtils.getContentCharSet(entity);
		page.setCharset(contentCharset);
		
//		byte[] contentData = read(entity.getContent());
//		page.setContentData(contentData);
		String charset = config.getCharset();
		String content = read(entity.getContent(), charset);
		page.setContent(content);
		if (charset == null || charset.trim().length() == 0)
			page.setContentData(content.getBytes());
		else
			page.setContentData(content.getBytes(charset));
		
		return page;
	}
	
	private String read(final InputStream inputStream, String charset) {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			if (charset == null || charset.trim().length() == 0)
				reader = new BufferedReader(new InputStreamReader(inputStream));
			else
				reader = new BufferedReader(new InputStreamReader(inputStream, charset));
			
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
		}

		return sb.toString();
	}
	
	private byte[] read(final InputStream inputStream) throws Exception {
		byte[] bytes = new byte[1000];
		int i = 0;
		int b;
		try {
			while ((b = inputStream.read()) != -1) {
				bytes[i++] = (byte) b;
				if (bytes.length == i) {
					byte[] newBytes = new byte[(bytes.length * 3) / 2 + 1];
					for (int j = 0; j < bytes.length; j++) {
						newBytes[j] = bytes[j];
					}
					bytes = newBytes;
				}
			}
		} catch (IOException e) {
			throw new Exception("There was a problem reading stream.", e);
		}

		byte[] copy = Arrays.copyOf(bytes, i);

		return copy;
	}


	public HttpClient getHttpClient() {
		return httpClient;
	}

	private static class GzipDecompressingEntity extends HttpEntityWrapper {

		public GzipDecompressingEntity(final HttpEntity entity) {
			super(entity);
		}

		@Override
		public InputStream getContent() throws IOException, IllegalStateException {

			// the wrapped entity's getContent() decides about repeatability
			InputStream wrappedin = wrappedEntity.getContent();

			return new GZIPInputStream(wrappedin);
		}

		@Override
		public long getContentLength() {
			// length of ungzipped content is not known
			return -1;
		}

	}
}

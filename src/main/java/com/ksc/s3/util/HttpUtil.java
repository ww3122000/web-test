package com.ksc.s3.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.Consts;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.BestMatchSpec;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

class AnyTrustStrategy implements TrustStrategy {

	public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		return true;
	}

}

public class HttpUtil {

	private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

	private static volatile HttpUtil instance;

	private ConnectionConfig connConfig;

	private SocketConfig socketConfig;

	private ConnectionSocketFactory plainSF;

	private KeyStore trustStore;

	private SSLContext sslContext;

	private LayeredConnectionSocketFactory sslSF;

	private Registry<ConnectionSocketFactory> registry;

	private PoolingHttpClientConnectionManager connManager;

	private volatile HttpClient client;

	private volatile BasicCookieStore cookieStore;

	public static String defaultEncoding = "utf-8";

	private static List<NameValuePair> paramsConverter(Map<String, String> params) {
		List<NameValuePair> nvps = new LinkedList<NameValuePair>();
		Set<Entry<String, String>> paramsSet = params.entrySet();
		for (Entry<String, String> paramEntry : paramsSet) {
			nvps.add(new BasicNameValuePair(paramEntry.getKey(), paramEntry.getValue()));
		}
		return nvps;
	}

	public static String readStream(InputStream in, String encoding) {
		if (in != null) {
			try {
				if (StringUtils.isBlank(encoding)) {
					encoding = defaultEncoding;
				}
				return IOUtils.toString(in, encoding);
			} catch (IOException e) {
				log.error("读取返回内容出错", e);
				log.error(Throwables.getStackTraceAsString(e));
			} finally {
				IOUtils.closeQuietly(in);
			}
		}
		return null;
	}
	
	/** 
     * 读取流 并丢弃数据
     *  
     * @param inStream 
     * @return 字节数组 
     * @throws Exception 
     */  
    public static void readStreamAndDiscard(InputStream inStream) throws Exception {  
        if(inStream != null){
            byte[] buffer = new byte[50 * 1024];  
            int len = -1;  
            while ((len = inStream.read(buffer)) != -1) { 
                Thread.sleep(1 * 10L);
            }  
            inStream.close();  
        } else {
            log.error("inStream is null");
        }
    } 
	

	private HttpUtil() {
		// 设置连接参数
		connConfig = ConnectionConfig.custom().setCharset(Charset.forName(defaultEncoding)).build();
		socketConfig = SocketConfig.custom()
		        .setSoTimeout(10 * 1000)  // 设置读数据超时时间(单位毫秒) 
		        .build();
		RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory> create();
		plainSF = new PlainConnectionSocketFactory();
		registryBuilder.register("http", plainSF);
		// 指定信任密钥存储对象和连接套接字工厂
		try {
			trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			sslContext = SSLContexts.custom().useTLS().loadTrustMaterial(trustStore, new AnyTrustStrategy()).build();
			sslSF = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			registryBuilder.register("https", sslSF);
		} catch (KeyStoreException | KeyManagementException | NoSuchAlgorithmException e) {
		    log.error(Throwables.getStackTraceAsString(e));
			throw new RuntimeException(e);
		}
		registry = registryBuilder.build();
		// 设置连接管理器
		connManager = new PoolingHttpClientConnectionManager(registry);
		
		connManager.setDefaultMaxPerRoute(1000);
		connManager.setMaxTotal(65535);
		
		connManager.setDefaultConnectionConfig(connConfig);
		connManager.setDefaultSocketConfig(socketConfig);
		// 指定cookie存储对象
		cookieStore = new BasicCookieStore();
		// 构建客户端
		client = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).setConnectionManager(connManager).build();
	}

	public static HttpUtil getInstance() {
		synchronized (HttpUtil.class) {
			if (HttpUtil.instance == null) {
				instance = new HttpUtil();
			}
			return instance;
		}
	}

	public InputStream doGet(String url) throws URISyntaxException {
		HttpResponse response = this.doGetWithRetry(url, null, 1, null, null);
		try {
			return response == null ? null : response.getEntity().getContent();
		} catch (IOException e) {
		    log.error(Throwables.getStackTraceAsString(e));
			return null;
		}
	}

	public String doGetRetString(String url) throws URISyntaxException {
		return HttpUtil.readStream(this.doGet(url), null);
	}
	
	public void doGetAndDiscard(String url) throws Exception {
	    HttpUtil.readStreamAndDiscard(this.doGet(url));
	}
	
	

	public InputStream doGet(String url, Map<String, String> queryParams) throws URISyntaxException {
		HttpResponse response = this.doGetWithRetry(url, queryParams, 1, null, null);
		try {
			return response == null ? null : response.getEntity().getContent();
		} catch (IOException e) {
		    log.error(Throwables.getStackTraceAsString(e));
			return null;
		}
	}

	public String doGetRetString(String url, Map<String, String> queryParams) throws URISyntaxException {
		return HttpUtil.readStream(this.doGet(url, queryParams), null);
	}

	/**
	 * 基本的Get请求
	 * 
	 * @param url
	 *            请求url
	 * @param queryParams
	 *            请求头的查询参数
	 * @param rangeStart
	 *            分块下载的开头字节下标, 为空时禁用断点续传
	 * @param rangeMax
	 *            分块下载的结尾字节下标, 小于0或小于rangeStart时不限定结尾,
	 * @return
	 * @throws URISyntaxException
	 */
	public HttpResponse doGetBasic(String url, Map<String, String> queryParams, Long rangeStart, Long rangeMax) throws URISyntaxException,
			ClientProtocolException, IOException {
		HttpGet gm = new HttpGet();
		URIBuilder builder = new URIBuilder(url);
		// 填入查询参数
		if (queryParams != null && queryParams.size() > 0) {
			builder.setParameters(HttpUtil.paramsConverter(queryParams));
		}
		gm.setURI(builder.build());
		gm.setHeader(HttpHeaders.HOST, getHost(url));
		// 填入头部断点续传字段
		do {
			if (rangeStart != null && rangeStart >= 0) {
				if (rangeMax != null || rangeMax < rangeStart) {
					gm.setHeader(HttpHeaders.RANGE, String.format("bytes=%d-", rangeStart));
					break;
				}
				if (rangeMax >= rangeStart) {
					gm.setHeader(HttpHeaders.RANGE, String.format("bytes=%d-%d", rangeStart, rangeMax));
					break;
				}
			}
		} while (false);
		log.debug("Http访问GET请求,URL:{}", url);
		return client.execute(gm);
	}

	private String getHost(String url) {
		if (url.startsWith("http://")) {
			url = url.substring("http://".length(), url.length());
		}
		if (url.startsWith("https://")) {
			url = url.substring("https://".length(), url.length());
		}
		if (url.indexOf(":") != -1) {
			url = url.substring(0, url.indexOf(":"));
		}
		if (url.indexOf("/") != -1) {
			url = url.substring(0, url.indexOf("/"));
		}
		return url;
	}

	/**
	 * 带有重试功能的Get请求
	 * 
	 * @param url
	 *            请求url
	 * @param queryParams
	 *            请求头的查询参数
	 * @param maxCount
	 *            最多尝试请求的次数
	 * @param rangeStart
	 *            分块下载的开头字节下标, 为空时禁用断点续传
	 * @param rangeMax
	 *            分块下载的结尾字节下标, 小于0或小于rangeStart时不限定结尾,
	 * @return
	 * @throws URISyntaxException
	 */
	public HttpResponse doGetWithRetry(String url, Map<String, String> queryParams, int maxCount, Long rangeStart, Long rangeMax)
			throws URISyntaxException {
		int retryCount = 0;
		// 失败重试
		while (retryCount < maxCount) {
			retryCount++;
			try {
				log.debug("正在尝试第{}次GET请求", retryCount);
				HttpResponse response = doGetBasic(url, queryParams, rangeStart, rangeMax);
				int status = response.getStatusLine().getStatusCode();
				log.debug("服务器状态:{}", status);
				if (status == HttpStatus.SC_OK || status == HttpStatus.SC_PARTIAL_CONTENT) {
					log.debug("GET请求成功");
					try {
						return response;
					} catch (Exception e) {
					    log.error(Throwables.getStackTraceAsString(e));
						log.error("返回数据流出错");
					}
					return null;
				}

				if (status == HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE) {
					log.error("指定传输区间超过目标大小");
					return response;
				}
			} catch (IOException e) {
			    log.error(Throwables.getStackTraceAsString(e));
				log.error("Http访问GET错误,错误信息:{}, 访问地址:{}", e.getMessage(), url);
			}
		}
		return null;
	}

	public InputStream doPost(String url, Map<String, String> queryParams) throws URISyntaxException {
		return this.doPostWithRetry(url, queryParams, null, 1);
	}

	public String doPostRetString(String url, Map<String, String> queryParams) throws URISyntaxException {
		return HttpUtil.readStream(this.doPost(url, queryParams), null);
	}

	public InputStream doPost(String url, Map<String, String> queryParams, Map<String, String> formParams) throws URISyntaxException {
		return this.doPostWithRetry(url, queryParams, formParams, 1);
	}

	public String doPostRetString(String url, Map<String, String> queryParams, Map<String, String> formParams) throws URISyntaxException {
		return HttpUtil.readStream(this.doPost(url, queryParams, formParams), null);
	}

	/**
	 * 基本Post请求
	 * 
	 * @param url
	 *            请求url
	 * @param queryParams
	 *            请求头的查询参数
	 * @param formParams
	 *            post表单的参数
	 * @return
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 */
	public HttpResponse doPostBasic(String url, Map<String, String> queryParams, Map<String, String> formParams) throws URISyntaxException,
			ClientProtocolException, IOException {
		HttpPost pm = new HttpPost();
		URIBuilder builder = new URIBuilder(url);
		// 填入查询参数
		if (queryParams != null && queryParams.size() > 0) {
			builder.setParameters(HttpUtil.paramsConverter(queryParams));
		}
		pm.setURI(builder.build());
		// 填入表单参数
		if (formParams != null && formParams.size() > 0) {
			pm.setEntity(new UrlEncodedFormEntity(HttpUtil.paramsConverter(formParams)));
		}
		log.debug("Http访问POST请求,URL:{}", url);
		return client.execute(pm);
	}

	/**
	 * 带有重试功能的Post请求
	 * 
	 * @param url
	 *            请求url
	 * @param queryParams
	 *            请求头的查询参数
	 * @param formParams
	 *            post表单的参数
	 * @param maxCount
	 *            最多尝试请求的次数
	 * @return
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 */
	public InputStream doPostWithRetry(String url, Map<String, String> queryParams, Map<String, String> formParams, int maxCount)
			throws URISyntaxException {
		int retryCount = 0;
		// 失败重试
		while (retryCount < maxCount) {
			retryCount++;
			try {
				log.debug("正在尝试第{}次POST请求", retryCount);
				HttpResponse response = doPostBasic(url, queryParams, formParams);
				int status = response.getStatusLine().getStatusCode();
				log.debug("服务器状态:{}", status);
				if (status == HttpStatus.SC_OK) {
					log.debug("POST请求成功");
					try {
						return response.getEntity().getContent();
					} catch (Exception e) {
					    log.error(Throwables.getStackTraceAsString(e));
						log.error("返回数据流出错");
					}
					return null;
				}
			}catch (IOException e) {
			    log.error(Throwables.getStackTraceAsString(e));
				log.error("Http传输POST错误,错误信息:{}, 访问地址:{}", e.getMessage(), url);
			}
		}
		return null;
	}

	/**
	 * 多块Post请求
	 * 
	 * @param url
	 *            请求url
	 * @param queryParams
	 *            请求头的查询参数
	 * @param formParts
	 *            post表单的参数,支持字符串-文件(FilePart)和字符串-字符串(StringPart)形式的参数
	 * @return
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public HttpResponse doMultipartPost(String url, Map<String, String> queryParams, List<FormBodyPart> formParts) throws URISyntaxException,
			ClientProtocolException, IOException {
		HttpPost pm = new HttpPost();
		URIBuilder builder = new URIBuilder(url);
		// 填入查询参数
		if (queryParams != null && queryParams.size() > 0) {
			builder.setParameters(HttpUtil.paramsConverter(queryParams));
		}
		pm.setURI(builder.build());
		// 填入表单参数
		if (formParts != null && formParts.size() > 0) {
			MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
			entityBuilder = entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			for (FormBodyPart formPart : formParts) {
				entityBuilder = entityBuilder.addPart(formPart.getName(), formPart.getBody());
			}
			pm.setEntity(entityBuilder.build());
		}
		return client.execute(pm);
	}

	/**
	 * 多块Post请求
	 * 
	 * @param url
	 *            请求url
	 * @param queryParams
	 *            请求头的查询参数
	 * @param formParts
	 *            post表单的参数,支持字符串-文件(FilePart)和字符串-字符串(StringPart)形式的参数
	 * @param maxCount
	 *            最多尝试请求的次数
	 * @return
	 * @throws URISyntaxException
	 * @throws HttpException
	 * @throws IOException
	 */
	public InputStream multipartPost(String url, Map<String, String> queryParams, List<FormBodyPart> formParts, int maxCount)
			throws URISyntaxException {
		HttpPost pm = new HttpPost();
		URIBuilder builder = new URIBuilder(url);
		// 填入查询参数
		if (queryParams != null && queryParams.size() > 0) {
			builder.setParameters(HttpUtil.paramsConverter(queryParams));
		}
		pm.setURI(builder.build());
		// 填入表单参数
		if (formParts != null && formParts.size() > 0) {
			MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
			entityBuilder = entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			for (FormBodyPart formPart : formParts) {
				entityBuilder = entityBuilder.addPart(formPart.getName(), formPart.getBody());
			}
			pm.setEntity(entityBuilder.build());
		}
		int retryCount = 0;
		// 失败重试
		while (retryCount < maxCount) {
			retryCount++;
			try {
				log.debug("Http访问POST请求,URL:{}", url);
				log.debug("正在尝试第{}次POST请求", retryCount);
				HttpResponse response = client.execute(pm);
				int status = response.getStatusLine().getStatusCode();
				log.debug("服务器状态:{}", status);
				if (status == HttpStatus.SC_OK) {
					log.debug("POST请求成功");
					retryCount = maxCount;
					try {
						return response.getEntity().getContent();
					} catch (Exception e) {
					    log.error(Throwables.getStackTraceAsString(e));
						log.error("返回数据流出错");
					}
				} else {
					return null;
				}
			} catch (IOException e) {
			    log.error("Http传输POST错误,错误信息:{}, 访问地址:{}", e.getMessage(), url);
			    log.error(Throwables.getStackTraceAsString(e));
			}
		}
		return null;
	}

	/**
	 * 多块Post请求
	 * 
	 * @param url
	 *            请求url
	 * @param queryParams
	 *            请求头的查询参数
	 * @param formParts
	 *            post表单的参数,支持字符串-文件(FilePart)和字符串-字符串(StringPart)形式的参数
	 * @param maxCount
	 *            最多尝试请求的次数
	 * @return
	 * @throws URISyntaxException
	 * @throws HttpException
	 * @throws IOException
	 */
	public static HttpResponse multipartPost(String url, Map<String, String> queryParams, Map<String, String> params, Map<String, String> headers,
			String name, File file, String fileName, int maxCount) throws URISyntaxException, IOException {
		HttpClient client = HttpClients.createDefault();
		HttpPost pm = new HttpPost();
		URIBuilder builder = new URIBuilder(url);
		// 填入查询参数
		if (queryParams != null && queryParams.size() > 0) {
			builder.setParameters(HttpUtil.paramsConverter(queryParams));
		}
		pm.setURI(builder.build());

		// 设置header
		for (Entry<String, String> enty : headers.entrySet()) {
			pm.setHeader(enty.getKey(), enty.getValue());
		}

		// 填入表单参数
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		if (StringUtils.isNotBlank(headers.get("Charset"))) {
			entityBuilder.setCharset(Charset.forName(headers.get("Charset")));
		}
		if (params != null && params.size() > 0) {
			entityBuilder = entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			for (Entry<String,String> entry : params.entrySet()) {
				entityBuilder = entityBuilder.addTextBody(entry.getKey(), entry.getValue(), ContentType.create("text/plain", Consts.UTF_8));
			}
		}
		entityBuilder.setBoundary("------------HV2ymHFg03ehbqgZCaKO6jyH");
		entityBuilder.addBinaryBody(name, file, ContentType.DEFAULT_BINARY, fileName);

		pm.setEntity(entityBuilder.build());
		int retryCount = 0;
		// 失败重试
		IOException ee = null;
		while (retryCount < maxCount) {
			retryCount++;
			try {
				log.debug("Http访问POST请求,URL:{}", url);
				log.debug("正在尝试第{}次POST请求", retryCount);
				HttpResponse response = client.execute(pm);
				return response;
			} catch (ClientProtocolException e) {
				log.error(ExceptionUtils.getStackTrace(e));
				ee = e;
				log.error("Http访问POST错误,错误代码:{}, 访问地址:{}", e.getMessage(), url);
			} catch (IOException e) {
				log.error(ExceptionUtils.getStackTrace(e));
				ee = e;
				log.error("Http传输POST错误,错误信息:{}, 访问地址:{}", e.getMessage(), url);
			}
		}
		if (ee != null) {
			throw ee;
		}
		return null;
	}

	/**
	 * 获取当前Http客户端状态中的Cookie
	 * 
	 * @param domain
	 *            作用域
	 * @param port
	 *            端口 传null 默认80
	 * @param path
	 *            Cookie路径 传null 默认"/"
	 * @param useSecure
	 *            Cookie是否采用安全机制 传null 默认false
	 * @return
	 */
	public Map<String, Cookie> getCookie(String domain, Integer port, String path, Boolean useSecure) {
		synchronized (cookieStore) {
			if (StringUtils.isBlank(domain)) {
				return null;
			}
			if (port == null) {
				port = 80;
			}
			if (StringUtils.isBlank(path)) {
				path = "/";
			}
			if (useSecure == null) {
				useSecure = false;
			}
			List<Cookie> cookies = cookieStore.getCookies();
			if (cookies == null || cookies.size() == 0) {
				return null;
			}

			CookieOrigin origin = new CookieOrigin(domain, port, path, useSecure);
			BestMatchSpec cookieSpec = new BestMatchSpec();
			Map<String, Cookie> retVal = new HashMap<String, Cookie>();
			for (Cookie cookie : cookies) {
				if (cookieSpec.match(cookie, origin)) {
					retVal.put(cookie.getName(), cookie);
				}
			}
			return retVal;
		}
	}

	/**
	 * 批量设置Cookie
	 * 
	 * @param cookies
	 *            cookie键值对图
	 * @param domain
	 *            作用域 不可为空
	 * @param path
	 *            路径 传null默认为"/"
	 * @param useSecure
	 *            是否使用安全机制 传null 默认为false
	 * @return 是否成功设置cookie
	 */
	public boolean setCookie(Map<String, String> cookies, String domain, String path, Boolean useSecure) {
		synchronized (cookieStore) {
			if (StringUtils.isBlank(domain)) {
				return false;
			}
			if (StringUtils.isBlank(path)) {
				path = "/";
			}
			if (useSecure == null) {
				useSecure = false;
			}
			if (cookies == null || cookies.size() == 0) {
				return true;
			}
			Set<Entry<String, String>> set = cookies.entrySet();
			String key = null;
			String value = null;
			for (Entry<String, String> entry : set) {
				key = entry.getKey();
				if (StringUtils.isBlank(key)) {
					return false;
				}
				value = entry.getValue();
				if (StringUtils.isBlank(value)) {
					return false;
				}
				BasicClientCookie cookie = new BasicClientCookie(key, value);
				cookie.setDomain(domain);
				cookie.setPath(path);
				cookie.setSecure(useSecure);
				cookieStore.addCookie(cookie);
			}
			return true;
		}

	}

	/**
	 * 设置单个Cookie
	 * 
	 * @param key
	 *            Cookie键
	 * @param value
	 *            Cookie值
	 * @param domain
	 *            作用域 不可为空
	 * @param path
	 *            路径 传null默认为"/"
	 * @param useSecure
	 *            是否使用安全机制 传null 默认为false
	 * @return 是否成功设置cookie
	 */
	public boolean setCookie(String key, String value, String domain, String path, Boolean useSecure) {
		Map<String, String> cookies = new HashMap<String, String>();
		cookies.put(key, value);
		return setCookie(cookies, domain, path, useSecure);
	}

	/**
	 * 上传文件 add by huofei 2014-7-2
	 * 
	 * @param url
	 *            上传的地址
	 * @param inputName
	 *            相当于表单input控件里的name属性
	 * @param fileName
	 *            上传文件时指定的文件名(浏览器中input控件会自动填入的值)
	 * @param file
	 *            真正要上传的文件
	 * @return 上传后返回的字符串
	 * @throws URISyntaxException
	 */
	public static String uploadFile(String url, String inputName, String fileName, File file) throws URISyntaxException {
		return uploadFile(url, inputName, fileName, file, 2);
	}

	/**
	 * 上传文件 add by chaijunkun 2014-10-14
	 * 
	 * @param url
	 *            上传的地址
	 * @param inputName
	 *            相当于表单input控件里的name属性
	 * @param fileName
	 *            上传文件时指定的文件名(浏览器中input控件会自动填入的值)
	 * @param file
	 *            真正要上传的文件
	 * @param maxCount
	 *            最多尝试次数
	 * @return 上传后返回的字符串
	 * @throws URISyntaxException
	 */
	public static String uploadFile(String url, String inputName, String fileName, File file, int maxCount) throws URISyntaxException {
		List<FormBodyPart> parts = new LinkedList<FormBodyPart>();
		InputStream in = null;
		try {
			// 从左至右参数含义
			// inputName 相当于表单input控件里的name属性
			// fileName 上传文件时指定的文件名(浏览器中input控件会自动填入的值)
			// file 真正要上传的文件
			parts.add(new FormBodyPart("file", new FileBody(file, ContentType.DEFAULT_BINARY, fileName)));

			HttpUtil util = getInstance();
			in = util.multipartPost(url, null, parts, maxCount);
			if (in == null) {
				log.debug("return null stream");
			} else {
				String retVal = readStream(in, null);
				log.debug("upload result:{}", retVal);
				return retVal;
			}
		} finally {
			IOUtils.closeQuietly(in);
		}
		return null;
	}

}

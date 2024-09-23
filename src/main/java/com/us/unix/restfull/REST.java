package com.us.unix.restfull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.us.unix.restfull.exceptions.*;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST class for handling HTTP requests and managing API connections.
 */
public class REST {
  static final Logger LOGGER = LogManager.getLogger(REST.class);
  private final String hostname;
  private String username;
  private String password;
  private String token = null;
  private final Boolean useSsl;
  private final Integer port;
  private final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
  private OkHttpClient client;
  private String credential;
  private boolean enableDebug;
  private final ObjectMapper mapper = new ObjectMapper();
  private int successStart = 200;
  private int successEnd = 299;
  private int permissionDeniedCode = 403;
  private int notFoundCode = 404;
  private int rateLimitCode = 429;
  private int serverErrorCode = 500;

  /**
   * The response code received from an HTTP request.
   * Typically used to assess the status of the HTTP operation.
   * Default value is 200, indicating a successful HTTP request.
   */
  public int responseCode = 200;

  /**
   * Holds the raw byte array of the response body from an HTTP request.
   * This field is populated after executing an HTTP call and contains
   * the received data from the server.
   */
  public byte[] responseBody;

  /**
   * This variable holds the request body used in HTTP requests.
   */
  public RequestBody requestBody;

  /**
   * Represents a list of JSON responses obtained from making REST API calls.
   */
  public ArrayNode responseList;

  /**
   * The JSON node containing the response data from an API request.
   */
  public JsonNode responseData;

  /**
   * The total number of items available across all pages when paginated data is retrieved from the API.
   * This variable helps in tracking how many items are available in total.
   */
  public int pagedTotal = 0;

  /**
   * A constant representing the MediaType for JSON, specifying the MIME type "application/json"
   * and the character set "utf-8".
   */
  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  /**
   * Initialize REST instance.
   *
   * @param hostname The hostname or IP address of the API.
   * @param username The username to use for Basic auth.
   * @param password The password to use for Basic auth.
   * @param useSsl Flag to indicate if the connection will use SSL.
   */
  public REST(String hostname, String username, String password, Boolean useSsl) {
    this.hostname = hostname;
    this.username = username;
    this.password = password;
    this.useSsl = useSsl;
    this.port = useSsl ? 443 : 80;
    this.enableDebug = false;
    this.init();
  }

  /**
   * Initialize REST instance.
   *
   * @param hostname The hostname or IP address of the API.
   * @param username The username to use for Basic auth.
   * @param password The password to use for Basic auth.
   * @param useSsl Flag to indicate if the connection will use SSL.
   * @param port Override the default port number.
   */
  public REST(String hostname, String username, String password, Boolean useSsl, Integer port) {
    this.hostname = hostname;
    this.username = username;
    this.password = password;
    this.useSsl = useSsl;
    this.port = port;
    this.enableDebug = false;
    this.init();
  }

  /**
   * Initialize REST instance.
   *
   * @param hostname The hostname or IP address of the API.
   * @param token The token to use for Bearer Token auth.
   * @param useSsl Flag to indicate if the connection will use SSL.
   */
  public REST(String hostname, String token, Boolean useSsl) {
    this.hostname = hostname;
    this.token = token;
    this.useSsl = useSsl;
    this.enableDebug = false;
    this.port = useSsl ? 443 : 80;
    this.init();
  }

  /**
   * Initializes the REST client's SSL context, configures timeout settings,
   * sets authentication credentials, and adds necessary interceptors.
   * <p>
   * Timeout settings for connection, read, and write operations are also configured to 20 seconds each.
   * Authentication credentials are set depending on whether a bearer token or basic authentication is used.
   * If debug mode is enabled, it enables an HTTP logging interceptor.
   * <p>
   * This method should be called during the instantiation of the REST class and not intended to be used directly by external code.
   * <p>
   * Preconditions:
   * - The hostname, username, password, and useSsl fields should be initialized prior to calling this method.
   * - The clientBuilder field must be initialized prior to this method; it is typically an instance of an HTTP client builder.
   * <p>
   * Postconditions:
   * - The clientBuilder is configured with SSL settings, authentication credentials, and optional logging interceptors.
   * - The fully configured client is built and assigned to the client field.
   * <p>
   * Throws:
   * - RuntimeException if the SSL context cannot be initialized or if there is an issue during the key management initialization.
   */
  public void init() {
    TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
          @Override
          public void checkClientTrusted(X509Certificate[] chain, String authType) {
          }

          @Override
          public void checkServerTrusted(X509Certificate[] chain, String authType) {
          }

          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
          }
        }
    };

    SSLContext sslContext;
    try {
      sslContext = SSLContext.getInstance("SSL");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    try {
      sslContext.init(null, trustAllCerts, new SecureRandom());
    } catch (KeyManagementException e) {
      throw new RuntimeException(e);
    }

    clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
    clientBuilder.hostnameVerifier((hostname, session) -> true);
    clientBuilder.connectTimeout(Duration.ofSeconds(20));
    clientBuilder.readTimeout(Duration.ofSeconds(20));
    clientBuilder.writeTimeout(Duration.ofSeconds(20));

    if (token != null) {
      credential = "Bearer " + token;
    } else {
      credential = Credentials.basic(username, password);
    }

    if (enableDebug) {
      HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
      logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
      clientBuilder.addInterceptor(logging);
    }

    client = clientBuilder.build();
  }

  /**
   * Reset REST instance.
   */
  public void reset() {
    this.responseCode = 200;
    this.responseBody = new byte[0];
    this.requestBody = null;
    this.responseList = null;
    this.responseData = null;
    this.pagedTotal = 0;
  }

  /**
   * Enables or disables the debug mode for the REST client.
   *
   * @param value A boolean indicating whether to enable (true) or disable (false) debug mode.
   * @return The current REST instance with the debug mode updated.
   */
  public REST enableDebug(boolean value) {
    this.enableDebug = value;
    return this;
  }

  /**
   * Sets the range of HTTP status codes that are considered successful.
   *
   * @param start The starting HTTP status code of the success range, inclusive.
   * @param end   The ending HTTP status code of the success range, inclusive.
   */
  public void setSuccessRange(int start, int end) {
    this.successStart = start;
    this.successEnd = end;
  }

  /**
   * Sets the HTTP status code that represents a "Permission Denied" response.
   *
   * @param code The HTTP status code indicating that permission is denied.
   */
  public void setPermissionDeniedCode(int code) {
    this.permissionDeniedCode = code;
  }

  /**
   * Sets the HTTP status code that represents a "Not Found" response.
   *
   * @param code The HTTP status code indicating that the resource was not found.
   */
  public void setNotFoundCode(int code) {
    this.notFoundCode = code;
  }

  /**
   * Sets the HTTP status code that represents a "Rate Limit Exceeded" response.
   *
   * @param code The HTTP status code indicating that the rate limit has been exceeded.
   */
  public void setRateLimitCode(int code) {
    this.rateLimitCode = code;
  }

  /**
   * Sets the HTTP status code that represents a server error.
   *
   * @param code The HTTP status code indicating a server error.
   */
  public void setServerErrorCode(int code) {
    this.serverErrorCode = code;
  }

  private void execHttpCall(Request request) {
    try {
      LOGGER.debug("Request: {}", request.url());
      try (Response response = client.newCall(request).execute()) {
        responseCode = response.code();
        responseBody = response.body() != null ? response.body().bytes() : new byte[0];
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * GET from endpoint.
   *
   * @param endpoint The hostname or IP address of the API.
   * @return REST instance.
   */
  public REST get(String endpoint) {
    execHttpCall(buildGetRequest(endpoint));
    return this;
  }

  /**
   * POST to endpoint with null body.
   *
   * @param endpoint The hostname or IP address of the API.
   * @return REST instance.
   */
  public REST post(String endpoint) {
    execHttpCall(buildPostRequest(endpoint));
    return this;
  }

  /**
   * POST to endpoint with data in body.
   *
   * @param endpoint The hostname or IP address of the API.
   * @param json JSON body.
   * @return REST instance.
   */
  public REST post(String endpoint, JsonNode json) {
    RequestBody requestBody = RequestBody.create(json.toString(), JSON);
    execHttpCall(buildPostRequest(endpoint, requestBody));
    return this;
  }

  /**
   * PUT to endpoint with null body.
   *
   * @param endpoint The hostname or IP address of the API.
   * @return REST instance.
   */
  public REST put(String endpoint) {
    execHttpCall(buildPutRequest(endpoint));
    return this;
  }

  /**
   * PUT to endpoint with data in body.
   *
   * @param endpoint The hostname or IP address of the API.
   * @param json JSON body.
   * @return REST instance.
   */
  public REST put(String endpoint, JsonNode json) {
    RequestBody requestBody = RequestBody.create(json.toString(), JSON);
    execHttpCall(buildPutRequest(endpoint, requestBody));
    return this;
  }

  /**
   * PATCH endpoint.
   *
   * @param endpoint The hostname or IP address of the API.
   * @param json JSON body.
   * @return REST instance.
   */
  public REST patch(String endpoint, JsonNode json) {
    RequestBody requestBody = RequestBody.create(json.toString(), JSON);
    execHttpCall(buildPatchRequest(endpoint, requestBody));
    return this;
  }

  /**
   * DELETE endpoint.
   *
   * @param endpoint The hostname or IP address of the API.
   * @return REST instance.
   */
  public REST delete(String endpoint) {
    execHttpCall(buildDeleteRequest(endpoint));
    return this;
  }

  /**
   * Return JSON result.
   *
   * @return JSON data.
   */
  public JsonNode json() {
    try {
      return mapper.readTree(new String(responseBody));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return JSON array result.
   *
   * @return JSON list.
   */
  public ArrayNode jsonArray() {
    try {
      return (ArrayNode) mapper.readTree(new String(responseBody));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return JSON data object list.
   *
   * @return JSON list value for data key.
   */
  public ArrayNode jsonList() {
    return responseList;
  }

  /**
   * Return JSON data object list.
   *
   * @param dataKey The data key name.
   * @return JSON list value for data key.
   */
  public ArrayNode jsonList(String dataKey) {
    return responseData.get(dataKey).deepCopy();
  }

  /**
   * Return JSON data object.
   *
   * @return JSON data.
   */
  public JsonNode jsonData() {
    return responseData;
  }

  /**
   * Return JSON data object.
   *
   * @param dataKey The data key name.
   * @return JSON data.
   */
  public JsonNode jsonData(String dataKey) {
    return responseData.get(dataKey);
  }

  /**
   * Returns the total number of pages from the REST API response.
   *
   * @return The total number of pages available.
   */
  public int pageCount() {
    return pagedTotal;
  }

  /**
   * Searches for the specified key in a JSON structure and returns a list of values associated with that key.
   *
   * @param key the key to search for in the JSON structure
   * @return a list of values associated with the specified key
   */
  public List<String> jsonSearch(String key) {
    return json().findValuesAsText(key);
  }

  /**
   * Sets the JSON body of the REST request.
   *
   * @param json the JSON content to be set as the body of the request
   * @return the current instance of the REST object with the updated JSON body
   */
  public REST jsonBody(JsonNode json) {
    requestBody = RequestBody.create(json.toString(), JSON);
    return this;
  }

  /**
   * Retrieves the response code.
   *
   * @return the response code as an integer.
   */
  public int code() {
    return responseCode;
  }

  /**
   * Validate response code.
   *
   * @return REST instance.
   * @throws HttpResponseException if response code is not within success range.
   */
  public REST validate() throws HttpResponseException {
    if (this.responseCode >= successStart && this.responseCode < successEnd) {
      return this;
    } else if (this.responseCode == permissionDeniedCode) {
      throw new PermissionDeniedError(new String(responseBody));
    } else if (this.responseCode == notFoundCode) {
      throw new NotFoundError(new String(responseBody));
    } else if (this.responseCode == rateLimitCode) {
      throw new RateLimitError(new String(responseBody));
    } else if (this.responseCode == serverErrorCode) {
      throw new InternalServerError(new String(responseBody));
    } else if (this.responseCode >= 400 && this.responseCode < 500) {
      throw new RetryableError(String.format("code: %d response: %s", this.responseCode, new String(responseBody)));
    } else {
      throw new NonRetryableError(String.format("code: %d response: %s", this.responseCode, new String(responseBody)));
    }
  }

  /**
   * Waits for a specific JSON value at a given endpoint. This method sends repeated
   * requests to the endpoint and checks if the JSON value associated with the provided key
   * matches the expected value. It retries the request for a specified number of times.
   *
   * @param endpoint The URL of the endpoint to send the request to.
   * @param key The JSON key to look for in the response.
   * @param value The expected value associated with the specified key.
   * @param retryCount The number of times to retry the request if the desired value is not found.
   * @return true if the expected value is found within the specified number of retries, false otherwise.
   * @throws HttpResponseException if there is an error in the HTTP response.
   */
  public boolean waitForJsonValue(String endpoint, String key, String value, int retryCount) throws HttpResponseException {
    long waitFactor = 100L;
    for (int retryNumber = 1; retryNumber <= retryCount; retryNumber++) {
      JsonNode response = get(endpoint).validate().json();
      String result = response.get(key).toString();
      if (result.equals(value)) {
        return true;
      }
      try {
        Thread.sleep(waitFactor);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
    return false;
  }

  /**
   * Waits for a specific HTTP response code by repeatedly polling an endpoint.
   *
   * @param endpoint The URL endpoint to be queried.
   * @param code The HTTP response code to wait for.
   * @param retryCount The number of times to retry before giving up.
   * @return true if the desired response code is received within the given number of retries; false otherwise.
   */
  public boolean waitForCode(String endpoint, int code, int retryCount) {
    long waitFactor = 100L;
    for (int retryNumber = 1; retryNumber <= retryCount; retryNumber++) {
      int result = get(endpoint).code();
      if (result == code) {
        return true;
      }
      try {
        Thread.sleep(waitFactor);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
    return false;
  }

  /**
   * Builds a complete URL using the provided endpoint.
   *
   * @param endpoint the endpoint to be added to the base URL.
   * @return the complete HttpUrl object.
   */
  public HttpUrl buildUrl(String endpoint) {
    HttpUrl.Builder builder = new HttpUrl.Builder();
    return builder.scheme(useSsl ? "https" : "http")
        .host(hostname)
        .port(port)
        .addPathSegments(endpoint.replaceAll("^/+", ""))
        .build();
  }

  /**
   * Constructs a basic HTTP request with the specified method and URL.
   *
   * @param method the HTTP method to use (e.g., "GET", "POST")
   * @param url the URL to which the request will be sent
   * @return a Request object representing the constructed HTTP request
   */
  public Request basicRequest(String method, HttpUrl url) {
    return new Request.Builder()
        .url(url)
        .header("Authorization", credential)
        .method(method, null)
        .build();
  }

  /**
   * Builds a GET request for the given endpoint.
   *
   * @param endpoint The API endpoint for the GET request.
   * @return A Request object configured with the specified endpoint and authorization header.
   */
  public Request buildGetRequest(String endpoint) {
    HttpUrl url = buildUrl(endpoint);
    LOGGER.debug("buildGetRequest: {}", url);
    return new Request.Builder()
        .url(url)
        .header("Authorization", credential)
        .build();
  }

  /**
   * Constructs a POST request targeted at the specified endpoint with an empty body.
   *
   * @param endpoint the URL endpoint to which the POST request is to be sent
   * @return A new Request object representing the constructed POST request
   */
  public Request buildPostRequest(String endpoint) {
    HttpUrl url = buildUrl(endpoint);
    LOGGER.debug("buildPostRequest (null body): {}", url);
    return new Request.Builder()
        .url(url)
        .method("POST", null)
        .header("Authorization", credential)
        .build();
  }

  /**
   * Constructs a POST request to the specified endpoint with the provided request body.
   *
   * @param endpoint The API endpoint to which the request should be made.
   * @param body The request body to be sent with the POST request.
   * @return A new Request object representing the POST request.
   */
  public Request buildPostRequest(String endpoint, RequestBody body) {
    HttpUrl url = buildUrl(endpoint);
    LOGGER.debug("buildPostRequest: {}", url);
    return new Request.Builder()
        .url(url)
        .post(body)
        .header("Authorization", credential)
        .build();
  }

  /**
   * Builds a PUT request for the given endpoint with an empty body.
   *
   * @param endpoint The API endpoint to which the PUT request should be sent.
   * @return A new Request object representing the PUT request.
   */
  public Request buildPutRequest(String endpoint) {
    HttpUrl url = buildUrl(endpoint);
    LOGGER.debug("buildPutRequest (null body): {}", url);
    return new Request.Builder()
        .url(url)
        .method("PUT", null)
        .header("Authorization", credential)
        .build();
  }

  /**
   * Builds a PUT request to the specified endpoint with the provided request body.
   *
   * @param endpoint The endpoint for the PUT request.
   * @param body The request body to be sent with the PUT request.
   * @return A new Request object representing the PUT request.
   */
  public Request buildPutRequest(String endpoint, RequestBody body) {
    HttpUrl url = buildUrl(endpoint);
    LOGGER.debug("buildPutRequest: {}", url);
    return new Request.Builder()
        .url(url)
        .put(body)
        .header("Authorization", credential)
        .build();
  }

  /**
   * Constructs a PATCH request for a given endpoint.
   *
   * @param endpoint the API endpoint to which the PATCH request will be sent
   * @param body the body content to be included in the PATCH request
   * @return A new Request object representing the PATCH request.
   */
  public Request buildPatchRequest(String endpoint, RequestBody body) {
    HttpUrl url = buildUrl(endpoint);
    LOGGER.debug("buildPatchRequest: {}", url);
    return new Request.Builder()
        .url(url)
        .patch(body)
        .header("Authorization", credential)
        .build();
  }

  /**
   * Constructs a DELETE request to the specified endpoint.
   *
   * @param endpoint the target API endpoint for the DELETE request
   * @return A new Request object representing the DELETE request.
   */
  public Request buildDeleteRequest(String endpoint) {
    HttpUrl url = buildUrl(endpoint);
    LOGGER.debug("buildDeleteRequest: {}", url);
    return new Request.Builder()
        .url(url)
        .delete()
        .header("Authorization", credential)
        .build();
  }

  /**
   * Fetches data from a paged API endpoint and returns the data as a single aggregated ArrayNode.
   *
   * @param endpoint the API endpoint to request data from
   * @param pageTag the tag used to denote page number in the request
   * @param totalTag the tag used to denote the total number of items (optional)
   * @param pagesTag the tag used to denote the number of pages
   * @param perPageTag the tag used to denote the number of items per page
   * @param perPage the number of items to request per page
   * @param dataKey the key to retrieve the data array from the response
   * @param cursor the cursor tag to navigate through paginated data (optional)
   * @param category the category tag within the data (optional)
   * @return a CompletableFuture containing an aggregated ArrayNode of all data from the paged endpoint
   */
  public CompletableFuture<ArrayNode> getPagedEndpoint(String endpoint, String pageTag, String totalTag, String pagesTag, String perPageTag, int perPage, String dataKey, String cursor, String category) {
    return getByPage(endpoint, pageTag, 1, perPageTag, perPage)
        .thenCompose(initialResponse -> {
          if (!initialResponse.has(dataKey)) {
            return CompletableFuture.completedFuture(mapper.createArrayNode());
          }
          ArrayNode data = initialResponse.get(dataKey).deepCopy();
          JsonNode record = initialResponse;
          if (cursor != null) {
            record = initialResponse.get(cursor);
          }
          if (category != null) {
            record = record.get(category);
          }
          int pages = record.get(pagesTag).asInt();
          if (totalTag != null && record.has(totalTag))
            pagedTotal = record.get(totalTag).asInt();

          if (pages > 1) {
            List<CompletableFuture<Void>> subsequentRequests = new ArrayList<>();
            for (int page = 2; page <= pages; page++) {
              subsequentRequests.add(getByPage(endpoint, pageTag, page, perPageTag, perPage)
                  .thenAccept(response -> {
                    if (response.has(dataKey)) {
                      ArrayNode newData = response.get(dataKey).deepCopy();
                      data.addAll(newData);
                    }
                  }));
            }

            return CompletableFuture.allOf(subsequentRequests.toArray(new CompletableFuture[0]))
                .thenApply(v -> data);
          } else {
            return CompletableFuture.completedFuture(data);
          }
        });
  }

  /**
   * Retrieves data from the specified endpoint for a given page.
   *
   * @param endpoint The API endpoint to send the request to.
   * @param page The page number to retrieve from the endpoint.
   * @return The REST object containing the response data after fetching the specified page.
   */
  public REST getPage(String endpoint, int page) {
    this.responseData = getByPage(endpoint, "page", page, "per_page", 10).join();
    return this;
  }

  /**
   * Retrieves a specific page of data from a given endpoint.
   *
   * @param endpoint the API endpoint to fetch data from
   * @param pageTag the tag used in the API to denote the page number
   * @param page the page number to retrieve
   * @return the current instance of the REST object with updated response data
   */
  public REST getPage(String endpoint, String pageTag, int page) {
    this.responseData = getByPage(endpoint, pageTag, page, "per_page", 10).join();
    return this;
  }

  /**
   * Retrieves a specific page of data from a given endpoint.
   *
   * @param endpoint The API endpoint to request data from.
   * @param pageTag The tag used in the URL to indicate the page number.
   * @param page The page number to retrieve.
   * @param perPageTag The tag used in the URL to indicate the number of items per page.
   * @return The current instance of the REST object.
   */
  public REST getPage(String endpoint, String pageTag, int page, String perPageTag) {
    this.responseData = getByPage(endpoint, pageTag, page, perPageTag, 10).join();
    return this;
  }

  /**
   * Gets a specific page from an endpoint.
   *
   * @param endpoint The endpoint to query.
   * @param pageTag The tag used to specify the page number.
   * @param page The specific page number to retrieve.
   * @param perPageTag The tag used to specify the number of items per page.
   * @param perPage The number of items to retrieve per page.
   * @return A REST object containing the retrieved page data.
   */
  public REST getPage(String endpoint, String pageTag, int page, String perPageTag, int perPage) {
    this.responseData = getByPage(endpoint, pageTag, page, perPageTag, perPage).join();
    return this;
  }

  /**
   * Fetches a paged response from the given endpoint with the specified parameters.
   *
   * @param endpoint The API endpoint to fetch data from.
   * @param pageTag The parameter name for the current page number in the request.
   * @param pagesTag The parameter name for the total number of pages in the response.
   * @param perPageTag The parameter name for the number of items per page.
   * @return The current instance of the REST class with the paged response list.
   */
  public REST getPaged(String endpoint, String pageTag, String pagesTag, String perPageTag) {
    this.responseList = getPagedEndpoint(endpoint, pageTag, "total", pagesTag, perPageTag, 10, "data", null, null).join();
    return this;
  }

  /**
   * Fetches paginated data from the specified endpoint and updates the response list.
   *
   * @param endpoint The API endpoint to fetch data from.
   * @param pageTag The parameter name in the API that specifies the current page number.
   * @param totalTag The parameter name in the API that specifies the total number of records.
   * @param pagesTag The parameter name in the API that specifies the total number of pages.
   * @param perPageTag The parameter name in the API that specifies the number of records per page.
   * @return The updated REST instance with the fetched paginated data.
   */
  public REST getPaged(String endpoint, String pageTag, String totalTag, String pagesTag, String perPageTag) {
    this.responseList = getPagedEndpoint(endpoint, pageTag, totalTag, pagesTag, perPageTag, 10, "data", null, null).join();
    return this;
  }

  /**
   * Retrieves paginated data from the specified endpoint.
   *
   * @param endpoint The API endpoint from which to fetch the paginated data.
   * @param pageTag The tag that represents the current page number in the API response.
   * @param totalTag The tag that represents the total number of items in the API response.
   * @param pagesTag The tag that represents the total number of pages in the API response.
   * @param perPageTag The tag that represents the number of items per page in the API response.
   * @param perPage The number of items to request per page.
   * @return The REST object with the paginated response data.
   */
  public REST getPaged(String endpoint, String pageTag, String totalTag, String pagesTag, String perPageTag, int perPage) {
    this.responseList = getPagedEndpoint(endpoint, pageTag, totalTag, pagesTag, perPageTag, perPage, "data", null, null).join();
    return this;
  }

  /**
   * Retrieves paginated data from the specified endpoint and stores the results in the responseList.
   *
   * @param endpoint The API endpoint to retrieve data from.
   * @param pageTag The JSON key that represents the current page number.
   * @param totalTag The JSON key that represents the total number of items.
   * @param pagesTag The JSON key that represents the total number of pages.
   * @param perPageTag The JSON key that represents the number of items per page.
   * @param perPage The number of items to retrieve per page.
   * @param dataKey The JSON key that contains the relevant data items.
   * @return The current REST object instance with the paginated data stored in responseList.
   */
  public REST getPaged(String endpoint, String pageTag, String totalTag, String pagesTag, String perPageTag, int perPage, String dataKey) {
    this.responseList = getPagedEndpoint(endpoint, pageTag, totalTag, pagesTag, perPageTag, perPage, dataKey, null, null).join();
    return this;
  }

  /**
   * Retrieves paginated data from a specified endpoint and stores the response in the responseList.
   *
   * @param endpoint The URL of the endpoint to request data from.
   * @param pageTag The tag used to identify the current page in the response.
   * @param totalTag The tag used to identify the total number of items in the response.
   * @param pagesTag The tag used to identify the total number of pages in the response.
   * @param perPageTag The tag used to identify the number of items per page in the response.
   * @param perPage The number of items to be retrieved per page.
   * @param dataKey The key used to access the data within the response.
   * @param cursor The cursor used to hold the paginated data structure.
   * @return The current instance of the REST class containing the paginated data in the responseList.
   */
  public REST getPaged(String endpoint, String pageTag, String totalTag, String pagesTag, String perPageTag, int perPage, String dataKey, String cursor) {
    this.responseList = getPagedEndpoint(endpoint, pageTag, totalTag, pagesTag, perPageTag, perPage, dataKey, cursor, null).join();
    return this;
  }

  /**
   * Retrieves paged data from the specified endpoint and updates the response list.
   *
   * @param endpoint the REST API endpoint to query.
   * @param pageTag the tag used to identify the current page in the response.
   * @param totalTag the tag used to identify the total number of records in the response.
   * @param pagesTag the tag used to identify the total number of pages in the response.
   * @param perPageTag the tag used to identify the number of records per page in the response.
   * @param perPage the number of records to fetch per page.
   * @param dataKey the key used to extract data from the response.
   * @param cursor The cursor used to hold the paginated data structure.
   * @param category The category is the structure within the cursor that holds the pagination data.
   * @return the current instance of the REST class after updating the response list.
   */
  public REST getPaged(String endpoint, String pageTag, String totalTag, String pagesTag, String perPageTag, int perPage, String dataKey, String cursor, String category) {
    this.responseList = getPagedEndpoint(endpoint, pageTag, totalTag, pagesTag, perPageTag, perPage, dataKey, cursor, category).join();
    return this;
  }

  /**
   * Retrieves paginated JSON data from the specified endpoint.
   *
   * @param endpoint The base URL of the API endpoint.
   * @param pageTag The query parameter name for the page number.
   * @param page The page number to retrieve.
   * @param perPageTag The query parameter name for the number of items per page.
   * @param perPage The number of items per page.
   * @return A CompletableFuture that, when completed, contains a JsonNode representing the paginated data.
   */
  public CompletableFuture<JsonNode> getByPage(String endpoint, String pageTag, int page, String perPageTag, int perPage) {
    HttpUrl url = pagedEndpoint(endpoint, pageTag, page, perPageTag, perPage);
    return getDataAsync(url)
        .thenCompose(response -> {
          JsonNode json = mapper.createObjectNode();
          responseCode = responseCode < 400 ? response.code() : responseCode;
          try {
            byte[] body = response.body() != null ? response.body().bytes() : "{}".getBytes();
            responseBody = body;
            json = new ObjectMapper().readTree(new String(body));
          } catch (IOException e) {
            LOGGER.debug("getByPage: {}", e.getMessage());
          }
          return CompletableFuture.completedFuture(json);
        });
  }

  /**
   * Initiates an asynchronous GET request to the specified URL and returns
   * a CompletableFuture that will be completed with the response.
   *
   * @param url the URL to which the GET request is sent
   * @return a CompletableFuture that will contain the response
   */
  public CompletableFuture<Response> getDataAsync(HttpUrl url) {
    OkHttpResponseFuture callback = new OkHttpResponseFuture();
    Request request = basicRequest("GET", url);
    client.newCall(request).enqueue(callback);
    return callback.future.thenCompose(CompletableFuture::completedFuture);
  }

  /**
   * Constructs a paged API endpoint URL with the specified parameters.
   *
   * @param endpoint the base URL path of the API endpoint.
   * @param pageTag the query parameter name for the page number.
   * @param page the page number to request.
   * @param perPageTag the query parameter name for the items per page count.
   * @param perPage the number of items to display per page.
   * @return the constructed URL with the specified paging parameters.
   */
  public HttpUrl pagedEndpoint(String endpoint, String pageTag, int page, String perPageTag, int perPage) {
    HttpUrl.Builder builder = new HttpUrl.Builder();
    builder.scheme(useSsl ? "https" : "http")
        .host(hostname)
        .port(port)
        .addPathSegments(endpoint.replaceAll("^/+", ""))
        .addQueryParameter(pageTag, Integer.toString(page));
    if (perPageTag != null && perPage > 0)
        builder.addQueryParameter(perPageTag, Integer.toString(perPage));
    return builder.build();
  }
}

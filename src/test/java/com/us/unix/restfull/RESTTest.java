package com.us.unix.restfull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.us.unix.restfull.exceptions.HttpResponseException;
import com.us.unix.restfull.exceptions.NotFoundError;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RESTTest {
  static final Logger LOGGER = LogManager.getLogger(RESTTest.class);
  public boolean enableDebug = true;
  public String username = "username";
  public String password = "password";
  public String hostname = "reqres.in";
  public boolean useSsl = true;
  ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testRest1() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users/1";
    try {
      JsonNode results = client.get(endpoint).validate().json();
      Assertions.assertTrue(results.has("data"));
      Assertions.assertTrue(results.get("data").has("id"));
      Assertions.assertEquals(1, results.get("data").get("id").asInt());
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
  }

  @Test
  public void testRest2() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users";
    try {
      JsonNode result = client.getPage(endpoint, 1).validate().jsonList("data").get(8);
      Assertions.assertTrue(result.has("id"));
      Assertions.assertEquals(9, result.get("id").asInt());
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
  }

  @Test
  public void testRest3() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users";
    try {
      int count = client.getPaged(endpoint, "page", "total_pages", "per_page").validate().pageCount();
      Assertions.assertEquals(12, count);
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
  }

  @Test
  public void testRest4() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users/23";
    try {
      client.get(endpoint).validate().json();
    } catch (NotFoundError e) {
      LOGGER.info("testRest4: pass");
      return;
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
    Assertions.fail();
  }

  @Test
  public void testRest5() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users";
    try {
      ObjectNode body = mapper.createObjectNode();
      body.put("name", "morpheus");
      body.put("job", "leader");
      JsonNode result = client.post(endpoint, body).validate().json();
      Assertions.assertEquals("morpheus", result.get("name").asText());
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
  }

  @Test
  public void testRest6() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users/2";
    try {
      ObjectNode body = mapper.createObjectNode();
      body.put("name", "morpheus");
      body.put("job", "leader");
      JsonNode result = client.put(endpoint, body).validate().json();
      Assertions.assertEquals("morpheus", result.get("name").asText());
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
  }

  @Test
  public void testRest7() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users/2";
    try {
      ObjectNode body = mapper.createObjectNode();
      body.put("name", "morpheus");
      body.put("job", "leader");
      JsonNode result = client.patch(endpoint, body).validate().json();
      Assertions.assertEquals("morpheus", result.get("name").asText());
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
  }

  @Test
  public void testRest8() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users/2";
    try {
      int code = client.delete(endpoint).validate().code();
      Assertions.assertEquals(204, code);
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
  }

  @Test
  public void testRest9() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users/1";
    try {
      JsonNode results = client.get(endpoint).validate().json().get("data");
      Assertions.assertEquals(1, results.get("id").asInt());
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
  }

  @Test
  public void testRest10() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users";
    try {
      ArrayNode result = client.getPage(endpoint, 1).validate().jsonList("data");
      Assertions.assertEquals(10, result.size());
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
  }

  @Test
  public void testRest11() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users";
    try {
      ArrayNode result = client.getPaged(endpoint, "page", "total_pages", "per_page").validate().jsonList();
      Assertions.assertEquals(12, result.size());
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
  }

  @Test
  public void testRest12() {
    REST client = new REST(hostname, username, password, useSsl).enableDebug(enableDebug);
    String endpoint = "/api/users/23";
    try {
      ArrayNode result = client.getPaged(endpoint, "page", "total_pages", "per_page").validate().jsonList();
      Assertions.assertEquals(12, result.size());
    } catch (NotFoundError e) {
      LOGGER.info("testRest12: pass");
      return;
    } catch (HttpResponseException e) {
      LOGGER.error(e.getMessage(), e);
      Assertions.fail();
    }
    Assertions.fail();
  }
}

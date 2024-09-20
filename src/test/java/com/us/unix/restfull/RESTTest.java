package com.us.unix.restfull;

import com.fasterxml.jackson.databind.JsonNode;
import com.us.unix.restfull.exceptions.HttpResponseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RESTTest {
  public boolean enableDebug = true;
  public String username = "username";
  public String password = "password";
  public String hostname = "reqres.in";
  public boolean useSsl = true;

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
      throw new RuntimeException(e);
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
      throw new RuntimeException(e);
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
      throw new RuntimeException(e);
    }
  }
}

package iudx.catalogue.server.apiserver;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResponseHandler {

  private final String status;
  private JsonArray results = new JsonArray();

  /**
   * Handles the response format to manage Uniformity of the response body.
   * 
   * @param status is a string based error status like: invalidValue or invalidSyntax
   * @param results is a JsonArray, may contains other response related values
   */
  public ResponseHandler(String status, JsonArray results) {
    super();
    this.status = status;
    this.results = results;
  }

  /**
   * Creates a JsonObject of status and results.
   * 
   * @return json of status and results
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("status", status);
    json.put("results", results);
    return json;
  }

  public String toJsonString() {
    return toJson().toString();
  }

  public static class Builder {
    private String status;
    private JsonArray results = new JsonArray();

    public Builder() {}

    public Builder withStatus(String status) {
      this.status = status;
      return this;
    }

    /**
     * Builds the results JsonArray.
     * 
     * @param results is a JsonArrays, may contains other response related values
     * @return all properties
     */
    public Builder withResults(JsonArray results) {
      if (results == null) {
        this.results = new JsonArray();
      } else {
        this.results = results;
      }
      return this;
    }

    public ResponseHandler build() {
      return new ResponseHandler(status, results);
    }

  }

}

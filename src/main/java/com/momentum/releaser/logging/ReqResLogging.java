package com.momentum.releaser.logging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.util.Map;

public class ReqResLogging {

    // 추적 식별 번호
    @JsonProperty(value = "trace_id")
    String traceId;

    // 클래스 이름
    @JsonProperty(value = "class")
    String className;

    // HTTP method
    @JsonProperty(value = "http_method")
    String httpMethod;

    // URI
    @JsonProperty(value = "uri")
    String uri;

    @JsonProperty(value = "method")
    String method;

    @JsonProperty(value = "params")
    Map<String, String> params;

    @JsonProperty(value = "log_time")
    String logTime;

    @JsonProperty(value = "server_ip")
    String serverIp;

    @JsonProperty(value = "device_type")
    String deviceType;

    @JsonProperty(value = "request_body")
    JsonNode requestBody;

    @JsonProperty(value = "response_body")
    JsonNode responseBody;

    @JsonProperty(value = "elapsed_time")
    String elapsedTime;

    @Builder
    public ReqResLogging(String traceId, String className, String httpMethod, String uri, String method, Map<String, String> params, String logTime, String serverIp, String deviceType, JsonNode requestBody, JsonNode responseBody, String elapsedTime) {
        this.traceId = traceId;
        this.className = className;
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.method = method;
        this.params = params;
        this.logTime = logTime;
        this.serverIp = serverIp;
        this.deviceType = deviceType;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
        this.elapsedTime = elapsedTime;
    }

    public ReqResLogging copyWithOther(JsonNode responseBody, String elapsedTime) {
        return new ReqResLogging(
                this.traceId,
                this.className,
                this.httpMethod,
                this.uri,
                this.method,
                this.params,
                this.logTime,
                this.serverIp,
                this.deviceType,
                this.requestBody,
                responseBody,
                elapsedTime);
    }

    void updateResponseBody(JsonNode responseBody) {
        this.responseBody = responseBody;
    }

    void updateElapsedTime(String elapsedTime) {
        this.elapsedTime = elapsedTime;
    }
}

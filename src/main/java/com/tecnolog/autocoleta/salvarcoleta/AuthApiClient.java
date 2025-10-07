package com.tecnolog.autocoleta.salvarcoleta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "authApi")
public interface AuthApiClient {

    @PostMapping(value = "/api/v1/Auth", consumes = "application/json")
    AuthResponse getAccessToken(@RequestBody AuthRequest body);

    class AuthRequest {
        @JsonProperty("Token")
        private String token;

        public AuthRequest(String token) {
            this.token = token;
        }
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
    class AuthResponse {
        @JsonProperty("error")
        private boolean error;

        @JsonProperty("AccessToken")
        private String accessToken;
        
        public boolean isError() { return error; }
        public String getAccessToken() { return accessToken; }
    }
}
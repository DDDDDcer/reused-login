package com.example.userservice.model;

public class ThirdPartyBinding {
    private Long userId;
    private String provider;
    private String openId;

    public ThirdPartyBinding() {
    }

    public ThirdPartyBinding(Long userId, String provider, String openId) {
        this.userId = userId;
        this.provider = provider;
        this.openId = openId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }
}

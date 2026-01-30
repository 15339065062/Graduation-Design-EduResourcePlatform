package com.edu.model;

import java.io.Serializable;
import java.util.Date;

public class OperationLog implements Serializable {
    private Integer id;
    private Integer userId;
    private String module;
    private String operation;
    private Integer targetId;
    private String details;
    private String ipAddress;
    private Date createTime;
    
    // Additional fields for display
    private String username;

    public OperationLog() {
    }

    public OperationLog(Integer userId, String module, String operation, Integer targetId, String details, String ipAddress) {
        this.userId = userId;
        this.module = module;
        this.operation = operation;
        this.targetId = targetId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createTime = new Date();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public void setTargetId(Integer targetId) {
        this.targetId = targetId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

package com.edu.model;

import java.io.Serializable;
import java.util.Date;

public class Notification implements Serializable {
    private Integer id;
    private Integer userId;
    private String title;
    private String content;
    private String type; // 'system', 'role_change', 'resource'
    private Boolean isRead;
    private Date createTime;

    public Notification() {
    }

    public Notification(Integer userId, String title, String content, String type) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.isRead = false;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}

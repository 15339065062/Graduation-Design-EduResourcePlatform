package com.edu.model;

import java.io.Serializable;
import java.util.Date;

public class Collection implements Serializable {
    private Integer id;
    private Integer resourceId;
    private Resource resource;
    private Integer userId;
    private User user;
    private Date createdAt;

    public Collection() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Collection{" +
                "id=" + id +
                ", resourceId=" + resourceId +
                ", userId=" + userId +
                ", createdAt=" + createdAt +
                '}';
    }
}

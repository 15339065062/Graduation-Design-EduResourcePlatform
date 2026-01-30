package com.edu.model;

import java.io.Serializable;
import java.util.Date;

public class RoleChangeRequest implements Serializable {
    private Integer id;
    private Integer userId;
    private String currentRole;
    private String targetRole;
    private String reason;
    private String proofMaterial;
    private String status; // 'pending', 'approved', 'rejected'
    private Date createTime;
    private Date auditTime;
    private Integer auditorId;
    private String auditRemark;
    
    // Additional fields for display
    private String username;
    private String nickname;
    private String auditorName;

    public RoleChangeRequest() {
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

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getProofMaterial() {
        return proofMaterial;
    }

    public void setProofMaterial(String proofMaterial) {
        this.proofMaterial = proofMaterial;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(Date auditTime) {
        this.auditTime = auditTime;
    }

    public Integer getAuditorId() {
        return auditorId;
    }

    public void setAuditorId(Integer auditorId) {
        this.auditorId = auditorId;
    }

    public String getAuditRemark() {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAuditorName() {
        return auditorName;
    }

    public void setAuditorName(String auditorName) {
        this.auditorName = auditorName;
    }
}

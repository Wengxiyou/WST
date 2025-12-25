package com.world.wst.data;

import org.bukkit.entity.Player;
import java.util.*;
import java.time.LocalDateTime;

/**
 * 聊天室数据类
 * 
 * @author World Studio
 */
public class ChatRoom {
    
    private final String name;
    private final String owner;
    private final boolean isDefault;
    private final Set<UUID> members;
    private final LocalDateTime createdTime;
    private String description;
    private int maxMembers;
    
    /**
     * 构造函数
     * 
     * @param name 聊天室名称
     * @param owner 创建者
     * @param isDefault 是否为默认聊天室
     */
    public ChatRoom(String name, String owner, boolean isDefault) {
        this.name = name;
        this.owner = owner;
        this.isDefault = isDefault;
        this.members = Collections.synchronizedSet(new HashSet<>());
        this.createdTime = LocalDateTime.now();
        this.description = "";
        this.maxMembers = 100;
    }
    
    /**
     * 添加成员
     * 
     * @param playerId 玩家UUID
     * @return 是否添加成功
     */
    public boolean addMember(UUID playerId) {
        if (members.size() >= maxMembers) {
            return false;
        }
        return members.add(playerId);
    }
    
    /**
     * 移除成员
     * 
     * @param playerId 玩家UUID
     * @return 是否移除成功
     */
    public boolean removeMember(UUID playerId) {
        return members.remove(playerId);
    }
    
    /**
     * 检查是否为成员
     * 
     * @param playerId 玩家UUID
     * @return 是否为成员
     */
    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }
    
    /**
     * 检查是否为房主
     * 
     * @param playerName 玩家名称
     * @return 是否为房主
     */
    public boolean isOwner(String playerName) {
        return owner.equals(playerName);
    }
    
    /**
     * 获取成员数量
     */
    public int getMemberCount() {
        return members.size();
    }
    
    /**
     * 获取所有成员ID
     */
    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }
    
    /**
     * 获取聊天室名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取创建者
     */
    public String getOwner() {
        return owner;
    }
    
    /**
     * 是否为默认聊天室
     */
    public boolean isDefault() {
        return isDefault;
    }
    
    /**
     * 获取创建时间
     */
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    /**
     * 获取描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 设置描述
     */
    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }
    
    /**
     * 获取最大成员数
     */
    public int getMaxMembers() {
        return maxMembers;
    }
    
    /**
     * 设置最大成员数
     */
    public void setMaxMembers(int maxMembers) {
        this.maxMembers = Math.max(1, maxMembers);
    }
    
    /**
     * 清空所有成员
     */
    public void clearMembers() {
        members.clear();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChatRoom chatRoom = (ChatRoom) obj;
        return Objects.equals(name, chatRoom.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return String.format("ChatRoom{name='%s', owner='%s', members=%d, isDefault=%s}", 
                           name, owner, members.size(), isDefault);
    }
}
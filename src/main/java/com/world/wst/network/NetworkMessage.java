package com.world.wst.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * 网络消息类
 * 
 * @author World Studio
 */
public class NetworkMessage {
    
    public enum Type {
        CHAT_MESSAGE,    // 聊天消息
        ROOM_CREATE,     // 聊天室创建
        ROOM_DELETE,     // 聊天室删除
        PLAYER_JOIN,     // 玩家加入
        PLAYER_LEAVE,    // 玩家离开
        HEARTBEAT,       // 心跳包
        SERVER_INFO      // 服务器信息
    }
    
    private Type type;
    private String serverId;
    private String serverName;
    private String roomName;
    private String playerName;
    private String message;
    private long timestamp;
    private String data; // 额外数据，JSON格式
    
    /**
     * 默认构造函数
     */
    public NetworkMessage() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 聊天消息构造函数
     */
    public NetworkMessage(Type type, String serverId, String serverName, String roomName, String playerName, String message) {
        this();
        this.type = type;
        this.serverId = serverId;
        this.serverName = serverName;
        this.roomName = roomName;
        this.playerName = playerName;
        this.message = message;
    }
    
    /**
     * 转换为JSON字符串
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
    
    /**
     * 从JSON字符串创建消息对象
     */
    public static NetworkMessage fromJson(String json) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(json, NetworkMessage.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
    
    /**
     * 创建聊天消息
     */
    public static NetworkMessage createChatMessage(String serverId, String serverName, String roomName, String playerName, String message) {
        return new NetworkMessage(Type.CHAT_MESSAGE, serverId, serverName, roomName, playerName, message);
    }
    
    /**
     * 创建心跳包
     */
    public static NetworkMessage createHeartbeat(String serverId, String serverName) {
        return new NetworkMessage(Type.HEARTBEAT, serverId, serverName, null, null, "heartbeat");
    }
    
    /**
     * 创建服务器信息消息
     */
    public static NetworkMessage createServerInfo(String serverId, String serverName, String data) {
        NetworkMessage msg = new NetworkMessage(Type.SERVER_INFO, serverId, serverName, null, null, null);
        msg.setData(data);
        return msg;
    }
    
    // Getters and Setters
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public String getServerId() {
        return serverId;
    }
    
    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public String getRoomName() {
        return roomName;
    }
    
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return String.format("NetworkMessage{type=%s, serverId='%s', serverName='%s', roomName='%s', playerName='%s', message='%s'}", 
                           type, serverId, serverName, roomName, playerName, message);
    }
}
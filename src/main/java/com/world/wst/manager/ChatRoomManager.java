package com.world.wst.manager;

import com.world.wst.WorldStudioTalk;
import com.world.wst.data.ChatRoom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天室管理器
 * 
 * @author World Studio
 */
public class ChatRoomManager implements Listener {
    
    private final WorldStudioTalk plugin;
    private final Map<String, ChatRoom> chatRooms;
    private final Map<UUID, String> playerRooms; // 玩家当前所在聊天室
    private final Map<String, Integer> playerRoomCount; // 玩家创建的聊天室数量
    
    public ChatRoomManager(WorldStudioTalk plugin) {
        this.plugin = plugin;
        this.chatRooms = new ConcurrentHashMap<>();
        this.playerRooms = new ConcurrentHashMap<>();
        this.playerRoomCount = new ConcurrentHashMap<>();
        
        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 创建聊天室
     * 
     * @param roomName 聊天室名称
     * @param owner 创建者
     * @param isDefault 是否为默认聊天室
     * @return 是否创建成功
     */
    public boolean createRoom(String roomName, String owner, boolean isDefault) {
        // 检查聊天室是否已存在
        if (chatRooms.containsKey(roomName)) {
            return false;
        }
        
        // 检查聊天室数量限制
        if (chatRooms.size() >= plugin.getConfigManager().getMaxRooms()) {
            return false;
        }
        
        // 检查玩家创建的聊天室数量限制
        if (!isDefault) {
            int playerCount = playerRoomCount.getOrDefault(owner, 0);
            if (playerCount >= plugin.getConfigManager().getMaxRoomsPerPlayer()) {
                return false;
            }
            playerRoomCount.put(owner, playerCount + 1);
        }
        
        // 创建聊天室
        ChatRoom room = new ChatRoom(roomName, owner, isDefault);
        room.setMaxMembers(plugin.getConfigManager().getMaxMembersPerRoom());
        chatRooms.put(roomName, room);
        
        plugin.debug("创建聊天室: " + roomName + " (创建者: " + owner + ")");
        return true;
    }
    
    /**
     * 删除聊天室
     * 
     * @param roomName 聊天室名称
     * @param requester 请求删除的玩家
     * @return 删除结果 (0: 成功, 1: 聊天室不存在, 2: 无权限, 3: 不能删除默认聊天室)
     */
    public int deleteRoom(String roomName, String requester) {
        ChatRoom room = chatRooms.get(roomName);
        
        if (room == null) {
            return 1; // 聊天室不存在
        }
        
        if (room.isDefault()) {
            return 3; // 不能删除默认聊天室
        }
        
        // 检查权限
        Player player = Bukkit.getPlayer(requester);
        if (!room.isOwner(requester) && (player == null || !player.hasPermission("wst.admin"))) {
            return 2; // 无权限
        }
        
        // 将所有成员移到默认聊天室
        String defaultRoom = plugin.getConfigManager().getDefaultRoomName();
        for (UUID memberId : room.getMembers()) {
            playerRooms.put(memberId, defaultRoom);
            ChatRoom defaultChatRoom = chatRooms.get(defaultRoom);
            if (defaultChatRoom != null) {
                defaultChatRoom.addMember(memberId);
            }
        }
        
        // 更新创建者的聊天室计数
        int ownerCount = playerRoomCount.getOrDefault(room.getOwner(), 0);
        if (ownerCount > 0) {
            playerRoomCount.put(room.getOwner(), ownerCount - 1);
        }
        
        // 删除聊天室
        chatRooms.remove(roomName);
        
        plugin.debug("删除聊天室: " + roomName + " (删除者: " + requester + ")");
        return 0; // 成功
    }
    
    /**
     * 玩家加入聊天室
     * 
     * @param player 玩家
     * @param roomName 聊天室名称
     * @return 是否加入成功
     */
    public boolean joinRoom(Player player, String roomName) {
        ChatRoom room = chatRooms.get(roomName);
        if (room == null) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        
        // 检查是否已在此聊天室
        String currentRoom = playerRooms.get(playerId);
        if (roomName.equals(currentRoom)) {
            return false;
        }
        
        // 离开当前聊天室
        if (currentRoom != null) {
            leaveRoom(player);
        }
        
        // 加入新聊天室
        if (room.addMember(playerId)) {
            playerRooms.put(playerId, roomName);
            plugin.debug("玩家 " + player.getName() + " 加入聊天室: " + roomName);
            return true;
        }
        
        return false;
    }
    
    /**
     * 玩家离开聊天室
     * 
     * @param player 玩家
     * @return 是否离开成功
     */
    public boolean leaveRoom(Player player) {
        UUID playerId = player.getUniqueId();
        String currentRoom = playerRooms.get(playerId);
        
        if (currentRoom == null) {
            return false;
        }
        
        ChatRoom room = chatRooms.get(currentRoom);
        if (room != null) {
            room.removeMember(playerId);
        }
        
        playerRooms.remove(playerId);
        plugin.debug("玩家 " + player.getName() + " 离开聊天室: " + currentRoom);
        return true;
    }
    
    /**
     * 获取玩家当前所在聊天室
     */
    public String getPlayerRoom(Player player) {
        return playerRooms.get(player.getUniqueId());
    }
    
    /**
     * 获取聊天室
     */
    public ChatRoom getRoom(String roomName) {
        return chatRooms.get(roomName);
    }
    
    /**
     * 获取所有聊天室名称
     */
    public Set<String> getAllRoomNames() {
        return new HashSet<>(chatRooms.keySet());
    }
    
    /**
     * 获取所有聊天室
     */
    public Collection<ChatRoom> getAllRooms() {
        return new ArrayList<>(chatRooms.values());
    }
    
    /**
     * 检查聊天室名称是否有效
     */
    public boolean isValidRoomName(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            return false;
        }
        
        roomName = roomName.trim();
        int minLength = plugin.getConfigManager().getMinRoomNameLength();
        int maxLength = plugin.getConfigManager().getMaxRoomNameLength();
        
        return roomName.length() >= minLength && roomName.length() <= maxLength;
    }
    
    /**
     * 向聊天室发送消息
     */
    public void sendMessageToRoom(String roomName, String playerName, String message) {
        ChatRoom room = chatRooms.get(roomName);
        if (room == null) {
            return;
        }
        
        String formattedMessage = plugin.getConfigManager().formatChatMessage(roomName, playerName, message);
        
        // 发送给聊天室内的所有玩家
        for (UUID memberId : room.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(formattedMessage);
            }
        }
        
        // 发送到其他服务器
        if (plugin.getNetworkManager() != null) {
            plugin.getNetworkManager().broadcastMessage(roomName, playerName, message);
        }
        
        plugin.debug("聊天室 " + roomName + " 消息: " + playerName + ": " + message);
    }
    
    /**
     * 清理数据
     */
    public void cleanup() {
        chatRooms.clear();
        playerRooms.clear();
        playerRoomCount.clear();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 自动加入默认聊天室
        String defaultRoom = plugin.getConfigManager().getDefaultRoomName();
        ChatRoom room = chatRooms.get(defaultRoom);
        if (room != null) {
            room.addMember(player.getUniqueId());
            playerRooms.put(player.getUniqueId(), defaultRoom);
            
            // 发送欢迎消息
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(plugin.getConfigManager().formatSystemMessage(
                    "欢迎使用 WorldStudioTalk! 输入 /wst help 查看帮助"));
                player.sendMessage(plugin.getConfigManager().formatSystemMessage(
                    "您已自动加入聊天室: " + defaultRoom));
            }, 20L); // 延迟1秒发送
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        leaveRoom(player);
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String roomName = getPlayerRoom(player);
        
        if (roomName != null) {
            // 取消原始聊天事件
            event.setCancelled(true);
            
            // 通过聊天室系统发送消息
            Bukkit.getScheduler().runTask(plugin, () -> {
                sendMessageToRoom(roomName, player.getName(), message);
            });
        }
    }
}
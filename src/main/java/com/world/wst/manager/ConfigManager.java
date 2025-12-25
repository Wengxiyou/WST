package com.world.wst.manager;

import com.world.wst.WorldStudioTalk;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

/**
 * 配置管理器
 * 
 * @author World Studio
 */
public class ConfigManager {
    
    private final WorldStudioTalk plugin;
    private FileConfiguration config;
    
    public ConfigManager(WorldStudioTalk plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        plugin.getLogger().info("配置文件已加载");
    }
    
    /**
     * 重载配置文件
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        plugin.getLogger().info("配置文件已重载");
    }
    
    /**
     * 获取配置文件
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 获取消息
     * 
     * @param key 消息键
     * @return 格式化的消息
     */
    public String getMessage(String key) {
        String message = config.getString("messages." + key, "消息未找到: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 获取消息并替换占位符
     * 
     * @param key 消息键
     * @param placeholders 占位符键值对
     * @return 格式化的消息
     */
    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        
        return message;
    }
    
    /**
     * 获取帮助消息列表
     */
    public List<String> getHelpMessages() {
        List<String> messages = config.getStringList("messages.help");
        return messages.stream()
                .map(msg -> ChatColor.translateAlternateColorCodes('&', msg))
                .toList();
    }
    
    /**
     * 获取系统消息格式
     */
    public String getSystemMessageFormat() {
        return config.getString("message.system-format", "&7[&aWST&7] &f{message}");
    }
    
    /**
     * 获取错误消息格式
     */
    public String getErrorMessageFormat() {
        return config.getString("message.error-format", "&7[&cWST&7] &c{message}");
    }
    
    /**
     * 获取聊天消息格式
     */
    public String getChatMessageFormat() {
        return config.getString("message.chat-format", "&7[&b{room}&7] &f{player}&7: &f{message}");
    }
    
    /**
     * 获取跨服务器消息格式
     */
    public String getCrossServerMessageFormat() {
        return config.getString("message.cross-server-format", "&7[&e{server}&7] &7[&b{room}&7] &f{player}&7: &f{message}");
    }
    
    /**
     * 格式化系统消息
     */
    public String formatSystemMessage(String message) {
        String format = getSystemMessageFormat();
        format = format.replace("{message}", message);
        return ChatColor.translateAlternateColorCodes('&', format);
    }
    
    /**
     * 格式化错误消息
     */
    public String formatErrorMessage(String message) {
        String format = getErrorMessageFormat();
        format = format.replace("{message}", message);
        return ChatColor.translateAlternateColorCodes('&', format);
    }
    
    /**
     * 格式化聊天消息
     */
    public String formatChatMessage(String room, String player, String message) {
        String format = getChatMessageFormat();
        format = format.replace("{room}", room);
        format = format.replace("{player}", player);
        format = format.replace("{message}", message);
        return ChatColor.translateAlternateColorCodes('&', format);
    }
    
    /**
     * 格式化跨服务器聊天消息
     */
    public String formatCrossServerMessage(String server, String room, String player, String message) {
        String format = getCrossServerMessageFormat();
        format = format.replace("{server}", server);
        format = format.replace("{room}", room);
        format = format.replace("{player}", player);
        format = format.replace("{message}", message);
        return ChatColor.translateAlternateColorCodes('&', format);
    }
    
    /**
     * 获取聊天室最大数量
     */
    public int getMaxRooms() {
        return config.getInt("chatroom.max-rooms", 50);
    }
    
    /**
     * 获取每个玩家最大聊天室数量
     */
    public int getMaxRoomsPerPlayer() {
        return config.getInt("chatroom.max-rooms-per-player", 5);
    }
    
    /**
     * 获取聊天室最大成员数
     */
    public int getMaxMembersPerRoom() {
        return config.getInt("chatroom.max-members", 100);
    }
    
    /**
     * 获取聊天室名称最大长度
     */
    public int getMaxRoomNameLength() {
        return config.getInt("chatroom.max-name-length", 20);
    }
    
    /**
     * 获取聊天室名称最小长度
     */
    public int getMinRoomNameLength() {
        return config.getInt("chatroom.min-name-length", 3);
    }
    
    /**
     * 获取默认聊天室名称
     */
    public String getDefaultRoomName() {
        return config.getString("chatroom.default-room", "全服大厅");
    }
}
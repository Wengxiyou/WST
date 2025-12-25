package com.world.wst;

import com.world.wst.manager.ChatRoomManager;
import com.world.wst.manager.ConfigManager;
import com.world.wst.manager.NetworkManager;
import com.world.wst.command.CommandHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * WorldStudioTalk - 我的世界聊天室插件
 * 
 * @author World Studio
 * @version 1.0.0
 */
public class WorldStudioTalk extends JavaPlugin {
    
    private static WorldStudioTalk instance;
    private ChatRoomManager chatRoomManager;
    private NetworkManager networkManager;
    private ConfigManager configManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        
        // 输出启动信息
        getLogger().info("========================================");
        getLogger().info("  WorldStudioTalk v1.0.0");
        getLogger().info("  聊天室插件 - 支持跨服务器文字聊天互通");
        getLogger().info("  制作: World Studio");
        getLogger().info("========================================");
        
        // 初始化管理器
        chatRoomManager = new ChatRoomManager(this);
        networkManager = new NetworkManager(this);
        
        // 注册命令
        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("wst").setExecutor(commandHandler);
        getCommand("wst").setTabCompleter(commandHandler);
        
        // 启动网络管理器
        if (configManager.getConfig().getBoolean("network.enabled", true)) {
            networkManager.start();
        }
        
        // 创建默认聊天室
        String defaultRoom = configManager.getConfig().getString("chatroom.default-room", "全服大厅");
        chatRoomManager.createRoom(defaultRoom, "服务器", true);
        
        getLogger().info("插件启动完成!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("正在关闭 WorldStudioTalk...");
        
        // 关闭网络管理器
        if (networkManager != null) {
            networkManager.stop();
        }
        
        // 清理聊天室数据
        if (chatRoomManager != null) {
            chatRoomManager.cleanup();
        }
        
        getLogger().info("WorldStudioTalk 已关闭");
    }
    
    /**
     * 获取插件实例
     */
    public static WorldStudioTalk getInstance() {
        return instance;
    }
    
    /**
     * 获取聊天室管理器
     */
    public ChatRoomManager getChatRoomManager() {
        return chatRoomManager;
    }
    
    /**
     * 获取网络管理器
     */
    public NetworkManager getNetworkManager() {
        return networkManager;
    }
    
    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 发送彩色消息给所有在线玩家
     */
    public void broadcastMessage(String message) {
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    /**
     * 调试日志
     */
    public void debug(String message) {
        if (configManager.getConfig().getBoolean("plugin.debug", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
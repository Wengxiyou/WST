package com.world.wst.command;

import com.world.wst.WorldStudioTalk;
import com.world.wst.data.ChatRoom;
import com.world.wst.manager.ChatRoomManager;
import com.world.wst.manager.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 命令处理器
 * 
 * @author World Studio
 */
public class CommandHandler implements CommandExecutor, TabCompleter {
    
    private final WorldStudioTalk plugin;
    private final ChatRoomManager chatRoomManager;
    private final ConfigManager configManager;
    
    public CommandHandler(WorldStudioTalk plugin) {
        this.plugin = plugin;
        this.chatRoomManager = plugin.getChatRoomManager();
        this.configManager = plugin.getConfigManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 检查基本权限
        if (!player.hasPermission("wst.use")) {
            player.sendMessage(configManager.formatErrorMessage(configManager.getMessage("no-permission")));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
            case "h":
                sendHelp(player);
                break;
                
            case "list":
                handleListCommand(player);
                break;
                
            case "join":
                handleJoinCommand(player, args);
                break;
                
            case "add":
                handleAddCommand(player, args);
                break;
                
            case "del":
            case "delete":
                handleDeleteCommand(player, args);
                break;
                
            case "exit":
            case "leave":
                handleExitCommand(player);
                break;
                
            case "info":
                handleInfoCommand(player, args);
                break;
                
            case "reload":
                handleReloadCommand(player);
                break;
                
            case "status":
                handleStatusCommand(player);
                break;
                
            default:
                player.sendMessage(configManager.formatErrorMessage("未知命令! 输入 /wst help 查看帮助"));
        }
        
        return true;
    }
    
    /**
     * 发送帮助信息
     */
    private void sendHelp(Player player) {
        List<String> helpMessages = configManager.getHelpMessages();
        for (String message : helpMessages) {
            player.sendMessage(message);
        }
    }
    
    /**
     * 处理列表命令
     */
    private void handleListCommand(Player player) {
        Collection<ChatRoom> rooms = chatRoomManager.getAllRooms();
        
        if (rooms.isEmpty()) {
            player.sendMessage(configManager.formatSystemMessage("当前没有任何聊天室"));
            return;
        }
        
        player.sendMessage("§7========== §b聊天室列表 §7==========");
        
        for (ChatRoom room : rooms) {
            String status = room.isDefault() ? "§a[默认]" : "§7[普通]";
            String memberInfo = "§e" + room.getMemberCount() + "§7/§e" + room.getMaxMembers();
            String ownerInfo = room.isDefault() ? "§7系统" : "§7" + room.getOwner();
            
            player.sendMessage(String.format("§f%s %s §7- §7成员: %s §7- §7创建者: %s", 
                                            status, room.getName(), memberInfo, ownerInfo));
        }
        
        String currentRoom = chatRoomManager.getPlayerRoom(player);
        if (currentRoom != null) {
            player.sendMessage("§7当前聊天室: §b" + currentRoom);
        } else {
            player.sendMessage("§7您当前不在任何聊天室中");
        }
        
        player.sendMessage("§7================================");
    }
    
    /**
     * 处理加入命令
     */
    private void handleJoinCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(configManager.formatErrorMessage("用法: /wst join <聊天室名称>"));
            return;
        }
        
        String roomName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // 检查聊天室是否存在
        ChatRoom room = chatRoomManager.getRoom(roomName);
        if (room == null) {
            player.sendMessage(configManager.formatErrorMessage(
                configManager.getMessage("room-not-found", "room", roomName)));
            return;
        }
        
        // 检查是否已在此聊天室
        String currentRoom = chatRoomManager.getPlayerRoom(player);
        if (roomName.equals(currentRoom)) {
            player.sendMessage(configManager.formatErrorMessage(
                configManager.getMessage("already-in-room", "room", roomName)));
            return;
        }
        
        // 尝试加入聊天室
        if (chatRoomManager.joinRoom(player, roomName)) {
            player.sendMessage(configManager.formatSystemMessage(
                configManager.getMessage("joined-room", "room", roomName)));
        } else {
            player.sendMessage(configManager.formatErrorMessage("加入聊天室失败，可能已满员"));
        }
    }
    
    /**
     * 处理创建命令
     */
    private void handleAddCommand(Player player, String[] args) {
        if (!player.hasPermission("wst.create")) {
            player.sendMessage(configManager.formatErrorMessage(configManager.getMessage("no-permission")));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(configManager.formatErrorMessage("用法: /wst add <聊天室名称>"));
            return;
        }
        
        String roomName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // 验证聊天室名称
        if (!chatRoomManager.isValidRoomName(roomName)) {
            int minLength = configManager.getMinRoomNameLength();
            int maxLength = configManager.getMaxRoomNameLength();
            
            if (roomName.length() < minLength) {
                player.sendMessage(configManager.formatErrorMessage(
                    configManager.getMessage("room-name-too-short", "min", String.valueOf(minLength))));
            } else {
                player.sendMessage(configManager.formatErrorMessage(
                    configManager.getMessage("room-name-too-long", "max", String.valueOf(maxLength))));
            }
            return;
        }
        
        // 检查聊天室是否已存在
        if (chatRoomManager.getRoom(roomName) != null) {
            player.sendMessage(configManager.formatErrorMessage(
                configManager.getMessage("room-exists", "room", roomName)));
            return;
        }
        
        // 尝试创建聊天室
        if (chatRoomManager.createRoom(roomName, player.getName(), false)) {
            player.sendMessage(configManager.formatSystemMessage(
                configManager.getMessage("room-created", "room", roomName)));
            
            // 自动加入创建的聊天室
            chatRoomManager.joinRoom(player, roomName);
        } else {
            // 检查具体失败原因
            if (chatRoomManager.getAllRooms().size() >= configManager.getMaxRooms()) {
                player.sendMessage(configManager.formatErrorMessage(
                    configManager.getMessage("max-rooms-reached")));
            } else {
                player.sendMessage(configManager.formatErrorMessage(
                    configManager.getMessage("max-rooms-per-player-reached")));
            }
        }
    }
    
    /**
     * 处理删除命令
     */
    private void handleDeleteCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(configManager.formatErrorMessage("用法: /wst del <聊天室名称>"));
            return;
        }
        
        String roomName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        int result = chatRoomManager.deleteRoom(roomName, player.getName());
        
        switch (result) {
            case 0: // 成功
                player.sendMessage(configManager.formatSystemMessage(
                    configManager.getMessage("room-deleted", "room", roomName)));
                break;
            case 1: // 聊天室不存在
                player.sendMessage(configManager.formatErrorMessage(
                    configManager.getMessage("room-not-found", "room", roomName)));
                break;
            case 2: // 无权限
                player.sendMessage(configManager.formatErrorMessage(
                    configManager.getMessage("cannot-delete-not-owner")));
                break;
            case 3: // 不能删除默认聊天室
                player.sendMessage(configManager.formatErrorMessage(
                    configManager.getMessage("cannot-delete-default")));
                break;
            default:
                player.sendMessage(configManager.formatErrorMessage("删除聊天室时发生未知错误"));
        }
    }
    
    /**
     * 处理退出命令
     */
    private void handleExitCommand(Player player) {
        String currentRoom = chatRoomManager.getPlayerRoom(player);
        
        if (currentRoom == null) {
            player.sendMessage(configManager.formatErrorMessage(
                configManager.getMessage("not-in-room")));
            return;
        }
        
        // 不能离开默认聊天室
        String defaultRoom = configManager.getDefaultRoomName();
        if (currentRoom.equals(defaultRoom)) {
            player.sendMessage(configManager.formatErrorMessage("不能离开默认聊天室！"));
            return;
        }
        
        if (chatRoomManager.leaveRoom(player)) {
            player.sendMessage(configManager.formatSystemMessage(
                configManager.getMessage("left-room", "room", currentRoom)));
            
            // 自动加入默认聊天室
            chatRoomManager.joinRoom(player, defaultRoom);
            player.sendMessage(configManager.formatSystemMessage(
                "已自动加入默认聊天室: " + defaultRoom));
        } else {
            player.sendMessage(configManager.formatErrorMessage("离开聊天室失败"));
        }
    }
    
    /**
     * 处理信息命令
     */
    private void handleInfoCommand(Player player, String[] args) {
        String roomName;
        
        if (args.length >= 2) {
            roomName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        } else {
            roomName = chatRoomManager.getPlayerRoom(player);
            if (roomName == null) {
                player.sendMessage(configManager.formatErrorMessage(
                    configManager.getMessage("not-in-room")));
                return;
            }
        }
        
        ChatRoom room = chatRoomManager.getRoom(roomName);
        if (room == null) {
            player.sendMessage(configManager.formatErrorMessage(
                configManager.getMessage("room-not-found", "room", roomName)));
            return;
        }
        
        player.sendMessage("§7========== §b聊天室信息 §7==========");
        player.sendMessage("§f名称: §b" + room.getName());
        player.sendMessage("§f类型: " + (room.isDefault() ? "§a默认聊天室" : "§7普通聊天室"));
        player.sendMessage("§f创建者: §7" + (room.isDefault() ? "系统" : room.getOwner()));
        player.sendMessage("§f成员数: §e" + room.getMemberCount() + "§7/§e" + room.getMaxMembers());
        player.sendMessage("§f创建时间: §7" + room.getCreatedTime().toString());
        
        if (!room.getDescription().isEmpty()) {
            player.sendMessage("§f描述: §7" + room.getDescription());
        }
        
        player.sendMessage("§7================================");
    }
    
    /**
     * 处理重载命令
     */
    private void handleReloadCommand(Player player) {
        if (!player.hasPermission("wst.admin")) {
            player.sendMessage(configManager.formatErrorMessage(configManager.getMessage("no-permission")));
            return;
        }
        
        configManager.reloadConfig();
        player.sendMessage(configManager.formatSystemMessage("配置文件已重载！"));
    }
    
    /**
     * 处理状态命令
     */
    private void handleStatusCommand(Player player) {
        if (!player.hasPermission("wst.admin")) {
            player.sendMessage(configManager.formatErrorMessage(configManager.getMessage("no-permission")));
            return;
        }
        
        player.sendMessage("§7========== §bWorldStudioTalk 状态 §7==========");
        player.sendMessage("§f插件版本: §a" + plugin.getDescription().getVersion());
        player.sendMessage("§f聊天室数量: §e" + chatRoomManager.getAllRooms().size());
        
        // 网络状态
        if (plugin.getNetworkManager() != null) {
            Map<String, Object> networkStatus = plugin.getNetworkManager().getNetworkStatus();
            player.sendMessage("§f网络状态: " + (((Boolean) networkStatus.get("running")) ? "§a运行中" : "§c已停止"));
            player.sendMessage("§f服务器ID: §7" + networkStatus.get("serverId"));
            player.sendMessage("§f连接的客户端: §e" + networkStatus.get("connectedClients"));
            player.sendMessage("§f连接的服务器: §e" + networkStatus.get("serverConnections"));
        } else {
            player.sendMessage("§f网络状态: §c未启用");
        }
        
        player.sendMessage("§7==========================================");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "list", "join", "add", "del", "exit", "info");
            
            // 管理员命令
            if (player.hasPermission("wst.admin")) {
                List<String> adminCommands = new ArrayList<>(subCommands);
                adminCommands.addAll(Arrays.asList("reload", "status"));
                return adminCommands.stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            return subCommands.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if ("join".equals(subCommand) || "info".equals(subCommand) || "del".equals(subCommand)) {
                // 返回聊天室名称列表
                return chatRoomManager.getAllRoomNames().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
}
package com.world.wst.manager;

import com.world.wst.WorldStudioTalk;
import com.world.wst.data.ChatRoom;
import com.world.wst.network.NetworkMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 网络管理器 - 处理跨服务器通信
 * 
 * @author World Studio
 */
public class NetworkManager {
    
    private final WorldStudioTalk plugin;
    private final String serverId;
    private final String serverName;
    private final int port;
    private final String bindIp;
    
    private ServerSocket serverSocket;
    private final Set<Socket> connectedClients;
    private final Map<String, Socket> serverConnections;
    private final ExecutorService threadPool;
    private volatile boolean running;
    
    public NetworkManager(WorldStudioTalk plugin) {
        this.plugin = plugin;
        this.serverId = plugin.getConfig().getString("network.server-id", "server1");
        this.serverName = plugin.getConfig().getString("network.server-name", "主服务器");
        this.port = plugin.getConfig().getInt("network.port", 25580);
        this.bindIp = plugin.getConfig().getString("network.bind-ip", "0.0.0.0");
        
        this.connectedClients = Collections.synchronizedSet(new HashSet<>());
        this.serverConnections = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.running = false;
    }
    
    /**
     * 启动网络服务
     */
    public void start() {
        if (running) {
            return;
        }
        
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(bindIp));
            running = true;
            
            plugin.getLogger().info("网络服务启动成功，监听端口: " + port);
            
            // 启动服务器监听线程
            threadPool.submit(this::serverListenLoop);
            
            // 连接到其他服务器
            connectToOtherServers();
            
            // 启动心跳任务
            startHeartbeatTask();
            
        } catch (IOException e) {
            plugin.getLogger().severe("网络服务启动失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止网络服务
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        plugin.getLogger().info("正在关闭网络服务...");
        
        // 关闭所有连接
        synchronized (connectedClients) {
            for (Socket client : connectedClients) {
                try {
                    client.close();
                } catch (IOException e) {
                    // 忽略关闭错误
                }
            }
            connectedClients.clear();
        }
        
        // 关闭服务器连接
        for (Socket connection : serverConnections.values()) {
            try {
                connection.close();
            } catch (IOException e) {
                // 忽略关闭错误
            }
        }
        serverConnections.clear();
        
        // 关闭服务器Socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // 忽略关闭错误
            }
        }
        
        // 关闭线程池
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
        
        plugin.getLogger().info("网络服务已关闭");
    }
    
    /**
     * 服务器监听循环
     */
    private void serverListenLoop() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                connectedClients.add(client);
                threadPool.submit(() -> handleClient(client));
                plugin.debug("新的客户端连接: " + client.getRemoteSocketAddress());
            } catch (IOException e) {
                if (running) {
                    plugin.getLogger().warning("接受客户端连接时出错: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 处理客户端连接
     */
    private void handleClient(Socket client) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter writer = new PrintWriter(client.getOutputStream(), true)) {
            
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleMessage(line, writer);
            }
            
        } catch (IOException e) {
            plugin.debug("客户端连接断开: " + client.getRemoteSocketAddress());
        } finally {
            connectedClients.remove(client);
            try {
                client.close();
            } catch (IOException e) {
                // 忽略关闭错误
            }
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void handleMessage(String messageData, PrintWriter sender) {
        NetworkMessage message = NetworkMessage.fromJson(messageData);
        if (message == null) {
            return;
        }
        
        plugin.debug("收到网络消息: " + message.toString());
        
        switch (message.getType()) {
            case CHAT_MESSAGE:
                handleChatMessage(message);
                break;
            case HEARTBEAT:
                handleHeartbeat(message, sender);
                break;
            case SERVER_INFO:
                handleServerInfo(message);
                break;
            default:
                plugin.debug("未知消息类型: " + message.getType());
        }
    }
    
    /**
     * 处理聊天消息
     */
    private void handleChatMessage(NetworkMessage message) {
        String roomName = message.getRoomName();
        ChatRoom room = plugin.getChatRoomManager().getRoom(roomName);
        
        if (room == null) {
            return;
        }
        
        String formattedMessage = plugin.getConfigManager().formatCrossServerMessage(
            message.getServerName(), roomName, message.getPlayerName(), message.getMessage());
        
        // 发送给聊天室内的所有玩家
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (UUID memberId : room.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    member.sendMessage(formattedMessage);
                }
            }
        });
    }
    
    /**
     * 处理心跳包
     */
    private void handleHeartbeat(NetworkMessage message, PrintWriter sender) {
        // 回复心跳包
        NetworkMessage response = NetworkMessage.createHeartbeat(serverId, serverName);
        sender.println(response.toJson());
    }
    
    /**
     * 处理服务器信息
     */
    private void handleServerInfo(NetworkMessage message) {
        plugin.debug("收到服务器信息: " + message.getServerId() + " - " + message.getServerName());
    }
    
    /**
     * 连接到其他服务器
     */
    private void connectToOtherServers() {
        ConfigurationSection connections = plugin.getConfig().getConfigurationSection("network.connections");
        if (connections == null) {
            return;
        }
        
        for (String serverKey : connections.getKeys(false)) {
            String host = connections.getString(serverKey + ".host");
            int port = connections.getInt(serverKey + ".port");
            String name = connections.getString(serverKey + ".name", serverKey);
            
            threadPool.submit(() -> connectToServer(serverKey, host, port, name));
        }
    }
    
    /**
     * 连接到指定服务器
     */
    private void connectToServer(String serverKey, String host, int port, String name) {
        try {
            Socket socket = new Socket(host, port);
            serverConnections.put(serverKey, socket);
            
            // 启动消息处理线程
            threadPool.submit(() -> handleServerConnection(serverKey, socket));
            
            plugin.getLogger().info("成功连接到服务器: " + name + " (" + host + ":" + port + ")");
            
            // 发送服务器信息
            sendToServer(serverKey, NetworkMessage.createServerInfo(serverId, serverName, "连接建立"));
            
        } catch (IOException e) {
            plugin.getLogger().warning("连接服务器失败: " + name + " (" + host + ":" + port + ") - " + e.getMessage());
        }
    }
    
    /**
     * 处理服务器连接
     */
    private void handleServerConnection(String serverKey, Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleMessage(line, writer);
            }
            
        } catch (IOException e) {
            plugin.debug("服务器连接断开: " + serverKey);
        } finally {
            serverConnections.remove(serverKey);
            try {
                socket.close();
            } catch (IOException e) {
                // 忽略关闭错误
            }
        }
    }
    
    /**
     * 发送消息到指定服务器
     */
    public void sendToServer(String serverKey, NetworkMessage message) {
        Socket connection = serverConnections.get(serverKey);
        if (connection != null && !connection.isClosed()) {
            try {
                PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                writer.println(message.toJson());
                plugin.debug("发送消息到服务器 " + serverKey + ": " + message.toString());
            } catch (IOException e) {
                plugin.debug("发送消息到服务器失败: " + serverKey + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * 广播消息到所有连接的服务器
     */
    public void broadcastMessage(String roomName, String playerName, String message) {
        NetworkMessage networkMessage = NetworkMessage.createChatMessage(serverId, serverName, roomName, playerName, message);
        String jsonMessage = networkMessage.toJson();
        
        // 发送给所有连接的客户端
        synchronized (connectedClients) {
            for (Socket client : connectedClients) {
                try {
                    PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                    writer.println(jsonMessage);
                } catch (IOException e) {
                    plugin.debug("发送消息给客户端失败: " + e.getMessage());
                }
            }
        }
        
        // 发送给所有连接的服务器
        for (String serverKey : serverConnections.keySet()) {
            sendToServer(serverKey, networkMessage);
        }
        
        plugin.debug("广播消息: " + networkMessage.toString());
    }
    
    /**
     * 启动心跳任务
     */
    private void startHeartbeatTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!running) {
                return;
            }
            
            NetworkMessage heartbeat = NetworkMessage.createHeartbeat(serverId, serverName);
            
            // 发送心跳到所有连接的服务器
            for (String serverKey : serverConnections.keySet()) {
                sendToServer(serverKey, heartbeat);
            }
            
        }, 20L * 30, 20L * 30); // 每30秒发送一次心跳
    }
    
    /**
     * 获取连接状态信息
     */
    public Map<String, Object> getNetworkStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", running);
        status.put("serverId", serverId);
        status.put("serverName", serverName);
        status.put("port", port);
        status.put("connectedClients", connectedClients.size());
        status.put("serverConnections", serverConnections.size());
        status.put("connectionsList", new ArrayList<>(serverConnections.keySet()));
        return status;
    }
}
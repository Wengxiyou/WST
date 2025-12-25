#!/bin/bash

# WorldStudioTalk 编译脚本
# World Studio 制作

echo "=========================================="
echo "  WorldStudioTalk 编译脚本"
echo "  制作: World Studio"
echo "=========================================="

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven 未安装或不在PATH中"
    echo "请安装Maven 3.6.0或更高版本"
    exit 1
fi

# 检查Java版本
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "错误: Java版本过低，需要Java 17或更高版本"
    echo "当前Java版本: $(java -version 2>&1 | head -1)"
    exit 1
fi

echo "Java版本检查通过: Java $JAVA_VERSION"
echo "Maven版本: $(mvn -version | head -1)"
echo ""

# 清理之前的构建
echo "正在清理之前的构建..."
mvn clean

if [ $? -ne 0 ]; then
    echo "清理失败!"
    exit 1
fi

# 编译项目
echo ""
echo "正在编译项目..."
mvn package -q

if [ $? -ne 0 ]; then
    echo "编译失败!"
    exit 1
fi

# 检查生成的文件
JAR_FILE="target/WorldStudioTalk-1.0.0.jar"
if [ -f "$JAR_FILE" ]; then
    echo ""
    echo "=========================================="
    echo "  编译成功!"
    echo "=========================================="
    echo "生成的插件文件: $JAR_FILE"
    echo "文件大小: $(du -h $JAR_FILE | cut -f1)"
    echo ""
    echo "安装说明:"
    echo "1. 将 $JAR_FILE 复制到服务器的 plugins/ 目录"
    echo "2. 重启服务器"
    echo "3. 编辑 plugins/WorldStudioTalk/config.yml 配置文件"
    echo "4. 使用 /wst help 查看帮助"
    echo ""
    echo "感谢使用 WorldStudioTalk!"
    echo "制作: World Studio"
    echo "=========================================="
else
    echo "编译失败: 未找到生成的jar文件"
    exit 1
fi
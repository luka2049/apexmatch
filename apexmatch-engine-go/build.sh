#!/bin/bash

# ApexMatch Golang 引擎构建脚本

set -e

echo "=========================================="
echo "构建 ApexMatch Golang 撮合引擎"
echo "=========================================="

cd "$(dirname "$0")"

# 检查 Go 是否安装
if ! command -v go &> /dev/null; then
    echo "错误: 未找到 Go 编译器，请先安装 Go 1.21+"
    exit 1
fi

echo "Go 版本: $(go version)"

# 下载依赖
echo ""
echo "下载依赖..."
go mod download
go mod tidy

# 构建动态库
echo ""
echo "构建动态库..."
cd cmd/lib

OS=$(uname -s)
case "$OS" in
    Darwin)
        echo "检测到 macOS，构建 .dylib"
        go build -buildmode=c-shared -o libapexmatch_go.dylib
        echo "✓ 构建完成: libapexmatch_go.dylib"
        ;;
    Linux)
        echo "检测到 Linux，构建 .so"
        go build -buildmode=c-shared -o libapexmatch_go.so
        echo "✓ 构建完成: libapexmatch_go.so"
        ;;
    MINGW*|MSYS*|CYGWIN*)
        echo "检测到 Windows，构建 .dll"
        go build -buildmode=c-shared -o apexmatch_go.dll
        echo "✓ 构建完成: apexmatch_go.dll"
        ;;
    *)
        echo "错误: 不支持的操作系统 $OS"
        exit 1
        ;;
esac

echo ""
echo "=========================================="
echo "构建成功！"
echo "=========================================="
echo ""
echo "动态库位置: $(pwd)"
echo ""
echo "使用方法："
echo "1. 将动态库复制到 Java 项目的 resources 目录"
echo "2. 配置 application.yml:"
echo "   apexmatch:"
echo "     engine:"
echo "       type: golang"
echo "       golang-library-path: /path/to/libapexmatch_go.dylib"
echo ""

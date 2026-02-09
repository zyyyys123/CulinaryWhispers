# 构建阶段
FROM golang:1.24-alpine AS builder

# 设置环境变量
ENV GO111MODULE=on \
    CGO_ENABLED=0 \
    GOOS=linux \
    GOARCH=amd64 \
    GOPROXY=https://goproxy.cn,direct

WORKDIR /app

# 复制 go.mod 和 go.sum
COPY go.mod go.sum ./
RUN go mod download

# 复制源码
COPY . .

# 编译 Gateway
RUN go build -o gateway-server ./gateway/cmd/main.go

# 运行阶段
FROM alpine:latest

WORKDIR /app

# 复制编译产物
COPY --from=builder /app/gateway-server .

# 暴露端口
EXPOSE 8081

# 启动命令
CMD ["./gateway-server"]

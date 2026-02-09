package handler

import (
	"log"
	"net/http"
	"sync"

	"culinary-go-edge/im/internal/config"
	"github.com/gorilla/websocket"
	"github.com/redis/go-redis/v9"
	"context"
)

var (
	rdb *redis.Client
	ctx = context.Background()
	upgrader = websocket.Upgrader{
		CheckOrigin: func(r *http.Request) bool {
			return true // 允许所有来源 (开发环境)
		},
	}
	// 简单的内存连接管理
	clients = make(map[*websocket.Conn]bool)
	broadcast = make(chan []byte)
	mutex = &sync.Mutex{}
)

// Init 初始化 Redis 和 协程
func Init() {
	rdb = redis.NewClient(&redis.Options{
		Addr:     config.GlobalConfig.RedisAddr,
		Password: "", 
		DB:       0,  
	})

	// 启动 Redis 订阅
	go subscribeToRedis()

	// 启动广播处理
	go handleMessages()
}

// HandleWebSocket 处理 WebSocket 连接
func HandleWebSocket(w http.ResponseWriter, r *http.Request) {
	// 升级 HTTP 请求为 WebSocket
	ws, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println(err)
		return
	}
	defer ws.Close()

	// 注册客户端
	mutex.Lock()
	clients[ws] = true
	mutex.Unlock()

	log.Println("新客户端已连接")

	for {
		// 读取消息
		_, msg, err := ws.ReadMessage()
		if err != nil {
			log.Printf("读取错误: %v", err)
			mutex.Lock()
			delete(clients, ws)
			mutex.Unlock()
			break
		}

		// 发布到 Redis (实现多节点同步)
		// 格式: "UserID:Message" (简化版)
		err = rdb.Publish(ctx, "chat_channel", msg).Err()
		if err != nil {
			log.Printf("Redis 发布错误: %v", err)
		}
	}
}

func subscribeToRedis() {
	pubsub := rdb.Subscribe(ctx, "chat_channel")
	defer pubsub.Close()

	ch := pubsub.Channel()

	for msg := range ch {
		broadcast <- []byte(msg.Payload)
	}
}

func handleMessages() {
	for {
		msg := <-broadcast
		
		mutex.Lock()
		for client := range clients {
			err := client.WriteMessage(websocket.TextMessage, msg)
			if err != nil {
				log.Printf("写入错误: %v", err)
				client.Close()
				delete(clients, client)
			}
		}
		mutex.Unlock()
	}
}

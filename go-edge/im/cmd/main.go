package main

import (
	"log"
	"net/http"

	"culinary-go-edge/im/internal/config"
	"culinary-go-edge/im/internal/handler"
)

func main() {
	// 初始化组件
	handler.Init()

	// 注册路由
	http.HandleFunc("/ws", handler.HandleWebSocket)
	
	log.Printf("Go IM Server 启动于 %s", config.GlobalConfig.Port)
	err := http.ListenAndServe(config.GlobalConfig.Port, nil)
	if err != nil {
		log.Fatal("监听失败: ", err)
	}
}

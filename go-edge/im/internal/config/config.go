package config

// AppConfig 应用配置
type AppConfig struct {
	Port      string
	RedisAddr string
}

// GlobalConfig 全局配置实例
var GlobalConfig = &AppConfig{
	Port:      ":8082",
	RedisAddr: "redis:6379", // Docker内部网络
}

package config

// AppConfig 应用配置
type AppConfig struct {
	Port      string
	TargetURL string
	JwtSecret string
}

// GlobalConfig 全局配置实例
var GlobalConfig = &AppConfig{
	Port:      ":8081",
	TargetURL: "http://app:8080", // Docker内部网络
	JwtSecret: "mySecretKeyForCulinaryWhispersProject2026",
}

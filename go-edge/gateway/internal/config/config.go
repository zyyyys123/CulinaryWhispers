package config

import "os"

// AppConfig 应用配置
type AppConfig struct {
	Port      string
	TargetURL string
	JwtSecret string
}

// GlobalConfig 全局配置实例
var GlobalConfig = &AppConfig{
	Port:      getEnvOrDefault("CW_GATEWAY_PORT", ":8081"),
	TargetURL: getEnvOrDefault("CW_GATEWAY_TARGET_URL", "http://app:8080"),
	JwtSecret: getEnvOrDefault("CW_JWT_SECRET", "mySecretKeyForCulinaryWhispersProject2026"),
}

func getEnvOrDefault(key string, defaultValue string) string {
	v := os.Getenv(key)
	if v == "" {
		return defaultValue
	}
	return v
}

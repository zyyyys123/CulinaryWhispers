package middleware

import (
	"encoding/json"
	"fmt"
	"strings"

	"culinary-go-edge/gateway/internal/config"
	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/time/rate"
)

var (
	// 限流器: 1000 QPS, 突发 2000
	limiter = rate.NewLimiter(1000, 2000)
)

// Response 统一返回结构
type Response struct {
	Code    int         `json:"code"`
	Message string      `json:"message"`
	Data    interface{} `json:"data"`
}

// RateLimitMiddleware 限流中间件
func RateLimitMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		if !limiter.Allow() {
			c.JSON(429, Response{Code: 429, Message: "请求过于频繁", Data: nil})
			c.Abort()
			return
		}
		c.Next()
	}
}

// CorsMiddleware 跨域中间件
func CorsMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
		c.Writer.Header().Set("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE")
		c.Writer.Header().Set("Access-Control-Allow-Headers", "Origin, Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization")

		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}
		c.Next()
	}
}

// AuthMiddleware 鉴权中间件 (可选，如果路由组需要)
func AuthMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		// 具体的鉴权逻辑可以在这里复用 validateToken
		if err := ValidateToken(c); err != nil {
			c.JSON(401, Response{Code: 401, Message: "未授权: " + err.Error(), Data: nil})
			c.Abort()
			return
		}
		c.Next()
	}
}

// ValidateToken 校验 Token 并注入 UserID
func ValidateToken(c *gin.Context) error {
	authHeader := c.GetHeader("Authorization")
	if authHeader == "" {
		return fmt.Errorf("缺少 Authorization 头")
	}

	tokenString := strings.TrimPrefix(authHeader, "Bearer ")
	if tokenString == authHeader {
		return fmt.Errorf("Token 格式错误")
	}

	parser := jwt.NewParser(jwt.WithJSONNumber())
	token, err := parser.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("意外的签名方法: %v", token.Header["alg"])
		}
		return []byte(config.GlobalConfig.JwtSecret), nil
	})

	if err != nil {
		return err
	}

	if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
		// 将 userId 透传给下游服务
		val := claims["userId"]
		switch v := val.(type) {
		case float64:
			c.Request.Header.Set("X-User-Id", fmt.Sprintf("%.0f", v))
		case json.Number:
			c.Request.Header.Set("X-User-Id", v.String())
		case string:
			c.Request.Header.Set("X-User-Id", v)
		}
		return nil
	}

	return fmt.Errorf("无效的 Token")
}

package main

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strings"

	"culinary-go-edge/gateway/internal/config"
	"culinary-go-edge/gateway/internal/middleware"
	"github.com/gin-gonic/gin"
)

func main() {
	r := gin.Default()

	// 全局中间件
	r.Use(middleware.RateLimitMiddleware())
	r.Use(middleware.CorsMiddleware())

	// 健康检查
	r.GET("/health", func(c *gin.Context) {
		c.JSON(200, middleware.Response{Code: 0, Message: "ok", Data: gin.H{"service": "go-gateway"}})
	})

	// 统一代理处理
	// 1. 免鉴权接口 (白名单)
	// 2. 需鉴权接口 (校验 Token)
	r.NoRoute(func(c *gin.Context) {
		if strings.HasPrefix(c.Request.URL.Path, "/api") {
			if !isWhitelisted(c.Request.URL.Path) {
				// 校验 Token
				if err := middleware.ValidateToken(c); err != nil {
					c.JSON(401, middleware.Response{Code: 401, Message: "未授权: " + err.Error(), Data: nil})
					return
				}
			}
			proxyHandler(c)
		} else {
			c.JSON(404, middleware.Response{Code: 404, Message: "未找到资源", Data: nil})
		}
	})

	log.Printf("Go Gateway 启动端口 %s, 转发目标 %s", config.GlobalConfig.Port, config.GlobalConfig.TargetURL)
	r.Run(config.GlobalConfig.Port)
}

// isWhitelisted 检查路径是否在白名单中
func isWhitelisted(path string) bool {
	// 白名单: 登录、注册、公开食谱列表、搜索
	whitelist := []string{
		"/api/user/login",
		"/api/user/register",
		"/api/recipe/list", // 允许游客访问
		"/api/search",
	}
	for _, p := range whitelist {
		if strings.HasPrefix(path, p) {
			return true
		}
	}
	return false
}

// proxyHandler 反向代理处理器
func proxyHandler(c *gin.Context) {
	remote, err := url.Parse(config.GlobalConfig.TargetURL)
	if err != nil {
		c.JSON(500, middleware.Response{Code: 500, Message: "目标 URL 解析失败", Data: nil})
		return
	}

	proxy := httputil.NewSingleHostReverseProxy(remote)
	proxy.Director = func(req *http.Request) {
		req.Header = c.Request.Header
		req.Host = remote.Host
		req.URL.Scheme = remote.Scheme
		req.URL.Host = remote.Host
		req.URL.Path = c.Request.URL.Path
	}

	proxy.ServeHTTP(c.Writer, c.Request)
}

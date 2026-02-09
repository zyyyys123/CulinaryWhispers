package handler

import (
	"net/http/httptest"
	"testing"
)

func TestCheckOrigin(t *testing.T) {
	req := httptest.NewRequest("GET", "/", nil)
	if !upgrader.CheckOrigin(req) {
		t.Error("CheckOrigin should return true for all requests in dev mode")
	}
}

package main

import (
	"testing"
)

func TestIsWhitelisted(t *testing.T) {
	tests := []struct {
		method   string
		path     string
		expected bool
	}{
		{"POST", "/api/user/login", true},
		{"POST", "/api/user/register", true},
		{"GET", "/api/recipe/list", true},
		{"GET", "/api/search", true},
		{"GET", "/api/search/recipe", true},
		{"GET", "/api/recipe/123", true},
		{"POST", "/api/recipe/publish", false},
		{"GET", "/api/user/profile", false},
	}

	for _, test := range tests {
		result := isWhitelisted(test.method, test.path)
		if result != test.expected {
			t.Errorf("isWhitelisted(%s %s) = %v; expected %v", test.method, test.path, result, test.expected)
		}
	}
}

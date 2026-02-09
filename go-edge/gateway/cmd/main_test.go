package main

import (
	"testing"
)

func TestIsWhitelisted(t *testing.T) {
	tests := []struct {
		path     string
		expected bool
	}{
		{"/api/user/login", true},
		{"/api/user/register", true},
		{"/api/recipe/list", true},
		{"/api/search", true},
		{"/api/search/recipe", true},
		{"/api/user/profile", false},
		{"/api/recipe/create", false},
	}

	for _, test := range tests {
		result := isWhitelisted(test.path)
		if result != test.expected {
			t.Errorf("isWhitelisted(%s) = %v; expected %v", test.path, result, test.expected)
		}
	}
}

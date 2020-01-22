package main

import (
	"os"
	"os/exec"
	"path/filepath"
	"net/http"
	"time"
	"fmt"
)

// CONFIGURATION
// PLACEHOLDER

var java_search_path = [...] {
	"/usr/lib/jvm/java-*/bin/java",
	"/usr/java/*/bin/java",
}

func main() {
	base, _ := os.UserHomeDir()
	base = filepath.Join(base, ".sandpolis")

	os.MkdirAll(base, os.ModePerm)

	fmt.Println(locateJava())
}

func test() {
	tr := &http.Transport {
		ResponseHeaderTimeout: 60 * time.Second,
	}
	client := &http.Client {
		Transport: tr,
	}

	_, _ = client.Get("https://jlink.online")
}

func locateJava() string {
	_, err := exec.Command("java", "-version").Output()
	if err == nil {
		// Just use command
		return "java"
	}

	paths, _ := filepath.Glob("/usr/lib/jvm/java-*/bin/java")
	if paths != nil {
		return paths[0]
	}
	return ""
}
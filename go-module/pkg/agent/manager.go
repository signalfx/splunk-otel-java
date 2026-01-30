package agent

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"go.uber.org/zap"
)

const (
	// Default download URL for Splunk Java agent
	defaultDownloadURL = "https://github.com/signalfx/splunk-otel-java/releases/download/v%s/splunk-otel-javaagent.jar"
	latestReleaseURL   = "https://api.github.com/repos/signalfx/splunk-otel-java/releases/latest"
	agentJarName       = "splunk-otel-javaagent.jar"
)

// Manager handles Java agent operations
type Manager struct {
	config *Config
	logger *zap.Logger
}

// NewManager creates a new agent manager
func NewManager(config *Config, logger *zap.Logger) *Manager {
	// Set default backup folder if not specified
	if config.BackupFolder == "" {
		config.BackupFolder = filepath.Join(config.DestFolder, "backup")
	}

	return &Manager{
		config: config,
		logger: logger,
	}
}

// Install downloads and installs the Java agent
func (m *Manager) Install() (*Result, error) {
	m.logger.Info("Starting Java agent installation",
		zap.String("version", m.config.AgentVersion),
		zap.String("dest_folder", m.config.DestFolder))

	result := &Result{
		Operation: "install",
		Version:   m.config.AgentVersion,
		Details:   make(map[string]interface{}),
	}

	// Create destination directory
	if err := os.MkdirAll(m.config.DestFolder, 0755); err != nil {
		result.Success = false
		result.Error = fmt.Sprintf("Failed to create destination directory: %v", err)
		return result, err
	}

	// Resolve version if "latest"
	version := m.config.AgentVersion
	if version == "latest" {
		resolvedVersion, err := m.getLatestVersion()
		if err != nil {
			result.Success = false
			result.Error = fmt.Sprintf("Failed to resolve latest version: %v", err)
			return result, err
		}
		version = resolvedVersion
		result.Version = version
	}

	// Check if agent already exists
	agentPath := filepath.Join(m.config.DestFolder, agentJarName)
	if _, err := os.Stat(agentPath); err == nil {
		// Create backup if agent exists
		if err := m.createBackup(agentPath); err != nil {
			m.logger.Warn("Failed to create backup", zap.Error(err))
		}
	}

	// Download the agent
	downloadURL := fmt.Sprintf(defaultDownloadURL, version)
	if err := m.downloadAgent(downloadURL, agentPath); err != nil {
		result.Success = false
		result.Error = fmt.Sprintf("Failed to download agent: %v", err)
		return result, err
	}

	result.Success = true
	result.Message = fmt.Sprintf("Successfully installed Java agent version %s", version)
	result.Path = agentPath
	result.Details["download_url"] = downloadURL
	result.Details["java_home"] = m.config.JavaHome
	result.Details["service_name"] = m.config.ServiceName

	m.logger.Info("Java agent installation completed successfully",
		zap.String("version", version),
		zap.String("path", agentPath))

	return result, nil
}

// Uninstall removes the Java agent
func (m *Manager) Uninstall() (*Result, error) {
	m.logger.Info("Starting Java agent uninstallation")

	result := &Result{
		Operation: "uninstall",
		Details:   make(map[string]interface{}),
	}

	agentPath := filepath.Join(m.config.DestFolder, agentJarName)

	// Check if agent exists
	if _, err := os.Stat(agentPath); os.IsNotExist(err) {
		result.Success = true
		result.Message = "Java agent is not installed"
		return result, nil
	}

	// Create backup before uninstalling if requested
	if m.config.KeepBackup {
		if err := m.createBackup(agentPath); err != nil {
			m.logger.Warn("Failed to create backup before uninstall", zap.Error(err))
		}
	}

	// Remove the agent
	if err := os.Remove(agentPath); err != nil {
		result.Success = false
		result.Error = fmt.Sprintf("Failed to remove agent: %v", err)
		return result, err
	}

	result.Success = true
	result.Message = "Successfully uninstalled Java agent"
	result.Path = agentPath

	m.logger.Info("Java agent uninstallation completed successfully")

	if !m.config.KeepBackup {
		if err := os.RemoveAll(m.config.BackupFolder); err != nil {
			m.logger.Warn("Failed to remove backup folder", zap.Error(err))
		}
	}

	return result, nil
}

// Rollback restores the previous version from backup
func (m *Manager) Rollback() (*Result, error) {
	m.logger.Info("Starting Java agent rollback")

	result := &Result{
		Operation: "rollback",
		Details:   make(map[string]interface{}),
	}

	agentPath := filepath.Join(m.config.DestFolder, agentJarName)
	backupPath := m.getLatestBackupPath()

	if backupPath == "" {
		result.Success = false
		result.Error = "No backup found for rollback"
		return result, fmt.Errorf("no backup found")
	}

	// Create backup of current version before rollback
	if _, err := os.Stat(agentPath); err == nil {
		if err := m.createBackup(agentPath); err != nil {
			m.logger.Warn("Failed to backup current version", zap.Error(err))
		}
	}

	// Copy backup to agent location
	if err := m.copyFile(backupPath, agentPath); err != nil {
		result.Success = false
		result.Error = fmt.Sprintf("Failed to restore from backup: %v", err)
		return result, err
	}

	result.Success = true
	result.Message = "Successfully rolled back Java agent"
	result.Path = agentPath
	result.BackupPath = backupPath

	m.logger.Info("Java agent rollback completed successfully",
		zap.String("backup_path", backupPath))

	return result, nil
}

// Upgrade upgrades the Java agent to a new version
func (m *Manager) Upgrade() (*Result, error) {
	m.logger.Info("Starting Java agent upgrade",
		zap.String("version", m.config.AgentVersion))

	// Upgrade is essentially the same as install for Java agents
	return m.Install()
}

// Helper methods

func (m *Manager) getLatestVersion() (string, error) {
	client := &http.Client{
		Timeout: 15 * time.Second,
	}

	req, err := http.NewRequest(http.MethodGet, latestReleaseURL, nil)
	if err != nil {
		return "", err
	}
	req.Header.Set("Accept", "application/vnd.github+json")
	req.Header.Set("User-Agent", "splunk-otel-java-manager")

	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("failed to get latest release: HTTP %d", resp.StatusCode)
	}

	var payload struct {
		TagName string `json:"tag_name"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
		return "", err
	}

	tag := strings.TrimSpace(payload.TagName)
	if tag == "" {
		return "", fmt.Errorf("latest release tag not found")
	}

	return strings.TrimPrefix(tag, "v"), nil
}

func (m *Manager) downloadAgent(url, destPath string) error {
	m.logger.Info("Downloading agent", zap.String("url", url))

	resp, err := http.Get(url)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("failed to download: HTTP %d", resp.StatusCode)
	}

	out, err := os.Create(destPath)
	if err != nil {
		return err
	}
	defer out.Close()

	_, err = io.Copy(out, resp.Body)
	return err
}

func (m *Manager) createBackup(sourcePath string) error {
	if err := os.MkdirAll(m.config.BackupFolder, 0755); err != nil {
		return err
	}

	timestamp := time.Now().Format("20060102_150405")
	backupName := fmt.Sprintf("%s.%s", agentJarName, timestamp)
	backupPath := filepath.Join(m.config.BackupFolder, backupName)

	return m.copyFile(sourcePath, backupPath)
}

func (m *Manager) copyFile(src, dst string) error {
	sourceFile, err := os.Open(src)
	if err != nil {
		return err
	}
	defer sourceFile.Close()

	destFile, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer destFile.Close()

	_, err = io.Copy(destFile, sourceFile)
	return err
}

func (m *Manager) getLatestBackupPath() string {
	backupDir := m.config.BackupFolder
	entries, err := os.ReadDir(backupDir)
	if err != nil {
		return ""
	}

	var latestBackup string
	var latestTime time.Time

	for _, entry := range entries {
		if entry.IsDir() {
			continue
		}

		name := entry.Name()
		if !strings.HasPrefix(name, agentJarName) {
			continue
		}

		info, err := entry.Info()
		if err != nil {
			continue
		}

		if info.ModTime().After(latestTime) {
			latestTime = info.ModTime()
			latestBackup = filepath.Join(backupDir, name)
		}
	}

	return latestBackup
}

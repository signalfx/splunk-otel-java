package agent

// Config holds the configuration for the Java agent manager
type Config struct {
	DestFolder    string
	BackupFolder  string
	AgentVersion  string
	AccessToken   string
	OTLPEndpoint  string
	KeepBackup    bool
	JavaHome      string
	ServiceName   string
}

// Result represents the result of an agent operation
type Result struct {
	Success     bool                   `json:"success"`
	Message     string                 `json:"message"`
	Operation   string                 `json:"operation"`
	Version     string                 `json:"version,omitempty"`
	Path        string                 `json:"path,omitempty"`
	BackupPath  string                 `json:"backup_path,omitempty"`
	Details     map[string]interface{} `json:"details,omitempty"`
	Error       string                 `json:"error,omitempty"`
}

# labv-sftp-transfer

**labv-sftp-transfer** is a lightweight, platform-independent command-line tool for automated SFTP file transfers.

The tool:

- regularly scans defined local folders for new files (based on configured patterns),
- uploads matching files to a remote server via SFTP using public key authentication,
- archives or deletes files after upload, depending on configuration,

---

## Quick Start

### Using native binary (no Java required):

```bash
./labv-sftp-transfer --config path/to/config.yaml
```

### Using JAR:

```bash
java -jar labv-sftp-transfer.jar --config path/to/config.yaml
```

---

## Command-Line Options

| Option         | Description                                      |
|----------------|--------------------------------------------------|
| `--config`     | Path to the `config.yaml` file                   |
| `--dry-run`    | Simulate all operations without modifying files  |
| `--logLevel`   | Logging level: `SEVERE`, `WARNING`, `INFO`, `FINE`, `FINER`, `FINEST` |
| `--help`       | Show command-line help                           |

---

## Configuration (`config.yaml`)

### Interval

```yaml
intervalSeconds: 300
```

Time (in seconds) between scans.

---

### Folder Configuration

```yaml
folders:
  - path: "./to_upload"
    pattern:
      - "*.csv"
      - "*.xml"
    postAction: archive
    archiveDir: "./archive"

  - path: "./to_delete"
    pattern:
      - "*.csv"
    postAction: delete
```

| Field         | Description                                                                   |
|---------------|-------------------------------------------------------------------------------|
| `path`        | Path to folder to monitor                                                     |
| `pattern`     | One or more wildcard patterns to match files (e.g. `*.csv`, `report-*.xml`)  |
| `postAction`  | Action after upload: `archive` or `delete`                                   |
| `archiveDir`  | Required if `postAction` is `archive`; archive target directory              |

---

### SFTP Configuration

```yaml
sftp:
  host: sftp.example.com
  port: 22
  username: your-username
  privateKeyPath: "./id_rsa"
  remoteDir: "/upload/"
  knownHostsPath: "./known_hosts"
  # trustedHostPublicKey: "ssh-ed25519 AAAAC3..."
```

- Authentication is based on an SSH private key.
- Either `knownHostsPath` or `trustedHostPublicKey` must be provided.
- `trustedHostPublicKey` must be provided in the full OpenSSH format, e.g. `ssh-rsa AAAAB3...`.

---

### Logging Configuration

```yaml
log:
  directory: "./log"
  enableFileLogging: true
  retentionDays: 14
```

| Field                | Description                                                      |
|----------------------|------------------------------------------------------------------|
| `directory`          | Directory for log files (default: `./log`)                       |
| `enableFileLogging`  | If `false`, only console logging is used                         |
| `retentionDays`      | Log files older than this value will be deleted on startup       |

Log file format: `yyyyMMdd-labv-sftp-transfer.log`  
Example: `20250602-labv-sftp-transfer.log`

---

## Dry-Run Mode

Simulates scanning and uploading without performing actual file transfers or deletions.

```bash
java -jar labv-sftp-transfer.jar --config config.yaml --dry-run
```

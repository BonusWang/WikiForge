# WikiForge CI 与 Docker 标准 CI Docker Standard

## 目标

每个服务必须能独立完成：

- 测试。
- 打包。
- 镜像构建。
- 健康检查。
- Docker Compose 启动。

## MVP Compose 服务

```text
mysql
wikiforge-core-service
wikiforge-worker-service
wikiforge-ui
wikiforge-orchestration-service
wikiforge-orchestration-ui
```

后续可加入：

```text
wikiforge-gateway
wikiforge-agent-service
wikiforge-mcp-service
wikiforge-connector-service
wikiforge-vector-service
```

## 镜像规则

- 每个运行服务一个 Dockerfile。
- 镜像内不写死宿主机路径。
- API key、token、Vault、Raw Sources 不进入镜像。
- Runtime 配置通过环境变量或挂载配置传入。
- Healthcheck 必须使用镜像内真实存在的命令。

## Volume 规则

持久化目录必须挂载：

```text
mysql data
raw-sources
obsidian-vault
logs
imports
```

容器内路径与宿主机路径必须分清。生成 Obsidian URI 时使用宿主机可识别路径和 Vault 相对路径。

## CI 最小检查

后端：

```text
mvn -B test
mvn -B -DskipTests package
```

前端：

```text
npm ci
npm run build
```

Docker：

```text
docker build
docker compose config
docker compose build
```

本地如果使用 `E:\repository` 作为 Maven 仓库，PowerShell 参数必须加引号：

```text
"-Dmaven.repo.local=E:\repository"
```

该本地路径不能写入项目 CI 配置。

## MVP 暂缓

MVP 0/1 不强制引入：

- Nacos
- Redis
- Kafka
- XXL-JOB
- Kubernetes
- Service Mesh

这些能力必须先有明确压力、协作或部署需求，再进入架构评审。

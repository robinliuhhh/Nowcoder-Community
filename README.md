# Nowcoder-Community
Java仿牛客社区 改造版

## 各服务启动命令
### Kafka
```bash
# 进入d盘
d:
# 进入Kafka安装目录
cd Software\apache\kafka_2.13-3.2.0
# 开启Zookeeper
bin\windows\zookeeper-server-start.bat config\zookeeper.properties
# 打开新的cmd窗口 进入Kafka安装目录
# 开启Kafka
bin\windows\kafka-server-start.bat config\server.properties
```

### Elasticsearch

```bash
# 进入Elasticsearch安装目录
d:
cd Software\Elasticsearch\elasticsearch-7.17.3\bin
# 双击
elasticsearch.bat
```


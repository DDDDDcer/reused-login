# log-service-mock

`log-service-mock` 是课程项目“可复用微服务访问日志管理服务”的最小可运行模拟实现。它模拟一个可被 TopBiz 调用的底层日志微服务，用于演示访问日志查询、告警规则、告警历史、聚合统计、健康检测和内部日志上报。

当前版本不接真实数据库、ClickHouse、Vector、Redis、认证框架或复杂异常检测。访问日志、告警规则、告警事件、采集配置和保留策略全部保存在内存中，适合课堂演示和接口联调。

## 环境要求

- Java 17
- Maven 3.8+

## 启动

```bash
mvn spring-boot:run
```

服务端口：`18083`

如果已生成 Maven Wrapper，也可以使用：

```bash
.\mvnw.cmd spring-boot:run
```

## 主要接口

- `GET /api/v1/log/health`：健康检测
- `POST /api/v1/log/search`：访问日志组合查询
- `GET /api/v1/log/config/retention`：查询保留策略
- `PUT /api/v1/log/config/retention`：更新保留策略
- `POST /api/v1/log/alerts/rules`：创建告警规则
- `GET /api/v1/log/alerts/rules`：查询告警规则
- `PUT /api/v1/log/alerts/rules/{ruleId}`：更新告警规则
- `DELETE /api/v1/log/alerts/rules/{ruleId}`：删除告警规则
- `GET /api/v1/log/alerts/history`：查询告警触发历史
- `POST /api/v1/log/metrics/query`：聚合统计查询
- `POST /internal/logs/access`：模拟 TopBiz 上报访问日志

## 为什么使用内存模拟 ClickHouse 和 Vector

课程当前阶段关注可复用日志微服务的接口契约和核心流程。真实生产链路通常包含本地日志、采集代理、队列、ClickHouse 存储和告警通知，但这些组件会显著增加部署复杂度。本项目用内存 `List` 模拟结构化访问日志表，用内部接口模拟 TopBiz 上报，用简单规则计算模拟告警触发，便于快速运行和演示。

## Debug 接口

- `GET /debug/logs`：查看当前内存中的 `AccessLog`、`AlertRule`、`AlertEvent`、`CollectionConfig`、`RetentionPolicy`
- `POST /debug/reset`：重置内存数据并重新初始化模拟日志、告警规则和配置
- `POST /debug/retention/cleanup`：根据 `RetentionPolicy.retentionDays` 手动触发保留清理

## 测试样例

见 [docs/api-test.http](docs/api-test.http)。

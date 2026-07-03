const components = [
  {
    id: "user",
    name: "可复用用户管理服务",
    shortName: "用户管理",
    summary: "账号、登录、用户组、权限不再由每个业务重复实现。",
    reusePoint: "统一账号模型、认证流程、用户组和权限管理。",
    removed: ["登录注册流程", "用户与权限表设计", "账号状态与权限判断散落在业务代码中"],
    provided: ["账号密码/验证码登录注册", "用户、用户组、权限管理", "跨业务统一 RBAC 能力"],
    steps: ["配置 user-service-url", "注入 AuthApiClient / UserAdminApiClient", "业务流程中直接调用用户接口"]
  },
  {
    id: "message",
    name: "可复用消息通告服务",
    shortName: "消息通告",
    summary: "邮件、短信、站内信、定时发送和重试统一沉淀。",
    reusePoint: "统一消息载体、模板、发送任务和投递记录。",
    removed: ["各业务分别接发送渠道", "重复维护消息模板和发送记录", "重复处理幂等、定时和失败重试"],
    provided: ["即时发送与定时发送", "Idempotency-Key 幂等提交", "任务状态和投递记录统一追踪"],
    steps: ["配置 msg-service-url", "注入 MessageApiClient", "业务只提交通告内容和接收人"]
  },
  {
    id: "log",
    name: "可复用访问日志服务",
    shortName: "访问日志",
    summary: "访问采集、检索、指标和告警由公共日志服务统一处理。",
    reusePoint: "统一访问日志采集、查询、统计和告警规则。",
    removed: ["每个服务各自建访问日志表", "Controller 中手写耗时和状态码记录", "重复建设日志检索和统计功能"],
    provided: ["WebConfig 拦截器自动采集", "日志检索与指标查询", "保留策略和告警规则复用"],
    steps: ["配置 log-service-url", "注册 AccessLogInterceptor", "需要时通过 LogApiClient 查询日志"]
  }
];

function idempotencyKey() {
  return window.crypto?.randomUUID ? window.crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
}

async function requestJson(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json",
      "X-Sender-Id": "reusable-ui",
      "Idempotency-Key": idempotencyKey()
    },
    ...options
  });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : {};
  if (!response.ok) {
    throw new Error(payload.message || payload.error || `HTTP ${response.status}`);
  }
  return payload.data ?? payload;
}

function pretty(value) {
  return JSON.stringify(value, null, 2);
}

function totalOf(payload) {
  return Number(payload?.total ?? 0);
}

function toQuery(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (String(value || "").trim()) {
      search.set(key, value);
    }
  });
  const query = search.toString();
  return query ? `?${query}` : "";
}

function defaultPlannedTime(minutes = 10) {
  const date = new Date(Date.now() + minutes * 60 * 1000);
  const localTime = new Date(date.getTime() - date.getTimezoneOffset() * 60 * 1000);
  return localTime.toISOString().slice(0, 16);
}

function demoSuffix() {
  return Date.now().toString().slice(-6);
}

function providerLabel(provider) {
  return ({ QQ: "QQ", WECHAT: "微信", FEISHU: "飞书" })[provider] || provider;
}

function buildThirdPartyAuthUrl(payload, form) {
  const remoteUrl = valueOf(payload, ["authorize_url", "authorizeUrl"], "");
  if (remoteUrl) return remoteUrl;
  const params = new URLSearchParams({
    provider: form.provider,
    open_id: form.open_id,
    nickname: form.nickname,
    state: valueOf(payload, ["state"], ""),
    redirect_uri: valueOf(payload, ["redirect_uri", "redirectUri"], "")
  });
  return `/third-party-auth.html?${params.toString()}`;
}

function oauthState(provider) {
  const random = window.crypto?.randomUUID ? window.crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
  return `${provider}:${random}`;
}

function readOauthCallback() {
  const params = new URLSearchParams(window.location.search);
  const code = params.get("code") || "";
  const error = params.get("error") || "";
  if (!code && !error) return null;
  const state = params.get("state") || "";
  const provider = state.includes(":") ? state.split(":")[0].toUpperCase() : "";
  return {
    type: "third-party-oauth-code",
    provider,
    code,
    state,
    error,
    error_description: params.get("error_description") || params.get("error-description") || ""
  };
}

function listOf(payload, keys) {
  if (Array.isArray(payload)) return payload;
  for (const key of keys) {
    if (Array.isArray(payload?.[key])) return payload[key];
  }
  return [];
}

function valueOf(item, keys, fallback = "") {
  for (const key of keys) {
    if (item?.[key] !== undefined && item?.[key] !== null && item?.[key] !== "") {
      return item[key];
    }
  }
  return fallback;
}

function parseIdList(value) {
  return String(value || "")
    .split(/[,，\s]+/)
    .map(item => Number(item))
    .filter(item => Number.isFinite(item) && item > 0);
}

document.addEventListener("DOMContentLoaded", () => {
  if (!window.Vue) {
    document.getElementById("app").innerHTML = "<main class='page'><section class='panel'><h1>Vue 加载失败</h1><p>当前页面需要 Vue 3 运行时。</p></section></main>";
    return;
  }

  const { createApp } = window.Vue;

  createApp({
    data() {
      return {
        components,
        selectedId: "user",
        serviceState: { user: "idle", message: "idle", log: "idle" },
        forms: {
          user: { username: "demo_user", email: "demo@example.com", mobile: "13800000000" },
          userQuery: { keyword: "", status: "", groupKeyword: "" },
          userRelation: { userId: "", groupId: "", permissionIds: "" },
          thirdParty: {
            provider: "QQ",
            open_id: `demo-openid-${demoSuffix()}`,
            nickname: "第三方演示用户",
            auto_register: "true"
          },
          message: {
            receiver_id: "10001",
            carrier_type: "EMAIL",
            template_id: "",
            template_name: "张三",
            title: "系统通知",
            content: "这是一条复用消息组件的测试通告。",
            planned_time: defaultPlannedTime()
          },
          messageTemplate: {
            template_id: "",
            template_code: `welcome_email_${demoSuffix()}`,
            template_name: "欢迎通知模板",
            carrier_type: "EMAIL",
            template_content: "你好 {{name}}，欢迎使用可复用消息通告服务。"
          },
          messageCarrier: {
            carrier_id: "",
            carrier_type: `WEBHOOK_${demoSuffix()}`,
            carrier_name: "Webhook 通道",
            description: "第三方平台 Webhook 消息通道",
            account_name: `webhook-default-${demoSuffix()}`,
            provider: "MOCK",
            status: ""
          },
          messageReceipt: {
            task_id: "",
            platform: "MOCK_PROVIDER",
            status: "DELIVERED",
            provider_message_id: "mock-receipt-001"
          },
          messageStrategy: {
            strategy_id: "",
            strategy_name: `fast-retry-${demoSuffix()}`,
            max_retries: "3",
            retry_interval_seconds: "30",
            status: "ENABLED"
          },
          log: { serviceName: "", path: "", statusCode: "", traceId: "" }
        },
        latestResponse: "点击右侧按钮后，这里显示对应可复用服务的返回结果。",
        latestPayload: null,
        latestAction: "",
        toast: ""
      };
    },
    computed: {
      selected() {
        return this.components.find(item => item.id === this.selectedId);
      },
      userVisualResult() {
        if (this.selected.id !== "user" || !this.latestPayload || !this.latestAction) {
          return null;
        }

        const payload = this.latestPayload;
        let title = "用户组件结果";
        let hint = "公共用户能力返回的结构化数据";
        let cards = [];

        if (this.latestAction === "create-user") {
          const userId = valueOf(payload, ["user_id", "userId", "id"], "-");
          cards = [{
            label: "新用户",
            title: `ID ${userId}`,
            description: `${this.forms.user.username} / ${this.forms.user.email}`,
            meta: "下一步可把该用户加入某个分组"
          }];
          title = "用户创建";
          hint = "账号档案由用户服务统一创建";
        }

        if (this.latestAction === "users" || this.latestAction === "group-users") {
          const users = listOf(payload, ["user_list", "users", "records"]);
          title = this.latestAction === "group-users" ? "组内用户" : "用户列表";
          hint = this.latestAction === "group-users"
            ? `分组 ${this.forms.userRelation.groupId || "-"} 下的用户`
            : "业务系统复用统一用户档案";
          cards = users.map(user => ({
            label: valueOf(user, ["status"], "USER"),
            title: valueOf(user, ["username", "name"], `用户 ${valueOf(user, ["id"], "-")}`),
            description: `${valueOf(user, ["email"], "未设置邮箱")} / ${valueOf(user, ["mobile"], "未设置手机号")}`,
            meta: `user_id: ${valueOf(user, ["id", "user_id", "userId"], "-")}`
          }));
        }

        if (this.latestAction === "groups") {
          const groups = listOf(payload, ["group_list", "groups", "records"]);
          title = "分组列表";
          hint = "分组是用户和权限之间的复用关系层";
          cards = groups.map(group => ({
            label: "GROUP",
            title: valueOf(group, ["name", "group_name", "groupName"], `分组 ${valueOf(group, ["id"], "-")}`),
            description: valueOf(group, ["description"], "暂无描述"),
            meta: `group_id: ${valueOf(group, ["id", "group_id", "groupId"], "-")}`
          }));
        }

        if (this.latestAction === "permissions" || this.latestAction === "group-permissions") {
          const permissions = listOf(payload, ["permission_list", "permissions", "records"]);
          title = this.latestAction === "group-permissions" ? "组内权限" : "权限库";
          hint = this.latestAction === "group-permissions"
            ? `分组 ${this.forms.userRelation.groupId || "-"} 已获得的权限`
            : "权限由用户服务统一维护，业务侧按权限码判断";
          cards = permissions.map(permission => ({
            label: valueOf(permission, ["resourceType", "resource_type"], "PERMISSION"),
            title: valueOf(permission, ["code"], `权限 ${valueOf(permission, ["id"], "-")}`),
            description: valueOf(permission, ["name", "description"], "暂无说明"),
            meta: `permission_id: ${valueOf(permission, ["id", "permission_id", "permissionId"], "-")}`
          }));
        }

        if (this.latestAction === "add-user-group") {
          title = "加入分组";
          hint = "用户通过分组继承该组权限";
          cards = [{
            label: "RELATION",
            title: `用户 ${this.forms.userRelation.userId} -> 分组 ${this.forms.userRelation.groupId}`,
            description: "用户与分组关系已提交到用户服务。",
            meta: "可继续点击“组内用户”查看结果"
          }];
        }

        if (this.latestAction === "grant-permissions") {
          title = "给组授权";
          hint = "授权给分组后，组内用户可复用这些权限";
          cards = parseIdList(this.forms.userRelation.permissionIds).map(id => ({
            label: "GRANT",
            title: `分组 ${this.forms.userRelation.groupId} 获得权限 ${id}`,
            description: "权限关系已提交到用户服务。",
            meta: "可继续点击“组内权限”查看结果"
          }));
        }

        if (this.latestAction === "third-party-authorize") {
          title = "拉起第三方授权";
          hint = "业务系统通过统一接口生成真实平台授权地址，并在新窗口打开";
          cards = [{
            label: valueOf(payload, ["provider"], this.forms.thirdParty.provider),
            title: `state: ${valueOf(payload, ["state"], "-")}`,
            description: `${providerLabel(this.forms.thirdParty.provider)}真实授权页已打开，请使用平台有效 app_id/client_id`,
            meta: valueOf(payload, ["authorize_url", "authorizeUrl"], "未返回授权地址")
          }];
        }

        if (this.latestAction === "third-party-login") {
          const user = payload.user || {};
          title = "第三方唯一凭证登录";
          hint = "QQ / 微信 / 飞书使用 provider + open_id 作为唯一登录凭证";
          cards = [{
            label: valueOf(payload, ["provider"], this.forms.thirdParty.provider),
            title: valueOf(user, ["username"], `用户 ${valueOf(user, ["id"], "-")}`),
            description: `open_id: ${valueOf(payload, ["open_id", "openId"], this.forms.thirdParty.open_id)}`,
            meta: `auto_registered: ${valueOf(payload, ["auto_registered", "autoRegistered"], false)}`
          }];
        }

        if (this.latestAction === "third-party-callback") {
          title = "第三方授权回调";
          hint = "真实平台已回跳到业务系统，下一步应由后端用 code 换取 open_id";
          cards = [{
            label: valueOf(payload, ["provider"], this.forms.thirdParty.provider),
            title: valueOf(payload, ["code"], valueOf(payload, ["error"], "未返回 code")),
            description: `state: ${valueOf(payload, ["state"], "-")}`,
            meta: valueOf(payload, ["error_description", "errorDescription"], "callback 已接收")
          }];
        }

        if (!cards.length) {
          cards = [{
            label: "EMPTY",
            title: "暂无数据",
            description: "当前接口没有返回可展示的记录。",
            meta: ""
          }];
        }

        return {
          title,
          hint,
          cards: cards.slice(0, 6).map((card, index) => ({ ...card, key: `${this.latestAction}-${index}-${card.title}` }))
        };
      },
      messageVisualResult() {
        if (this.selected.id !== "message" || !this.latestPayload || !this.latestAction) {
          return null;
        }

        const payload = this.latestPayload;
        let title = "消息组件结果";
        let hint = "公共消息能力返回的结构化数据";
        let cards = [];

        if (["send-now", "send-scheduled"].includes(this.latestAction)) {
          title = this.latestAction === "send-scheduled" ? "定时发送任务" : "即时发送任务";
          hint = "业务只提交消息请求，投递任务由消息服务统一维护";
          cards = [{
            label: valueOf(payload, ["carrier_type"], "MESSAGE"),
            title: `任务 ${valueOf(payload, ["task_id", "taskId"], "-")}`,
            description: `接收人 ${valueOf(payload, ["receiver_id", "receiverId"], this.forms.message.receiver_id)}，状态 ${valueOf(payload, ["send_status", "task_status"], "-")}`,
            meta: `message_id: ${valueOf(payload, ["message_id", "messageId"], "-")}`
          }];
        }

        if (this.latestAction === "message-records") {
          title = "投递记录";
          hint = "统一追踪不同载体的发送任务";
          cards = listOf(payload, ["task_list", "records"]).map(task => ({
            label: valueOf(task, ["carrier_type"], "TASK"),
            title: `任务 ${valueOf(task, ["task_id", "taskId"], "-")}`,
            description: `接收人 ${valueOf(task, ["receiver_id"], "-")}，状态 ${valueOf(task, ["send_status", "task_status"], "-")}`,
            meta: `retry: ${valueOf(task, ["retry_count"], 0)}`
          }));
        }

        if (this.latestAction === "message-templates") {
          title = "模板列表";
          hint = "模板由消息服务统一维护，业务只传模板 ID 和变量";
          cards = listOf(payload, ["template_list", "templates"]).map(template => ({
            label: valueOf(template, ["carrierType", "carrier_type"], "TEMPLATE"),
            title: valueOf(template, ["templateName", "template_name"], valueOf(template, ["templateCode", "template_code"], "模板")),
            description: valueOf(template, ["templateContent", "template_content"], "暂无模板内容"),
            meta: `template_id: ${valueOf(template, ["id", "template_id", "templateId"], "-")}`
          }));
        }

        if (this.latestAction === "create-template" || this.latestAction === "template-detail") {
          title = this.latestAction === "template-detail" ? "模板详情" : "创建模板";
          hint = "模板内容和变量定义可复用到多业务发送流程";
          const info = payload.template_info || payload;
          cards = [{
            label: valueOf(info, ["carrierType", "carrier_type"], this.forms.messageTemplate.carrier_type),
            title: valueOf(info, ["templateName", "template_name"], this.forms.messageTemplate.template_name),
            description: valueOf(info, ["templateContent", "template_content"], this.forms.messageTemplate.template_content),
            meta: `template_id: ${valueOf(payload, ["template_id", "templateId", "id"], valueOf(info, ["id"], "-"))}`
          }];
        }

        if (this.latestAction === "message-carriers") {
          title = "消息载体";
          hint = "邮件、短信、站内信、飞书等通道统一接入";
          cards = listOf(payload, ["carrier_list", "carriers"]).map(carrier => ({
            label: valueOf(carrier, ["status"], "CARRIER"),
            title: valueOf(carrier, ["carrierName", "carrier_name"], valueOf(carrier, ["carrierType", "carrier_type"], "载体")),
            description: valueOf(carrier, ["description"], "暂无描述"),
            meta: `carrier_id: ${valueOf(carrier, ["id", "carrier_id", "carrierId"], "-")}`
          }));
        }

        if (this.latestAction === "create-carrier" || this.latestAction === "create-carrier-account") {
          title = this.latestAction === "create-carrier-account" ? "创建载体账号" : "创建消息载体";
          hint = "通道与账号配置沉淀在消息服务中复用";
          cards = [{
            label: this.forms.messageCarrier.carrier_type,
            title: this.latestAction === "create-carrier-account"
              ? this.forms.messageCarrier.account_name
              : this.forms.messageCarrier.carrier_name,
            description: this.latestAction === "create-carrier-account"
              ? `服务商 ${this.forms.messageCarrier.provider}`
              : this.forms.messageCarrier.description,
            meta: `${this.latestAction === "create-carrier-account" ? "account" : "carrier"}_id: ${valueOf(payload, ["account_id", "carrier_id", "id"], "-")}`
          }];
        }

        if (this.latestAction === "process-receipt") {
          title = "第三方回执";
          hint = "第三方平台状态回调统一转换为消息任务状态";
          cards = [{
            label: valueOf(payload, ["platform"], this.forms.messageReceipt.platform),
            title: `任务 ${valueOf(payload, ["task_id"], this.forms.messageReceipt.task_id || "-")}`,
            description: `回执 ${valueOf(payload, ["receipt_status"], "-")}，内部状态 ${valueOf(payload, ["task_status"], "未匹配任务")}`,
            meta: `matched: ${valueOf(payload, ["matched"], false)}`
          }];
        }

        if (this.latestAction === "message-strategies") {
          title = "发送策略";
          hint = "重试次数和间隔由公共消息服务统一配置";
          cards = listOf(payload, ["strategy_list", "strategies"]).map(strategy => ({
            label: valueOf(strategy, ["status"], "STRATEGY"),
            title: valueOf(strategy, ["strategyName", "strategy_name"], "策略"),
            description: `最大重试 ${valueOf(strategy, ["maxRetries", "max_retries"], 0)} 次，间隔 ${valueOf(strategy, ["retryIntervalSeconds", "retry_interval_seconds"], 0)} 秒`,
            meta: `strategy_id: ${valueOf(strategy, ["id", "strategy_id", "strategyId"], "-")}`
          }));
        }

        if (this.latestAction === "create-strategy" || this.latestAction === "update-strategy") {
          title = this.latestAction === "create-strategy" ? "创建发送策略" : "更新发送策略";
          hint = "发送策略作为公共配置给多业务复用";
          cards = [{
            label: this.forms.messageStrategy.status,
            title: this.forms.messageStrategy.strategy_name,
            description: `最大重试 ${this.forms.messageStrategy.max_retries} 次，间隔 ${this.forms.messageStrategy.retry_interval_seconds} 秒`,
            meta: `strategy_id: ${valueOf(payload, ["strategy_id", "strategyId", "id"], this.forms.messageStrategy.strategy_id || "-")}`
          }];
        }

        if (!cards.length) {
          cards = [{
            label: "EMPTY",
            title: "暂无数据",
            description: "当前接口没有返回可展示的记录。",
            meta: ""
          }];
        }

        return {
          title,
          hint,
          cards: cards.slice(0, 6).map((card, index) => ({ ...card, key: `${this.latestAction}-${index}-${card.title}` }))
        };
      },
      visualResult() {
        return this.userVisualResult || this.messageVisualResult;
      }
    },
    mounted() {
      this.thirdPartyMessageHandler = (event) => {
        if (event.origin !== window.location.origin) return;
        const payload = event.data || {};
        if (payload.type === "third-party-oauth-code") {
          this.serviceState.user = payload.error ? "error" : "ok";
          this.latestPayload = payload;
          this.latestAction = "third-party-callback";
          this.latestResponse = pretty(payload);
          this.notify(payload.error ? "第三方授权返回异常" : "第三方授权 code 已返回");
          return;
        }
        if (payload.type !== "third-party-authorized") return;
        this.forms.thirdParty.provider = payload.provider || this.forms.thirdParty.provider;
        this.forms.thirdParty.open_id = payload.open_id || this.forms.thirdParty.open_id;
        this.forms.thirdParty.nickname = payload.nickname || this.forms.thirdParty.nickname;
        this.notify(`${providerLabel(this.forms.thirdParty.provider)}授权完成，正在统一登录`);
        this.loginByThirdPartyCredential();
      };
      window.addEventListener("message", this.thirdPartyMessageHandler);
      const callbackPayload = readOauthCallback();
      if (callbackPayload) {
        if (window.opener && !window.opener.closed) {
          window.opener.postMessage(callbackPayload, window.location.origin);
          window.setTimeout(() => window.close(), 900);
        }
        this.selectedId = "user";
        this.serviceState.user = callbackPayload.error ? "error" : "ok";
        this.latestPayload = callbackPayload;
        this.latestAction = "third-party-callback";
        this.latestResponse = pretty(callbackPayload);
        this.notify(callbackPayload.error ? "第三方授权返回异常" : "第三方授权 code 已返回");
      }
    },
    beforeUnmount() {
      if (this.thirdPartyMessageHandler) {
        window.removeEventListener("message", this.thirdPartyMessageHandler);
      }
    },
    methods: {
      statusText(id) {
        return ({ idle: "未调用", ok: "调用成功", error: "调用异常" })[this.serviceState[id]];
      },
      notify(message) {
        this.toast = message;
        window.clearTimeout(this.toastTimer);
        this.toastTimer = window.setTimeout(() => {
          this.toast = "";
        }, 2400);
      },
      async callService(id, action, actionName = "") {
        try {
          console.log(`${id} service request start`);
          const data = await action();
          console.log(`${id} service response`, data);
          this.serviceState[id] = "ok";
          this.latestResponse = pretty(data);
          this.latestPayload = data;
          this.latestAction = actionName;
          const createdUserId = valueOf(data, ["user_id", "userId", "id"]);
          if (id === "user" && createdUserId && actionName === "create-user") {
            this.forms.userRelation.userId = String(createdUserId);
          }
          this.notify(`${this.selected.shortName}组件调用成功`);
          return data;
        } catch (error) {
          console.error(`${id} service request failed`, error);
          this.serviceState[id] = "error";
          this.latestResponse = pretty({ error: error.message });
          this.latestPayload = null;
          this.latestAction = "";
          this.notify(`${this.selected.shortName}组件调用失败`);
          return null;
        }
      },
      inputError(message, id = "user") {
        this.serviceState[id] = "error";
        this.latestPayload = null;
        this.latestAction = "";
        this.latestResponse = pretty({ error: message });
        this.notify(message);
      },
      createUser() {
        const body = { ...this.forms.user, password: "Demo@123456" };
        return this.callService("user", () => requestJson("/api/v1/topbiz/users", {
          method: "POST",
          body: JSON.stringify(body)
        }), "create-user");
      },
      async queryUsers() {
        const query = toQuery({
          keyword: this.forms.userQuery.keyword,
          status: this.forms.userQuery.status,
          page: 1,
          page_size: 10
        });
        const data = await this.callService("user", () => requestJson(`/api/v1/topbiz/users${query}`), "users");
        const firstUser = listOf(data, ["user_list", "users", "records"])[0];
        const firstUserId = valueOf(firstUser, ["id", "user_id", "userId"]);
        if (!this.forms.userRelation.userId && firstUserId) {
          this.forms.userRelation.userId = String(firstUserId);
        }
      },
      async queryGroups() {
        const query = toQuery({ keyword: this.forms.userQuery.groupKeyword, page: 1, page_size: 10 });
        const data = await this.callService("user", () => requestJson(`/api/v1/topbiz/groups${query}`), "groups");
        const firstGroup = listOf(data, ["group_list", "groups", "records"])[0];
        const firstGroupId = valueOf(firstGroup, ["id", "group_id", "groupId"]);
        if (!this.forms.userRelation.groupId && firstGroupId) {
          this.forms.userRelation.groupId = String(firstGroupId);
        }
      },
      async queryPermissions() {
        const data = await this.callService("user", () => requestJson("/api/v1/topbiz/permissions"), "permissions");
        const firstPermission = listOf(data, ["permission_list", "permissions", "records"])[0];
        const firstPermissionId = valueOf(firstPermission, ["id", "permission_id", "permissionId"]);
        if (!this.forms.userRelation.permissionIds && firstPermissionId) {
          this.forms.userRelation.permissionIds = String(firstPermissionId);
        }
      },
      addUserToGroup() {
        const groupId = Number(this.forms.userRelation.groupId);
        const userId = Number(this.forms.userRelation.userId);
        if (!groupId || !userId) {
          this.inputError("请先填写用户 ID 和分组 ID。");
          return null;
        }
        return this.callService("user", () => requestJson(`/api/v1/topbiz/groups/${groupId}/users`, {
          method: "POST",
          body: JSON.stringify({ user_id: userId })
        }), "add-user-group");
      },
      queryGroupUsers() {
        const groupId = Number(this.forms.userRelation.groupId);
        if (!groupId) {
          this.inputError("请先填写分组 ID。");
          return null;
        }
        return this.callService("user", () => requestJson(`/api/v1/topbiz/groups/${groupId}/users?page=1&page_size=10`), "group-users");
      },
      grantGroupPermissions() {
        const groupId = Number(this.forms.userRelation.groupId);
        const permissionIds = parseIdList(this.forms.userRelation.permissionIds);
        if (!groupId || !permissionIds.length) {
          this.inputError("请先填写分组 ID 和权限 ID。");
          return null;
        }
        return this.callService("user", () => requestJson(`/api/v1/topbiz/groups/${groupId}/permissions`, {
          method: "POST",
          body: JSON.stringify({ permission_ids: permissionIds })
        }), "grant-permissions");
      },
      queryGroupPermissions() {
        const groupId = Number(this.forms.userRelation.groupId);
        if (!groupId) {
          this.inputError("请先填写分组 ID。");
          return null;
        }
        return this.callService("user", () => requestJson(`/api/v1/topbiz/groups/${groupId}/permissions`), "group-permissions");
      },
      async openThirdPartyLogin() {
        const popup = window.open("about:blank", "_blank", "width=520,height=680");
        if (popup) {
          popup.document.title = "第三方授权核验";
          popup.document.body.innerHTML = "<p style='font-family: sans-serif; padding: 24px;'>正在准备授权核验页...</p>";
        }
        const data = await this.callService("user", () => requestJson("/api/v1/topbiz/auth/third-party/authorize-url", {
          method: "POST",
          body: JSON.stringify({
            provider: this.forms.thirdParty.provider,
            redirect_uri: `${window.location.origin}/`,
            state: oauthState(this.forms.thirdParty.provider)
          })
        }), "third-party-authorize");
        if (!data) {
          if (popup && !popup.closed) popup.close();
          return null;
        }
        const authUrl = buildThirdPartyAuthUrl(data, this.forms.thirdParty);
        if (popup && !popup.closed) {
          popup.location.href = authUrl;
        } else {
          window.open(authUrl, "_blank");
        }
        return data;
      },
      async loginByThirdPartyCredential() {
        if (!String(this.forms.thirdParty.open_id || "").trim()) {
          this.inputError("请先填写第三方 OpenID / UnionID。");
          return null;
        }
        const data = await this.callService("user", () => requestJson("/api/v1/topbiz/auth/third-party-login", {
          method: "POST",
          body: JSON.stringify({
            provider: this.forms.thirdParty.provider,
            open_id: this.forms.thirdParty.open_id,
            nickname: this.forms.thirdParty.nickname,
            auto_register: this.forms.thirdParty.auto_register === "true",
            extra_info: {
              source: "topbiz-demo"
            }
          })
        }), "third-party-login");
        const userId = valueOf(data?.user, ["id"]);
        if (userId) this.forms.userRelation.userId = String(userId);
      },
      buildMessageBody(includePlannedTime = false) {
        const body = {
          receiver_id: this.forms.message.receiver_id,
          carrier_type: this.forms.message.carrier_type,
          title: this.forms.message.title,
          content: this.forms.message.content,
          variables: { name: this.forms.message.template_name }
        };
        if (String(this.forms.message.template_id || "").trim()) {
          body.template_id = Number(this.forms.message.template_id);
        }
        if (includePlannedTime) {
          body.planned_time = this.forms.message.planned_time;
        }
        return body;
      },
      async sendMessage() {
        const data = await this.callService("message", () => requestJson("/api/v1/topbiz/messages/send-now", {
          method: "POST",
          body: JSON.stringify(this.buildMessageBody(false))
        }), "send-now");
        const taskId = valueOf(data, ["task_id", "taskId"]);
        if (taskId) this.forms.messageReceipt.task_id = String(taskId);
      },
      async sendScheduledMessage() {
        if (!this.forms.message.planned_time || new Date(this.forms.message.planned_time).getTime() <= Date.now()) {
          this.forms.message.planned_time = defaultPlannedTime();
        }
        const data = await this.callService("message", () => requestJson("/api/v1/topbiz/messages/send-scheduled", {
          method: "POST",
          body: JSON.stringify(this.buildMessageBody(true))
        }), "send-scheduled");
        const taskId = valueOf(data, ["task_id", "taskId"]);
        if (taskId) this.forms.messageReceipt.task_id = String(taskId);
      },
      async queryMessageRecords() {
        const query = toQuery({
          receiver_id: this.forms.message.receiver_id,
          carrier_type: this.forms.message.carrier_type,
          page: 1,
          page_size: 10
        });
        const data = await this.callService("message", () => requestJson(`/api/v1/topbiz/messages/records${query}`), "message-records");
        const firstTask = listOf(data, ["task_list", "records"])[0];
        const taskId = valueOf(firstTask, ["task_id", "taskId"]);
        if (!this.forms.messageReceipt.task_id && taskId) this.forms.messageReceipt.task_id = String(taskId);
      },
      async queryMessageTemplates() {
        const query = toQuery({
          carrier_type: this.forms.messageTemplate.carrier_type,
          status: "ENABLED"
        });
        const data = await this.callService("message", () => requestJson(`/api/v1/topbiz/message-templates${query}`), "message-templates");
        const firstTemplate = listOf(data, ["template_list", "templates"])[0];
        const templateId = valueOf(firstTemplate, ["id", "template_id", "templateId"]);
        if (templateId) {
          this.forms.messageTemplate.template_id = String(templateId);
          if (!this.forms.message.template_id) this.forms.message.template_id = String(templateId);
        }
      },
      async createMessageTemplate() {
        const body = {
          template_code: this.forms.messageTemplate.template_code,
          template_name: this.forms.messageTemplate.template_name,
          carrier_type: this.forms.messageTemplate.carrier_type,
          template_content: this.forms.messageTemplate.template_content,
          variables: [{
            variable_name: "name",
            description: "接收人名称",
            required: true,
            default_value: this.forms.message.template_name || "用户"
          }]
        };
        const data = await this.callService("message", () => requestJson("/api/v1/topbiz/message-templates", {
          method: "POST",
          body: JSON.stringify(body)
        }), "create-template");
        const templateId = valueOf(data, ["template_id", "templateId", "id"]);
        if (templateId) {
          this.forms.messageTemplate.template_id = String(templateId);
          this.forms.message.template_id = String(templateId);
        }
      },
      queryMessageTemplateDetail() {
        const templateId = Number(this.forms.messageTemplate.template_id || this.forms.message.template_id);
        if (!templateId) {
          this.inputError("请先填写模板 ID。", "message");
          return null;
        }
        return this.callService("message", () => requestJson(`/api/v1/topbiz/message-templates/${templateId}`), "template-detail");
      },
      async queryMessageCarriers() {
        const query = toQuery({
          carrier_type: this.forms.messageCarrier.carrier_type,
          status: this.forms.messageCarrier.status
        });
        const data = await this.callService("message", () => requestJson(`/api/v1/topbiz/message-carriers${query}`), "message-carriers");
        const firstCarrier = listOf(data, ["carrier_list", "carriers"])[0];
        const carrierId = valueOf(firstCarrier, ["id", "carrier_id", "carrierId"]);
        if (carrierId) this.forms.messageCarrier.carrier_id = String(carrierId);
      },
      async createMessageCarrier() {
        const body = {
          carrier_type: this.forms.messageCarrier.carrier_type,
          carrier_name: this.forms.messageCarrier.carrier_name,
          description: this.forms.messageCarrier.description,
          status: "ENABLED"
        };
        const data = await this.callService("message", () => requestJson("/api/v1/topbiz/message-carriers", {
          method: "POST",
          body: JSON.stringify(body)
        }), "create-carrier");
        const carrierId = valueOf(data, ["carrier_id", "carrierId", "id"]);
        if (carrierId) this.forms.messageCarrier.carrier_id = String(carrierId);
      },
      createCarrierAccount() {
        const carrierId = Number(this.forms.messageCarrier.carrier_id);
        if (!carrierId) {
          this.inputError("请先填写载体 ID。", "message");
          return null;
        }
        return this.callService("message", () => requestJson(`/api/v1/topbiz/message-carriers/${carrierId}/accounts`, {
          method: "POST",
          body: JSON.stringify({
            account_name: this.forms.messageCarrier.account_name,
            provider: this.forms.messageCarrier.provider,
            config: { mock: true },
            status: "ENABLED"
          })
        }), "create-carrier-account");
      },
      processMessageReceipt() {
        const body = {
          task_id: this.forms.messageReceipt.task_id ? Number(this.forms.messageReceipt.task_id) : null,
          platform: this.forms.messageReceipt.platform,
          status: this.forms.messageReceipt.status,
          provider_message_id: this.forms.messageReceipt.provider_message_id,
          received_at: new Date().toISOString()
        };
        return this.callService("message", () => requestJson("/api/v1/topbiz/message-receipts", {
          method: "POST",
          body: JSON.stringify(body)
        }), "process-receipt");
      },
      async queryMessageStrategies() {
        const data = await this.callService("message", () => requestJson("/api/v1/topbiz/message-strategies?status=ENABLED"), "message-strategies");
        const firstStrategy = listOf(data, ["strategy_list", "strategies"])[0];
        const strategyId = valueOf(firstStrategy, ["id", "strategy_id", "strategyId"]);
        if (strategyId) this.forms.messageStrategy.strategy_id = String(strategyId);
      },
      async createMessageStrategy() {
        const body = {
          strategy_name: this.forms.messageStrategy.strategy_name,
          max_retries: Number(this.forms.messageStrategy.max_retries),
          retry_interval_seconds: Number(this.forms.messageStrategy.retry_interval_seconds),
          status: this.forms.messageStrategy.status
        };
        const data = await this.callService("message", () => requestJson("/api/v1/topbiz/message-strategies", {
          method: "POST",
          body: JSON.stringify(body)
        }), "create-strategy");
        const strategyId = valueOf(data, ["strategy_id", "strategyId", "id"]);
        if (strategyId) this.forms.messageStrategy.strategy_id = String(strategyId);
      },
      updateMessageStrategy() {
        const strategyId = Number(this.forms.messageStrategy.strategy_id);
        if (!strategyId) {
          this.inputError("请先填写策略 ID。", "message");
          return null;
        }
        const body = {
          strategy_name: this.forms.messageStrategy.strategy_name,
          max_retries: Number(this.forms.messageStrategy.max_retries),
          retry_interval_seconds: Number(this.forms.messageStrategy.retry_interval_seconds),
          status: this.forms.messageStrategy.status
        };
        return this.callService("message", () => requestJson(`/api/v1/topbiz/message-strategies/${strategyId}`, {
          method: "PUT",
          body: JSON.stringify(body)
        }), "update-strategy");
      },
      searchLogs(options = {}) {
        const fallbackWhenEmpty = options?.fallbackWhenEmpty !== false;
        const body = {};
        Object.entries(this.forms.log).forEach(([key, value]) => {
          if (String(value || "").trim()) {
            body[key] = key === "statusCode" ? Number(value) : value;
          }
        });
        console.log("search logs request", body);
        return this.callService("log", async () => {
          const data = await requestJson("/api/v1/topbiz/logs/search", {
            method: "POST",
            body: JSON.stringify(body)
          });
          if (fallbackWhenEmpty && Object.keys(body).length > 0 && totalOf(data) === 0) {
            console.log("search logs returned 0, fallback to all logs");
            const fallback = await requestJson("/api/v1/topbiz/logs/search", {
              method: "POST",
              body: JSON.stringify({})
            });
            return {
              ...fallback,
              note: "当前条件未命中，已自动展示全部日志。"
            };
          }
          return data;
        });
      },
      queryAllLogs() {
        this.forms.log = { serviceName: "", path: "", statusCode: "", traceId: "" };
        return this.searchLogs();
      },
      queryErrorLogs() {
        this.forms.log = { serviceName: "", path: "", statusCode: "500", traceId: "" };
        return this.searchLogs({ fallbackWhenEmpty: false });
      },
      resetLogFilters() {
        this.forms.log = { serviceName: "", path: "", statusCode: "", traceId: "" };
        this.serviceState.log = "idle";
        this.latestResponse = "日志查询条件已清空。";
      }
    }
  }).mount("#app");
});

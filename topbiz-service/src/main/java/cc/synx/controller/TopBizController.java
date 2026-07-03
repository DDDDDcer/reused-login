package cc.synx.controller;

import cc.synx.api.AuthApiClient;
import cc.synx.api.LogApiClient;
import cc.synx.api.MessageAdminApiClient;
import cc.synx.api.MessageApiClient;
import cc.synx.api.UserAdminApiClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/topbiz")
public class TopBizController {
    private final AuthApiClient authClient;
    private final UserAdminApiClient userClient;
    private final MessageApiClient messageClient;
    private final MessageAdminApiClient messageAdminClient;
    private final LogApiClient logClient;

    public TopBizController(AuthApiClient authClient, UserAdminApiClient userClient,
                            MessageApiClient messageClient, MessageAdminApiClient messageAdminClient,
                            LogApiClient logClient) {
        this.authClient = authClient;
        this.userClient = userClient;
        this.messageClient = messageClient;
        this.messageAdminClient = messageAdminClient;
        this.logClient = logClient;
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        return authClient.loginByCredentialPassword(body);
    }

    @PostMapping("/auth/third-party/authorize-url")
    public Map<String, Object> thirdPartyAuthorizeUrl(@RequestBody Map<String, Object> body) {
        return authClient.thirdPartyAuthorizeUrl(body);
    }

    @PostMapping("/auth/third-party-login")
    public Map<String, Object> thirdPartyLogin(@RequestBody Map<String, Object> body) {
        return authClient.loginByThirdPartyCredential(body);
    }

    @GetMapping("/users")
    public Map<String, Object> users(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return userClient.listUsers(keyword, status, page, pageSize);
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody Map<String, Object> body) {
        return userClient.createUser(body);
    }

    @GetMapping("/groups")
    public Map<String, Object> groups(@RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return userClient.listGroups(keyword, page, pageSize);
    }

    @PostMapping("/groups/{groupId}/users")
    public Map<String, Object> addUserToGroup(@PathVariable Long groupId, @RequestBody Map<String, Object> body) {
        return userClient.addUserToGroup(groupId, body);
    }

    @GetMapping("/groups/{groupId}/users")
    public Map<String, Object> groupUsers(@PathVariable Long groupId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return userClient.groupUsers(groupId, page, pageSize);
    }

    @GetMapping("/permissions")
    public Map<String, Object> permissions() {
        return userClient.listPermissions();
    }

    @GetMapping("/groups/{groupId}/permissions")
    public Map<String, Object> groupPermissions(@PathVariable Long groupId) {
        return userClient.groupPermissions(groupId);
    }

    @PostMapping("/groups/{groupId}/permissions")
    public Map<String, Object> grantPermissions(@PathVariable Long groupId, @RequestBody Map<String, Object> body) {
        return userClient.grantPermissions(groupId, body);
    }

    @PostMapping("/messages/send-now")
    public Map<String, Object> sendNow(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return messageClient.sendNow(request.getHeader("X-Sender-Id"),
                request.getHeader("Idempotency-Key"), body);
    }

    @PostMapping("/messages/send-scheduled")
    public Map<String, Object> sendScheduled(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return messageClient.sendScheduled(request.getHeader("X-Sender-Id"),
                request.getHeader("Idempotency-Key"), body);
    }

    @GetMapping("/messages/tasks/{taskId}")
    public Map<String, Object> messageTask(@PathVariable Long taskId) {
        return messageClient.taskDetail(taskId);
    }

    @GetMapping("/messages/records")
    public Map<String, Object> messageRecords(@RequestParam(required = false) String status,
                                              @RequestParam(name = "carrier_type", required = false) String carrierType,
                                              @RequestParam(name = "receiver_id", required = false) String receiverId,
                                              @RequestParam(name = "start_time", required = false) String startTime,
                                              @RequestParam(name = "end_time", required = false) String endTime,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
                                              HttpServletRequest request) {
        return messageClient.records(request.getHeader("X-Sender-Id"), status, carrierType, receiverId,
                startTime, endTime, page, pageSize);
    }

    @GetMapping("/message-templates")
    public Map<String, Object> messageTemplates(@RequestParam(name = "carrier_type", required = false) String carrierType,
                                                @RequestParam(required = false) String status) {
        return messageAdminClient.listTemplates(carrierType, status);
    }

    @PostMapping("/message-templates")
    public Map<String, Object> createMessageTemplate(@RequestBody Map<String, Object> body) {
        return messageAdminClient.createTemplate(body);
    }

    @GetMapping("/message-templates/{templateId}")
    public Map<String, Object> messageTemplate(@PathVariable Long templateId) {
        return messageAdminClient.templateDetail(templateId);
    }

    @GetMapping("/message-carriers")
    public Map<String, Object> messageCarriers(@RequestParam(name = "carrier_type", required = false) String carrierType,
                                               @RequestParam(required = false) String status) {
        return messageAdminClient.listCarriers(carrierType, status);
    }

    @PostMapping("/message-carriers")
    public Map<String, Object> createMessageCarrier(@RequestBody Map<String, Object> body) {
        return messageAdminClient.createCarrier(body);
    }

    @PostMapping("/message-carriers/{carrierId}/accounts")
    public Map<String, Object> createMessageCarrierAccount(@PathVariable Long carrierId,
                                                           @RequestBody Map<String, Object> body) {
        return messageAdminClient.createCarrierAccount(carrierId, body);
    }

    @PostMapping("/message-receipts")
    public Map<String, Object> processMessageReceipt(@RequestBody Map<String, Object> body) {
        return messageAdminClient.processReceipt(body);
    }

    @GetMapping("/message-strategies")
    public Map<String, Object> messageStrategies(@RequestParam(required = false) String status) {
        return messageAdminClient.listStrategies(status);
    }

    @PostMapping("/message-strategies")
    public Map<String, Object> createMessageStrategy(@RequestBody Map<String, Object> body) {
        return messageAdminClient.createStrategy(body);
    }

    @PutMapping("/message-strategies/{strategyId}")
    public Map<String, Object> updateMessageStrategy(@PathVariable Long strategyId,
                                                     @RequestBody Map<String, Object> body) {
        return messageAdminClient.updateStrategy(strategyId, body);
    }

    @PostMapping("/logs/search")
    public Map<String, Object> searchLogs(@RequestBody Map<String, Object> body) {
        return logClient.search(body);
    }
}

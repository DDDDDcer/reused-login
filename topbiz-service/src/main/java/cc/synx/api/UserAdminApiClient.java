package cc.synx.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.Map;

@HttpExchange("/api/v1/admin")
public interface UserAdminApiClient {
    @GetExchange("/users")
    Map<String, Object> listUsers(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(name = "page_size", defaultValue = "20") int pageSize);

    @GetExchange("/users/{userId}")
    Map<String, Object> getUser(@PathVariable Long userId);

    @PostExchange("/users")
    Map<String, Object> createUser(@RequestBody Map<String, Object> body);

    @PutExchange("/users/{userId}")
    Map<String, Object> updateUser(@PathVariable Long userId, @RequestBody Map<String, Object> body);

    @DeleteExchange("/users/{userId}")
    Map<String, Object> deleteUser(@PathVariable Long userId);

    @GetExchange("/groups")
    Map<String, Object> listGroups(@RequestParam(required = false) String keyword,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(name = "page_size", defaultValue = "20") int pageSize);

    @PostExchange("/groups/{groupId}/users")
    Map<String, Object> addUserToGroup(@PathVariable Long groupId, @RequestBody Map<String, Object> body);

    @GetExchange("/groups/{groupId}/users")
    Map<String, Object> groupUsers(@PathVariable Long groupId,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(name = "page_size", defaultValue = "20") int pageSize);

    @GetExchange("/permissions")
    Map<String, Object> listPermissions();

    @GetExchange("/groups/{groupId}/permissions")
    Map<String, Object> groupPermissions(@PathVariable Long groupId);

    @PostExchange("/groups/{groupId}/permissions")
    Map<String, Object> grantPermissions(@PathVariable Long groupId, @RequestBody Map<String, Object> body);
}

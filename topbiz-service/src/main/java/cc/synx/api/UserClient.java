package cc.synx.api;

import cc.synx.domain.User;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

public interface UserClient {

    @GetExchange
    List<User> queryUsers(@RequestParam(required = false) String id,
                          @RequestParam(required = false) String name);

    @PostExchange
    boolean addUsers(@RequestBody List<User> users);
}

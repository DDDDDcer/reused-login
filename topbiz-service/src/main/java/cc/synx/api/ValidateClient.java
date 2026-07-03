package cc.synx.api;

import cc.synx.domain.User;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * 远程调用验证用户接口的客户端
 */
public interface ValidateClient {

    @Async
    @PostExchange("/biz/validate")
    boolean validatesUsers(@RequestBody List<User> users, @RequestParam String url);
}

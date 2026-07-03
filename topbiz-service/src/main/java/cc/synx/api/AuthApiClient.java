package cc.synx.api;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

@HttpExchange("/api/v1/auth")
public interface AuthApiClient {
    @PostExchange("/login/credential-password")
    Map<String, Object> loginByCredentialPassword(@RequestBody Map<String, Object> body);

    @PostExchange("/third-party/authorize-url")
    Map<String, Object> thirdPartyAuthorizeUrl(@RequestBody Map<String, Object> body);

    @PostExchange("/login/third-party-credential")
    Map<String, Object> loginByThirdPartyCredential(@RequestBody Map<String, Object> body);

    @PostExchange("/logout")
    Map<String, Object> logout();

    @PostExchange("/register/account-password")
    Map<String, Object> registerByAccountPassword(@RequestBody Map<String, Object> body);
}

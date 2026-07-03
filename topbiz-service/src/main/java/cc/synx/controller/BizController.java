package cc.synx.controller;

import cc.synx.api.UserClient;


import cc.synx.api.ValidateClient;
import cc.synx.domain.User;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/biz")
public class BizController {

    private final UserClient userClient;
    private final ValidateClient validateClient;

    public BizController(UserClient userClient, ValidateClient validateClient) {
        this.userClient = userClient;
        this.validateClient = validateClient;
    }

/*    *//**
     * 新增用户并返回新增后的用户列表
     *
     * @param users
     * @return
     *//*
    @PostMapping
    public List<User> addAndGetUsers(@RequestBody List<User> users) {
        boolean b = userClient.addUsers(users);
        if (b) {
            return userClient.queryUsers(null, null);
        }
        return null;
    }*/

    @PostMapping("/validate")
    public boolean validateUsers(@RequestBody List<User> users,
                                 @RequestParam String url) {
        // 1. 遍历users，验证用户信息（示例：简单判断name不为空）
        List<String> normalUserIds = users.stream()
                .filter(user -> user.getName() != null && !user.getName().isEmpty())
                .map(User::getId)
                .collect(Collectors.toList());

        List<String> abnormalUserIds = users.stream()
                .filter(user -> user.getName() == null || user.getName().isEmpty())
                .map(User::getId)
                .collect(Collectors.toList());

        // 2. 构造返回数据
        String json = "{" +
                "\"normalUserIds\": " + normalUserIds + "," +
                "\"abnormalUserIds\": " + abnormalUserIds +
                "}";

        // 3. 请求URL将用户ID列表传回

        return true;
    }

    @PutMapping("/callback-user-info")
    public boolean updateUserInfo(@RequestBody String jsonString) {
        JSONObject jsonObject = new JSONObject(Integer.parseInt(jsonString));
        List<String> normalUserIds = jsonObject.getJSONArray("normalUserIds").toJavaList(String.class);
        List<String> abnormalUserIds = jsonObject.getJSONArray("abnormalUserIds").toJavaList(String.class);

        // 对正常用户和异常用户进行处理（示例：打印日志）
        System.out.println("Normal user IDs: " + normalUserIds);
        System.out.println("Abnormal user IDs: " + abnormalUserIds);

        return true;
    }

    @PostMapping
    public List<User> addAndGetUsers(@RequestBody List<User> users) {
        boolean b = userClient.addUsers(users);
        if (b) {
            validateClient.validatesUsers(users, "http://localhost:8081/biz/callback-user-info");
            return userClient.queryUsers(null, null);
        }
        return null;
    }
}
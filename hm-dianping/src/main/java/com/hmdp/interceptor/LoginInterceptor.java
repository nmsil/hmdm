package com.hmdp.interceptor;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    private static final ObjectMapper mapper = new ObjectMapper();

    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头中的token
        String token = request.getHeader("authorization");
        // 2.基于token获取redis中的用户json字符串，并转为userDTO对象
        String tokenKey = LOGIN_USER_KEY + token;
        String json = stringRedisTemplate.opsForValue().get(tokenKey);
        UserDTO userDTO = mapper.readValue(json, UserDTO.class);
        if(userDTO == null){
            //3.不存在，拦截,返回401状态码
            response.setStatus(401);
            return false;
        }
        //4.将userDTO保存到TreadLocal
        UserHolder.saveUser(userDTO);
        //5.刷新token有效期
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL, TimeUnit.MINUTES);
        //6.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //销毁用户，释放内存
        UserHolder.removeUser();
    }
}

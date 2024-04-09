package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserMapper userMapper;

    private static final ObjectMapper mapper = new ObjectMapper();
    /*
     * 发送短信验证码并保存验证码
     * */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.验证手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            //2.手机号违规
            return Result.fail("手机号格式错误");
        }
        //3.生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4.保存验证码 —> redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5.发送验证码
        log.debug("发送验证码成功，验证码: {}",code);
        //返回Ok
        return Result.ok();
    }

    /*
     * 验证码实现登录，注册
     * */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机格式错误");
        }
        //2.检验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if(cacheCode == null || !cacheCode.equals(code)){
            //验证码错误
            return Result.fail("验证码错误");
        }

        //3.根据 手机号 查询 用户
        User user = userMapper.getUserByPhone(loginForm.getPhone());
        if(user == null){
            //4.如果用户不存在，注册新用户并保存
            user = CreateUserWithPhone(phone);
        }

        //5.保存用户到redis
        //5.1生成随机token
        String token = UUID.randomUUID().toString(true);
        //5.2将userDTO对象转为 json字符串
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        String json = null;
        try {
            json = mapper.writeValueAsString(userDTO);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        //5.3将json字符串保存到redis
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForValue().set(tokenKey,json,LOGIN_USER_TTL,TimeUnit.MINUTES);

        //6.返回token到前端
        return Result.ok(token);
    }

    private User CreateUserWithPhone(String phone) {
        User user = new User();
        //1.创建 默认名称
        String nickName = USER_NICK_NAME_PREFIX + RandomUtil.randomString(10);
        user.setPhone(phone);
        user.setNickName(nickName);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        //2.保存用户
        userMapper.saveUser(user);
        return user;
    }


}








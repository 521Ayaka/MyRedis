package com.ganga.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ganga.dto.UserDTO;
import com.ganga.utils.RedisConstants;
import com.ganga.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取用户token信息
        String token = request.getHeader("authorization");
        //判断请求用户是否存在
        if (StrUtil.isBlank(token)) {
            return true;
        }

        //基于token获取用户信息
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(token);
        if (ObjectUtil.isEmpty(entries)) {
            return true;
        }

        //转为 UserDTO 对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(entries, new UserDTO(), false);

        //保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);

        //更行 redis 中 token 有效日期
        stringRedisTemplate.expire(token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        //放行
        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //删除ThreadLocal保存的用户信息
        UserHolder.removeUser();
    }

}

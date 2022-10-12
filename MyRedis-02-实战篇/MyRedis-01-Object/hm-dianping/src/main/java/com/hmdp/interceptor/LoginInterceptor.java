package com.hmdp.interceptor;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取session中的用户信息
        Object user =request.getSession().getAttribute("user");
        //判断请求用户是否存在
        if (ObjectUtils.isEmpty(user)){
            //不存在：返回状态码提示 并 拦截
            response.setStatus(401);
            return false;
        }
        //存在：保存用户信息到ThreadLocal

        //UserHolder.saveUser(new UserDTO(user.getId(), user.getNickName(), user.getIcon()));
        UserHolder.saveUser((UserDTO) user);

        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //删除ThreadLocal保存的用户信息
        UserHolder.removeUser();
    }
}

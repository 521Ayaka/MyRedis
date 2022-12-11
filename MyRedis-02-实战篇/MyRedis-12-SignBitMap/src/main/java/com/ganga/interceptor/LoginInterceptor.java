package com.ganga.interceptor;

import cn.hutool.core.util.ObjectUtil;
import com.ganga.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //获取ThreadLocal中是否由用户
        if (ObjectUtil.isEmpty(UserHolder.getUser())){
            //报异常
            response.setStatus(401);
            //拦截
            return false;
        }

        //放行
        return true;

    }

}

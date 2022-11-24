package com.ganga.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ganga.entity.User;
import com.ganga.dto.LoginFormDTO;
import com.ganga.dto.Result;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    User createUserWithPhone(String phone);

    Result getUserById(Long id);
}

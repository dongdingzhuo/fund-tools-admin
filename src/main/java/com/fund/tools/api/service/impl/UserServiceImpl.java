package com.fund.tools.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fund.tools.api.entity.User;
import com.fund.tools.api.mapper.UserMapper;
import com.fund.tools.api.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户 Service 实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User findByUserName(String userName) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserName, userName);
        return this.getOne(wrapper);
    }
    
    @Override
    public User createUser(String userName, String nickName) {
        // 检查用户名是否已存在
        User existingUser = this.findByUserName(userName);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 创建新用户（ID由数据库自动增长生成）
        User user = new User();
        user.setUserName(userName);
        user.setNickName(nickName);
        
        this.save(user);
        return user;
    }
}

package com.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.review.entity.UserInfo;
import com.review.mapper.UserInfoMapper;
import com.review.service.IUserInfoService;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}

package com.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.review.entity.BlogComments;
import com.review.mapper.BlogCommentsMapper;
import com.review.service.IBlogCommentsService;
import org.springframework.stereotype.Service;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}

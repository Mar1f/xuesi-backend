package com.xuesi.xuesisi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.model.entity.UserClass;
import com.xuesi.xuesisi.service.UserClassService;
import com.xuesi.xuesisi.mapper.UserClassMapper;
import org.springframework.stereotype.Service;

/**
* @author mar1
* @description 针对表【user_class(学生班级关系表)】的数据库操作Service实现
* @createDate 2025-03-02 15:47:32
*/
@Service
public class UserClassServiceImpl extends ServiceImpl<UserClassMapper, UserClass>
    implements UserClassService{

}





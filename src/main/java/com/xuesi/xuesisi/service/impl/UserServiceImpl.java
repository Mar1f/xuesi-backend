package com.xuesi.xuesisi.service.impl;

import static com.xuesi.xuesisi.constant.UserConstant.USER_LOGIN_STATE;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuesi.xuesisi.common.ErrorCode;
import com.xuesi.xuesisi.constant.CommonConstant;
import com.xuesi.xuesisi.exception.BusinessException;
import com.xuesi.xuesisi.mapper.UserMapper;
import com.xuesi.xuesisi.model.dto.user.UserQueryRequest;
import com.xuesi.xuesisi.model.entity.User;
import com.xuesi.xuesisi.model.enums.UserRoleEnum;
import com.xuesi.xuesisi.model.vo.LoginUserVO;
import com.xuesi.xuesisi.model.vo.UserVO;
import com.xuesi.xuesisi.service.UserService;
import com.xuesi.xuesisi.utils.SqlUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
/**
 * 用户服务实现
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public User createStudent(User user) {
        // 强制设置角色为 "student"
        user.setUserRole("student");
        save(user);
        return user;
    }

    @Override
    public User getStudentById(Long id) {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("id", id).eq("user_role", "student");
        return getOne(query);
    }

    @Override
    public List<User> getAllStudents() {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("user_role", "student");
        return list(query);
    }

    @Override
    public User updateStudent(Long id, User user) {
        // 确保更新时角色依然为 "student"
        user.setId(id);
        user.setUserRole("student");
        if (updateById(user)) {
            return getStudentById(id);
        }
        return null;
    }

    @Override
    public void deleteStudent(Long id) {
        removeById(id);
    }

    @Override
    public Page<User> getStudentsPage(int pageNumber, int pageSize) {
        try {
            // 参数校验和边界处理
            if (pageNumber <= 0) {
                pageNumber = 1;
            }
            if (pageSize <= 0) {
                pageSize = 10;
            }
            if (pageSize > 100) {
                pageSize = 100;
            }

            // 创建分页对象
            Page<User> page = new Page<>(pageNumber, pageSize);
            
            // 构建查询条件
            QueryWrapper<User> query = new QueryWrapper<>();
            query.eq("user_role", "student");
            
            // 执行分页查询
            return this.page(page, query);
        } catch (Exception e) {
            log.error("分页查询异常", e);
            // 发生异常时返回空的第一页
            return new Page<>(1, 10);
        }
    }

    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "mar";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String userName, Integer gender) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            user.setUserName(userName);
            user.setGender(gender);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return getLoginUser(request);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 从 session 中获取用户信息
        User user = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        return user;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        try {
            Long id = userQueryRequest.getId();
            String userName = userQueryRequest.getUserName();
            String userProfile = userQueryRequest.getUserProfile();
            String userRole = userQueryRequest.getUserRole();
            Integer gender = userQueryRequest.getGender();
            String sortField = userQueryRequest.getSortField();
            String sortOrder = userQueryRequest.getSortOrder();

            // 基本字段查询
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }

            if (StringUtils.isNotBlank(userRole)) {
                queryWrapper.eq("user_role", userRole);
            }

            if (gender != null) {
                queryWrapper.eq("gender", gender);
            }

            // 模糊查询（限制长度）
            if (StringUtils.isNotBlank(userName)) {
                String safeUserName = StringUtils.substring(userName, 0, 50);
                queryWrapper.like("user_name", safeUserName);
            }

            if (StringUtils.isNotBlank(userProfile)) {
                String safeUserProfile = StringUtils.substring(userProfile, 0, 50);
                queryWrapper.like("user_profile", safeUserProfile);
            }

            // 排序处理
            if (StringUtils.isNotBlank(sortField)) {
                if (sortField.matches("^[a-zA-Z0-9_]{1,20}$")) {
                    boolean isAsc = CommonConstant.SORT_ORDER_ASC.equals(sortOrder);
                    queryWrapper.orderBy(true, isAsc, sortField);
                }
            } else {
                // 默认按照创建时间降序
                queryWrapper.orderByDesc("createTime");
            }

        } catch (Exception e) {
            log.error("构建查询条件异常", e);
        }

        return queryWrapper;
    }
}

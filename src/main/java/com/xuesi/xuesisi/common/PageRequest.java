package com.xuesi.xuesisi.common;

import com.xuesi.xuesisi.constant.CommonConstant;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 分页请求
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private long current = 1;

    /**
     * 页面大小
     */
    private long pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;

    /**
     * 获取当前页号（带校验）
     */
    public long getCurrent() {
        if (current <= 0) {
            return 1;
        }
        return Math.min(current, 1000);
    }

    /**
     * 获取页面大小（带校验）
     */
    public long getPageSize() {
        if (pageSize <= 0) {
            return 10;
        }
        if (pageSize > 100) {
            return 100;
        }
        return pageSize;
    }

    /**
     * 获取排序字段（带校验）
     */
    public String getSortField() {
        if (StringUtils.isBlank(sortField)) {
            return null;
        }
        // 安全性检查：只允许字母、数字和下划线，且长度不超过20
        if (!sortField.matches("^[a-zA-Z0-9_]{1,20}$")) {
            return null;
        }
        return sortField;
    }

    /**
     * 获取排序顺序（带校验）
     */
    public String getSortOrder() {
        if (StringUtils.isBlank(sortOrder)) {
            return CommonConstant.SORT_ORDER_ASC;
        }
        if (CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder)) {
            return CommonConstant.SORT_ORDER_ASC;
        }
        if (CommonConstant.SORT_ORDER_DESC.equalsIgnoreCase(sortOrder)) {
            return CommonConstant.SORT_ORDER_DESC;
        }
        return CommonConstant.SORT_ORDER_ASC;
    }
}

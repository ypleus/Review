package com.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.review.dto.Result;
import com.review.entity.ShopType;

public interface IShopTypeService extends IService<ShopType> {

    /**
     * 获取商品类型列表
     *
     * @return {@link Result}
     */
    Result getTypeList();
}

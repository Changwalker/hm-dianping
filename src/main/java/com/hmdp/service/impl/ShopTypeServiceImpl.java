package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryType() {
        String key = CACHE_SHOP_TYPE_KEY;
        // 查询redis缓存
        List<String> typeJson = stringRedisTemplate.opsForList().range(key, 0, -1);

        // 判断是否命中
        if(CollectionUtil.isNotEmpty(typeJson)){
            // 如果redis中存的为空对象(防止缓存穿透时存入的空对象)
            if(StrUtil.isBlank(typeJson.get(0))){
                return Result.fail("商品分类信息为空！");
            }
            // 命中则转换List<String> -> List<ShopType> 并返回、
            List<ShopType> typeList = new ArrayList<>();
            for(String jsonString : typeJson){
                ShopType shopType = JSONUtil.toBean(jsonString, ShopType.class);
                typeList.add(shopType);
            }
        }
        // 未命中：查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        // 数据库中不存在
        if(CollectionUtil.isEmpty(typeList)){
            // 添加空对象到redis，解决缓存穿透
            stringRedisTemplate.opsForList().rightPushAll(key, CollectionUtil.newArrayList(""));
            stringRedisTemplate.expire(key, CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("商品分类信息为空！");
        }
        // 数据库中存在，转换List<ShopType> -> List<String> 类型
        List<String> shopTypeList = new ArrayList<>();
        for(ShopType shopType : typeList){
            String jsonStr = JSONUtil.toJsonStr(shopType);
            shopTypeList.add(jsonStr);
        }
        stringRedisTemplate.opsForList().rightPushAll(key, shopTypeList);
        return Result.ok(typeList);
    }
}

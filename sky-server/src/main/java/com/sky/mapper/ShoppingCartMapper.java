package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 动态条件查询
     * @param shoppingCart
     * @return
     */
    //动态查询，可能返回多个数据，所以选择List来接收
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart);

    /**
     * 插入一条购物车数据
     * @param shoppingCart
     */
    void insert(ShoppingCart shoppingCart);

    /**
     * 清空购物车
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);

    /**
     * 根据主键删除购物车数据
     * @param shoppingCart
     */
    @Delete("delete from shopping_cart where id = #{id}")
    void deleteById(ShoppingCart shoppingCart);
}

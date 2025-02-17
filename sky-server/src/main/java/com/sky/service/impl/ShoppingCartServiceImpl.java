package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {

        //判断当前加到购物车中的商品是否已经存在 --Service层负责构建ShoppingCart对象，查询商品是否存在交给mapper层
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart); //如有，则dishId，dishFlavor，setmealId属性就已拷贝进来

        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        //如果已存在，只需要将数量 +1
        if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        } else {

            //如果不存在，需要插入一条购物车数据
            //判断本次添加到购物车的是菜品还是套餐
            Long dishId = shoppingCart.getDishId();
            if (dishId != null) {
                //说明本次添加到购物车的是菜品
                Dish dish = dishMapper.getById(dishId);

                shoppingCart.setImage(dish.getImage());
                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());

            } else {
                //说明本次添加到购物车的是套餐
                Long setmealId = shoppingCart.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);

                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setAmount(setmeal.getPrice());

            }

            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);

        }
    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    /**
     * 清空购物车
     */
    @Override
    public void cleanShoppingCart() {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 从购物车减少商品
     * @param shoppingCartDTO
     */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {

        //cart --是构建对象，提供userId，和shoppingCartDTO，用于mapper进行删减操作
        ShoppingCart cart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, cart);

        Long userId = BaseContext.getCurrentId();
        cart.setUserId(userId);

        //设置查询条件，查询当前用户登录的购物车数据
        List<ShoppingCart> list = shoppingCartMapper.list(cart);

        if(list != null && list.size() > 0) {
            //shoppingCart --是查询数据库里的真正对象
            ShoppingCart shoppingCart = list.get(0);

            if (shoppingCart.getNumber() == 1) {
                //如果菜品/套餐数量 number 等于1，直接删除当前记录
                shoppingCartMapper.deleteById(shoppingCart);
                //这里参考答案的形参是shoppingCart.getId() sql语句是一样的
            } else {
                //如果菜品/套餐数量 number 大于1，执行更新操作，修改份数
                shoppingCart.setNumber(shoppingCart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(shoppingCart);

            }
        }

    }
}

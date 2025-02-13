package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        /*
        停售的菜品不能加入套餐
         */
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //向套餐表插入一条数据
        setmealMapper.insert(setmeal);
        //获取insert语句生成的套餐主键值 --要结合sql语句操作
        Long setmealId = setmeal.getId();
        //把DTO封装的菜品扔进一个集合
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //给dishId赋值
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setDishId(setmealDish.getDishId());
        }
        if(setmealDishes.size() > 0 && setmealDishes != null) {
            //给lambda表达式中遍历出来的每一项起名字
            setmealDishes.forEach(setmealDish -> {setmealDish.setSetmealId(setmealId);});
            //批量插入套餐菜品
            setmealDishMapper.insertBatch(setmealDishes);
        }
//        if(flavors.size() > 0 && flavors != null) {
//            flavors.forEach(dishFlavor -> {
//                dishFlavor.setDishId(dishId);
//            });
//            //向口味表插入n条数据
//            dishFlavorMapper.insertBatch(flavors);
//        }

    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }
}

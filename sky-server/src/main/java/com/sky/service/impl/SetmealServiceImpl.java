package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
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

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    //参考答案注入了dishMapper，我自己没注入
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐, 同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //向套餐表插入一条数据
        setmealMapper.insert(setmeal);
        //获取insert语句生成的套餐主键值,即生成的套餐id --要结合sql语句操作
        Long setmealId = setmeal.getId();
        //把DTO封装的菜品扔进一个集合
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //给dishId赋值，参考答案没有这一步
//        for (SetmealDish setmealDish : setmealDishes) {
//            setmealDish.setDishId(setmealDish.getDishId());
//        }
        if(setmealDishes.size() > 0 && setmealDishes != null) {
            //给lambda表达式中遍历出来的每一项起名字
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            //批量插入套餐菜品
            setmealDishMapper.insertBatch(setmealDishes);
        }


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

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Transactional
    @Override
    public void updateWithDish(SetmealDTO setmealDTO) {

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //1. 修改套餐基本信息
        setmealMapper.update(setmeal);

        Long setmealId = setmealDTO.getId();
        //2. 删除原来套餐包含的菜品信息
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();

        if(setmealDishes.size() > 0 && setmealDishes != null) {
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
        }
        setmealDishMapper.deleteSetmealDish(setmealId);
        //3. 把新的菜品信息插入套餐
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 根据套餐id查询套餐和套餐菜品关系
     * @param id
     * @return
     */
    @Override
    public SetmealVO getBySetmealId(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);

        //SetmealVO对象好像是不能直接返回的
        //SetmealVO setmealVO = setmealMapper.getBySetmealId(id);
        //setmealMapper.getBySetmealId(id)这里选择返回一个List对象

        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 根据id批量删除套餐
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {

//        //启售中的套餐不能删除
//        for (Long id : ids) {
//            Integer status = setmealMapper.getBySetmealId(id).getStatus();
//            if(!(status == StatusConstant.DISABLE)) {
//                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
//            }
//        }
//
//        //删除套餐关联的菜品
//        setmealDishMapper.deleteBySetmealIds(ids);
//
//        //批量删除套餐
//        setmealMapper.deleteBySetmealIds(ids);

        //参考答案没有优化批量删除，而是取出一个id，操作一次数据库
        //TODO 只能删除一个，不能批量删除，我自己的代码问题好像是查询时返回的是VO对象
        ids.forEach(id -> {
            Setmeal setmeal =setmealMapper.getById(id);
            if(StatusConstant.ENABLE == setmeal.getStatus()) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        ids.forEach(setmealId -> {
            //删除套餐table中的数据
            setmealMapper.deleteById(setmealId);
            //删除套餐菜品关系表中的数据
            setmealDishMapper.deleteBySetmealId(setmealId);
        });

    }

    /**
     * 根据套餐id起售和停售套餐
     * @param id
     * @param status
     */
    @Override
    @Transactional
    public void startOrStop(Long id, Integer status) {
        //启售套餐时，先判断套餐内是否有停售菜品
        if(status == StatusConstant.ENABLE) {
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if(dishList != null && dishList.size() > 0) {
                dishList.forEach(dish -> {
                    if(StatusConstant.DISABLE == dish.getStatus()) {
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        //通过id和status构建Setmeal对象 先进行Setmeal对象的状态更新
        Setmeal setmeal = Setmeal.builder()
                                .status(status)
                                .id(id)
                                .build();

        setmealMapper.update(setmeal);
        //如果套餐关联的菜品处于停售状态，则套餐不允许起售status == 1
        //通过id和status构建



    }
}

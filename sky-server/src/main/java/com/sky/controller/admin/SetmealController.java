package com.sky.controller.admin;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.jdbc.core.JdbcOperationsExtensionsKt.query;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐相关接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;


    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增套餐")
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId")
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增菜品：{}", setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 修改套餐
     * @param setmealDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐：{}", setmealDTO);
        setmealService.updateWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 根据套餐id查询套餐，用于修改页面回显数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据套餐id查询套餐")
    public Result<SetmealVO> getBySetmealId(@PathVariable Long id) {
        log.info("根据套餐id查询套餐：{}", id);
        SetmealVO setmealVO = setmealService.getBySetmealId(id);
        return Result.success(setmealVO);
    }

    /**
     * 批量删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result delete(@RequestParam List<Long> ids) {
        log.info("根据套餐id批量删除套餐：{}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据id起售和停售套餐
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("套餐启售停售")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("根据id起售和停售套餐：{}, {}", id, status);
        setmealService.startOrStop(id, status);
        return Result.success();
    }

}

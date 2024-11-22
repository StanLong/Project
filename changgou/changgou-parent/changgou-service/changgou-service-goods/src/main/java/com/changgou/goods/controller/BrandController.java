package com.changgou.goods.controller;

import com.changgou.goods.pojo.Brand;
import com.changgou.goods.service.BrandService;
import com.github.pagehelper.PageInfo;
import entity.Result;
import entity.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/brand")
@CrossOrigin // 跨域
public class BrandController {

    @Autowired
    private BrandService brandService;

    @GetMapping
    public Result<List<Brand>> findAll(){
        List<Brand> brandList = brandService.findAll();
        return new Result<List<Brand>>(true, StatusCode.OK, "查询品牌集合成功", brandList);

    }

    @GetMapping(value = "/{id}")
    public Result<Brand> findById(@PathVariable(value = "id") Integer id){
        Brand brand = brandService.findById(id);
        return new Result<Brand>(true, StatusCode.OK, "根据ID查询品牌成功", brand);
    }

    @PostMapping
    public Result addBrand(@RequestBody Brand brand){
        brandService.addBrand(brand);
        return new Result(true, StatusCode.OK, "新增品牌成功");
    }

    @PutMapping(value = "/{id}")
    public Result updateBrand(@PathVariable(value = "id")Integer id, @RequestBody Brand brand){
        brand.setId(id);
        brandService.updateBrand(brand);
        return new Result(true, StatusCode.OK, "修改品牌成功");
    }

    @DeleteMapping(value = "/{id}")
    public Result deleteBrand(@PathVariable(value = "id") Integer id){
        brandService.deleteBrand(id);
        return new Result(true, StatusCode.OK, "删除品牌成功");
    }

    @PostMapping(value = "/search")
    public Result<List<Brand>> findBrand(@RequestBody Brand brand){
        List<Brand> brandList = brandService.findBrand(brand);
        return new Result<List<Brand>>(true, StatusCode.OK, "条件搜索查询品牌成功", brandList);
    }

    @GetMapping(value = "/search/{page}/{size}")
    public Result<PageInfo<Brand>> findPage(@PathVariable(value = "page") Integer page, @PathVariable(value = "size") Integer size){
        PageInfo<Brand> pageInfo = brandService.findPage(page, size);
        return new Result<PageInfo<Brand>>(true, StatusCode.OK, "分页查询成功", pageInfo);
    }

    @PostMapping(value = "/search/{page}/{size}")
    public Result<PageInfo<Brand>> findPage(@PathVariable(value = "page") Integer page, @PathVariable(value = "size") Integer size, @RequestBody Brand brand){
        PageInfo<Brand> pageInfo = brandService.findPage(page, size, brand);
        return new Result<PageInfo<Brand>>(true, StatusCode.OK, "条件搜索分页查询成功", pageInfo);
    }
}

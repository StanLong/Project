<template>
  <el-dialog
    :title="!dataForm.id ? '新增' : '修改'"
    :close-on-click-modal="false"
    :visible.sync="visible"
  >
    <el-form
      :model="dataForm"
      :rules="dataRule"
      ref="dataForm"
      @keyup.enter.native="dataFormSubmit()"
      label-width="120px"
    >
      <el-form-item label="SKU" prop="skuId">
        <!-- <el-input v-model="dataForm.skuId" placeholder="sku_id"></el-input> -->
        <el-select v-model="dataForm.skuId" placeholder="请选择SKU" clearable>
          <el-option :label="sku.skuName" :value="sku.skuId" v-for="sku in skuList" :key="sku.skuId"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="仓库" prop="wareId">
        <el-select v-model="dataForm.wareId" placeholder="请选择仓库" clearable>
          <el-option :label="w.name" :value="w.id" v-for="w in wareList" :key="w.id"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="库存数" prop="stock">
        <el-input v-model.number="dataForm.stock" placeholder="库存数"></el-input>
      </el-form-item>
      <el-form-item label="锁定库存" prop="stockLocked">
        <el-input v-model.number="dataForm.stockLocked" placeholder="锁定库存"></el-input>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
    </span>
  </el-dialog>
</template>

<script>
export default {
  data() {
    return {
      visible: false,
      wareList: [],
      skuList: [],
      skuDict: {},
      dataForm: {
        id: 0,
        skuId: "",
        wareId: "",
        stock: 0,
        skuName: "",
        stockLocked: 0
      },
      dataRule: {
        skuId: [{ required: true, message: "sku_id不能为空", trigger: "blur" }],
        wareId: [
          { required: true, message: "仓库id不能为空", trigger: "blur" }
        ],
        stock: [{ required: true, message: "库存数不能为空", trigger: "blur" }]
      }
    };
  },
  created(){
    this.getWares();
    this.getSkuList();
  },
  methods: {
    getWares() {
      this.$http({
        url: this.$http.adornUrl("/ware/wareinfo/list"),
        method: "get",
        params: this.$http.adornParams({
          page: 1,
          limit: 500
        })
      }).then(({ data }) => {
        this.wareList = data.page.list;
      });
    },
    getSkuList() {
      this.$http({
        url: this.$http.adornUrl("/product/skuinfo/list"),
        method: "get",
        params: this.$http.adornParams({
          page: 1,
          limit: 500
        })
      }).then(({ data }) => {
        this.skuList = data.page.list;
        this.skuList.forEach(item => {
          this.skuDict[item.skuId] = item.skuName;
        })
        console.log(this.skuDict)
      });
    },
    init(id) {
      this.dataForm.id = id || 0;
      this.visible = true;
      this.$nextTick(() => {
        this.$refs["dataForm"].resetFields();
        if (this.dataForm.id) {
          this.$http({
            url: this.$http.adornUrl(`/ware/waresku/info/${this.dataForm.id}`),
            method: "get",
            params: this.$http.adornParams()
          }).then(({ data }) => {
            if (data && data.code === 0) {
              this.dataForm.skuId = data.wareSku.skuId;
              this.dataForm.wareId = data.wareSku.wareId;
              this.dataForm.stock = data.wareSku.stock;
              this.dataForm.skuName = data.wareSku.skuName;
              this.dataForm.stockLocked = data.wareSku.stockLocked;
            }
          });
        }
      });
    },
    // 表单提交
    dataFormSubmit() {
      this.$refs["dataForm"].validate(valid => {
        if (valid) {
          this.$confirm(
            `实际环境中，库存的变化随着采购需求完成而实时变化，不能手动添加，此处只是为了测试功能，是否继续?`,
            "提示",
            {
              confirmButtonText: "确定",
              cancelButtonText: "取消",
              type: "warning"
            }
          ).then(() => {
            this.$http({
              url: this.$http.adornUrl(
                `/ware/waresku/${!this.dataForm.id ? "save" : "update"}`
              ),
              method: "post",
              data: this.$http.adornData({
                id: this.dataForm.id || undefined,
                skuId: this.dataForm.skuId,
                wareId: this.dataForm.wareId,
                stock: this.dataForm.stock,
                skuName: this.skuDict[this.dataForm.skuId],
                stockLocked: this.dataForm.stockLocked
              })
            }).then(({ data }) => {
              if (data && data.code === 0) {
                this.$message({
                  message: "操作成功",
                  type: "success",
                  duration: 1500,
                  onClose: () => {
                    this.visible = false;
                    this.$emit("refreshDataList");
                  }
                });
              } else {
                this.$message.error(data.msg);
              }
            });
          })
        }
      });
    }
  }
};
</script>

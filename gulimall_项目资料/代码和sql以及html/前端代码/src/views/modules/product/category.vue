<!--  -->
<template>
  <div>
    <el-button
      type="danger"
      @click="batchDeleteHandle()"
      >批量删除</el-button
    >
    <el-tree
      :data="menus"
      :props="defaultProps"
      :expand-on-click-node="false"
      show-checkbox
      node-key="catId"
      :default-expanded-keys="expandedKeys"
      :draggable="false"
      ref="tree"
    >
      <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <el-button
            v-if="node.level <= 2"
            type="text"
            size="mini"
            @click="() => append(data)"
          >
            Append
          </el-button>
          <el-button
            v-if="node.childNodes.length == 0"
            type="text"
            size="mini"
            @click="() => remove(node, data)"
          >
            Delete
          </el-button>
          <el-button type="text" size="mini" @click="() => edit(node, data)">
            Edit
          </el-button>
        </span>
      </span>
    </el-tree>
    <el-dialog
      :title="title"
      :visible.sync="dialogFormVisible"
      width="30%"
      :close-on-click-modal="false"
      :show-close="false"
    >
      <!-- form里面的所有数据都和model指定的对象进行绑定 -->
      <el-form :model="category" :rules="rules" ref="category">
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model.trim="category.name" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="菜单图标">
          <el-row>
            <el-col :span="24">
              <el-popover
                ref="iconListPopover"
                placement="bottom-start"
                trigger="click"
                popper-class="mod-menu__icon-popover"
              >
                <div class="mod-menu__icon-inner">
                  <div class="mod-menu__icon-list">
                    <el-button
                      v-for="(item, index) in iconList"
                      :key="index"
                      @click="iconActiveHandle(item)"
                      :class="{ 'is-active': item === category.icon }"
                    >
                      <icon-svg :name="item"></icon-svg>
                    </el-button>
                  </div>
                </div>
              </el-popover>
              <el-input
                v-model="category.icon"
                v-popover:iconListPopover
                :readonly="true"
                placeholder="菜单图标名称"
                class="icon-list__input"
              ></el-input>
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input
            v-model="category.productUnit"
            autocomplete="off"
          ></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="resetForm()">取 消</el-button>
        <el-button type="primary" @click="submit()">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
//这里可以导入其他文件（比如：组件，工具js，第三方插件js，json文件，图片文件等等）
//例如：import 《组件名称》 from '《组件路径》';
import Icon from "@/icons";
export default {
  data() {
    return {
      // 默认要展开的节点
      expandedKeys: [],
      // 所有一级菜单
      menus: [],
      defaultProps: {
        // 菜单对象的哪个属性是它的下一级菜单列表
        children: "children",
        //   菜单对象的那个属性用于显示
        label: "name",
      },
      // 是否显示对话框
      dialogFormVisible: false,
      // 对话框要显示的标题
      title: "",
      // 添加和修改共用一个对话框，需要一个变量来区别点击确定时进行添加还是修改操作
      dialogType: "",
      // 对话框表单绑定的对象
      category: {
        // catId由数据库自己生成
        catId: null,
        name: "",
        parentCid: 0,
        catLevel: 0,
        showStatus: 0,
        icon: "",
        productUnit: "",
      },
      // 校验规则
      rules: {
        name: [
          {
            type: "string",
            required: true,
            message: "请输入菜单名称",
            trigger: "blur",
          },
        ],
      },
      // 图标集合
      iconList: [],
      // 被选中的菜单项集合
      categorySelections: [],
    };
  },
  //方法集合
  methods: {
    // 获取全部菜单列表(一级菜单)
    getMenus() {
      this.$http({
        url: this.$http.adornUrl("/product/category/list/tree"),
        method: "get",
        params: this.$http.adornParams({}),
      }).then(({ data }) => {
        console.log(data);
        this.menus = data.list;
      });
    },
    // 点击添加菜单按钮(添加孩子)
    append(data) {
      console.log("append-->", data);
      // 设置对话框显示标题
      this.title = "添加菜单";
      // 设置当前是添加操作
      this.dialogType = "append";
      // 弹出填写框
      this.dialogFormVisible = true;
      // 为表单对象复制，name属性是双向绑定，不用手动赋值
      this.category.parentCid = data.catId;
      // *1 可以自动转型
      this.category.catLevel = data.catLevel * 1 + 1;
      // 新添加的菜单都默认是显示状态
      this.category.showStatus = 1;
      // 点击确定发起请求
    },
    edit(node, data) {
      console.log("edit-->", data);
      // 设置对话框显示标题
      this.title = "修改菜单";
      // 设置当前是添加操作
      this.dialogType = "edit";
      // 当前菜单(节点)的数据回显(表单绑定的是category对象，我们需要去数据库重新获取它，防止缓存)
      this.$http({
        url: this.$http.adornUrl("/product/category/info/" + data.catId),
        method: "get",
      }).then(({ data }) => {
        console.log("最新的category", data);
        // 把最新数据赋值给表单对象
        this.category = data.category;
      });
      // 弹出对话框
      this.dialogFormVisible = true;
      // 点击确定发起更新请求
    },
    // 点击删除按钮
    remove(node, data) {
      console.log("remove-->", node, data);
      // 弹出确认提示框
      // 点击确定后发送删除请求
      // 删除成功后弹出提示信息
      // 重新从后端获取菜单列表
      // 自动展开父级菜单
      // 注意此处的反引号
      this.$confirm(`是否删除【${data.name}】菜单项?`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
        //   点击确定
      })
        .then(() => {
          let ids = [data.catId];
          console.log(data.catId);
          // 发送httppost请求，进行删除
          this.$http({
            url: this.$http.adornUrl("/product/category/delete"),
            method: "post",
            data: this.$http.adornData(ids, false),
          }).then(({ data }) => {
            // 删除成功弹出提示框
            this.$message({
              type: "success",
              message: "删除成功!",
            });
            // 重新获取菜单列表
            this.getMenus();
            // 展开父级菜单
            this.expandedKeys = [node.parent.data.catId];
          });
        })
        .catch(() => {
          this.$message({
            type: "info",
            message: "已取消删除",
          });
        });
    },
    // 对话框点击确定处理
    submit() {
      // 先进行表单内容校验
      this.$refs["category"].validate((valid) => {
        if (valid) {
          if (this.dialogType === "append") {
            this.addCategory();
          } else if (this.dialogType === "edit") {
            this.updateCategory();
          }
          // 清空表单内容
          // this.category = {}
          this.resetForm();
        } else {
          console.log("error submit!!");
          return false;
        }
      });
    },
    // 清空指定表单对象内容
    resetForm() {
      // this.category = {}
      this.category.icon = "";
      this.category.productUnit = "";
      this.category.name = "";
      // 关闭对话框的显示
      this.dialogFormVisible = false;
    },
    // 在表单数据准备好后发起post请求修改菜单
    updateCategory() {
      console.log("updateCategory", this.category);
      this.$http({
        url: this.$http.adornUrl("/product/category/update"),
        method: "post",
        data: this.$http.adornData(this.category, false),
      }).then(({ data }) => {
        // 修改成功弹出提示框
        this.$message({
          type: "success",
          message: "修改成功!",
        });
        // 关闭对话框
        this.dialogFormVisible = false;
        // 重新获取全部菜单列表
        this.getMenus();
        // 设置默认展开的菜单节点(自己)
        this.expandedKeys = [this.category.parentCid];
      });
    },
    // 在表单数据准备好后发起post请求添加菜单
    addCategory() {
      // 判断菜单名字是否位填
      // if()
      // 发起httppost请求，将表单数据提交到后台
      this.$http({
        url: this.$http.adornUrl("/product/category/save"),
        method: "post",
        data: this.$http.adornData(this.category, false),
      }).then(({ data }) => {
        // 弹出消息提示框
        // 删除成功弹出提示框
        this.$message({
          type: "success",
          message: "添加成功!",
        });
        // 关闭对话框
        this.dialogFormVisible = false;
        // 重新获取全部菜单列表
        this.getMenus();
        // 设置默认展开的菜单节点(自己)
        this.expandedKeys = [this.category.parentCid];
      });
    },
    // 图标选中
    iconActiveHandle(iconName) {
      this.category.icon = iconName;
    },
    // 批量删除
    batchDeleteHandle() {
      // 获取被选中的全部节点
      let categorySelections = this.$refs.tree.getCheckedNodes();
      console.log(categorySelections);
      if (categorySelections.length == 0) {
        this.$message({
          message: "请先选择要删除的菜单项！",
          type: "warning",
        });
        return false;
      }
      let ids = [],
        nameList = [];
      for (let i = 0; i < categorySelections.length; ++i) {
        ids.push(categorySelections[i].catId);
        nameList.push(categorySelections[i].name);
      }
      console.log(ids, nameList);
      this.$confirm(
        `确定对【${nameList}】菜单项进行【批量删除】操作?`,
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
      )
        .then(() => {
          this.$http({
            url: this.$http.adornUrl("/product/category/delete"),
            method: "post",
            data: this.$http.adornData(ids, false),
          }).then(({ data }) => {
            console.log(data);
            if (data && data.code === 0) {
              this.$message({
                message: "操作成功",
                type: "success",
              });
              // 重新获取菜单列表
              this.getMenus();
            } else {
              this.$message.error(data.msg);
            }
          });
        })
        .catch(() => {});
    },
  },
  //监听属性 类似于data概念
  computed: {},
  //监控data中的数据变化
  watch: {},

  //生命周期 - 创建完成（可以访问当前this实例）
  created() {
    // 访问后端，拿到所有分类信息
    this.getMenus();
    this.iconList = Icon.getNameList();
  },
  //生命周期 - 挂载完成（可以访问DOM元素）
  mounted() {},
  beforeCreate() {}, //生命周期 - 创建之前
  beforeMount() {}, //生命周期 - 挂载之前
  beforeUpdate() {}, //生命周期 - 更新之前
  updated() {}, //生命周期 - 更新之后
  beforeDestroy() {}, //生命周期 - 销毁之前
  destroyed() {}, //生命周期 - 销毁完成
  activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
};
</script>
<style lang="scss">
.mod-menu {
  .menu-list__input,
  .icon-list__input {
    > .el-input__inner {
      cursor: pointer;
    }
  }
  &__icon-popover {
    width: 458px;
    overflow: hidden;
  }
  &__icon-inner {
    width: 478px;
    max-height: 258px;
    overflow-x: hidden;
    overflow-y: auto;
  }
  &__icon-list {
    width: 458px;
    padding: 0;
    margin: -8px 0 0 -8px;
    > .el-button {
      padding: 8px;
      margin: 8px 0 0 8px;
      > span {
        display: inline-block;
        vertical-align: middle;
        width: 18px;
        height: 18px;
        font-size: 18px;
      }
    }
  }
}
</style>
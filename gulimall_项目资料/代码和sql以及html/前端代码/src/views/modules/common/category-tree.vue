<!--  -->
<template>
  <el-tree
    :data="menus"
    :props="defaultProps"
    node-key="catId"
    :default-expanded-keys="expandedKeys"
    ref="tree"
    @node-click="handleClick"
    highlight-current
  >
  </el-tree>
</template>

<script>
//这里可以导入其他文件（比如：组件，工具js，第三方插件js，json文件，图片文件等等）
//例如：import 《组件名称》 from '《组件路径》';

export default {
  //import引入的组件需要注入到对象中才能使用
  components: {},
  data() {
    //这里存放数据
    return {
        // 所有三级分类菜单
      menus: [],
      defaultProps: {
        // 菜单对象的哪个属性是它的下一级菜单列表
        children: "children",
        //   菜单对象的那个属性用于显示
        label: "name",
      },
      expandedKeys: []
    };
  },
  //监听属性 类似于data概念
  computed: {},
  //监控data中的数据变化
  watch: {},
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
    // 处理某个分类被点击
    handleClick(data, node, component) {
        console.log("子组件category-tree发生点击事件", data, node, component)
        // 向父组件(导入此组件的组件)传递数据或发送一个事件
        // 这个事件名自己定义，在父组件引用此组件的标签上使用  @事件名="方法名(参数)" 即可处理
        this.$emit("category-tree-node-click", data, node, component)
    }
  },
  //生命周期 - 创建完成（可以访问当前this实例）
  created() {
      this.getMenus()
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
<style scoped>
/* @import url(); 引入公共css类 */
</style>
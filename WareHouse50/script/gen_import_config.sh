#!/bin/bash
XDATA_PATH=/opt/datax

python $XDATA_PATH/bin/gen_import_config.py -d gmall -t activity_info
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t activity_rule
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t base_category1
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t base_category2
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t base_category3
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t base_dic
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t base_province
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t base_region
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t base_trademark
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t cart_info
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t coupon_info
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t sku_attr_value
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t sku_info
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t sku_sale_attr_value
python $XDATA_PATH/bin/gen_import_config.py -d gmall -t spu_info


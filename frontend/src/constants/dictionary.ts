/**
 * 数据字典统一映射文件
 *
 * 原则：数据库只存英文编码（UPPER_SNAKE_CASE），前端通过映射表展示中文
 * 所有业务字典常量统一维护于此，禁止在各组件内重复定义
 */

// ==================== 配送方式 ====================
export const DELIVERY_TYPES = [
  { label: '自取', value: 'SELF_PICKUP' },
  { label: '配送', value: 'DELIVERY' },
  { label: '院内配送', value: 'IN_HOUSE_DELIVERY' },
  { label: '院内配送', value: 'HOSPITAL_DELIVERY' },
]

// ==================== 制剂类型（仅煎药相关） ====================
export const PREPARATION_TYPES = [
  { label: '汤剂', value: 'DECOCTION' },
  { label: '汤剂', value: '汤剂' },
  { label: '浓煎剂', value: 'CONCENTRATED_DECOCTION' },
  { label: '膏方', value: 'MEDICINAL_PASTE' },
  { label: '配方颗粒', value: 'GRANULES' },
  { label: '茶剂', value: 'MEDICINAL_TEA' },
  { label: '露剂', value: 'AROMATIC_WATER' },
]

// ==================== 服用方法 ====================
export const USAGE_METHODS = [
  { label: '内服', value: 'ORAL_INTERNAL' },
  { label: '内服', value: '内服' },
  { label: '外用', value: 'TOPICAL' },
  { label: '泡酒', value: 'WINE_SOAK' },
  { label: '熏蒸', value: 'FUMIGATION' },
  { label: '代茶饮', value: 'HERBAL_TEA' },
  { label: '水煎服', value: 'WATER_DECOCTION' },
  { label: '口服', value: 'ORAL' },
  { label: '温服', value: 'WARM_TAKE' },
  { label: '泡水代茶饮', value: 'INFUSION' },
  { label: '擦洗', value: 'WIPE_WASH' },
  { label: '敷贴', value: 'POULTICE' },
  { label: '浸泡', value: 'SOAK' },
  { label: '涂抹', value: 'APPLY' },
  { label: '煎服', value: 'DECOCT_TAKE' },
  { label: '冲服', value: 'DISSOLVE_TAKE' },
  { label: '灌肠', value: 'ENEMA' },
  { label: '含漱', value: 'GARGLE' },
  { label: '酊剂外用', value: 'TINCTURE_APPLY' },
  { label: '喷雾', value: 'SPRAY' },
  { label: '撒粉', value: 'DUSTING' },
  { label: '滴眼', value: 'EYE_DROPS' },
  { label: '滴耳', value: 'EAR_DROPS' },
  { label: '炖服', value: 'STEW_TAKE' },
  { label: '熏洗', value: 'STEAM_WASH' },
  { label: '涂擦', value: 'RUB' },
  { label: '输液', value: 'INFUSION_IV' },
]

// ==================== 处方来源 ====================
export const SOURCE_TYPES = [
  { label: '接口导入', value: 'INTERFACE' },
  { label: '手工录入', value: 'MANUAL' },
  { label: 'CSV导入', value: 'CSV_IMPORT' },
]

// ==================== 辅助函数 ====================
export function getDictLabel(value: string | undefined, dict: { label: string; value: string }[]): string {
  if (!value) return '-'
  return dict.find(d => d.value === value)?.label || value
}

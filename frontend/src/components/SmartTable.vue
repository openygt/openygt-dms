<template>
  <div class="smart-table">
    <!-- 工具栏 -->
    <div v-if="showToolbar" class="st-toolbar">
      <div class="st-toolbar-left">
        <slot name="toolbar-left" />
      </div>
      <div class="st-toolbar-right">
        <el-tooltip content="刷新" placement="top">
          <el-button text :icon="Refresh" circle size="small" @click="$emit('refresh')" />
        </el-tooltip>
        <el-tooltip content="密度" placement="top">
          <el-dropdown @command="handleDensityChange" trigger="click">
            <el-button text :icon="Grid" circle size="small" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="default" :class="{ active: density === 'default' }">默认</el-dropdown-item>
                <el-dropdown-item command="compact" :class="{ active: density === 'compact' }">紧凑</el-dropdown-item>
                <el-dropdown-item command="loose" :class="{ active: density === 'loose' }">宽松</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-tooltip>
        <el-tooltip content="列设置" placement="top">
          <el-dropdown trigger="click" :hide-on-click="false">
            <el-button text :icon="Setting" circle size="small" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="col in columnOptions" :key="col.prop">
                  <el-checkbox v-model="col.visible" :label="col.label" size="small" />
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-tooltip>
      </div>
    </div>

    <!-- 表格 -->
    <el-skeleton :rows="6" animated v-if="loading && skeleton" />
    <el-table
      v-else
      :data="data"
      v-bind="$attrs"
      :size="tableSize"
      :row-class-name="rowClassName"
      @selection-change="(sel: any[]) => $emit('selection-change', sel)"
    >
      <template v-for="col in visibleColumns" :key="col.prop">
        <el-table-column
          v-bind="col"
          :class-name="col.numeric ? 'ygt-num-col' : ''"
        >
          <template #default="scope" v-if="col.slot">
            <slot :name="col.slot" v-bind="scope" />
          </template>
          <template #header="scope" v-if="col.headerSlot">
            <slot :name="col.headerSlot" v-bind="scope" />
          </template>
        </el-table-column>
      </template>
      <template #empty>
        <EmptyState :title="emptyTitle" :description="emptyDescription">
          <template #action v-if="$slots.emptyAction">
            <slot name="emptyAction" />
          </template>
        </EmptyState>
      </template>
    </el-table>

    <!-- 分页 -->
    <div v-if="pagination && !loading" class="st-pagination">
      <el-pagination
        v-model:current-page="pageModel"
        v-model:page-size="pageSizeModel"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Refresh, Grid, Setting } from '@element-plus/icons-vue'
import EmptyState from './states/EmptyState.vue'

export interface SmartColumn {
  prop?: string
  label?: string
  width?: string | number
  minWidth?: string | number
  fixed?: string | boolean
  sortable?: boolean | string
  numeric?: boolean
  slot?: string
  headerSlot?: string
  visible?: boolean
  [key: string]: any
}

type Density = 'default' | 'compact' | 'loose'

interface Props {
  data: any[]
  columns: SmartColumn[]
  loading?: boolean
  skeleton?: boolean
  showToolbar?: boolean
  pagination?: boolean
  total?: number
  page?: number
  pageSize?: number
  emptyTitle?: string
  emptyDescription?: string
  density?: Density
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  skeleton: true,
  showToolbar: true,
  pagination: false,
  total: 0,
  page: 1,
  pageSize: 10,
  emptyTitle: '暂无数据',
  emptyDescription: '',
  density: 'default'
})

const emit = defineEmits<{
  refresh: []
  'update:page': [page: number]
  'update:pageSize': [size: number]
  'selection-change': [selection: any[]]
}>()

const density = ref<Density>(props.density)

const tableSize = computed(() => {
  const map: Record<Density, any> = { default: 'default', compact: 'small', loose: 'large' }
  return map[density.value]
})

const columnOptions = ref(
  props.columns.map(c => ({ ...c, visible: c.visible !== false }))
)

const visibleColumns = computed(() =>
  columnOptions.value.filter(c => c.visible !== false)
)

const pageModel = computed({
  get: () => props.page,
  set: (v) => emit('update:page', v)
})

const pageSizeModel = computed({
  get: () => props.pageSize,
  set: (v) => emit('update:pageSize', v)
})

function handleDensityChange(d: Density) {
  density.value = d
}

function rowClassName({ row, rowIndex }: { row: any; rowIndex: number }) {
  const base = ''
  // 可以在这里添加行状态样式
  return base
}

// 监听 columns prop 变化
watch(() => props.columns, (newCols) => {
  columnOptions.value = newCols.map(c => ({ ...c, visible: c.visible !== false }))
}, { deep: true })
</script>

<style scoped>
.smart-table {
  background: var(--ygt-bg-surface);
  border-radius: var(--ygt-radius-lg);
  border: 1px solid var(--ygt-border);
  overflow: hidden;
}

.st-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ygt-space-3) var(--ygt-space-4);
  border-bottom: 1px solid var(--ygt-border);
}

.st-toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--ygt-space-1);
}

.st-pagination {
  display: flex;
  justify-content: flex-end;
  padding: var(--ygt-space-3) var(--ygt-space-4);
  border-top: 1px solid var(--ygt-border);
}

:deep(.ygt-num-col) {
  font-variant-numeric: tabular-nums;
  font-feature-settings: "tnum";
}

:deep(.el-table) {
  --el-table-header-bg-color: var(--ygt-gray-50);
  --el-table-row-hover-bg-color: var(--ygt-bg-hover);
}

:deep(.el-table th) {
  font-weight: var(--ygt-fw-semibold);
  color: var(--ygt-text-secondary);
  font-size: var(--ygt-text-sm);
}
</style>

<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备清洗记录</span>
        </div>
      </template>
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="deviceCode" label="设备编码" width="120" />
        <el-table-column prop="washType" label="清洗类型" width="100">
          <template #default="{row}">
            <el-tag :type="row.washType === 2 ? 'danger' : 'info'">
              {{ row.washType === 2 ? '强化' : '常规' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="standardDuration" label="标准时长(分)" width="110" />
        <el-table-column prop="durationMin" label="实际时长(分)" width="110" />
        <el-table-column prop="result" label="结果" width="80">
          <template #default="{row}">
            <el-tag :type="row.result === 1 ? 'success' : row.result === 0 ? 'danger' : 'info'">
              {{ row.result === 1 ? '合格' : row.result === 0 ? '不合格' : '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const list = ref([])
const loading = ref(false)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/v1/eq/wash/list')
    list.value = res.data.data?.list || []
  } catch (e) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

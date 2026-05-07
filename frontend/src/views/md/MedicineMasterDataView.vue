<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>标准药材主数据</span>
          <span style="color: #999; font-size: 14px">来源：SPD药材主数据（{{ total }}条）</span>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="编码/名称/拼音" clearable />
        </el-form-item>
        <el-form-item label="毒性分类">
          <el-select v-model="search.drugLevel" placeholder="全部" clearable style="width: 120px">
            <el-option label="剧毒" value="剧毒" />
            <el-option label="大毒" value="大毒" />
            <el-option label="有毒" value="有毒" />
            <el-option label="小毒" value="小毒" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="pinyin" label="拼音" width="120" />
        <el-table-column prop="unitName" label="单位" width="80" />
        <el-table-column prop="drugLevel" label="毒性" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.drugLevel" :type="row.drugLevel === '剧毒' ? 'danger' : row.drugLevel === '有毒' ? 'warning' : 'info'" size="small">{{ row.drugLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="efficacyCategory" label="功效分类" width="100" />
        <el-table-column prop="medicinalPart" label="药用部位" width="100" />
        <el-table-column prop="processingMethod" label="炮制方法" width="100" />
        <el-table-column prop="isEnabled" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'info'" size="small">{{ row.isEnabled === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
      <el-pagination
        v-if="total > 0"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMdMedicineList } from '@/api/masterdata'

interface Medicine {
  id: number
  code: string
  name: string
  pinyin: string
  unitName: string
  drugLevel: string
  efficacyCategory: string
  medicinalPart: string
  processingMethod: string
  isEnabled: number
}

const list = ref<Medicine[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const search = reactive({ keyword: '', drugLevel: '' })

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (search.keyword) params.keyword = search.keyword
    const res: any = await getMdMedicineList(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function resetSearch() {
  search.keyword = ''
  search.drugLevel = ''
  page.value = 1
  fetchData()
}

onMounted(fetchData)
</script>

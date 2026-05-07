<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" clearable placeholder="模板编号/名称" style="width: 200px" />
        </el-form-item>
        <el-form-item label="模板类型">
          <el-select v-model="searchForm.templateType" clearable placeholder="全部" style="width: 140px" @change="handleSearch">
            <el-option label="浸泡" value="SOAK" />
            <el-option label="煎煮" value="DECOCT" />
            <el-option label="包装" value="PACKAGE" />
            <el-option label="配送" value="DELIVER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
          <el-button type="success" data-testid="create-btn" @click="handleCreate">新增</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" data-testid="data-table">
        <el-table-column type="index" width="50" />
        <el-table-column prop="templateCode" label="模板编号" min-width="120" />
        <el-table-column prop="templateName" label="模板名称" min-width="140" />
        <el-table-column prop="templateType" label="模板类型" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.templateType === 'SOAK'" type="primary">浸泡</el-tag>
            <el-tag v-else-if="row.templateType === 'DECOCT'" type="warning">煎煮</el-tag>
            <el-tag v-else-if="row.templateType === 'PACKAGE'" type="success">包装</el-tag>
            <el-tag v-else-if="row.templateType === 'DELIVER'" type="info">配送</el-tag>
            <span v-else>{{ row.templateType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="widthMm" label="宽度(mm)" min-width="100" />
        <el-table-column prop="heightMm" label="高度(mm)" min-width="100" />
        <el-table-column prop="status" label="状态" min-width="80">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">启用</el-tag>
            <el-tag v-else type="info">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @change="handleSearch"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板编号" prop="templateCode">
          <el-input v-model="form.templateCode" placeholder="请输入模板编号" />
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板类型" prop="templateType">
          <el-select v-model="form.templateType" placeholder="请选择模板类型" style="width: 100%">
            <el-option label="浸泡" value="SOAK" />
            <el-option label="煎煮" value="DECOCT" />
            <el-option label="包装" value="PACKAGE" />
            <el-option label="配送" value="DELIVER" />
          </el-select>
        </el-form-item>
        <el-form-item label="宽度(mm)" prop="widthMm">
          <el-input-number v-model="form.widthMm" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="高度(mm)" prop="heightMm">
          <el-input-number v-model="form.heightMm" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="模板内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入JSON格式的模板内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button data-testid="dialog-cancel-btn" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" data-testid="dialog-save-btn" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getLabelTemplateList,
  createLabelTemplate,
  updateLabelTemplate,
  deleteLabelTemplate
} from '@/api/print'

interface LabelTemplate {
  id?: number | string
  templateCode: string
  templateName: string
  templateType: string
  widthMm: number
  heightMm: number
  status: number
  content: string
}

const loading = ref(false)
const searchForm = reactive({
  keyword: '',
  templateType: ''
})
const tableData = ref<LabelTemplate[]>([])
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => (isEdit.value ? '编辑标签模板' : '新增标签模板'))
const formRef = ref<any>(null)
const form = reactive<LabelTemplate>({
  templateCode: '',
  templateName: '',
  templateType: '',
  widthMm: 50,
  heightMm: 30,
  status: 1,
  content: ''
})

const rules = {
  templateCode: [{ required: true, message: '请输入模板编号', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateType: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
  widthMm: [{ required: true, message: '请输入宽度', trigger: 'blur' }],
  heightMm: [{ required: true, message: '请输入高度', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

async function handleSearch() {
  loading.value = true
  try {
    const res: any = await getLabelTemplateList({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword || undefined,
      templateType: searchForm.templateType || undefined
    })
    const data = res.data || {}
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.templateCode = ''
  form.templateName = ''
  form.templateType = ''
  form.widthMm = 50
  form.heightMm = 30
  form.status = 1
  form.content = ''
}

function handleCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: LabelTemplate) {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleDelete(row: LabelTemplate) {
  try {
    await ElMessageBox.confirm('确认删除该标签模板？', '提示', { type: 'warning' })
    if (row.id) {
      await deleteLabelTemplate(row.id)
      ElMessage.success('删除成功')
      handleSearch()
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      // handled by interceptor
    }
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    if (isEdit.value && (form as any).id) {
      await updateLabelTemplate((form as any).id, form)
      ElMessage.success('更新成功')
    } else {
      await createLabelTemplate(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    handleSearch()
  } catch (e) {
    // handled by interceptor
  }
}

handleSearch()
</script>

<style scoped>
.page-container {
  padding: var(--ygt-space-4);
}
.search-card {
  margin-bottom: 16px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

<template>
  <div class="water-formula">
    <div class="page-header-title">加水公式：<span class="page-header-sub">按方剂类型/付数/药材计算加水量</span></div>
    <div class="page-header">

      <el-button type="primary" @click="handleAdd">新增公式</el-button>
    </div>

    <el-card>
      <el-table :data="formulaList" v-loading="loading">
        <el-table-column prop="formulaCode" label="公式编码" width="150" />
        <el-table-column prop="formulaName" label="公式名称" width="150" />
        <el-table-column prop="expression" label="表达式" />
        <el-table-column prop="expressionDesc" label="说明" />
        <el-table-column label="默认" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="success">是</el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleTest(row)">测试</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑公式' : '新增公式'" width="600px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="公式编码">
          <el-input v-model="form.formulaCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="公式名称">
          <el-input v-model="form.formulaName" />
        </el-form-item>
        <el-form-item label="表达式">
          <el-input v-model="form.expression" placeholder="如: prescription.volume * 2.5 + 50" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.expressionDesc" type="textarea" />
        </el-form-item>
        <el-form-item label="默认公式">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 测试弹窗 -->
    <el-dialog v-model="showTestDialog" title="测试公式" width="500px">
      <el-form label-width="120px">
        <el-form-item label="处方体积(ml)">
          <el-input-number v-model="testVars.volume" :min="0" />
        </el-form-item>
        <el-form-item label="剂数">
          <el-input-number v-model="testVars.dose" :min="1" />
        </el-form-item>
      </el-form>
      <div v-if="testResult" class="test-result">
        <el-divider />
        <p><strong>计算结果:</strong> {{ testResult.result }} ml</p>
        <p><strong>每剂约:</strong> {{ perDose }} ml</p>
      </div>
      <template #footer>
        <el-button @click="showTestDialog = false">关闭</el-button>
        <el-button type="primary" @click="runTest">计算</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWaterFormulas, createWaterFormula, updateWaterFormula, deleteWaterFormula, calculateWaterFormula } from '@/api/equipment'

const loading = ref(false)
const formulaList = ref<any[]>([])
const showDialog = ref(false)
const showTestDialog = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const form = reactive({
  formulaCode: '',
  formulaName: '',
  expression: '',
  expressionDesc: '',
  isDefault: false
})

const formRef = ref<any>(null)
const formRules = {
  formulaCode: [{ required: true, message: '公式编码不能为空', trigger: 'blur' }],
  formulaName: [{ required: true, message: '公式名称不能为空', trigger: 'blur' }],
  expression: [{ required: true, message: '表达式不能为空', trigger: 'blur' }],
  isDefault: [{ required: true, message: '是否默认不能为空', trigger: 'change' }],
}

const testVars = reactive({ volume: 200, dose: 7 })
const testResult = ref<any>(null)

const perDose = computed(() => {
  if (!testResult.value?.result || !testVars.dose) return '--'
  return (parseFloat(testResult.value.result) / testVars.dose).toFixed(1)
})

async function loadFormulas() {
  loading.value = true
  try {
    const res: any = await getWaterFormulas()
    formulaList.value = res.data || []
  } catch (err) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  currentId.value = null
  form.formulaCode = ''
  form.formulaName = ''
  form.expression = ''
  form.expressionDesc = ''
  form.isDefault = false
  showDialog.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  currentId.value = row.id
  form.formulaCode = row.formulaCode
  form.formulaName = row.formulaName
  form.expression = row.expression
  form.expressionDesc = row.expressionDesc
  form.isDefault = row.isDefault
  showDialog.value = true
}

function handleTest(row: any) {
  currentId.value = row.id
  testResult.value = null
  showTestDialog.value = true
}

async function runTest() {
  try {
    const res: any = await calculateWaterFormula(currentId.value!, {
      'prescription.volume': testVars.volume,
      'prescription.dose': testVars.dose
    })
    testResult.value = res.data
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '计算失败')
  }
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate()
  try {
    if (isEdit.value) {
      await updateWaterFormula(currentId.value!, { ...form })
    } else {
      await createWaterFormula({ ...form })
    }
    ElMessage.success('保存成功')
    showDialog.value = false
    loadFormulas()
  } catch (err) {
    ElMessage.error('保存失败')
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该公式?', '提示', { type: 'warning' })
    await deleteWaterFormula(row.id)
    ElMessage.success('删除成功')
    loadFormulas()
  } catch (err) {
    // cancelled
  }
}

onMounted(() => {
  loadFormulas()
})
</script>

<style scoped lang="scss">
.water-formula {
  padding: 16px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h2 { margin: 0; font-size: 20px; }
  }

  .test-result {
    padding: 16px;
    background: #f6ffed;
    border-radius: 4px;
    margin-top: 8px;

    p {
      margin: 8px 0;
      font-size: 16px;
    }
  }
}
</style>

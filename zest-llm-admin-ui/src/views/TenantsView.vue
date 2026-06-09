<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <p class="page-desc">管理多租户隔离与编码标识</p>
        <el-button type="primary" @click="openCreate">新建租户</el-button>
      </div>
    </div>

    <div v-loading="loading" class="table-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">租户列表</h3>
          <p class="table-panel-subtitle">共 {{ total }} 个</p>
        </div>
      </div>
      <el-table :data="tenants" stripe empty-text="暂无租户">
        <el-table-column prop="tenantCode" label="租户编码" min-width="160">
          <template #default="{ row }">
            <span class="code-link">{{ row.tenantCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="tenantName" label="名称" min-width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : row.status || '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
      </el-table>
      <div class="page-pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="新建租户" width="480px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="tenantRules" label-width="90px">
        <el-form-item label="租户编码" prop="tenantCode">
          <el-input v-model="createForm.tenantCode" placeholder="如 zest-demo" />
        </el-form-item>
        <el-form-item label="名称" prop="tenantName">
          <el-input v-model="createForm.tenantName" placeholder="租户显示名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { adminApi, normalizePage, type TenantVO } from '../api/admin'

const loading = ref(false)
const submitting = ref(false)
const tenants = ref<TenantVO[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ tenantCode: '', tenantName: '' })

const tenantRules: FormRules = {
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  tenantName: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const data = await adminApi.listTenants(page.value, pageSize.value)
    const pageData = normalizePage(data, page.value, pageSize.value)
    tenants.value = pageData.records
    total.value = pageData.total
  } finally {
    loading.value = false
  }
}

function onSizeChange() {
  page.value = 1
  load()
}

function openCreate() {
  createForm.tenantCode = ''
  createForm.tenantName = ''
  createVisible.value = true
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await adminApi.createTenant({ ...createForm })
    ElMessage.success('租户创建成功')
    createVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

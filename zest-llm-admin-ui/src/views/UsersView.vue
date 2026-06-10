<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <p class="page-desc">Admin 控制台用户与 RBAC（ADMIN / OPERATOR）</p>
        <el-button v-if="isAdmin" type="primary" @click="openCreate">新建用户</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="users" stripe empty-text="暂无用户">
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column prop="displayName" label="显示名" min-width="140" />
      <el-table-column prop="role" label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">{{ row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createdAt" label="创建时间" min-width="180" />
      <el-table-column v-if="isAdmin" label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item v-if="dialogMode === 'create'" label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password :placeholder="dialogMode === 'edit' ? '留空则不修改' : ''" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员 ADMIN" value="ADMIN" />
            <el-option label="运维 OPERATOR" value="OPERATOR" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dialogMode === 'edit'" label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, type AdminUserVO } from '../api/admin'

const loading = ref(false)
const saving = ref(false)
const users = ref<AdminUserVO[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogMode = ref<'create' | 'edit'>('create')
const form = ref({
  username: '',
  displayName: '',
  password: '',
  role: 'OPERATOR',
  status: 'ACTIVE'
})

const isAdmin = computed(() => localStorage.getItem('zest-llm-role') === 'ADMIN')

async function load() {
  loading.value = true
  try {
    users.value = await adminApi.listAdminUsers()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  dialogTitle.value = '新建用户'
  form.value = { username: '', displayName: '', password: '', role: 'OPERATOR', status: 'ACTIVE' }
  dialogVisible.value = true
}

function openEdit(row: AdminUserVO) {
  dialogMode.value = 'edit'
  dialogTitle.value = `编辑 ${row.username}`
  form.value = {
    username: row.username,
    displayName: row.displayName || '',
    password: '',
    role: row.role || 'OPERATOR',
    status: row.status || 'ACTIVE'
  }
  dialogVisible.value = true
}

async function submit() {
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      await adminApi.createAdminUser({
        username: form.value.username,
        displayName: form.value.displayName,
        password: form.value.password,
        role: form.value.role
      })
    } else {
      await adminApi.updateAdminUser(form.value.username, {
        displayName: form.value.displayName,
        password: form.value.password || undefined,
        role: form.value.role,
        status: form.value.status
      })
    }
    dialogVisible.value = false
    ElMessage.success('保存成功')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

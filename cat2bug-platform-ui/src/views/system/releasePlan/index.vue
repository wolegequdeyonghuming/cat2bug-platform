<template>
  <div class="app-container release-plan-page">
    <project-label class="release-plan-project-label" />

    <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="small" :inline="true" label-width="0">
      <el-form-item prop="releasePlanName">
        <el-input
          v-model="queryParams.releasePlanName"
          :placeholder="$t('release-plan.enter-name')"
          prefix-icon="el-icon-search"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">{{ $t('search') }}</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">{{ $t('reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['system:releasePlan:add']"
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
        >{{ $t('release-plan.create') }}</el-button>
      </el-col>
      <right-toolbar :show-search.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="releasePlanList" @sort-change="handleSortChange">
      <el-table-column :label="$t('release-plan.name')" align="left" prop="releasePlanName" min-width="220" sortable="custom">
        <template slot-scope="scope">
          <span class="release-plan-name-cell">{{ scope.row.releasePlanName }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('release-date')" align="center" prop="releaseDate" width="140" sortable="custom">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.releaseDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('remark')" align="left" prop="remark" min-width="200" :show-overflow-tooltip="true" />
      <el-table-column :label="$t('update-time')" align="center" prop="updateTime" width="170">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" align="left" class-name="small-padding fixed-width" width="240" fixed="right">
        <template slot-scope="scope">
          <el-button
            v-hasPermi="['system:releasePlan:edit']"
            size="mini"
            type="text"
            icon="el-icon-link"
            @click="handleAssociateDefect(scope.row)"
          >{{ $t('release-plan.associate-defect') }}</el-button>
          <el-button
            v-hasPermi="['system:releasePlan:edit']"
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
          >{{ $t('modify') }}</el-button>
          <el-button
            v-hasPermi="['system:releasePlan:remove']"
            size="mini"
            type="text"
            icon="el-icon-delete"
            class="red"
            @click="handleDelete(scope.row)"
          >{{ $t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改发版计划对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="560px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="$t('release-plan.name')" prop="releasePlanName">
          <el-input v-model="form.releasePlanName" :placeholder="$t('release-plan.enter-name')" maxlength="255" />
        </el-form-item>
        <el-form-item :label="$t('release-date')" prop="releaseDate">
          <el-date-picker
            v-model="form.releaseDate"
            type="date"
            value-format="yyyy-MM-dd"
            :placeholder="$t('please-select')"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="$t('remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :placeholder="$t('please-enter')" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">{{ $t('confirm') }}</el-button>
        <el-button @click="dialogVisible = false">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>

    <!-- 关联缺陷对话框 -->
    <el-dialog :title="associateDialogTitle" :visible.sync="associateDialogVisible" width="800px" append-to-body :close-on-click-modal="false">
      <el-form :inline="true" size="small" class="release-plan-defect-search">
        <el-form-item>
          <el-input
            v-model="associateKeyword"
            :placeholder="$t('defect.enter-name-or-version')"
            prefix-icon="el-icon-search"
            clearable
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="loadAllProjectDefects">{{ $t('search') }}</el-button>
        </el-form-item>
        <span class="release-plan-defect-tip">{{ $t('release-plan.select-defect') }}</span>
      </el-form>
      <el-table
        ref="associateDefectTable"
        v-loading="associateDefectLoading"
        :data="allProjectDefects"
        height="420"
        row-key="defectId"
        @selection-change="handleAssociateSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" :reserve-selection="false" />
        <el-table-column label="#" align="center" prop="projectNum" width="90">
          <template slot-scope="scope">
            <span>#{{ scope.row.projectNum }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="defectName" min-width="240" :show-overflow-tooltip="true">
          <template slot-scope="scope">
            <span class="associate-defect-name">{{ scope.row.defectName }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('module')" align="center" prop="moduleName" width="160" :show-overflow-tooltip="true" />
        <el-table-column :label="$t('version')" align="center" prop="moduleVersion" width="120" :show-overflow-tooltip="true" />
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitAssociateDefects">{{ $t('confirm') }}</el-button>
        <el-button @click="associateDialogVisible = false">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ProjectLabel from '@/components/Project/ProjectLabel'
import { listReleasePlan, getReleasePlan, addReleasePlan, updateReleasePlan, delReleasePlan, listDefectOfReleasePlan, associateDefects } from '@/api/system/releasePlan'
import { listDefect } from '@/api/system/defect'

export default {
  name: 'ReleasePlan',
  dicts: [],
  components: { ProjectLabel },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 发版计划列表
      releasePlanList: [],
      // 总数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        releasePlanName: null,
        orderByColumn: 'releaseDate',
        isAsc: 'desc',
        projectId: null
      },
      // 表单弹窗
      dialogVisible: false,
      dialogTitle: '',
      form: {},
      rules: {
        releasePlanName: [
          { required: true, message: this.$t('release-plan.enter-name'), trigger: 'blur' }
        ]
      },
      // 关联缺陷弹窗
      associateDialogVisible: false,
      associateDialogTitle: '',
      associateDefectLoading: false,
      associateKeyword: '',
      currentReleasePlanId: null,
      allProjectDefects: [],
      associateSelectedDefectIds: [],
      associatedDefectIds: []
    }
  },
  computed: {
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId)
    }
  },
  created() {
    this.queryParams.projectId = this.projectId
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      this.queryParams.projectId = this.projectId
      listReleasePlan(this.queryParams).then(response => {
        this.releasePlanList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleSortChange(column) {
      if (column.order) {
        this.queryParams.orderByColumn = column.prop
        this.queryParams.isAsc = column.order === 'ascending' ? 'asc' : 'desc'
      }
      this.getList()
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.releasePlanName = null
      this.handleQuery()
    },
    reset() {
      this.form = {
        releasePlanId: undefined,
        projectId: this.projectId,
        releasePlanName: undefined,
        releaseDate: undefined,
        remark: undefined
      }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.dialogTitle = this.$t('release-plan.create')
      this.dialogVisible = true
    },
    handleUpdate(row) {
      this.reset()
      getReleasePlan(row.releasePlanId).then(response => {
        this.form = response.data
        this.dialogTitle = this.$t('release-plan.edit')
        this.dialogVisible = true
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.form.projectId = this.projectId
          if (this.form.releasePlanId != null) {
            updateReleasePlan(this.form).then(() => {
              this.$modal.msgSuccess(this.$t('modify-success'))
              this.dialogVisible = false
              this.getList()
            })
          } else {
            addReleasePlan(this.form).then(() => {
              this.$modal.msgSuccess(this.$t('create-success'))
              this.dialogVisible = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      this.$modal.confirm(this.$t('confirm-delete') + '「' + row.releasePlanName + '」？').then(() => {
        return delReleasePlan(row.releasePlanId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess(this.$t('delete-success'))
      }).catch(() => {})
    },
    handleAssociateDefect(row) {
      this.currentReleasePlanId = row.releasePlanId
      this.associateDialogTitle = this.$t('release-plan.associate-defect') + '：' + row.releasePlanName
      this.associateDialogVisible = true
      this.associatedDefectIds = []
      this.associateSelectedDefectIds = []
      this.allProjectDefects = []
      this.$nextTick(() => {
        if (this.$refs.associateDefectTable) {
          this.$refs.associateDefectTable.clearSelection()
        }
        this.loadAssociatedDefectIds()
        this.loadAllProjectDefects()
      })
    },
    loadAssociatedDefectIds() {
      listDefectOfReleasePlan(this.currentReleasePlanId, { projectId: this.projectId, pageSize: 1000, pageNum: 1 }).then(response => {
        this.associatedDefectIds = (response.rows || []).map(d => d.defectId)
        this.preselectAssociatedDefects()
      })
    },
    loadAllProjectDefects() {
      this.associateDefectLoading = true
      const params = {
        projectId: this.projectId,
        pageNum: 1,
        pageSize: 1000,
        orderByColumn: 'projectNum',
        isAsc: 'desc',
        params: { delFlag: '0' }
      }
      if (this.associateKeyword && this.associateKeyword.trim()) {
        params.nameVersionKeyword = this.associateKeyword.trim()
      }
      listDefect(params).then(response => {
        this.allProjectDefects = response.rows || []
        this.preselectAssociatedDefects()
        this.associateDefectLoading = false
      }).catch(() => {
        this.associateDefectLoading = false
      })
    },
    preselectAssociatedDefects() {
      this.$nextTick(() => {
        const table = this.$refs.associateDefectTable
        if (!table) return
        this.allProjectDefects.forEach(row => {
          if (this.associatedDefectIds.indexOf(row.defectId) >= 0) {
            table.toggleRowSelection(row, true)
          } else {
            table.toggleRowSelection(row, false)
          }
        })
      })
    },
    handleAssociateSelectionChange(rows) {
      this.associateSelectedDefectIds = (rows || []).map(r => r.defectId)
    },
    submitAssociateDefects() {
      associateDefects(this.currentReleasePlanId, { defectIds: this.associateSelectedDefectIds }).then(() => {
        this.$modal.msgSuccess(this.$t('modify-success'))
        this.associateDialogVisible = false
      })
    }
  }
}
</script>

<style scoped>
.release-plan-page .release-plan-project-label {
  margin-bottom: 12px;
}
.release-plan-name-cell {
  font-weight: 600;
}
.release-plan-defect-search {
  margin-bottom: 8px;
}
.release-plan-defect-tip {
  color: #909399;
  font-size: 12px;
}
.associate-defect-name {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
</style>

<template>
  <div class="app-container">
    <el-card class="statistic-card">
      <div class="statistic-tools">
        <el-radio-group v-model="query.timeType" size="small" @change="handleTimeTypeChange">
          <el-radio-button :label="'date'">{{ $t('member-operation.day') }}</el-radio-button>
          <el-radio-button :label="'week'">{{ $t('member-operation.week') }}</el-radio-button>
          <el-radio-button :label="'month'">{{ $t('member-operation.month') }}</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-model="query.date"
          :type="query.timeType"
          value-format="yyyy-MM-dd"
          format="yyyy-MM-dd"
          :clearable="false"
          size="small"
          @change="handleQuery"
        />
        <el-button
          v-hasPermi="['system:memberOperation:statistic:query']"
          size="mini"
          type="primary"
          icon="el-icon-download"
          @click="handleExport"
        >{{ $t('member-operation.export') }}</el-button>
      </div>
      <el-table v-loading="loading" :data="list">
        <el-table-column :label="$t('member-operation.member')" align="left" min-width="200" sortable>
          <template slot-scope="scope">
            <span class="member-cell">
              <cat2-bug-avatar :member="scope.row" />
              <span class="member-name">{{ scope.row.nickName || scope.row.userName }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('member-operation.period')" align="center" prop="period" width="220" />
        <el-table-column :label="$t('member-operation.create')" align="center" prop="createCount" width="120" sortable />
        <el-table-column :label="$t('member-operation.repair')" align="center" prop="repairCount" width="120" sortable />
        <el-table-column :label="$t('member-operation.verify')" align="center" prop="verifyCount" width="120" sortable />
        <el-table-column :label="$t('member-operation.total')" align="center" width="120" sortable :sort-method="sortTotal">
          <template slot-scope="scope">
            <span>{{ scope.row.createCount + scope.row.repairCount + scope.row.verifyCount }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { listMemberOperationStatistic } from '@/api/system/statistic/memberOperation'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'

function formatDate(date) {
  const year = date.getFullYear()
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  return `${year}-${month}-${day}`
}

export default {
  name: 'MemberOperationStatistic',
  components: { Cat2BugAvatar },
  data() {
    return {
      loading: true,
      list: [],
      query: {
        timeType: 'date',
        date: formatDate(new Date())
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listMemberOperationStatistic(this.query).then(res => {
        this.list = res.data
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.getList()
    },
    handleTimeTypeChange() {
      this.getList()
    },
    sortTotal(a, b) {
      const totalA = a.createCount + a.repairCount + a.verifyCount
      const totalB = b.createCount + b.repairCount + b.verifyCount
      return totalA - totalB
    },
    handleExport() {
      this.download('/system/member-operation/statistic/export', {
        ...this.query
      }, `${this.$t('member-operation.statistic')}.xlsx`)
    }
  }
}
</script>

<style lang="scss" scoped>
.statistic-card {
  .statistic-tools {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
  }
  .member-cell {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    .member-name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
</style>

package com.appvoyager.litememo.domain.model

import com.appvoyager.litememo.domain.model.value.MemoId

data class ApplyMemoBulkActionCommand(val memoIds: List<MemoId>, val action: MemoBulkAction)

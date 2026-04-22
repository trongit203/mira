package com.apollo.mira.utils

// ============================================================
// TEST TAGS — tập trung tất cả semantics tag ở một chỗ
//
// Dùng trong production code:
//   Modifier.testTag(TestTags.FAB_ADD)
//
// Dùng trong test:
//   composeRule.onNodeWithTag(TestTags.FAB_ADD).performClick()
//
// Tại sao không dùng string trực tiếp?
// → Typo trong test hoặc UI sẽ không compile error, chỉ fail runtime
// → Centralize ở đây → rename dễ dàng, không bỏ sót chỗ nào
// ============================================================
object TestTags {
    // Dashboard
    const val FAB_ADD              = "fab_add"
    const val DASHBOARD_BALANCE    = "dashboard_balance"
    const val TRANSACTION_LIST     = "transaction_list"
    const val EMPTY_STATE          = "empty_state"

    // Add Transaction
    const val AMOUNT_FIELD         = "amount_field"
    const val NOTE_FIELD           = "note_field"
    const val SUBMIT_BUTTON        = "submit_button"
    const val BTN_NAVIGATE_BACK    = "btn_navigate_back"
    const val TYPE_TOGGLE_EXPENSE  = "type_toggle_expense"
    const val TYPE_TOGGLE_INCOME   = "type_toggle_income"
}

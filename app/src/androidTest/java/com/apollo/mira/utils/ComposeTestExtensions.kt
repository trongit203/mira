package com.apollo.mira.utils

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput

// ============================================================
// COMPOSE TEST EXTENSIONS — helper functions tái sử dụng
//
// Thay vì lặp lại boilerplate trong mỗi test:
//   composeTestRule.onNodeWithTag("amount_field").performTextInput("150000")
//
// Dùng extension:
//   composeTestRule.typeIntoField("amount_field", "150000")
//
// Làm test dễ đọc hơn, gần với ngôn ngữ user hơn

fun ComposeContentTestRule.typeIntoField(testTag: String, text: String) {
    onNodeWithTag(testTag)
        .performTextClearance() // clear text cũ trước
    onNodeWithTag(testTag)
        .performTextInput(text)
}

fun ComposeContentTestRule.clickButton(text: String) {
    onNodeWithText(text).performClick()
}

fun ComposeContentTestRule.assertTextDisplayed(text: String): SemanticsNodeInteraction =
    onNodeWithText(text).assertIsDisplayed()

fun ComposeContentTestRule.assertTextNotDisplayed(text: String) {
    onNodeWithText(text, useUnmergedTree = true).assertDoesNotExist()
}

fun ComposeContentTestRule.assertButtonEnabled(text: String) {
    onNodeWithText(text).assertIsEnabled()
}

fun ComposeContentTestRule.assertButtonDisabled(text: String) {
    onNodeWithText(text).assertIsNotEnabled()
}

// Dùng cho Snackbar — cần wait vì Snackbar animate vào
fun ComposeContentTestRule.waitForSnackbar(message: String) {
    waitUntil(timeoutMillis = 3_000) {
        runCatching {
            onNodeWithText(message).assertIsDisplayed()
            true
        }.getOrDefault(false)
    }
}



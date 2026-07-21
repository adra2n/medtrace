package com.yy.medtrace.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class ErrorViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `ErrorView should display error message`() {
        composeTestRule.setContent {
            ErrorView(message = "Test error")
        }
        composeTestRule.onNodeWithText("Test error").assertIsDisplayed()
    }
}
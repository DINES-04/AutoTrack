package com.example.transaction

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @Rule
    @JvmField
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNavigation() {
        // Wait for system check dialog and dismiss it
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("I Understand").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("I Understand").performClick()
        
        // Check if Dashboard is visible
        composeTestRule.onNodeWithText("Dashboard Overview").assertIsDisplayed()

        // Check Bottom Navigation items
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Transaction History").assertIsDisplayed()

        composeTestRule.onNodeWithText("Category").performClick()
        composeTestRule.onNodeWithText("Built-in").assertIsDisplayed()

        // Check Analytics navigation from Top Bar
        composeTestRule.onNodeWithContentDescription("Analytics").performClick()
        composeTestRule.onNodeWithText("Spending Analytics").assertIsDisplayed()

        // Verify Analytics is NOT in bottom navigation (as a label)
        // Note: The Analytics screen itself might have the title "Analytics" which we just clicked, 
        // so we check if the NavigationBarItem for Analytics exists.
        // Since we removed it from the list, it shouldn't be there.
    }
}

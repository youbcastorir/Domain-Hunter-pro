package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DomainHunterViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainAppScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testMainAppScreenInitializes() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DomainHunterViewModel(application)
    composeTestRule.setContent {
      MyApplicationTheme {
        MainAppScreen(viewModel = viewModel)
      }
    }
  }
}

package com.longweekend.optimizer.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records which code the app runs at startup and on its main screens, so it is compiled ahead
 * of time on install. Run with `./gradlew :app:generateBaselineProfile`; the profiles land in
 * app/src/release/generated/baselineProfiles.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    /** Launch until the first results; also goes into the Startup Profile, laid out first in the dex files. */
    @Test
    fun startup() = rule.collect(packageName = PACKAGE_NAME, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        finishOnboardingIfShown()
        waitForResults()
    }

    /** The main screens, compiled ahead of time but kept out of the startup layout. */
    @Test
    fun journeys() = rule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
        finishOnboardingIfShown()
        browseApp()
    }
}

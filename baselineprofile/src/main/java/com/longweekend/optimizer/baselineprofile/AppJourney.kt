package com.longweekend.optimizer.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

const val PACKAGE_NAME = "com.longweekend.optimizer"

private const val TIMEOUT_MS = 10_000L

// Labels in English and Croatian, so the journey works whichever language the phone uses
private val SKIP = label("Skip", "Preskoči")
private val GET_STARTED = label("Get started", "Započni")
private val SEE_ALL = label("See all", "Prikaži sve")
private val VIEW_PLAN = label("View plan", "Pogledaj plan")
private val HOLIDAYS = label("Holidays", "Blagdani")
private val SAVED = label("Saved", "Spremljeno")
private val SETTINGS = label("Settings", "Postavke")

private fun label(vararg texts: String): Pattern = Pattern.compile(texts.joinToString("|") { Pattern.quote(it) })

/** Finishes the welcome screens on a fresh install, keeping the country and allowance they suggest. */
fun MacrobenchmarkScope.finishOnboardingIfShown() {
    device.findObject(By.text(SKIP))?.click() ?: return
    device.wait(Until.findObject(By.text(GET_STARTED)), TIMEOUT_MS)?.click()
}

/** Waits until Discover has its first results. */
fun MacrobenchmarkScope.waitForResults() {
    device.wait(Until.hasObject(By.text(SEE_ALL)), TIMEOUT_MS)
}

/** The paths most people take: results, a plan, the holiday calendar, saved plans and settings. */
fun MacrobenchmarkScope.browseApp() {
    waitForResults()
    device.findObject(By.text(SEE_ALL))?.click()
    device.wait(Until.hasObject(By.text(VIEW_PLAN)), TIMEOUT_MS)
    device.flingDownAndBack()
    device.findObject(By.text(VIEW_PLAN))?.click()
    device.waitForIdle()
    device.flingDownAndBack()
    device.pressBack()
    device.pressBack()

    device.wait(Until.findObject(By.text(HOLIDAYS)), TIMEOUT_MS)?.click()
    device.waitForIdle()
    device.flingDownAndBack()

    device.findObject(By.text(SAVED))?.click()
    device.waitForIdle()

    device.findObject(By.desc(SETTINGS))?.click()
    device.waitForIdle()
    device.flingDownAndBack()
    device.pressBack()
}

private fun UiDevice.flingDownAndBack() {
    fling(Direction.DOWN)
    fling(Direction.UP)
}

private fun UiDevice.fling(direction: Direction) {
    // Looked up each time: Compose may replace the list's node while it scrolls
    val list = findObject(By.scrollable(true)) ?: return
    try {
        // Keep clear of the gesture navigation area at the edges
        list.setGestureMargin(displayWidth / 5)
        list.fling(direction)
    } catch (e: StaleObjectException) {
        // The screen changed under the gesture; skipping one fling only shortens the journey
    }
    waitForIdle()
}

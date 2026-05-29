package com.salles.scrapper.scrappers

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright

/**
 * One lazy headless Chromium for the whole test run (relaunching per test is slow).
 * [newPage] renders a static HTML string via setContent so DOM-extraction logic can be
 * tested without any network access. Callers must close the returned page.
 */
object PlaywrightFixture {
    private val playwright: Playwright by lazy { Playwright.create() }
    private val browser: Browser by lazy {
        playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(true))
    }

    fun newPage(html: String): Page = browser.newPage().apply { setContent(html) }
}

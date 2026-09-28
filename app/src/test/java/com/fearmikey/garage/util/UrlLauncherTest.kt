package com.fearmikey.garage.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlLauncherTest {

    @Test
    fun isAmazonUrl_returnsTrueForAmazonLinks() {
        assertTrue(UrlLauncher.isAmazonUrl("https://link.amazon/B0ecZNiyV"))
        assertTrue(UrlLauncher.isAmazonUrl("https://www.amazon.com/dp/B0ecZNiyV"))
        assertTrue(UrlLauncher.isAmazonUrl("https://amzn.to/3xyz"))
        assertTrue(UrlLauncher.isAmazonUrl("https://amazon.co.uk/dp/1234"))
        assertTrue(UrlLauncher.isAmazonUrl("https://www.amazon.ca/product/B0123456"))
    }

    @Test
    fun isAmazonUrl_returnsFalseForNonAmazonLinks() {
        assertFalse(UrlLauncher.isAmazonUrl("https://github.com/fearmikey/Garage"))
        assertFalse(UrlLauncher.isAmazonUrl("https://buymeacoffee.com/XimW7nXI1j"))
        assertFalse(UrlLauncher.isAmazonUrl("https://www.nhtsa.gov/recalls"))
        assertFalse(UrlLauncher.isAmazonUrl("invalid-url"))
    }
}

package com.fearmikey.garage.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderCheckWorkerTest {

    @Test
    fun `worker constants are defined for unique work names`() {
        assertEquals("reminder-check", ReminderCheckWorker.UNIQUE_WORK_NAME)
        assertEquals("reminder-check-immediate", ReminderCheckWorker.IMMEDIATE_WORK_NAME)
    }
}

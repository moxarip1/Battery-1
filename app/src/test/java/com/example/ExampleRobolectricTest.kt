package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.diagnostics.KernelBatteryDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BatteryCare Pro", appName)
    }

    @Test
    fun `kernel diagnostics captures valid snapshot`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val diagnostics = KernelBatteryDiagnostics(context)
        val snapshot = diagnostics.captureSnapshot(customDesignCapacityMah = 5000)

        assertNotNull(snapshot)
        assertEquals(5000, snapshot.designCapacityMah)
        assertTrue(snapshot.healthPercentage in 35f..100f)
        assertTrue(snapshot.voltageMillivolts > 0)
        assertTrue(snapshot.kernelNodes.isNotEmpty())
    }
}

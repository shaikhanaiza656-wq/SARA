package com.yourorg.systemcore.presentation.console

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsoleCommandTest {

    @Test
    fun `battery keyword parses to BatteryStatus command`() {
        assertEquals(ConsoleCommand.BatteryStatus, ConsoleCommand.parse("battery"))
        assertEquals(ConsoleCommand.BatteryStatus, ConsoleCommand.parse("Battery Status"))
        assertEquals(ConsoleCommand.BatteryStatus, ConsoleCommand.parse("  battery.status  "))
    }

    @Test
    fun `network keyword parses to NetworkStatus command`() {
        assertEquals(ConsoleCommand.NetworkStatus, ConsoleCommand.parse("network"))
        assertEquals(ConsoleCommand.NetworkStatus, ConsoleCommand.parse("Network Status"))
        assertEquals(ConsoleCommand.NetworkStatus, ConsoleCommand.parse("  network.status  "))
    }

    @Test
    fun `help keyword parses to Help command`() {
        assertEquals(ConsoleCommand.Help, ConsoleCommand.parse("help"))
        assertEquals(ConsoleCommand.Help, ConsoleCommand.parse("?"))
    }

    @Test
    fun `unrecognized input parses to Unknown with raw text preserved`() {
        val result = ConsoleCommand.parse("launch nuke")
        assertTrue(result is ConsoleCommand.Unknown)
        assertEquals("launch nuke", (result as ConsoleCommand.Unknown).raw)
    }
}

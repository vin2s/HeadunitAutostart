package com.vin2s.headunitautostart

data class TargetApp(
    val packageName: String,
    val label: String,
    val launchDelayMs: Long = 3_000L,
    val minimizeAfterLaunch: Boolean = true,
    val minimizeDelayMs: Long = 2_000L
)

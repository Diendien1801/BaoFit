package com.visionfit.data.remote

/** The iOS simulator shares the Mac's network, so `localhost` is the host machine. */
internal actual val devMachineUrl: String = "http://localhost"

package com.sloth.runveal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
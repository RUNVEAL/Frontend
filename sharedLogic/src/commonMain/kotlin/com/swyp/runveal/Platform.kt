package com.swyp.runveal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
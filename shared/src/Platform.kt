package com.usuallycoding.openadhd

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

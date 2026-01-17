package com.casavirupa.voluntariat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
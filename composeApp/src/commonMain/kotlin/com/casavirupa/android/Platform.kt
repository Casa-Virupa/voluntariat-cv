package com.casavirupa.android

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
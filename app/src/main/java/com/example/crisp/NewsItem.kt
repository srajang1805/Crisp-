package com.example.crisp

data class NewsItem(
    val title: String,
    val link: String,
    val description: String,
    val imageUrl: String? = null
)

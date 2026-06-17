package com.brunobrandao.expensetracker.domain.util

fun interface Clock {
    fun now(): Long
}

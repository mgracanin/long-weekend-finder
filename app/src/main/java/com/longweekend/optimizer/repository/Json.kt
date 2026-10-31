package com.longweekend.optimizer.repository

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDate

/** Gson for everything the app stores on the phone; dates are kept as yyyy-MM-dd. */
internal fun createGson(): Gson = GsonBuilder()
    .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
    .create()

private class LocalDateAdapter : TypeAdapter<LocalDate>() {
    override fun write(out: JsonWriter, value: LocalDate?) {
        if (value == null) out.nullValue() else out.value(value.toString())
    }

    override fun read(input: JsonReader): LocalDate = LocalDate.parse(input.nextString())
}

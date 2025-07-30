package com.personal.adoptioncenterapplication.model

import java.io.Serializable

data class Animal(
    val name: String = "",
    val breed: String = "",
    val age: String = "",
    val photoPath: String = ""
) : Serializable

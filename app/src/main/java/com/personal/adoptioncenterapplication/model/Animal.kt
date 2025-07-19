package com.personal.adoptioncenterapplication.model

class Animal {
    val id: Int;
    val name: String;
    val breed: String;
    val age: String;
    val photoUrl: String;
    val description: String;
    val vaccinated: Boolean;

    constructor(id:Int, name:String, breed:String, age:String, photoUrl:String, description:String, vaccinated: Boolean) {
        this.id = id;
        this.name = name;
        this.breed = breed;
        this.age = age;
        this.photoUrl = photoUrl;
        this.description = description;
        this.vaccinated = vaccinated;
    }
}
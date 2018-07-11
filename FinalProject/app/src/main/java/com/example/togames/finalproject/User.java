package com.example.togames.finalproject;

public class User{
    private String email, name, surname, age, weight, height;
    private String encodedProfilePhoto = null;
    private int stepGoal;

    public User() {
    }

    public User(String email, String name, String surname, String age, String weight,
                String height, int stepGoal, String encodedProfilePhoto){
        this.email = email;
        this.name = name;
        this.surname = surname;
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.stepGoal = stepGoal;

        this.encodedProfilePhoto = encodedProfilePhoto;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }
    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getAge() {
        return age;
    }
    public void setAge(String age) {
        this.age = age;
    }

    public String getWeight() {
        return weight;
    }
    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getHeight() {
        return height;
    }
    public void setHeight(String height) {
        this.height = height;
    }

    public int getStepGoal() {
        return stepGoal;
    }
    public void setStepGoal(int stepGoal) {
        this.stepGoal = stepGoal;
    }

    public String getEncodedProfilePhoto() {
        return encodedProfilePhoto;
    }
    public void setEncodedProfilePhoto(String encodedProfilePhoto) {
        this.encodedProfilePhoto = encodedProfilePhoto;
    }
}

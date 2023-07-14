package com.example;

import javax.persistence.*;
import jakarta.persistence.*;
import java.util.List;

@Embeddable
public class Address {
    private String city;
    private String firstLine;
    private String secondLine;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getFirstLine() {
        return firstLine;
    }

    public void setFirstLine(String firstLine) {
        this.firstLine = firstLine;
    }

    public String getSecondLine() {
        return secondLine;
    }

    public void setSecondLine(String secondLine) {
        this.secondLine = secondLine;
    }
}
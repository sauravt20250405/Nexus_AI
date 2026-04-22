package com.nexusrag.agent.tool;

import dev.langchain4j.agent.tool.Tool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AgentTools {

    @Tool("Returns the current date, time, and timezone.")
    public String currentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool("Adds two floating point numbers.")
    public double add(double a, double b) {
        return a + b;
    }

    @Tool("Subtracts the second floating point number from the first.")
    public double subtract(double a, double b) {
        return a - b;
    }

    @Tool("Multiplies two floating point numbers.")
    public double multiply(double a, double b) {
        return a * b;
    }

    @Tool("Divides the first floating point number by the second.")
    public double divide(double a, double b) {
        if (b == 0) throw new IllegalArgumentException("Cannot divide by zero.");
        return a / b;
    }

    @Tool("Calculates the square root of a number.")
    public double squareRoot(double a) {
        return Math.sqrt(a);
    }

    @Tool("Returns basic system information including OS and Java version.")
    public String getSystemInfo() {
        String os = System.getProperty("os.name");
        String version = System.getProperty("os.version");
        String javaVer = System.getProperty("java.version");
        return String.format("OS: %s (%s), Java Version: %s", os, version, javaVer);
    }
}

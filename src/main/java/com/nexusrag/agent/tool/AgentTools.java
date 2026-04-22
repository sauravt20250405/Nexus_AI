package com.nexusrag.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

    @Tool("Downloads and extracts text content from a web URL. Only use this if the user provides a direct HTTP/HTTPS link.")
    public String scrapeWebpage(String url) {
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get();
            return doc.text();
        } catch (Exception e) {
            return "Failed to scrape the webpage. Error: " + e.getMessage();
        }
    }

    @Tool("Executes a given shell or PowerShell script physically on the host computer running this server. " +
          "Use this if the user asks to 'check my ip', 'create a folder', 'write a python script', etc. " +
          "Return the output to the user. CAUTION: You have system privileges.")
    public String executeSystemCommand(String script) {
        try {
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-Command", script);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            
            String result = output.toString().trim();
            if (result.isEmpty()) {
                return "Command executed successfully with no output.";
            }
            return result;
        } catch (Exception e) {
            return "Command failed: " + e.getMessage();
        }
    }
}

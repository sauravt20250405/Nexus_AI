package com.nexusrag.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ThreadLocal utility to safely pass out-of-band context (like Retrieval Scores and Sources)
 * from the LangChain4j Agent backend up to the REST Controller.
 */
public class RetrievalContextHolder {
    private static final ThreadLocal<Double> maxScoreContext = new ThreadLocal<>();
    private static final ThreadLocal<List<Map<String, String>>> sourcesContext = new ThreadLocal<>();

    public static void setMaxScore(Double score) {
        maxScoreContext.set(score);
    }

    public static Double getMaxScore() {
        return maxScoreContext.get();
    }

    public static void setSources(List<Map<String, String>> sources) {
        sourcesContext.set(sources);
    }

    public static List<Map<String, String>> getSources() {
        return sourcesContext.get() != null ? sourcesContext.get() : new ArrayList<>();
    }

    public static void clear() {
        maxScoreContext.remove();
        sourcesContext.remove();
    }
}

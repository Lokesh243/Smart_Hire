package com.smarthire.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── 1. Job Description Generator ──────────────────────────────
    public String generateJobDescription(String title,
                                         String location,
                                         String jobType) {
        String prompt = String.format("""
            You are an expert HR assistant. Write a professional,
            engaging job description for the following position:

            Title: %s
            Location: %s
            Job Type: %s

            Include these sections:
            1. About the Role (2-3 sentences)
            2. Key Responsibilities (5 bullet points)
            3. Requirements (5 bullet points)
            4. What We Offer (3 bullet points)

            Keep it under 300 words. Professional but friendly tone.
            """, title, location, jobType);

        return callGemini(prompt);
    }

    // ── 2. Resume Keyword Matcher ──────────────────────────────────
    public Map<String, Object> matchResume(String resumeUrl,
                                           String jobTitle,
                                           String jobDescription) {
        String prompt = String.format("""
            You are an ATS resume analyzer.

            Job Title: %s
            Job Description: %s
            Resume URL: %s

            Analyze and respond ONLY in this exact JSON format, no extra text:
            {
              "matchScore": 75,
              "matchLevel": "Good Match",
              "strengths": ["Spring Boot", "Java", "REST APIs"],
              "missingSkills": ["Docker", "AWS"],
              "recommendation": "Strong candidate for technical screening"
            }

            matchScore: 0-100
            matchLevel: one of "Excellent Match", "Good Match", "Average Match", "Poor Match"
            """, jobTitle, jobDescription, resumeUrl);

        String response = callGemini(prompt);
        return parseJsonToMap(response);
    }

    // ── 3. Natural Language Search Filters ────────────────────────
    public Map<String, String> extractSearchFilters(String query) {
        String prompt = String.format("""
            Extract job search filters from this query.
            Query: "%s"

            Respond ONLY in this exact JSON format, no extra text:
            {
              "title": "Java Developer",
              "location": "Hyderabad",
              "jobType": "Full-time"
            }

            Rules:
            - Use empty string "" for fields not mentioned
            - jobType must be: Full-time, Part-time, Contract, Remote, Internship, or ""
            - title should be short keywords only (1-3 words)
            """, query);

        String response = callGemini(prompt);
        return parseJsonToStringMap(response);
    }

    // ── 4. Interview Question Generator ───────────────────────────
    public List<String> generateInterviewQuestions(String jobTitle,
                                                   String jobDescription,
                                                   String candidateName) {
        String prompt = String.format("""
            You are a senior technical interviewer.

            Generate 5 tailored interview questions for:
            Candidate: %s
            Role: %s
            Description: %s

            Mix technical and behavioral questions.
            Respond ONLY as a JSON array of strings, no extra text:
            ["Question 1?", "Question 2?", "Question 3?", "Question 4?", "Question 5?"]
            """, candidateName, jobTitle, jobDescription);

        String response = callGemini(prompt);
        return parseJsonToList(response);
    }
    // ── 5. Candidate Summarizer ────────────────────────────────────
    public Map<String, String> summarizeCandidate(String candidateName,
                                                  String resumeUrl,
                                                  String jobTitle) {
        String prompt = String.format("""
        You are an expert HR analyst.
        
        Summarize this candidate for a hiring manager in 3 short lines:
        Candidate Name: %s
        Resume URL: %s
        Applying for: %s
        
        Respond ONLY in this exact JSON format, no extra text:
        {
          "summary": "2-3 sentence overview of who this candidate is",
          "strength": "Their single biggest strength for this role",
          "recommendation": "One sentence hiring recommendation"
        }
        """, candidateName, resumeUrl, jobTitle);

        String response = callGemini(prompt);
        return parseJsonToStringMap(response);
    }

    // ── 6. Rejection Email Writer ──────────────────────────────────
    public String writeRejectionEmail(String candidateName,
                                      String jobTitle,
                                      String companyName) {
        String prompt = String.format("""
        Write a warm, professional, and empathetic rejection email for:
        Candidate: %s
        Job Title: %s
        Company: %s
        
        Guidelines:
        - Be kind and encouraging
        - Thank them for their time
        - Wish them well in their search
        - Keep it under 150 words
        - Do NOT include subject line
        - Start directly with "Dear %s,"
        """, candidateName, jobTitle, companyName, candidateName);

        return callGemini(prompt);
    }

    // ── 7. Job Recommendations ─────────────────────────────────────
    public List<Map<String, Object>> recommendJobs(
            String candidateHistory,
            List<Map<String, Object>> availableJobs) {

        String prompt = String.format("""
        You are a job recommendation engine.
        
        Candidate's application history:
        %s
        
        Available jobs (JSON):
        %s
        
        Based on the candidate's history, recommend the top 3 most 
        relevant job IDs from the available jobs list.
        
        Respond ONLY as a JSON array with exactly 3 objects, no extra text:
        [
          {"jobId": 1, "reason": "Short reason why this fits"},
          {"jobId": 2, "reason": "Short reason why this fits"},
          {"jobId": 3, "reason": "Short reason why this fits"}
        ]
        
        Only use jobIds from the available jobs list.
        If fewer than 3 jobs available, return what's available.
        """, candidateHistory, availableJobs.toString());

        String response = callGemini(prompt);
        try {
            String clean = cleanJson(response);
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(clean, List.class);
        } catch (Exception e) {
            log.error("Failed to parse job recommendations: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Core Gemini API call ───────────────────────────────────────
    private String callGemini(String prompt) {
        String url = apiUrl + "?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, entity, Map.class);

            List<Map> candidates = (List<Map>) response.getBody()
                    .get("candidates");
            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            log.info("Gemini API call successful");
            return text.trim();

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new RuntimeException("AI service unavailable: "
                    + e.getMessage());
        }
    }

    // ── JSON parsers ───────────────────────────────────────────────
    private Map<String, Object> parseJsonToMap(String response) {
        try {
            String clean = cleanJson(response);
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(clean, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse AI map response: {}", e.getMessage());
            return Map.of(
                    "matchScore", 0,
                    "matchLevel", "Unable to analyze",
                    "strengths", List.of(),
                    "missingSkills", List.of(),
                    "recommendation", "Manual review required"
            );
        }
    }

    private Map<String, String> parseJsonToStringMap(String response) {
        try {
            String clean = cleanJson(response);
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(clean, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse AI string map: {}", e.getMessage());
            return Map.of("title", "", "location", "", "jobType", "");
        }
    }

    private List<String> parseJsonToList(String response) {
        try {
            String clean = cleanJson(response);
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(clean, List.class);
        } catch (Exception e) {
            log.error("Failed to parse AI list: {}", e.getMessage());
            return List.of(
                    "Tell me about yourself.",
                    "Why are you interested in this role?",
                    "Describe a challenging project you worked on.",
                    "How do you handle tight deadlines?",
                    "Where do you see yourself in 5 years?"
            );
        }
    }

    private String cleanJson(String response) {
        return response.trim()
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
    }
}
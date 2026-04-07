package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import dao.CVDAO;
import dao.JobOfferDAO;
import model.Cv;
import model.Job;
import model.User;
import service.AiService;
import service.AuthService;

import java.util.ArrayList;
import java.util.List;

public class JobGuideController {

    @FXML private VBox rootPane;
    @FXML private TextArea chatArea;
    @FXML private TextField userInput;

    private String mode = "chat"; // "chat", "interview", "cvadvice"
    private List<String> interviewQuestions = new ArrayList<>();
    private int questionIndex = 0;

    @FXML
    public void initialize() {
        appendBot("Hi! I'm your Job Guide assistant.\n"
                + "Use the buttons above or just type a question.\n"
                + "- Interview Practice: I'll ask you common interview questions\n"
                + "- CV Advice: I'll review your CV and suggest improvements\n"
                + "- Matching Jobs: I'll find jobs that match your skills\n");
    }

    @FXML
    private void handleSend() {
        String text = userInput.getText().trim();
        if (text.isEmpty()) return;
        userInput.setText("");
        appendUser(text);

        if ("interview".equals(mode)) {
            handleInterviewAnswer(text);
        } else {
            // general chat - ask AI
            appendBot("Thinking...");
            new Thread(() -> {
                String prompt = "You are a career coach chatbot. The user says: \"" + text
                        + "\". Give a helpful, concise career advice response.";
                String reply = AiService.askGemini(prompt);
                javafx.application.Platform.runLater(() -> {
                    removeLast("Thinking...");
                    appendBot(reply);
                });
            }).start();
        }
    }

    @FXML
    private void handleInterview() {
        mode = "interview";
        questionIndex = 0;
        interviewQuestions.clear();
        interviewQuestions.add("Tell me about yourself.");
        interviewQuestions.add("What are your strengths?");
        interviewQuestions.add("What are your weaknesses?");
        interviewQuestions.add("Why do you want this job?");
        interviewQuestions.add("Where do you see yourself in 5 years?");
        interviewQuestions.add("Describe a challenge you faced and how you handled it.");
        interviewQuestions.add("Why should we hire you?");
        interviewQuestions.add("Do you have any questions for us?");

        chatArea.setText("");
        appendBot("Interview Practice Mode!\nI'll ask you common interview questions. "
                + "Answer each one and I'll give you feedback.\n");
        askNextQuestion();
    }

    private void askNextQuestion() {
        if (questionIndex < interviewQuestions.size()) {
            appendBot("Question " + (questionIndex + 1) + ": " + interviewQuestions.get(questionIndex));
        } else {
            appendBot("Great job! You completed all the questions. Type anything to continue chatting.");
            mode = "chat";
        }
    }

    private void handleInterviewAnswer(String answer) {
        if (questionIndex >= interviewQuestions.size()) {
            mode = "chat";
            return;
        }
        String question = interviewQuestions.get(questionIndex);
        appendBot("Evaluating your answer...");
        new Thread(() -> {
            String prompt = "You are an interview coach. The interview question was: \"" + question
                    + "\". The candidate answered: \"" + answer
                    + "\". Give brief feedback: was it good? What could be improved? Be concise (3-4 lines max).";
            String reply = AiService.askGemini(prompt);
            javafx.application.Platform.runLater(() -> {
                removeLast("Evaluating your answer...");
                appendBot("Feedback: " + reply);
                questionIndex++;
                askNextQuestion();
            });
        }).start();
    }

    @FXML
    private void handleCvAdvice() {
        mode = "cvadvice";
        User user = AuthService.getCurrentUser();
        if (user == null) { appendBot("Please log in first."); return; }

        CVDAO cvDAO = new CVDAO();
        List<Cv> cvs = cvDAO.findByUserId(user.getId());
        if (cvs.isEmpty()) {
            appendBot("You have no CVs yet. Go to 'My CVs' to create one first.");
            return;
        }

        Cv cv = cvs.get(0); // use the first CV
        appendBot("Analyzing your CV (" + cv.getFullName() + ")...");
        new Thread(() -> {
            String prompt = "You are a CV expert. Review this CV and give specific, actionable advice:\n"
                    + "Name: " + safe(cv.getFullName()) + "\n"
                    + "Objective: " + safe(cv.getObjective()) + "\n"
                    + "Education: " + safe(cv.getEducation()) + "\n"
                    + "Experience: " + safe(cv.getExperience()) + "\n"
                    + "Skills: " + safe(cv.getSkills()) + "\n"
                    + "Languages: " + safe(cv.getLanguages()) + "\n"
                    + "List 5 specific improvements. Be concise.";
            String reply = AiService.askGemini(prompt);
            javafx.application.Platform.runLater(() -> {
                removeLast("Analyzing your CV (" + cv.getFullName() + ")...");
                appendBot("CV Advice:\n" + reply);
                mode = "chat";
            });
        }).start();
    }

    @FXML
    private void handleMatchingJobs() {
        User user = AuthService.getCurrentUser();
        if (user == null) { appendBot("Please log in first."); return; }

        CVDAO cvDAO = new CVDAO();
        List<Cv> cvs = cvDAO.findByUserId(user.getId());
        if (cvs.isEmpty()) {
            appendBot("Create a CV first so I can match your skills to jobs.");
            return;
        }

        String userSkills = safe(cvs.get(0).getSkills()).toLowerCase();
        if (userSkills.isEmpty()) {
            appendBot("Your CV has no skills listed. Add skills to get job matches.");
            return;
        }

        JobOfferDAO jobDAO = new JobOfferDAO();
        List<Job> allJobs = jobDAO.findAll();
        if (allJobs.isEmpty()) {
            appendBot("No jobs available right now. Ask an admin to add some.");
            return;
        }

        // simple matching: check if any user skill word appears in job requirements or description
        String[] skills = userSkills.split(",");
        List<Job> matched = new ArrayList<>();
        for (Job job : allJobs) {
            String jobText = (safe(job.getRequirements()) + " " + safe(job.getDescription())).toLowerCase();
            for (String skill : skills) {
                if (!skill.trim().isEmpty() && jobText.contains(skill.trim())) {
                    matched.add(job);
                    break;
                }
            }
        }

        if (matched.isEmpty()) {
            // if no exact match, just show all jobs
            appendBot("No exact skill matches found. Here are all available jobs:");
            for (Job j : allJobs) {
                appendBot("- " + j.getTitle() + " at " + j.getCompany()
                        + (j.getLink() != null && !j.getLink().isEmpty() ? " | Link: " + j.getLink() : ""));
            }
        } else {
            appendBot("Jobs matching your skills (" + userSkills + "):");
            for (Job j : matched) {
                appendBot("- " + j.getTitle() + " at " + j.getCompany()
                        + " [" + j.getLocation() + "]"
                        + (j.getLink() != null && !j.getLink().isEmpty() ? " | Link: " + j.getLink() : ""));
            }
        }
    }

    private void appendBot(String msg) {
        chatArea.appendText("Bot: " + msg + "\n\n");
    }

    private void appendUser(String msg) {
        chatArea.appendText("You: " + msg + "\n\n");
    }

    private void removeLast(String text) {
        String content = chatArea.getText();
        int idx = content.lastIndexOf("Bot: " + text);
        if (idx >= 0) {
            chatArea.setText(content.substring(0, idx));
        }
    }

    private String safe(String s) { return s == null ? "" : s; }

    @FXML private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
        } catch (Exception e) { e.printStackTrace(); }
    }
}

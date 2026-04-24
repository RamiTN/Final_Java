package controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
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
    @FXML private ComboBox<Cv> cvCombo;

    private String mode = "chat";
    private List<String> interviewQuestions = new ArrayList<>();
    private int questionIndex = 0;
    private boolean awaitingRetry = false;
    private Cv selectedCv = null;

    // Matched jobs for "preview <number>" command
    private List<Job> lastMatchedJobs = new ArrayList<>();

    @FXML
    public void initialize() {
        loadCvCombo();
        appendBot("Hi! I'm your Job Guide assistant.\n"
                + "1. Select a CV from the dropdown above\n"
                + "2. Choose a mode: Interview Practice, CV Advice, or Matching Jobs\n"
                + "3. Or just type a career question!\n");
    }

    private void loadCvCombo() {
        User user = AuthService.getCurrentUser();
        if (user == null || cvCombo == null) return;
        List<Cv> cvs = new CVDAO().findByUserId(user.getId());
        cvCombo.setItems(FXCollections.observableArrayList(cvs));
        if (!cvs.isEmpty()) {
            cvCombo.setValue(cvs.get(0));
            selectedCv = cvs.get(0);
        }
    }

    @FXML
    private void handleCvChange() {
        selectedCv = cvCombo.getValue();
        if (selectedCv != null) {
            appendBot("Switched to CV: " + selectedCv.getFullName());
        }
    }

    @FXML
    private void handleNewChat() {
        chatArea.setText("");
        mode = "chat";
        questionIndex = 0;
        awaitingRetry = false;
        lastMatchedJobs.clear();
        appendBot("Chat cleared! Select a mode or type a question.");
    }

    private boolean requireCv() {
        if (selectedCv == null) {
            appendBot("⚠ Please select a CV from the dropdown first.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleSend() {
        String text = userInput.getText().trim();
        if (text.isEmpty()) return;
        userInput.setText("");
        appendUser(text);

        // Handle "preview <number>" command for matched jobs
        if (text.toLowerCase().startsWith("preview ") && !lastMatchedJobs.isEmpty()) {
            try {
                int idx = Integer.parseInt(text.substring(8).trim()) - 1;
                if (idx >= 0 && idx < lastMatchedJobs.size()) {
                    Job job = lastMatchedJobs.get(idx);
                    appendBot("Opening preview for: " + job.getTitle() + " at " + job.getCompany() + "...");
                    // Navigate to Jobs page with preview
                    JobController.previewJobId = job.getId();
                    goToJobs();
                    return;
                } else {
                    appendBot("Invalid job number. Type 'preview 1' to 'preview " + lastMatchedJobs.size() + "'.");
                    return;
                }
            } catch (NumberFormatException e) {
                // Not a number, fall through to normal handling
            }
        }

        if ("interview".equals(mode)) {
            if (awaitingRetry) {
                handleRetryAnswer(text);
            } else {
                handleInterviewAnswer(text);
            }
        } else {
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
        if (!requireCv()) return;
        mode = "interview";
        questionIndex = 0;
        awaitingRetry = false;
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
        appendBot("Interview Practice Mode! (Using CV: " + selectedCv.getFullName() + ")\n"
                + "I'll ask you common interview questions. "
                + "Answer each one and I'll give you feedback + an example.\n"
                + "Try again or type 'next' to move on.\n");
        askNextQuestion();
    }

    private void askNextQuestion() {
        awaitingRetry = false;
        if (questionIndex < interviewQuestions.size()) {
            appendBot("Question " + (questionIndex + 1) + " of " + interviewQuestions.size()
                    + ": " + interviewQuestions.get(questionIndex));
        } else {
            appendBot("Great job! You completed all the questions!\n"
                    + "You've practiced " + interviewQuestions.size() + " interview questions. Keep it up!");
            mode = "chat";
        }
    }

    private void handleInterviewAnswer(String answer) {
        if (questionIndex >= interviewQuestions.size()) { mode = "chat"; return; }
        String question = interviewQuestions.get(questionIndex);
        appendBot("Evaluating your answer...");

        new Thread(() -> {
            String prompt = "You are an interview coach. The interview question was: \""
                    + question + "\"\n"
                    + "The candidate answered: \"" + answer + "\"\n\n"
                    + "Reply in this exact format:\n"
                    + "FEEDBACK: (2-3 lines max)\n"
                    + "EXAMPLE: (stronger model answer, 3-5 lines)\n"
                    + "Be direct and encouraging.";
            String reply = AiService.askGemini(prompt);

            javafx.application.Platform.runLater(() -> {
                removeLast("Evaluating your answer...");
                appendBot(reply + "\n\n--- Try again with a better answer, or type 'next' to move on. ---");
                awaitingRetry = true;
            });
        }).start();
    }

    private void handleRetryAnswer(String answer) {
        if ("next".equalsIgnoreCase(answer)) {
            questionIndex++;
            askNextQuestion();
            return;
        }

        String question = interviewQuestions.get(questionIndex);
        appendBot("Checking your improved answer...");

        new Thread(() -> {
            String prompt = "You are an interview coach. The interview question was: \""
                    + question + "\"\n"
                    + "The candidate just gave this improved answer: \"" + answer + "\"\n\n"
                    + "Acknowledge their improvement warmly in 1-2 lines. "
                    + "Mention one specific thing they did well. Be encouraging and concise.";
            String reply = AiService.askGemini(prompt);

            javafx.application.Platform.runLater(() -> {
                removeLast("Checking your improved answer...");
                appendBot(reply);
                questionIndex++;
                askNextQuestion();
            });
        }).start();
    }

    @FXML
    private void handleCvAdvice() {
        if (!requireCv()) return;
        mode = "cvadvice";

        appendBot("Analyzing your CV (" + selectedCv.getFullName() + ")...");
        new Thread(() -> {
            String prompt = "You are a CV expert. Review this CV and give specific, actionable advice:\n"
                    + "Name: " + safe(selectedCv.getFullName()) + "\n"
                    + "Objective: " + safe(selectedCv.getObjective()) + "\n"
                    + "Education: " + safe(selectedCv.getEducation()) + "\n"
                    + "Experience: " + safe(selectedCv.getExperience()) + "\n"
                    + "Skills: " + safe(selectedCv.getSkills()) + "\n"
                    + "Languages: " + safe(selectedCv.getLanguages()) + "\n"
                    + "List 5 specific improvements. Be concise.";
            String reply = AiService.askGemini(prompt);
            javafx.application.Platform.runLater(() -> {
                removeLast("Analyzing your CV (" + selectedCv.getFullName() + ")...");
                appendBot("CV Advice:\n" + reply);
                mode = "chat";
            });
        }).start();
    }

    @FXML
    private void handleMatchingJobs() {
        if (!requireCv()) return;

        String userSkills = safe(selectedCv.getSkills()).toLowerCase();
        if (userSkills.isEmpty()) {
            appendBot("Your selected CV has no skills listed. Add skills to get job matches.");
            return;
        }

        JobOfferDAO jobDAO = new JobOfferDAO();
        List<Job> allJobs = jobDAO.findAvailable();
        if (allJobs.isEmpty()) {
            appendBot("No available jobs right now. Ask an admin to add some.");
            return;
        }

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

        lastMatchedJobs.clear();

        if (matched.isEmpty()) {
            appendBot("No exact skill matches found. Here are all available jobs:");
            lastMatchedJobs.addAll(allJobs);
            for (int i = 0; i < allJobs.size(); i++) {
                Job j = allJobs.get(i);
                appendBot((i + 1) + ". " + j.getTitle() + " at " + j.getCompany()
                        + " — type 'preview " + (i + 1) + "' to view details");
            }
        } else {
            appendBot("Jobs matching your skills (" + userSkills + "):");
            lastMatchedJobs.addAll(matched);
            for (int i = 0; i < matched.size(); i++) {
                Job j = matched.get(i);
                appendBot("✔ " + (i + 1) + ". " + j.getTitle() + " at " + j.getCompany()
                        + " [" + j.getLocation() + "]"
                        + " — type 'preview " + (i + 1) + "' to view & apply");
            }
        }
        appendBot("\nType 'preview <number>' to open the job details page.");
    }

    private void appendBot(String msg) { chatArea.appendText("Bot: " + msg + "\n\n"); }
    private void appendUser(String msg) { chatArea.appendText("You: " + msg + "\n\n"); }

    private void removeLast(String text) {
        String content = chatArea.getText();
        int idx = content.lastIndexOf("Bot: " + text);
        if (idx >= 0) chatArea.setText(content.substring(0, idx));
    }

    private String safe(String s) { return s == null ? "" : s; }

    @FXML private void goToDashboard() { loadScene("/view/dashboard.fxml"); }

    private void goToJobs() { loadScene("/view/Job.fxml"); }

    private void loadScene(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
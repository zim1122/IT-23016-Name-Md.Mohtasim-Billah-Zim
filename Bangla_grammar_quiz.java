package com.example.banglagrammarquiz;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.sql.*;

public class BanglaGrammarQuiz extends Application {
    private Connection conn;
    private String studentId;
    private int currentQuestion = 0;
    private int score = 0;

    // Quiz questions and answers in Bangla
    private String[][] questions = {
            {
                    "বিশেষ্য কাকে বলে?",
                    "যে পদ দ্বারা কোনো ব্যক্তি, বস্তু, স্থান বা ভাবের নাম বোঝায়",
                    "যে পদ দ্বারা বিশেষণের কাজ করে",
                    "যে পদ দ্বারা ক্রিয়া সম্পাদন হয়",
                    "যে পদ দ্বারা সর্বনাম প্রকাশ পায়",
                    "1"
            },
            {
                    "সন্ধি কত প্রকার?",
                    "২ প্রকার",
                    "৩ প্রকার",
                    "৪ প্রকার",
                    "৫ প্রকার",
                    "2"
            }
    };

    private ToggleGroup answerGroup;
    private VBox quizPane;
    private Label questionLabel;
    private RadioButton[] options = new RadioButton[4];

    @Override
    public void start(Stage primaryStage) {
        initDatabase();
        primaryStage.setTitle("বাংলা ব্যাকরণ কুইজ");
        primaryStage.setScene(createLoginScene(primaryStage));
        primaryStage.show();
    }

    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:quiz.db");

            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS student (" +
                    "sl INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "student_id TEXT UNIQUE NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS quiz_scores (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "student_id TEXT NOT NULL, " +
                    "score INTEGER NOT NULL, " +
                    "total_questions INTEGER NOT NULL, " +
                    "quiz_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            try {
                // Insert 54 student IDs from IT-23001 to IT-23054
                for (int i = 1; i <= 54; i++) {
                    String id = String.format("IT-23%03d", i);
                    stmt.execute("INSERT INTO student (student_id) VALUES ('" + id + "')");
                }
            } catch (SQLException e) {
                // Ignore if already exists
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to initialize database: " + e.getMessage());
        }
    }

    private Scene createLoginScene(Stage stage) {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));

        Label titleLabel = new Label("বাংলা ব্যাকরণ কুইজ");
        titleLabel.setFont(new Font("Arial", 28));

        Label instructionLabel = new Label("আপনার স্টুডেন্ট আইডি দিন:");
        instructionLabel.setFont(new Font("Arial", 16));

        TextField studentIdField = new TextField();
        studentIdField.setPromptText("স্টুডেন্ট আইডি");
        studentIdField.setMaxWidth(250);
        studentIdField.setFont(new Font("Arial", 14));

        Button startButton = new Button("কুইজ শুরু করুন");
        startButton.setFont(new Font("Arial", 14));
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10 20;");

        Button viewScoresButton = new Button("সকল স্কোর দেখুন");
        viewScoresButton.setFont(new Font("Arial", 14));
        viewScoresButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 10 20;");

        Label infoLabel = new Label("(টেস্ট আইডি: IT-23001 থেকে IT-23054)");
        infoLabel.setFont(new Font("Arial", 12));
        infoLabel.setStyle("-fx-text-fill: gray;");

        startButton.setOnAction(e -> {
            String id = studentIdField.getText().trim();
            if (validateStudent(id)) {
                studentId = id;
                stage.setScene(createQuizScene(stage));
            } else {
                showAlert("Invalid Student ID", "এই স্টুডেন্ট আইডি ডাটাবেসে নেই।\nঅনুগ্রহ করে সঠিক আইডি দিন।");
            }
        });

        viewScoresButton.setOnAction(e -> stage.setScene(createScoresScene(stage)));

        layout.getChildren().addAll(titleLabel, instructionLabel, studentIdField, startButton, viewScoresButton, infoLabel);
        return new Scene(layout, 500, 450);
    }

    private boolean validateStudent(String id) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT student_id FROM student WHERE student_id = ?");
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Scene createQuizScene(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        Label headerLabel = new Label("প্রশ্ন " + (currentQuestion + 1) + " / " + questions.length);
        headerLabel.setFont(new Font("Arial", 18));
        headerLabel.setStyle("-fx-text-fill: #2196F3;");
        root.setTop(headerLabel);
        BorderPane.setAlignment(headerLabel, Pos.CENTER);
        BorderPane.setMargin(headerLabel, new Insets(0, 0, 20, 0));

        quizPane = new VBox(20);
        quizPane.setAlignment(Pos.CENTER_LEFT);
        quizPane.setPadding(new Insets(20));

        questionLabel = new Label();
        questionLabel.setFont(new Font("Arial", 20));
        questionLabel.setWrapText(true);

        answerGroup = new ToggleGroup();
        VBox optionsBox = new VBox(15);

        for (int i = 0; i < 4; i++) {
            options[i] = new RadioButton();
            options[i].setToggleGroup(answerGroup);
            options[i].setFont(new Font("Arial", 16));
            options[i].setWrapText(true);
            optionsBox.getChildren().add(options[i]);
        }

        quizPane.getChildren().addAll(questionLabel, optionsBox);
        root.setCenter(quizPane);

        Button submitButton = new Button("উত্তর জমা দিন");
        submitButton.setFont(new Font("Arial", 14));
        submitButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 10 30;");

        submitButton.setOnAction(e -> {
            if (answerGroup.getSelectedToggle() == null) {
                showAlert("Warning", "অনুগ্রহ করে একটি উত্তর নির্বাচন করুন।");
                return;
            }

            checkAnswer();
            currentQuestion++;

            if (currentQuestion < questions.length) {
                loadQuestion(currentQuestion);
                headerLabel.setText("প্রশ্ন " + (currentQuestion + 1) + " / " + questions.length);
            } else {
                saveScore();
                stage.setScene(createResultScene(stage));
            }
        });

        HBox buttonBox = new HBox(submitButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));
        root.setBottom(buttonBox);

        loadQuestion(0);
        return new Scene(root, 600, 500);
    }

    private void loadQuestion(int index) {
        questionLabel.setText(questions[index][0]);
        for (int i = 0; i < 4; i++) {
            options[i].setText(questions[index][i + 1]);
            options[i].setSelected(false);
        }
    }

    private void checkAnswer() {
        RadioButton selected = (RadioButton) answerGroup.getSelectedToggle();
        int selectedIndex = -1;
        for (int i = 0; i < 4; i++) {
            if (options[i] == selected) {
                selectedIndex = i + 1;
                break;
            }
        }

        int correctAnswer = Integer.parseInt(questions[currentQuestion][5]);
        if (selectedIndex == correctAnswer) {
            score++;
        }
    }

    private void saveScore() {
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO quiz_scores (student_id, score, total_questions) VALUES (?, ?, ?)");
            pstmt.setString(1, studentId);
            pstmt.setInt(2, score);
            pstmt.setInt(3, questions.length);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "স্কোর সংরক্ষণ করতে ব্যর্থ হয়েছে।");
        }
    }

    private Scene createResultScene(Stage stage) {
        VBox layout = new VBox(25);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));

        Label resultLabel = new Label("কুইজ সম্পন্ন হয়েছে!");
        resultLabel.setFont(new Font("Arial", 28));
        resultLabel.setStyle("-fx-text-fill: #4CAF50;");

        Label scoreLabel = new Label("আপনার স্কোর: " + score + " / " + questions.length);
        scoreLabel.setFont(new Font("Arial", 24));

        double percentage = (score * 100.0) / questions.length;
        Label percentageLabel = new Label(String.format("শতাংশ: %.1f%%", percentage));
        percentageLabel.setFont(new Font("Arial", 20));

        Button exitButton = new Button("প্রস্থান করুন");
        exitButton.setFont(new Font("Arial", 14));
        exitButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 10 30;");
        exitButton.setOnAction(e -> stage.close());

        layout.getChildren().addAll(resultLabel, scoreLabel, percentageLabel, exitButton);
        return new Scene(layout, 500, 400);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Scene createScoresScene(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("সকল স্টুডেন্টের স্কোর");
        titleLabel.setFont(new Font("Arial", 24));
        titleLabel.setStyle("-fx-text-fill: #2196F3;");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setMargin(titleLabel, new Insets(0, 0, 20, 0));
        root.setTop(titleLabel);

        VBox scoresBox = new VBox(10);
        scoresBox.setPadding(new Insets(10));
        scoresBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT student_id, score, total_questions, quiz_date " +
                            "FROM quiz_scores ORDER BY quiz_date DESC");

            if (!rs.next()) {
                Label noDataLabel = new Label("এখনও কোনো স্কোর নেই।");
                noDataLabel.setFont(new Font("Arial", 16));
                scoresBox.getChildren().add(noDataLabel);
            } else {
                do {
                    String sid = rs.getString("student_id");
                    int score = rs.getInt("score");
                    int total = rs.getInt("total_questions");
                    String date = rs.getString("quiz_date");

                    HBox scoreRow = new HBox(20);
                    scoreRow.setPadding(new Insets(10));
                    scoreRow.setStyle("-fx-background-color: white; -fx-background-radius: 3;");
                    scoreRow.setAlignment(Pos.CENTER_LEFT);

                    Label idLabel = new Label("ID: " + sid);
                    idLabel.setFont(new Font("Arial", 14));
                    idLabel.setMinWidth(120);

                    Label scoreLabel = new Label("স্কোর: " + score + "/" + total);
                    scoreLabel.setFont(new Font("Arial", 14));
                    scoreLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    scoreLabel.setMinWidth(100);

                    double percentage = (score * 100.0) / total;
                    Label percentLabel = new Label(String.format("%.0f%%", percentage));
                    percentLabel.setFont(new Font("Arial", 14));
                    percentLabel.setMinWidth(60);

                    Label dateLabel = new Label(date.substring(0, 16));
                    dateLabel.setFont(new Font("Arial", 12));
                    dateLabel.setStyle("-fx-text-fill: gray;");

                    scoreRow.getChildren().addAll(idLabel, scoreLabel, percentLabel, dateLabel);
                    scoresBox.getChildren().add(scoreRow);
                } while (rs.next());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("ডাটা লোড করতে সমস্যা হয়েছে।");
            errorLabel.setFont(new Font("Arial", 16));
            scoresBox.getChildren().add(errorLabel);
        }

        ScrollPane scrollPane = new ScrollPane(scoresBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        root.setCenter(scrollPane);

        Button backButton = new Button("ফিরে যান");
        backButton.setFont(new Font("Arial", 14));
        backButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-padding: 10 30;");
        backButton.setOnAction(e -> stage.setScene(createLoginScene(stage)));

        HBox buttonBox = new HBox(backButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));
        root.setBottom(buttonBox);

        return new Scene(root, 600, 500);
    }

    @Override
    public void stop() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

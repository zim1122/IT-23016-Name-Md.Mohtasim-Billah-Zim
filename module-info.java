
module com.example.banglagrammarquiz {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.banglagrammarquiz to javafx.fxml;
    exports com.example.banglagrammarquiz;
}

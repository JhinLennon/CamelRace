module tareas.camelrace {
    requires javafx.controls;
    requires javafx.fxml;
    // otros requires necesarios

    exports tarea.camelrace;
    exports programa;                // Exporta el paquete programa para que otros módulos puedan acceder

    opens tarea.camelrace to javafx.fxml, javafx.graphics;   // Abre el paquete tarea.camelrace para reflexión
    opens programa to javafx.fxml, javafx.graphics;           // Abre el paquete programa para reflexión
}
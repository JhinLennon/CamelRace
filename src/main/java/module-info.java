module tareas.camelrace {
    requires javafx.controls;
    requires javafx.fxml;
    // otros requires necesarios

    exports tarea.camelrace;
                // Exporta el paquete programa para que otros módulos puedan acceder

    opens tarea.camelrace to javafx.fxml, javafx.graphics;   // Abre el paquete tarea.camelrace para reflexión
    exports cliente;
    opens cliente to javafx.fxml, javafx.graphics;
    exports mensajes;
    opens mensajes to javafx.fxml, javafx.graphics;           // Abre el paquete programa para reflexión
}
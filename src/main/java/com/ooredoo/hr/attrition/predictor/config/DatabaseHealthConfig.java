package com.ooredoo.hr.attrition.predictor.config;

import com.ooredoo.hr.attrition.predictor.exception.DatabaseConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseHealthConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private DataSource dataSource;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println(" Connexion BD établie avec succès");
        } catch (SQLException e) {
            //  Utiliser votre exception personnalisée
            throw new DatabaseConnectionException(
                    " Impossible de se connecter à la base de données : " + e.getMessage(), e
            );
        }
    }
}
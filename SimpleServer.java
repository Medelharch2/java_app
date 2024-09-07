import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleServer {

    public static void main(String[] args) throws Exception {
        // Charger le driver PostgreSQL
        Class.forName("org.postgresql.Driver");

        // Créer le serveur HTTP
        HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);
        server.createContext("/", new HomeHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/user-space", new UserSpaceHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 8083");
    }

    // Handler pour la page d'accueil
    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "<html><head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f4f4f4; }" +
                ".container { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 400px; width: 100%; text-align: center; }" +
                "h1 { margin-bottom: 20px; color: #333; }" +
                "label { display: block; margin-bottom: 8px; color: #555; }" +
                "input { width: calc(100% - 22px); padding: 10px; margin-bottom: 20px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }" +
                "button { background-color: #007bff; color: #fff; border: none; padding: 10px 15px; border-radius: 4px; cursor: pointer; font-size: 16px; }" +
                "button:hover { background-color: #0056b3; }" +
                "p { margin-top: 15px; }" +
                "a { color: #007bff; text-decoration: none; }" +
                "a:hover { text-decoration: underline; }" +
                "</style></head><body>" +
                "<div class='container'>" +
                "<h1>Welcome to the Home Page</h1>" +
                "<form action='/login' method='post'>" +
                "<label for='name'>Name:</label>" +
                "<input type='text' id='name' name='name' required><br>" +
                "<label for='surname'>Surname:</label>" +
                "<input type='text' id='surname' name='surname' required><br>" +
                "<label for='email'>Email:</label>" +
                "<input type='email' id='email' name='email' required><br>" +
                "<button type='submit'>Enter</button>" +
                "</form></div></body></html>";

            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    // Handler pour la connexion
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                is.close();

                String[] params = body.split("&");
                String name = URLDecoder.decode(params[0].split("=")[1], StandardCharsets.UTF_8.toString());
                String surname = URLDecoder.decode(params[1].split("=")[1], StandardCharsets.UTF_8.toString());
                String email = URLDecoder.decode(params[2].split("=")[1], StandardCharsets.UTF_8.toString());

                try (Connection connection = DriverManager.getConnection("jdbc:postgresql://db:5432/mydatabase", "postgres", "postgres")) {
                    String query = "INSERT INTO users (name, surname, email) VALUES (?, ?, ?)";
                    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                        pstmt.setString(1, name);
                        pstmt.setString(2, surname);
                        pstmt.setString(3, email);
                        pstmt.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    String errorResponse = "<html><body><h1>Database Error</h1><p>Unable to save data.</p></body></html>";
                    exchange.sendResponseHeaders(500, errorResponse.getBytes(StandardCharsets.UTF_8).length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
                    os.close();
                    return;
                }

                String response = "<html><head>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f4f4f4; }" +
                    ".container { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 600px; width: 100%; text-align: center; }" +
                    "h1 { margin-bottom: 20px; color: #333; }" +
                    "p { margin-top: 15px; }" +
                    "a { color: #007bff; text-decoration: none; }" +
                    "a:hover { text-decoration: underline; }" +
                    "</style></head><body>" +
                    "<div class='container'>" +
                    "<h1>Login Successful</h1>" +
                    "<p><a href='/user-space'>Go to User Space</a></p></div></body></html>";

                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            } else {
                String response = new String(Files.readAllBytes(Paths.get("src/login.html")));
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
        }
    }

    // Handler pour l'enregistrement
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                is.close();

                String[] params = body.split("&");
                String name = URLDecoder.decode(params[0].split("=")[1], StandardCharsets.UTF_8.toString());
                String surname = URLDecoder.decode(params[1].split("=")[1], StandardCharsets.UTF_8.toString());
                String email = URLDecoder.decode(params[2].split("=")[1], StandardCharsets.UTF_8.toString());

                try (Connection connection = DriverManager.getConnection("jdbc:postgresql://db:5432/mydatabase", "postgres", "postgres")) {
                    String query = "INSERT INTO users (name, surname, email) VALUES (?, ?, ?)";
                    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                        pstmt.setString(1, name);
                        pstmt.setString(2, surname);
                        pstmt.setString(3, email);
                        pstmt.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    String errorResponse = "<html><body><h1>Database Error</h1><p>Unable to save data.</p></body></html>";
                    exchange.sendResponseHeaders(500, errorResponse.getBytes(StandardCharsets.UTF_8).length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
                    os.close();
                    return;
                }

                String response = "<html><head>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f4f4f4; }" +
                    ".container { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 600px; width: 100%; text-align: center; }" +
                    "h1 { margin-bottom: 20px; color: #333; }" +
                    "p { margin-top: 15px; }" +
                    "a { color: #007bff; text-decoration: none; }" +
                    "a:hover { text-decoration: underline; }" +
                    "</style></head><body>" +
                    "<div class='container'>" +
                    "<h1>Registration Successful</h1>" +
                    "<p><a href='/'>Go to Home</a></p></div></body></html>";

                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            } else {
                String response = new String(Files.readAllBytes(Paths.get("src/register.html")));
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
        }
    }

    // Handler pour l'espace utilisateur
    static class UserSpaceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "<html><head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f4f4f4; }" +
                ".container { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 800px; width: 100%; text-align: center; }" +
                "h1 { margin-bottom: 20px; color: #333; }" +
                "ul { list-style-type: none; padding: 0; }" +
                "li { margin: 10px 0; }" +
                "a { color: #007bff; text-decoration: none; }" +
                "a:hover { text-decoration: underline; }" +
                "</style></head><body>" +
                "<div class='container'>" +
                "<h1>Useful Learning Links</h1>" +
                "<ul>" +
                "<li><a href='https://www.oracle.com/java/' target='_blank'>Learn Java</a></li>" +
                "<li><a href='https://www.python.org/' target='_blank'>Learn Python</a></li>" +
                "<li><a href='https://www.w3schools.com/html/' target='_blank'>Learn HTML</a></li>" +
                "<li><a href='https://www.w3schools.com/css/' target='_blank'>Learn CSS</a></li>" +
                "<li><a href='https://www.w3schools.com/js/' target='_blank'>Learn JavaScript</a></li>" +
                "<li><a href='https://www.devops.com/' target='_blank'>Learn DevOps</a></li>" +
                "<li><a href='https://www.networkworld.com/category/networking/' target='_blank'>Learn Networking</a></li>" +
                "</ul>" +
                "</div></body></html>";

            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }
}

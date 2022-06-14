import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {

    public static void main(String[] args) throws IOException {
        ServerSocket serveur = new ServerSocket();

        if (args.length == 1) {
            serveur.bind(new InetSocketAddress(Integer.parseInt(args[0])));
        } else {
            serveur.bind(new InetSocketAddress(8080));
        }
        //En attente de la connection du client
        Socket client = serveur.accept();
        BufferedReader client_in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintStream ps = new PrintStream(client.getOutputStream());

        //Affichage de la page d'accueil
        StringBuilder sb_client = new StringBuilder();
        BufferedReader file_in = new BufferedReader(new FileReader("htdocs/index.html"));
        String line = file_in.readLine();
        while (line != null) {
            sb_client.append(line).append(" ");
            line = file_in.readLine();
        }

        ps.print("HTTP/1.1 200 OK\r\n");
        ps.print("Content-Type: text/html\r\n");
        ps.print("Content-Length: " + sb_client.length() + "\r\n");
        ps.print("\r\n");
        ps.print(sb_client);

        while (client.isConnected()) {
            //Ecoute de la reponse du client
            boolean trouve = false;
            String file = "";
            try {
                line = client_in.readLine();
            } catch (IOException e) {
                System.out.println("Buffered vide");
                line = "";
                file = "";
            }
            System.out.println("Avant while");
            while (!line.isEmpty()) {
                //si la requete contient un lien vers un fichier
                if (line.contains("GET")) {
                    String[] split = line.split(" ");
                    file = split[1];
                    trouve = true;
                }
                line = client_in.readLine();
            }
            System.out.println("Apres while");
            if (file.equals("/")) {
                file = "/index.html";
            }
            //Affichage de la page demandée
            try {
                //si le fichier existe
                if (new File("htdocs" + file).exists()) {
                    //Affichage de la page
                    sb_client = new StringBuilder();
                    file_in = new BufferedReader(new FileReader("htdocs" + file));
                    line = file_in.readLine();
                    while (line != null) {
                        System.out.println(line);
                        sb_client.append(line).append(" ");
                        line = file_in.readLine();
                    }
                }
            } catch (FileNotFoundException e) {
                trouve = false;
            }
            if (trouve && !file.isEmpty()) {
                ps = new PrintStream(client.getOutputStream());
                ps.print("HTTP/1.1 200 OK\r\n");
            } else if (!trouve && !file.isEmpty()) {
                ps = new PrintStream(client.getOutputStream());
                ps.print("HTTP/1.1 404 Not Found\r\n");
            }
            ps.print("Content-Type: text/html\r\n");
            ps.print("Content-Length: " + sb_client.length() + "\r\n");
            ps.print("\r\n");
            ps.print(sb_client);
        }
        client.close();
    }
}
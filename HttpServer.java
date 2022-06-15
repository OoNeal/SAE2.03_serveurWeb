package SiteWeb;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class HttpServer {

    public static void main(String[] args) throws IOException {
        int port;
        String root;
        boolean index;
        String[] acceptedIP;
        String[] rejectedIP;
        try {
            File file = new File("Configuration.tld");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("port");
            port = Integer.parseInt(nodeList.item(0).getTextContent());
            nodeList = doc.getElementsByTagName("root");
            root = nodeList.item(0).getTextContent();
            nodeList = doc.getElementsByTagName("index");
            index = Boolean.parseBoolean(nodeList.item(0).getTextContent());
            nodeList = doc.getElementsByTagName("accept");
            acceptedIP = nodeList.item(0).getTextContent().split(",");
            nodeList = doc.getElementsByTagName("reject");
            rejectedIP = nodeList.item(0).getTextContent().split(",");
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Server started on port " + port);
        System.out.println("Root directory: " + root);
        System.out.println("Index file: " + index);
        System.out.println("Accepted IP: " + Arrays.toString(acceptedIP));
        System.out.println("Rejected IP: " + Arrays.toString(rejectedIP));


        ServerSocket ss;
        boolean arret = false;
        String status200 = "HTTP/1.1 200 OK";
        String status404 = "HTTP/1.1 404 Not Found";
        String status501 = "HTTP/1.1 501 Not Implemented";
        String docroot = "web";
        byte[] crlf = new byte[2];
        crlf[0] = 0x0D;
        crlf[1] = 0x0A;
        if (port == 0)
            port = 8080;
        ss = new ServerSocket(port);
        System.out.println("Serveur en attente de connexions");
        while (!arret) {
            Socket clientS = ss.accept();
            for (String ip : rejectedIP) {
                if (clientS.getInetAddress().getHostAddress().equals(ip)) {
                    //Ecrit une page html pour dire que l'ip est refusée
                    PrintWriter out = new PrintWriter(clientS.getOutputStream());
                    out.println("HTTP/1.1 403 Forbidden");
                    out.println("Content-Type: text/html");
                    out.println("");
                    out.println("<html><body>");
                    out.println("<h1>403 Forbidden</h1>");
                    out.println("<p>Vous n'êtes pas autorisé à allez sur ce site.</p>");
                    out.println("</body></html>");
                    out.close();
                    clientS.close();
                    throw new RuntimeException("IP rejected");
                }
            }
            System.out.println("Nouveau client, adresse "
                    + clientS.getInetAddress() + " sur le port "
                    + clientS.getPort());
            OutputStream output = clientS.getOutputStream();
            InputStream input = clientS.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    input));
            /* lit la premiere ligne */
            String message = br.readLine();
            System.out.println("< " + message);
            /* Lit toutes les autres lignes en attente, jusqu'a la premiere ligne vide (fin de la requete) */
            String s;
            do {
                s = br.readLine();
                System.out.println("< " + s);
            } while (!s.equals(""));
            String[] requete = message.split(" ");
            /* /!\ IMPORTANT: pour lire le fichier et l'écrire sur la connexion TCP,
             * il faut utiliser un DataOutputStream (flux d'octets) et pas un
             * OutputStreamWriter/BufferWriter (flux de caractères). Sinon, les
             * conversions vont corrompre les images. */
            DataOutputStream data = new DataOutputStream(output);
            if (requete[0].equals("GET")) {
                /* Override manuel: / -> /index.html */
                if (requete[1].equals("/")) {
                    requete[1] = "/index.html";
                }
                if (new File("htdocs" + requete[1]).isFile() && index) {
                    /* le fichier existe, on l'envoie */
                    FileInputStream fis = new FileInputStream("htdocs" + requete[1]);
                    int size = fis.available();
                    byte[] fichier = new byte[size];
                    fis.read(fichier);
                    System.out.println("> [fichier] " + "htdocs" + requete[1]);
                    /* première solution: on utilise writeBytes().
                     * attention, writeChars() ne marche pas, car les caractères
                     * sont stockés sur 2 octets en Java. */
                    data.writeBytes(status200 + "\r\n\r\n");
                    data.write(fichier);
                } else if (!index) {
                    index = true;
                    PrintWriter out = new PrintWriter(clientS.getOutputStream());
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html");
                    out.println("");
                    out.println("<html><body>");
                    out.println("<h1>Index of /SiteWeb</h1>");
                    out.println("<a href = \"/\">index.html</a></br>");
                    out.println("<a href = \"miniweb.html\">miniweb.html</a></br>");
                    out.println("<a href = \"/technique.html\">miniweb.html</a></br>");
                    out.println("<a href = \"images/\">images</a></br>");
                    out.println("</body></html>");
                    out.flush();
                } else {
                    /* le fichier n'existe pas => erreur 404 */
                    /* autre solution: on écrit avec write(String.getbyte()) */
                    System.out.println("> [erreur] " + "htdocs" + requete[1]);
                    data.write(status404.getBytes());
                    data.write(crlf);
                    data.write(crlf);
                    System.out.println("> " + status404);
                }
            } else {
                data.write(status501.getBytes());
                data.write(crlf);
                data.write(crlf);
                System.out.println("> " + status501);
            }
            data.close();
            output.close();
            br.close();
            clientS.close();

        }
        ss.close();
    }
}
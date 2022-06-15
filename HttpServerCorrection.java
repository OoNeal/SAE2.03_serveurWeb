package SiteWeb;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServerCorrection {
	public static void main(String[] args) throws IOException {
		ServerSocket ss;
		boolean arret = false;
		String status200 = "HTTP/1.1 200 OK";
		String status404 = "HTTP/1.1 404 Not Found";
		String status501 = "HTTP/1.1 501 Not Implemented";
		String docroot = "web";
		byte[] crlf = new byte[2];
		crlf[0] = 0x0D;
		crlf[1] = 0x0A;
		if(args.length == 0)
			ss = new ServerSocket(8000);
		else
			ss = new ServerSocket(Integer.parseInt(args[0]));
		System.out.println("Serveur en attente de connexions");
		while (!arret) {
			Socket clientS = ss.accept();
			System.out.println("Nouveau client, adresse "
					+ clientS.getInetAddress() + " sur le port "
					+ clientS.getPort());
			OutputStream output = clientS.getOutputStream();
			InputStream input = clientS.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(
						input));
			/* lit la premiere ligne */
			String message = br.readLine();
			System.out.println("< "+message);
			/* Lit toutes les autres lignes en attente, jusqu'a la premiere ligne vide (fin de la requete) */
			String s;
			while (true) {
				s = br.readLine();
				System.out.println("< "+s);
				if (s.equals("")) break;
			}
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
				if (new File("htdocs" + requete[1]).isFile()) {
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


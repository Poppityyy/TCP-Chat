import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.net.SocketException;

public class TCPServer implements Runnable {

    private ArrayList <ConnectionHandler> connections; // lista di connessioni attive 
    private ServerSocket server; // socket del server
    private ExecutorService executor; // thread pool per gestire le connessioni

    public TCPServer() {
        connections = new ArrayList <>(); // inizializzo la lista di connessioni attive
    }

    @Override

    public void run() {
        try {
            server = new ServerSocket(6789);// creo il socket del server
            System.out.println("Server avviato!"); 
            executor = java.util.concurrent.Executors.newCachedThreadPool(); // creo un thread pool con un numero di thread variabile
            while (true) {
                Socket client = server.accept(); // accetto una nuova connessione
                ConnectionHandler handler = new ConnectionHandler(client); // creo un nuovo handler per la connessione
                connections.add(handler); // aggiungo il nuovo handler alla lista di connessioni attive
                executor.execute(handler); // eseguo il nuovo handler
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void broadcast(String message) { // metodo per inviare un messaggio a tutti i client connessi
        for (ConnectionHandler connection : connections) { // scorro la lista di connessioni attive
            if (connection != null) { // se la connessione è attiva
                connection.sendMessage(message);
            }
        }
    }

    public void removeConnection(ConnectionHandler connection) { // metodo per rimuovere una connessione dalla lista di connessioni attive
        connections.remove(connection);
    }

    class ConnectionHandler implements Runnable {

        private Socket client; 
        private BufferedReader in;
        private PrintWriter out;
        private String nickname = "Anonimo"; // nickname del client non loggato

        public ConnectionHandler(Socket client) { // costruttore del handler
            this.client = client;
        }

        public String getNickname() { // metodo per ottenere il nickname del client
            return nickname;
        }

        public boolean validName(String nickname) { // metodo per controllare se il nickname è valido
            if (nickname.contains("server") || nickname.contains("login") || nickname.contains("bye") || nickname.contains("who") || nickname.contains("all") || nickname.startsWith("@")) {
                return false;
            }
            return true;
        }

        public boolean nameInUse(String nickname) { // metodo per controllare se il nickname è già in uso
            for (ConnectionHandler connection : connections) {
                if (connection.getNickname().equals(nickname)) {
                    return true;
                }
            }
            return false;
        }

        public void sendMessageToOtherNickname(String message, ConnectionHandler connection) { // metodo per inviare un messaggio privato ad un altro client
            String[] messageSplit = message.split(" ", 2); // @nickname messaggio -> 2 elementi (0: @nickname, 1: messaggio)
            if (messageSplit.length == 2) { 
            connection.sendMessage("Messaggio privato da " + this.nickname + ": " + messageSplit[1]);
            this.sendMessage("Messaggio privato a " + connection.getNickname() + ": " + messageSplit[1]);
            }
        }

        public ConnectionHandler isPrivateMessage(String message) { // metodo per controllare se il messaggio è privato
            if (message.startsWith("@")) {
                String[] messageSplit = message.split(" ", 2); // @nickname messaggio -> 2 elementi (0: @nickname, 1: messaggio)
                if (messageSplit.length == 2) {
                    String nickname = messageSplit[0].substring(1); 
                    if (nickname.equals(this.nickname)) {
                        return null; // se il messaggio è privato a se stesso non lo invio
                    }
                    for (ConnectionHandler connection : connections) { // scorro la lista di connessioni attive
                        System.out.println(connection.getNickname()+ " " + nickname);
                        if (connection.getNickname().equals(nickname)) { // se il nickname è quello di un client connesso
                            return connection;
                        }
                    }
                }
            }
            return null; 
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);
                System.out.println("Client connesso: " + client.getInetAddress().getHostAddress()+ ":" + client.getPort());
                out.println ("Inserisci un nickname con @login nickname!");
                String message;
                while ((message = in.readLine()) != null) { // leggo i messaggi inviati dal client
                    if (this.nickname == "Anonimo") {
                        if (message.startsWith("@login ")) { // comando per effettuare l'accesso
                            String[] messageSplit = message.split(" ", 2); // @login nickname  -> 2 elementi (0: @login, 1: nickname)
                            if (messageSplit.length == 2) { // se il messaggio è corretto
                                if (!validName(messageSplit[1])) { // se il nickname non è valido
                                    out.println("Errore: non hai inserito un nickname valido!");
                                    continue;
                                }
                                if (nameInUse(messageSplit[1])) { // se il nickname è già in uso
                                    out.println("Errore: nickname già in uso!");
                                    continue;
                                }
                                this.nickname = messageSplit[1]; // se il nickname è valido e non è già in uso lo assegno al client
                                System.out.println(client.getInetAddress().getHostAddress() + ":" + client.getPort() + " si è connesso come " + this.nickname);
                                out.println("Accesso effettuato come: " + this.nickname);
                                continue;
                            } else {
                                out.println("Errore: non hai inserito un nickname valido!");
                                continue;
                            }
                        } else {
                            out.println("Devi autenticarti con @login nickname!");
                            continue;
                        }
                    } 
                    if (message.startsWith("@bye")) { // comando per disconnettersi
                        broadcast(this.nickname + " si è disconnesso dalla chat!");
                        // rimuovi il nome utente dall'elenco degli utenti connessi
                        connections.remove(this);
                        close(); // chiudo la connessione

                    } else if (message.startsWith("@all ")) { // comando per inviare un messaggio a tutti i client connessi
                        broadcast(this.nickname + " to " + message.replace("@all", "All:")); 

                    } else if (message.startsWith("@who")) {
                        out.println("Utenti connessi: ");
                        for (ConnectionHandler connection : connections) { // scorro la lista di connessioni attive
                            if (connection != null) { // se la connessione è attiva
                                out.println(connection.getNickname()); // stampo il nickname
                            }
                        } 
                    } else if (message.startsWith("@")) { // comando per inviare un messaggio privato ad un altro client
                        ConnectionHandler connection = isPrivateMessage(message); // controllo se il messaggio è privato
                        if (connection != null) {
                            sendMessageToOtherNickname(message, connection);
                        } else {
                            out.println("Comando non valido!");
                            // Non controllo se l'utente esiste perchè potrebbe essere un comando non valido e non un nickname inesistente :D
                        } 
                    }  

                }

            } catch (SocketException ex) {
                System.out.println("Connessione chiusa per l'utente: " + this.nickname+ ", IP: "+ client.getInetAddress().getHostAddress() + ":" + client.getPort());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) { // metodo per inviare un messaggio al client
            out.println(message);
        }

        public void close() { // metodo per chiudere la connessione con il client
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        TCPServer server = new TCPServer(); // creo un nuovo server
        server.run(); // avvio il server
    }
}
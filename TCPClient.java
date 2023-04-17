import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class TCPClient implements Runnable {

    private Socket client; // socket del client
    private PrintWriter out;
    private BufferedReader in;

    @Override

    public void run() {
        try {
            Socket client = new Socket("localhost", 6789); // creo il socket del client
            out = new PrintWriter(client.getOutputStream(), true); 
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler inputHandler = new InputHandler(); // creo un nuovo handler per la connessione
            Thread inputThread = new Thread(inputHandler); // creo un nuovo thread per l'handler
            inputThread.start(); // avvio il thread
            String erMessaggio; // variabile per memorizzare il messaggio ricevuto dal server

            while ((erMessaggio = in.readLine()) != null) { // leggo il messaggio ricevuto dal server
                System.out.println(erMessaggio);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() { // metodo per chiudere la connessione
        try {
            in.close();
            out.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class InputHandler implements Runnable { // classe per gestire l'input da tastiera

        @Override
        public void run() {
            try {
                BufferedReader console = new BufferedReader(new InputStreamReader(System.in)); // creo un buffer per leggere l'input da tastiera
                String message;
                while ((message = console.readLine()) != null) { // leggo l'input da tastiera
                    out.println(message);
                }

            } catch (SocketException ex) {
                System.out.println("Connessione chiusa");
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    public static void main(String[] args) {
        TCPClient client = new TCPClient(); // creo un nuovo client
        Thread clientThread = new Thread(client); // creo un nuovo thread per il client
        clientThread.start(); // avvio il thread
    }
}
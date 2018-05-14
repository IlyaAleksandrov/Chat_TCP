package Aleksandrov;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected;

    protected String getServerAddress() throws IOException, ClassNotFoundException {
        ConsoleHelper.writeMessage("введите адреса сервера");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("введите порта сервера");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("введите имя пользователя");
        return ConsoleHelper.readString();
    }


    protected void sendTextMessage(String text) {
        try{
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException e) {
            ConsoleHelper.writeMessage("ошибка отправки");
            clientConnected = false;
        }
    }
    public void action() {
        SocketThread socketThread = new SocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                wait();
                if(clientConnected) {
                    ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
                }
                else
                    ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
                String message;
                while (clientConnected) {
                    message = ConsoleHelper.readString();
                    if (message.equals("exit")) {
                        break;
                    }
                    sendTextMessage(message);
                }
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("ошибка");
            }
        }

    }

    public static void main(String[] args) {
        Client client = new Client();
        client.action();
    }



    public class SocketThread extends Thread {
        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }
        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage("Участник с именем " + userName + " присоединился к чату");
        }
        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник с именем " + userName + " покинул к чат");
        }
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (MessageType.NAME_REQUEST.equals(message.getType())) {
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                } else if (MessageType.NAME_ACCEPTED.equals(message.getType())) {
                    notifyConnectionStatusChanged(true);
                    return;
                } else
                    throw new IOException("Unexpected Aleksandrov.MessageType");
            }
        }


        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                String text = message.getData();
                if (MessageType.TEXT.equals(message.getType())) {
                    processIncomingMessage(text);
                }
                else if (MessageType.USER_ADDED.equals(message.getType())) {
                    informAboutAddingNewUser(text);
                }
                else if (MessageType.USER_REMOVED.equals(message.getType())) {
                    informAboutDeletingNewUser(text);
                }
                else
                    throw new IOException("Unexpected Aleksandrov.MessageType");
            }
        }

        public void run(){
            try {
                Socket socket = new Socket(getServerAddress(), getServerPort());
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();

            } catch (IOException e) {
                notifyConnectionStatusChanged(false);
            } catch (ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }
    }
}
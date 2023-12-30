import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/* class to demonstrate use of Gmail list labels API */
public class GmailAPITest {
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "Gmail API Test";
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/gmail.readonly");


    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = GmailAPITest.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        //returns an authorized Credential object.
        return credential;
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Print the labels in the user's account.
        String user = "me";
//        ListLabelsResponse listResponse = service.users().labels().list(user).execute();
//        List<Label> labels = listResponse.getLabels();
//        if (labels.isEmpty()) {
//            System.out.println("No labels found.");
//        } else {
//            System.out.println("Labels:");
//            for (Label label : labels) {
//                System.out.printf("- %s\n", label.getName());
//            }
//        }

        List<Message> messages = service.users().messages().list(user).execute().getMessages();

        // zapisywanie wiadomości w output.html
//        Message firstMessage = messages.get(0);
//        getMessageBody(firstMessage, service, user);

        Message firstMessage = messages.get(2);

        System.out.println(getSender(firstMessage, service, user));
    }

    private static String getSender(Message rawMessage, Gmail service, String user) throws IOException {
        Message message = getMessage(rawMessage, service, user);

        String fromHeader = message.getPayload().getHeaders().stream()
                .filter(header -> "From".equals(header.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse(null);

        return fromHeader;
    }

    public static void createHTMLFile(String htmlContent, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(htmlContent);
            System.out.println("Plik HTML został utworzony pomyślnie.");
        } catch (IOException e) {
            System.err.println("Błąd podczas zapisywania do pliku HTML: " + e.getMessage());
        }
    }

    private static Message getMessage(Message rawMessage, Gmail service, String user) throws IOException {
        String messageId = rawMessage.getId();

        return service.users().messages().get(user, messageId).execute();
    }

    private static void getMessageBody(Message rawMessage, Gmail service, String user) throws IOException {
        Message message = getMessage(rawMessage, service, user);

        if (message == null || message.getPayload() == null) {
            System.out.println("Brak treści wiadomości.");
            return;
        }

        MessagePart messagePart = message.getPayload();
        String mimeType = messagePart.getMimeType();
        System.out.println(mimeType);

        if ("multipart/alternative".equals(mimeType) && messagePart.getParts() != null) {
            List<MessagePart> parts = messagePart.getParts();
            if (!parts.isEmpty()) {
                for (MessagePart part : parts) {
                    String partMimeType = part.getMimeType();
                    if ("text/plain".equals(partMimeType) || "text/html".equals(partMimeType)) {
                        String bodyData = getBodyData(part);
                        if (bodyData != null && !bodyData.isEmpty()) {
                            String bodyText = new String(Base64.getUrlDecoder().decode(bodyData), StandardCharsets.UTF_8);
                            System.out.println(bodyText);
                            createHTMLFile(bodyText, "output.html");
                            return;
                        }
                    }
                }
            }
        } else {
            String bodyData = getBodyData(messagePart);
            if (bodyData != null && !bodyData.isEmpty()) {
                String bodyText = new String(Base64.getUrlDecoder().decode(bodyData), StandardCharsets.UTF_8);
                System.out.println(bodyText);
                createHTMLFile(bodyText, "output.html");
            } else {
                System.out.println("Treść wiadomości jest pusta.");
            }
        }
    }

    private static String getBodyData(MessagePart messagePart) {
        if (messagePart.getBody() != null) {
            return messagePart.getBody().getData();
        } else if (messagePart.getParts() != null && !messagePart.getParts().isEmpty()) {
            return getBodyData(messagePart.getParts().get(0));
        }
        return null;
    }
}
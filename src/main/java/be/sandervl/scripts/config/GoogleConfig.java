package be.sandervl.scripts.config;

import be.sandervl.scripts.Constants;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Configuration
public class GoogleConfig {
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = List.of(
            SheetsScopes.SPREADSHEETS,
            DocsScopes.DOCUMENTS,
            GmailScopes.GMAIL_COMPOSE,
            GmailScopes.GMAIL_SEND);

    @Deprecated
    private static Credential getServiceAccountCredentials(List<String> scopes) throws IOException {
        GoogleCredentials credentials = getGoogleCredentials(scopes);
        credentials.refresh();
        var token = credentials.getAccessToken().getTokenValue();
        return new GoogleCredential().setAccessToken(token);
    }

    private static GoogleCredentials getGoogleCredentials(List<String> scopes) throws IOException {
        FileInputStream serviceAccount = new FileInputStream(new ClassPathResource(Constants.SERVICE_ACCOUNT_FILE).getFile());

        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount).createScoped(scopes);
        return credentials;
    }

    private static Credential getUserCredentialsViaFlow(List<String> scopes) throws IOException {
        FileInputStream credentialsFile = new FileInputStream(new ClassPathResource(Constants.CLIENT_SECRET_FILE).getFile());
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(Constants.JSON_FACTORY, new InputStreamReader(credentialsFile));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                Constants.HTTP_TRANSPORT, Constants.JSON_FACTORY, clientSecrets, scopes)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    @Bean
    public Sheets sheets() throws IOException {
        var credentials = GoogleConfig.getServiceAccountCredentials(SCOPES);
        return new Sheets.Builder(Constants.HTTP_TRANSPORT, Constants.JSON_FACTORY, credentials).setApplicationName(Constants.APPLICATION_NAME).build();
    }

    @Bean
    public Gmail gmail() throws IOException {
        Credential credentialFlow = GoogleConfig.getUserCredentialsViaFlow(SCOPES);

        return new Gmail.Builder(Constants.HTTP_TRANSPORT, Constants.JSON_FACTORY, credentialFlow).setApplicationName(Constants.APPLICATION_NAME).build();
    }

    @Bean
    public Firestore getFirestore() throws IOException {
        var credentials = GoogleConfig.getGoogleCredentials(SCOPES);

        FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(credentials).build();

        FirebaseApp.initializeApp(options);
        return FirestoreClient.getFirestore();
    }
}

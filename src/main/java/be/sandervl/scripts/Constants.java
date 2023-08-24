package be.sandervl.scripts;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class Constants {
    public static final String APPLICATION_NAME = "SmartChiroExportApplication";
    /*
    The spreadsheet id of all members of the chiro camp
     */
    public static final String MEMBERS_SHEET_ID = "1ZcwdxwsBIKsqj-gstt2NYSHe4MOmgZsTLU2ZAG936ew";
    /*
    This is the tab name of the spreadsheet that contains all members of the chiro camp and will be read to send out the attesten via email
     */
    public static final String SHEETS_TAB_TO_READ = "Ledenlijst";

    /*
    This is the tab name of the spreadsheet that will be used to export all SmartChiro data to
     */
    public static final String SHEETS_TAB_TO_EXPORT = "SmartChiro";

    /*
    The template to use to generate the attesten (see e.g. https://docs.google.com/document/d/1d79m6uieyH7rLOOU1keaj2omF-QBmEmKHsmNW1dH-gM/edit)
     */
    public static final String ATTEST_FILE_TEMPLATE = "templates/template.docx";

    /*
    The email address that should be used to send out the emails.
    This should be the account that is authenticated via the authetication flow.
     */
    public static final String EMAIL_FROM_ADDRESS = "sandervl34@gmail.com";
    /*
    The service account file is the file that is downloaded from the Google Cloud Console.
    see https://console.cloud.google.com/iam-admin/serviceaccounts/details/109753145357061154936?project=smartchiro-9f399&supportedpurview=project
     */
    public static final String SERVICE_ACCOUNT_FILE = "smartchiro-9f399-firebase-adminsdk-x78p0-874b7e3cd4.json";

    /*
    The client secret file is the file that is downloaded from the Google Cloud Console.
    see https://console.cloud.google.com/apis/credentials/oauthclient/505688306972-m49ocg6oqj3h3tc9vdqdr0e7ds2jrvct.apps.googleusercontent.com?project=smartchiro-9f399
     */
    public static final String CLIENT_SECRET_FILE = "client_secret_505688306972-m49ocg6oqj3h3tc9vdqdr0e7ds2jrvct.apps.googleusercontent.com.json";
    public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    public static HttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}

package be.sandervl.scripts.mailmerge;

import be.sandervl.scripts.Constants;
import com.deepoove.poi.XWPFTemplate;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.sheets.v4.Sheets;
import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static be.sandervl.scripts.Constants.MEMBERS_SHEET_ID;

/**
 * This script will read all members from the configured spreadsheet.
 * It will then create a document (attest) for each member and save it to a file, replacing the placeholders with the actual values.
 * The template for the document is configured in Constants.ATTEST_FILE_TEMPLATE
 * Based on the age (14 or younger) it will send an email to the parents with the document attached.
 * It will use the gmail API to send the email.
 * <p>
 * Make sure to verify the subject and body of the email before running this script.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"be.sandervl.scripts.config"})
@Component
public class FiscaleAttestMailMerge {

    @Autowired
    private Sheets sheets;

    @Autowired
    private Gmail gmail;

    public static void main(String[] args) {
        SpringApplication.run(FiscaleAttestMailMerge.class, args);
    }

    private static Set<String> getEmailsFromData(Map<String, String> data) {
        Set<String> emails = new HashSet<>();
        if (StringUtils.isNotBlank(getEmailAddresseesFromData(data, "E-mail1"))) {
            emails.add(getEmailAddresseesFromData(data, "E-mail1"));
        }
        if (StringUtils.isNotBlank(getEmailAddresseesFromData(data, "E-mail2"))) {
            emails.add(getEmailAddresseesFromData(data, "E-mail2"));
        }
        if (StringUtils.isNotBlank(getEmailAddresseesFromData(data, "E-mail3"))) {
            emails.add(getEmailAddresseesFromData(data, "E-mail3"));
        }
        return emails;
    }

    private static String getEmailAddresseesFromData(Map<String, String> data, String key) {
        Pattern pattern = Pattern.compile("<(.*?)>");
        Matcher matcher = pattern.matcher(data.get(key));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static void addNonSpeelclubData(Map<String, String> data) {
        data.put("period", """
                van 18/07/2023
                tot 28/07/2023
                """);
        data.put("days", "11");
        data.put("dailyRate", "€15");
        data.put("amount", "€165");
    }

    private static void addSpeelclubData(Map<String, String> data) {
        data.put("period", """
                van 21/07/2023
                tot 28/07/2023
                """);
        data.put("days", "8");
        data.put("dailyRate", "€18.12");
        data.put("amount", "€145");
    }

    private static Function<Object, String> prepareValue(List<Object> lidFromSheet, List<Object> keys) {
        return v -> {
            if (keys.indexOf(v) >= lidFromSheet.size()) {
                return "";
            }
            return lidFromSheet.get(keys.indexOf(v)).toString().trim();
        };
    }

    private static Function<Object, String> prepareKey() {
        return k -> k.toString().trim()
                .replace(" ", "")
                .replace(".", "");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void run() throws Exception {
        System.out.println("hello world, I have just started up");

        var toProcess = getLedenToProcess();

        toProcess.forEach(this::processLid);
    }

    private void processLid(Map<String, String> data) {
        var isSpeelclub = data.get("Afdelingen").equals("SP");
        if (isSpeelclub) {
            addSpeelclubData(data);
        } else {
            addNonSpeelclubData(data);
        }

        String fileName = "attesten/" + data.get("Attestvolgnummer") + ".docx";
        try (XWPFTemplate template = XWPFTemplate.compile(new ClassPathResource(Constants.ATTEST_FILE_TEMPLATE).getFile())) {
            template.render(data).writeToFile(fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //Save the document to PDF
        Document doc = new Document(fileName);
        String pdfFile = fileName.replace("docx", "pdf");
        doc.saveToFile(pdfFile, FileFormat.PDF);
        var deleted = new File(fileName).delete();
        File attachment = new File(pdfFile);

        Set<String> emails = getEmailsFromData(data);
        System.out.println("Sending email to " + emails);

        emails.forEach(e -> sendEmail(e, attachment));
    }

    private void sendEmail(String toEmailAddress, File attachment) {
        try {
            // Create the email content
            String messageSubject = "Chirojongens Elzestraat - Fiscaal attest Kamp Poulseur 2023";
            String bodyText = """
                    Beste ouder,<br/>
                                        
                    <p>
                    In bijlage kan u het fiscale attest voor het voorbije kamp van uw zoon terugvinden.
                    Dit attest kan je gebruiken om de kosten van het kamp in te brengen bij de belastingen.
                    Alle info kan je hier terugvinden: https://chiro.be/info-voor-leiding/administratie/fiscaal-attest-kinderopvang.
                    </p>
                    <p>
                    Het attest is al gedeeltelijk door ons ingevuld. U hoeft enkel nog uw persoonlijke gegevens in te vullen in VAK II.
                    Moesten er gegevens ontbreken of foutief zijn, gelieve mij dan te contacteren.
                    </p>
                    <p>
                    Graag wil ik jullie er ook nog aan herinneren dat er ook tegemoetkomingen zijn vanuit de mutualiteit of van het OCMW.
                    De aanvraagprocedure voor de tegemoetkomingen via de mutualiteit verschilt van mutualiteit tot mutualiteit.
                    Alle info staat gebundeld op de website van Chiro nationaal: https://chiro.be/overzicht-tegemoetkomingen.
                    Vraag gerust aan de leiding om de benodigde documenten in te vullen.
                    </p>
                    <br/>
                    Met vriendelijke groeten,<br/>
                    Sander Van Loock<br/>
                    VB Chirojongens Elzestraat
                    """;

            // Encode as MIME message
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage email = new MimeMessage(session);
            email.setFrom(new InternetAddress(Constants.EMAIL_FROM_ADDRESS));
            email.addRecipient(javax.mail.Message.RecipientType.TO,
                    new InternetAddress(toEmailAddress));
            email.setSubject(messageSubject);
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(bodyText, "text/html");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            mimeBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(attachment);
            mimeBodyPart.setDataHandler(new DataHandler(source));
            mimeBodyPart.setFileName(attachment.getName());
            multipart.addBodyPart(mimeBodyPart);
            email.setContent(multipart);

            // Encode and wrap the MIME message into a gmail message
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            byte[] rawMessageBytes = buffer.toByteArray();
            String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
            Message message = new Message();
            message.setRaw(encodedEmail);
            message = gmail.users().messages().send("me", message).execute();
            System.out.println(message.toPrettyString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Map<String, String>> getLedenToProcess() throws IOException {
        var range = Constants.SHEETS_TAB_TO_READ + "!A1:AQ99";
        var leden = sheets.spreadsheets().values().get(MEMBERS_SHEET_ID, range).setValueRenderOption("FORMATTED_VALUE").execute();
        var toProcess = new ArrayList<Map<String, String>>();
        for (int i = 0; i < leden.getValues().size(); i++) {
            if (i == 0) {
                continue; // skip header
            }
            var lidFromSheet = leden.getValues().get(i);
            List<Object> keys = leden.getValues().get(0);
            var parsedLid = getParsedLid(lidFromSheet, keys);
            parsedLid.computeIfPresent("Leeftijd", (k, v) -> {
                if (StringUtils.isNotBlank(v) && Integer.parseInt(v) <= 14) {
                    toProcess.add(parsedLid);
                }
                return v;
            });
        }
        toProcess.forEach(System.out::println);
        return toProcess;
    }

    private Map<String, String> getParsedLid(List<Object> lidFromSheet, List<Object> keys) {
        return keys.stream().collect(Collectors.toMap(prepareKey(), prepareValue(lidFromSheet, keys), (a, b) -> b, TreeMap::new));
    }
}

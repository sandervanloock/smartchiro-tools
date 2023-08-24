package be.sandervl.scripts.smartchiroexport;

import be.sandervl.scripts.Constants;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This script will export all members from the SmartChiro database to a Google Sheet.
 * <p>
 * It will read all members from firestore, collection "leden" and export them to the configured sheet and tab.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"be.sandervl.scripts.config"})
@Component
public class SmartChiroExportApplication {

    @Autowired
    private Sheets sheets;
    @Autowired
    private Firestore firestore;

    public static void main(String[] args) {
        SpringApplication.run(SmartChiroExportApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() throws Exception {
        System.out.println("hello world, I have just started up");

        List<List<Object>> values = new ArrayList<>();
        values.add(List.of("naam", "huisdokter", "rijksregister", "paracetamol", "aandoeningen", "allergieen", "tetanus", "medischeHandelingen", "kanDeelnemen", "kanZwemmen", "dieet", "vlugMoe", "vroegereZiekten"));
        var documents = firestore.collection("leden").get().get().getDocuments();

        for (QueryDocumentSnapshot document : documents) {
            Spliterator<CollectionReference> subcollections = firestore.collection("leden").document(document.getId()).listCollections().spliterator();
            List<CollectionReference> betaling = StreamSupport.stream(subcollections, false).filter(cr -> cr.getId().equals("betaling")).toList();
            if (betaling.isEmpty()) {
                continue;
            }
            var betalingen = betaling.get(0).get().get();
            var isLid = betalingen.getDocuments().stream().filter(b -> Objects.equals(b.get("jaar", Integer.class), LocalDate.now().getYear() - 1)).findAny();
            if (isLid.isPresent()) {
                var parsed = document.toObject(Lid.class);
                System.out.printf("%s - %s - %s%n", parsed.getNaam(), parsed.getContact().getHuisarts().getNaam(), parsed.getMedischeFiche().toString());
                values.add(List.of(
                        parsed.getNaam(),
                        parsed.getContact().getHuisarts().getNaam(),
                        Optional.ofNullable(parsed.getMedischeFiche()).flatMap(mf -> Optional.ofNullable(mf.covid)).filter(m -> !m.isEmpty()).flatMap(c -> Optional.ofNullable(c.get("rijksregisternummer"))).map(Object::toString).orElse(""),
                        Optional.ofNullable(parsed.getMedischeFiche()).flatMap(mf -> Optional.ofNullable(mf.covid)).filter(m -> !m.isEmpty()).flatMap(c -> Optional.ofNullable(c.get("paracetamol"))).map(Object::toString).orElse(""),
                        Optional.ofNullable(parsed.getMedischeFiche()).flatMap(mf -> Optional.ofNullable(mf.aandoeningen)).filter(m -> !m.isEmpty()).map(Map::keySet).stream().flatMap(Collection::stream).collect(Collectors.joining(", ")),
                        Optional.ofNullable(parsed.getMedischeFiche()).flatMap(mf -> Optional.ofNullable(mf.allergieen)).filter(m -> !m.isEmpty()).stream().flatMap(Collection::stream).flatMap(e -> e.entrySet().stream().filter(al -> al.getKey().equals("beschrijving")).map(a -> a.getValue().toString())).collect(Collectors.joining(", ")),
                        Optional.ofNullable(parsed.getMedischeFiche()).flatMap(mf -> Optional.ofNullable(mf.tetanus)).filter(m -> !m.isEmpty()).map(e -> e.getOrDefault("gevaccineerd", "false") + " in het jaar " + e.getOrDefault("jaar", "?")).orElse(""),
                        Optional.ofNullable(parsed.getMedischeFiche()).flatMap(mf -> Optional.ofNullable(mf.medischeHandelingen)).map(Object::toString).orElse(""),
                        Optional.ofNullable(parsed.getMedischeFiche()).flatMap(mf -> Optional.ofNullable(mf.kanDeelnemen)).map(Object::toString).orElse(""),
                        Optional.ofNullable(parsed.getMedischeFiche()).flatMap(mf -> Optional.ofNullable(mf.kanZwemmen)).map(Object::toString).orElse(""),
                        Optional.ofNullable(parsed.getMedischeFiche()).flatMap(mf -> Optional.ofNullable(mf.dieet)).map(Object::toString).orElse(""),
                        Optional.ofNullable(parsed.getMedischeFiche()).flatMap(mf -> Optional.ofNullable(mf.vlugMoe)).map(Object::toString).orElse(""),
                        Optional.ofNullable(parsed.getMedischeFiche()).flatMap(mf -> Optional.ofNullable(mf.vroegereZiekten)).map(Object::toString).orElse("")
                ));
            }
        }

        var range = Constants.SHEETS_TAB_TO_EXPORT + "!A1:Z" + values.size();
        var body = new ValueRange().setValues(values);
        sheets.spreadsheets().values().update(Constants.MEMBERS_SHEET_ID, range, body).setValueInputOption("RAW").execute();
    }

    static class Lid {
        private String naam;
        private Contact contact;
        private Medical medischeFiche;

        public Lid() {
        }

        public java.lang.String getNaam() {
            return naam;
        }

        public void setNaam(java.lang.String naam) {
            this.naam = naam;
        }

        public Contact getContact() {
            return contact;
        }

        public void setContact(Contact contact) {
            this.contact = contact;
        }

        public Medical getMedischeFiche() {
            return medischeFiche;
        }

        public void setMedischeFiche(Medical medischeFiche) {
            this.medischeFiche = medischeFiche;
        }
    }

    static class Doctor {
        private String gsm;
        private String naam;

        public Doctor() {
        }

        public String getGsm() {
            return gsm;
        }

        public void setGsm(String gsm) {
            this.gsm = gsm;
        }

        public String getNaam() {
            return naam;
        }

        public void setNaam(String naam) {
            this.naam = naam;
        }

        @Override
        public String toString() {
            return "gsm='" + gsm + '\'' + ", naam='" + naam + '\'';
        }
    }

    static class Medical {
        private Map<String, Boolean> aandoeningen;
        private Map<String, Object> covid;
        private List<Map<String, Object>> allergieen;
        private Map<String, Object> tetanus;
        private Boolean medischeHandelingen;
        private Boolean kanDeelnemen;
        private Boolean kanZwemmen;
        private Boolean dieet;
        private Boolean vlugMoe;
        private String vroegereZiekten;

        public Medical() {
        }

        public Map<String, Boolean> getAandoeningen() {
            return aandoeningen;
        }

        public void setAandoeningen(Map<String, Boolean> aandoeningen) {
            this.aandoeningen = aandoeningen;
        }

        public Map<String, Object> getCovid() {
            return covid;
        }

        public void setCovid(Map<String, Object> covid) {
            this.covid = covid;
        }

        public List<Map<String, Object>> getAllergieen() {
            return allergieen;
        }

        public void setAllergieen(List<Map<String, Object>> allergieen) {
            this.allergieen = allergieen;
        }

        public Map<String, Object> getTetanus() {
            return tetanus;
        }

        public void setTetanus(Map<String, Object> tetanus) {
            this.tetanus = tetanus;
        }

        public boolean isMedischeHandelingen() {
            return medischeHandelingen;
        }

        public void setMedischeHandelingen(boolean medischeHandelingen) {
            this.medischeHandelingen = medischeHandelingen;
        }

        public boolean isKanDeelnemen() {
            return kanDeelnemen;
        }

        public void setKanDeelnemen(boolean kanDeelnemen) {
            this.kanDeelnemen = kanDeelnemen;
        }

        public boolean isKanZwemmen() {
            return kanZwemmen;
        }

        public void setKanZwemmen(boolean kanZwemmen) {
            this.kanZwemmen = kanZwemmen;
        }

        public boolean isDieet() {
            return dieet;
        }

        public void setDieet(boolean dieet) {
            this.dieet = dieet;
        }

        public boolean isVlugMoe() {
            return vlugMoe;
        }

        public void setVlugMoe(boolean vlugMoe) {
            this.vlugMoe = vlugMoe;
        }

        public String getVroegereZiekten() {
            return vroegereZiekten;
        }

        public void setVroegereZiekten(String vroegereZiekten) {
            this.vroegereZiekten = vroegereZiekten;
        }

        @Override
        public String toString() {
            return "aandoeningen=" + aandoeningen + ", covid=" + covid + ", allergieen=" + allergieen + ", tetanus=" + tetanus + ", medischeHandelingen=" + medischeHandelingen + ", kanDeelnemen=" + kanDeelnemen + ", kanZwemmen=" + kanZwemmen + ", dieet=" + dieet + ", vlugMoe=" + vlugMoe + ", vroegereZiekten='" + vroegereZiekten + '\'';
        }
    }

    static class Contact {
        private Doctor huisarts;

        public Contact() {
        }

        public Doctor getHuisarts() {
            return huisarts;
        }

        public void setHuisarts(Doctor huisarts) {
            this.huisarts = huisarts;
        }
    }
}

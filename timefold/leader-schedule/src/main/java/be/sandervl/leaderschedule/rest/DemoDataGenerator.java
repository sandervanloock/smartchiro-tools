package be.sandervl.leaderschedule.rest;

import be.sandervl.leaderschedule.domain.Affinity;
import be.sandervl.leaderschedule.domain.Group;
import be.sandervl.leaderschedule.domain.Leader;
import be.sandervl.leaderschedule.domain.LeaderScheduleSolution;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class DemoDataGenerator {

    public LeaderScheduleSolution generateDemoData() {
        var plan = new LeaderScheduleSolution();
        List<Leader> leaders = new ArrayList<>();

        var Lars = new Leader("Lars", 0);
        Lars.setGroupAffinityMap(Map.of("Speelclub", Affinity.HIGH));
        leaders.add(Lars);

        var Zeger = new Leader("Zeger", 1);
        Zeger.setGroupAffinityMap(Map.of("Rakkers", Affinity.HIGH, "Speelclub", Affinity.MEDIUM, "Kerels", Affinity.LOW));
        leaders.add(Zeger);

        var SimonS = new Leader("Simon Souvereyns", 1);
        SimonS.setGroupAffinityMap(Map.of("Speelclub", Affinity.HIGH, "Rakkers", Affinity.MEDIUM));
        leaders.add(SimonS);

        var Tom = new Leader("Tom", 1);
        Tom.setGroupAffinityMap(Map.of("Speelclub", Affinity.HIGH, "Rakkers", Affinity.MEDIUM));
        leaders.add(Tom);

        var Emiel = new Leader("Emiel", 1);
        Emiel.setGroupAffinityMap(Map.of("Kerels", Affinity.HIGH, "Speelclub", Affinity.MEDIUM));
        leaders.add(Emiel);

        var JasperC = new Leader("Jasper Ceunen", 1);
        JasperC.setGroupAffinityMap(Map.of("Kerels", Affinity.HIGH, "Rakkers", Affinity.MEDIUM));
        leaders.add(JasperC);

        var JasperN = new Leader("Jasper Neyens", 1);
        JasperN.setGroupAffinityMap(Map.of("Kerels", Affinity.HIGH, "Speelclub", Affinity.MEDIUM, "Rakkers", Affinity.LOW));
        leaders.add(JasperN);

        var SimonVS = new Leader("Simon van Straaten", 1);
        SimonVS.setGroupAffinityMap(Map.of("Aspiranten", Affinity.HIGH));
        leaders.add(SimonVS);

        var Lowie = new Leader("Lowie", 1);
        Lowie.setGroupAffinityMap(Map.of("Speelclub", Affinity.HIGH, "Rakkers", Affinity.MEDIUM));
        leaders.add(Lowie);

        var Jef = new Leader("Jef", 1);
        Jef.setGroupAffinityMap(Map.of("Aspiranten", Affinity.HIGH));
        leaders.add(Jef);

        var Brent = new Leader("Brent", 0);
        Brent.setGroupAffinityMap(Map.of("Speelclub", Affinity.HIGH, "Rakkers", Affinity.MEDIUM, "Toppers", Affinity.LOW));
        leaders.add(Brent);

        var Mats = new Leader("Mats", 0);
        Mats.setGroupAffinityMap(Map.of("Speelclub", Affinity.HIGH, "Rakkers", Affinity.MEDIUM, "Toppers", Affinity.LOW));
        leaders.add(Mats);

        var SenneVG = new Leader("SenneVG", 0);
        SenneVG.setGroupAffinityMap(Map.of("Rakkers", Affinity.HIGH, "Toppers", Affinity.MEDIUM, "Speelclub", Affinity.LOW));
        leaders.add(SenneVG);

        var Stan = new Leader("Stan", 0);
        Stan.setGroupAffinityMap(Map.of("Rakkers", Affinity.HIGH, "Toppers", Affinity.MEDIUM, "Kerels", Affinity.LOW));
        leaders.add(Stan);

        var SenneV = new Leader("SenneV", 0);
        SenneV.setGroupAffinityMap(Map.of("Speelclub", Affinity.HIGH, "Rakkers", Affinity.MEDIUM, "Toppers", Affinity.LOW));
        leaders.add(SenneV);

        SimonVS.setPreferredLeaders(Set.of(Jef));
        SimonVS.setUnwantedLeaders(Set.of(SenneV, Stan));
        Jef.setPreferredLeaders(Set.of(SimonVS));
        Jef.setUnwantedLeaders(Set.of(Stan, JasperN));
        Tom.setPreferredLeaders(Set.of(SimonVS, JasperC));
        Tom.setUnwantedLeaders(Set.of(JasperN, SenneV));
        Zeger.setUnwantedLeaders(Set.of(Lars, SenneV));
        JasperC.setUnwantedLeaders(Set.of(Stan, SenneV));
        Emiel.setPreferredLeaders(Set.of(JasperC, Jef, Mats));
        Emiel.setUnwantedLeaders(Set.of(Stan, SimonVS, SenneV, Lars));
        JasperN.setPreferredLeaders(Set.of(JasperC, Emiel));
        Lowie.setPreferredLeaders(Set.of(JasperC, SimonS, Mats));
        Lowie.setUnwantedLeaders(Set.of(JasperN, Lars, SimonVS, SenneV));
        SimonS.setPreferredLeaders(Set.of(Tom, JasperC, Lowie));
        SimonS.setUnwantedLeaders(Set.of(Lars, SimonVS, SenneV));
        Stan.setPreferredLeaders(Set.of(SenneVG));
        Stan.setUnwantedLeaders(Set.of(Jef, SimonVS));


        var groups = List.of(
                new Group("Speelclub", 5, 5),
                new Group("Rakkers", 2, 3),
                new Group("Toppers", 2, 3),
                new Group("Kerels", 2, 3),
                new Group("Aspiranten", 2, 3)
        );
        // Update the plan
        plan.setLeaders(leaders);
        plan.setGroups(groups);
        return plan;
    }
}

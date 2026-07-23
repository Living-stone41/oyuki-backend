package com.oyuki.delivery.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum NigeriaState {

    ABIA("Abia", "AB"),
    ADAMAWA("Adamawa", "AD"),
    AKWA_IBOM("Akwa Ibom", "AK"),
    ANAMBRA("Anambra", "AN"),
    BAUCHI("Bauchi", "BA"),
    BAYELSA("Bayelsa", "BY"),
    BENUE("Benue", "BE"),
    BORNO("Borno", "BO"),
    CROSS_RIVER("Cross River", "CR"),
    DELTA("Delta", "DE"),
    EBONYI("Ebonyi", "EB"),
    EDO("Edo", "ED"),
    EKITI("Ekiti", "EK"),
    ENUGU("Enugu", "EN"),
    GOMBE("Gombe", "GO"),
    IMO("Imo", "IM"),
    JIGAWA("Jigawa", "JI"),
    KADUNA("Kaduna", "KD"),
    KANO("Kano", "KN"),
    KATSINA("Katsina", "KT"),
    KEBBI("Kebbi", "KE"),
    KOGI("Kogi", "KO"),
    KWARA("Kwara", "KW"),
    LAGOS("Lagos", "LA"),
    NASARAWA("Nasarawa", "NA"),
    NIGER("Niger", "NI"),
    OGUN("Ogun", "OG"),
    ONDO("Ondo", "ON"),
    OSUN("Osun", "OS"),
    OYO("Oyo", "OY"),
    PLATEAU("Plateau", "PL"),
    RIVERS("Rivers", "RI"),
    SOKOTO("Sokoto", "SO"),
    TARABA("Taraba", "TA"),
    YOBE("Yobe", "YO"),
    ZAMFARA("Zamfara", "ZA"),
    FEDERAL_CAPITAL_TERRITORY("Federal Capital Territory", "FC");

    private final String displayName;
    private final String code;

    NigeriaState(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    @JsonValue
    public String jsonValue() {
        return name();
    }

    @JsonCreator
    public static NigeriaState fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase();

        return Arrays.stream(values())
                .filter(state ->
                        state.name().equals(normalized)
                                || state.code.equalsIgnoreCase(value.trim())
                                || state.displayName.equalsIgnoreCase(value.trim())
                                || (
                                state == FEDERAL_CAPITAL_TERRITORY
                                        && value.trim().equalsIgnoreCase("FCT")
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown Nigerian state: " + value
                        )
                );
    }
}

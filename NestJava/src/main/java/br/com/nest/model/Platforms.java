package br.com.nest.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Plataformas de controle parental suportadas e seus sites oficiais. */
public final class Platforms {
    private Platforms() {}

    public static final String GOOGLE = "Google Family Link";
    public static final String APPLE = "Apple Screen Time";
    public static final String NINTENDO = "Nintendo Switch";
    public static final String OTHER = "Outra";

    /** Nome da plataforma -> página oficial onde o controle parental original é gerenciado. */
    public static final Map<String, String> OFFICIAL_URLS = new LinkedHashMap<>();
    static {
        OFFICIAL_URLS.put(GOOGLE, "https://families.google/familylink/");
        OFFICIAL_URLS.put(APPLE, "https://support.apple.com/pt-br/105121");
        OFFICIAL_URLS.put(NINTENDO, "https://www.nintendo.com/pt-br/Parental-Controls/Parental-Controls-1183457.html");
        OFFICIAL_URLS.put(OTHER, "");
    }

    public static String[] names() {
        return OFFICIAL_URLS.keySet().toArray(new String[0]);
    }
}

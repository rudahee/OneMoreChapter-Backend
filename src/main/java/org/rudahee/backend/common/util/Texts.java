package org.rudahee.backend.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilidades de texto compartidas.
 *
 * Existe porque la normalizacion de titulos hace falta en dos sitios que no
 * deben conocerse entre si: el merge de candidatos de {@code metadata} y la
 * busqueda dentro del catalogo local de {@code catalog}. Duplicarla llevaria a
 * que ambas se separasen con el tiempo y dejasen de casar los mismos titulos.
 */
public final class Texts {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");

    private Texts() {
        throw new AssertionError("Clase de utilidades, no instanciable");
    }

    /**
     * Reduce un titulo a una forma comparable: sin acentos, en minusculas y con
     * la puntuacion colapsada a espacios.
     *
     * Usa clases Unicode y no {@code [a-z0-9]} para no destruir titulos en
     * japones o cirilico: "進撃の巨人" debe sobrevivir intacto.
     */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String withoutDiacritics = DIACRITICS.matcher(
                Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
        return NON_ALPHANUMERIC.matcher(withoutDiacritics.toLowerCase(Locale.ROOT))
                .replaceAll(" ")
                .trim();
    }

    /** {@code null} si la cadena es nula o solo espacios; util al mapear DTOs. */
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

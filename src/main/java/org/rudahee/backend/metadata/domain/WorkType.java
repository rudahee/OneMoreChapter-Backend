package org.rudahee.backend.metadata.domain;

/** Tipos de obra soportados. {@code episodic} indica si se espera lista de unidades. */
public enum WorkType {

    MOVIE(false),
    SERIES(true),
    ANIME(true),
    MANGA(true),
    COMIC(true),
    DOCUMENTARY(false),
    BOOK(false),
    ALBUM(true),
    SONG(false),
    PODCAST(true),
    GAME(false),
    OTHER(false);

    private final boolean episodic;

    WorkType(boolean episodic) {
        this.episodic = episodic;
    }

    public boolean isEpisodic() {
        return episodic;
    }
}

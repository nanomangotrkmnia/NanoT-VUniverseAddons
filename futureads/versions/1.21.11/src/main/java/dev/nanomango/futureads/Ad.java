package dev.nanomango.futureads;

/** The servers advertised on the title and multiplayer screens. */
public enum Ad {
    FUTURE("future", "The Future", "future.playsmp.xyz"),
    VERITY("verity", "Truth & Verity", "");

    public final String key;
    public final String defaultName;
    public final String defaultAddress;

    Ad(String key, String defaultName, String defaultAddress) {
        this.key = key;
        this.defaultName = defaultName;
        this.defaultAddress = defaultAddress;
    }

    /** Headline lines. {@code multiplayer} picks the shorter wording used next to the server list. */
    public String[] headline(boolean multiplayer) {
        return switch (this) {
            case FUTURE -> new String[] {"Join The Future!"};
            case VERITY -> multiplayer ? new String[] {"Try at", "T&V"} : new String[] {"Try the", "Verity"};
        };
    }
}

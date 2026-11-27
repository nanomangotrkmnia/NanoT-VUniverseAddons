package dev.nanomango.ads;

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

    /** Headline lines; the same on the title and multiplayer screens. */
    public String[] headline(boolean multiplayer) {
        return switch (this) {
            case FUTURE -> new String[] {"Join The Future!"};
            case VERITY -> new String[] {"Join Truth", "& Verity"};
        };
    }
}

package me.endnether.localization;

public enum Chinese implements Language {
    LOCKER_TITLE("付费");


    private final String text;

    Chinese(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return this.text;
    }
}

package tn.esprit.agri.entities.enums;

public enum Language {
    FR("fr", false),
    EN("en", false),
    AR("ar", true);  // RTL = true

    private final String code;
    private final boolean rtl;

    Language(String code, boolean rtl) {
        this.code = code;
        this.rtl = rtl;
    }

    public String getCode() {
        return code;
    }

    public boolean isRtl() {
        return rtl;
    }
}
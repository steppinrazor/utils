package com.krs.utils.currency;

/**
 * Created by KR Shabazz on 2/17/15 11:41 PM
 */
public enum CurrencyLiterals implements Currency {
    FRANC("\u20A3", "Franc"),
    LIRA("\u20A4", "Lira"),
    MILL("\u20A5", "Mill"),
    RUPEE("\u20A8", "Indian Rupee"),
    EURO("\u20AC", "Euro"),
    DRACHMA("\u20AF", "Drachma"),
    GERMAN_PENNY("\u20B0", "German Penny"),
    DOLLAR("\u0024", "Dollar"),
    CENT("\u00A2", "Cent"),
    POUND("\u00A3", "Pound Sterling"),
    YEN("\u00A5", "Chinese Yen"),
    BAHT("\u0E3F", "Thai Baht"),
    DONG("\u20AB", "Dong");

    private final String symbol, label;

    CurrencyLiterals(String symbol, String label) {
        this.symbol = symbol;
        this.label = label;
    }

    @Override
    public String symbol() {
        return symbol;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label + "(" + symbol + ")";
    }
}

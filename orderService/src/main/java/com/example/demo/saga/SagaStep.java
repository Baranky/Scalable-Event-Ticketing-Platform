package com.example.demo.saga;


public enum SagaStep {
    
    LOCK_TICKETS("Biletleri kilitle", "Kilidi aç"),
    
    PROCESS_PAYMENT("Ödemeyi işle", "Ödeme iptali (henüz desteklenmiyor)"),
    
    CONFIRM_SALE("Satışı onayla", "Satış onayını geri al"),
    
    CREATE_TICKETS("Biletleri oluştur", "Biletleri sil"),
    
    COMPLETE_ORDER("Siparişi tamamla", "Siparişi iptal et");
    
    private final String description;
    private final String compensationDescription;
    
    SagaStep(String description, String compensationDescription) {
        this.description = description;
        this.compensationDescription = compensationDescription;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCompensationDescription() {
        return compensationDescription;
    }
}


package com.aerolink.paymentservice.dto;

public class CheckoutSessionResponse {

    private String paymentId;
    private String stripeSessionId;
    private String checkoutUrl;

    public CheckoutSessionResponse() {
    }

    public CheckoutSessionResponse(String paymentId, String stripeSessionId, String checkoutUrl) {
        this.paymentId = paymentId;
        this.stripeSessionId = stripeSessionId;
        this.checkoutUrl = checkoutUrl;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }
}
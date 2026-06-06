package com.aerolink.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"stripe.secret-key=sk_test_dummy",
	"stripe.webhook-secret=whsec_dummy",
	"stripe.success-url=http://localhost/success",
	"stripe.cancel-url=http://localhost/cancel",
	"services.booking.base-url=http://localhost:8081",
	"services.internal.booking-key=test-internal-key",
	"aws.eventbridge.bus-name=test-bus",
	"aws.eventbridge.region=us-east-1",
	"aws.dynamodb.payments-table=test-payments",
	"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer",
	"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks"
})
class PaymentServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}

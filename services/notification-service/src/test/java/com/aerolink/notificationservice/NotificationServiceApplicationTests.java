package com.aerolink.notificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"aws.region=us-east-1",
	"aws.dynamodb.notifications-table=test-notifications",
	"aws.dynamodb.notifications-user-index=test-notifications-user-index"
})
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}

from locust import HttpUser, task, between


class AeroLinkPublicUser(HttpUser):
    wait_time = between(1, 3)

    @task
    def browse_flights(self):
        self.client.get("/flights", name="GET /flights")
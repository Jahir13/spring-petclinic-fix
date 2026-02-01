import random
import string
import re
from locust import HttpUser, task, between, SequentialTaskSet
from locust import events

class PetClinicTaskSet(SequentialTaskSet):
    owner_id = None

    def generate_random_data(self):
        first_name = ''.join(random.choices(string.ascii_letters, k=8)).capitalize()
        last_name = ''.join(random.choices(string.ascii_letters, k=10)).capitalize()
        address = f"{random.randint(100, 9999)} {random.choice(['Main', 'Oak', 'Pine', 'Elm', 'Cedar'])} Street"
        city = random.choice(['Quito', 'Guayaquil', 'Cuenca', 'Manta', 'Ambato', 'Loja'])
        telephone = ''.join(random.choices(string.digits, k=10))

        return {
            'firstName': first_name,
            'lastName': last_name,
            'address': address,
            'city': city,
            'telephone': telephone
        }

    def generate_pet_data(self):
        pet_names = ['Max', 'Bella', 'Luna', 'Charlie', 'Lucy', 'Cooper', 'Daisy', 'Rocky']
        pet_types = ['cat', 'dog', 'lizard', 'snake', 'bird', 'hamster']

        year = random.randint(2015, 2025)
        month = random.randint(1, 12)
        day = random.randint(1, 28)

        return {
            'name': f"{random.choice(pet_names)}_{random.randint(1, 9999)}",
            'birthDate': f"{year}-{month:02d}-{day:02d}",
            'type': random.choice(pet_types)
        }

    @task
    def create_owner(self):
        with self.client.get("/owners/new", catch_response=True) as response:
            if response.status_code != 200:
                response.failure(f"GET /owners/new failed: {response.status_code}")
                return

        owner_data = self.generate_random_data()

        with self.client.post(
            "/owners/new",
            data=owner_data,
            allow_redirects=False,
            catch_response=True,
            name="POST /owners/new"
        ) as response:
            if response.status_code == 302:
                location = response.headers.get('Location', '')
                match = re.search(r'/owners/(\d+)', location)

                if match:
                    self.owner_id = match.group(1)
                    response.success()
                else:
                    response.failure(f"Could not extract owner ID from: {location}")
            else:
                response.failure(f"Expected 302, got {response.status_code}")

    @task
    def create_pet(self):
        if not self.owner_id:
            return

        with self.client.get(
            f"/owners/{self.owner_id}/pets/new",
            catch_response=True,
            name="GET /owners/{id}/pets/new"
        ) as response:
            if response.status_code != 200:
                response.failure(f"GET pets/new failed: {response.status_code}")
                return

        pet_data = self.generate_pet_data()

        with self.client.post(
            f"/owners/{self.owner_id}/pets/new",
            data=pet_data,
            allow_redirects=False,
            catch_response=True,
            name="POST /owners/{id}/pets/new"
        ) as response:
            if response.status_code == 302:
                response.success()
            elif response.status_code == 200:
                if "error" in response.text.lower():
                    response.failure("Validation error in pet creation")
                else:
                    response.success()
            else:
                response.failure(f"Expected 302 or 200, got {response.status_code}")

        self.owner_id = None


class PetClinicUser(HttpUser):
    wait_time = between(1, 3)
    tasks = [PetClinicTaskSet]

    def on_start(self):
        self.client.get("/")


metrics_data = {
    'total_requests': 0,
    'failed_requests': 0,
    'response_times': [],
}

@events.request.add_listener
def on_request(request_type, name, response_time, response_length, response, context, exception, **kwargs):
    metrics_data['total_requests'] += 1
    metrics_data['response_times'].append(response_time)

    if exception:
        metrics_data['failed_requests'] += 1


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    total = metrics_data['total_requests']
    failed = metrics_data['failed_requests']

    if total > 0:
        error_rate = (failed / total) * 100
        avg_response_time = sum(metrics_data['response_times']) / len(metrics_data['response_times'])

        print("\n" + "="*60)
        print("RESUMEN DE MÉTRICAS")
        print("="*60)
        print(f"Total Requests: {total}")
        print(f"Failed Requests: {failed}")
        print(f"Error Rate: {error_rate:.2f}%")
        print(f"Average Response Time: {avg_response_time:.2f}ms")

        if error_rate > 2:
            print(f"\n⚠️  ALERTA: La tasa de error ({error_rate:.2f}%) supera el 2%!")
        else:
            print("\n✅ La tasa de error está dentro del límite aceptable.")
        print("="*60)

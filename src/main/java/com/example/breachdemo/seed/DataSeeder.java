package com.example.breachdemo.seed;

import com.example.breachdemo.domain.Customer;
import com.example.breachdemo.repository.CustomerRepository;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Populates the in-memory database with fake customer records on startup.
 *
 * All data is randomly generated and fictitious. The ownerUsername is set
 * deterministically ("user1", "user2", ...) so that later, in the v1/v2 BOLA
 * experiment, we can log in as "user1" and prove we can still read user2's
 * record until object-level authorization is added.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final int BATCH_SIZE = 1000;

    private final CustomerRepository repository;

    @Value("${app.seed.count:10000}")
    private int seedCount;

    public DataSeeder(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        Faker faker = new Faker(new Locale("en", "AU"));
        List<Customer> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 1; i <= seedCount; i++) {
            LocalDate dob = faker.timeAndDate().birthday(18, 90);
            Customer c = new Customer(
                    faker.name().fullName(),
                    faker.internet().emailAddress(),
                    faker.phoneNumber().cellPhone(),
                    dob,
                    faker.address().fullAddress(),
                    faker.numerify("#########"),   // fake 9-digit government-ID-style number
                    "user" + i                     // deterministic owner for the BOLA demo
            );
            batch.add(c);

            if (batch.size() == BATCH_SIZE) {
                repository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            repository.saveAll(batch);
        }

        System.out.println("[seed] Inserted " + repository.count() + " fake customer records.");
    }
}

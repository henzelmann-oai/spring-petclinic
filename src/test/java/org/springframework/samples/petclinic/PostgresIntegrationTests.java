/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.DockerClientFactory;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.docker.compose.skip.in-tests=false", //
		"spring.docker.compose.start.arguments=--force-recreate,--renew-anon-volumes,postgres" })
@ActiveProfiles("postgres")
@DisabledInNativeImage
public class PostgresIntegrationTests {

	@LocalServerPort
	int port;

	@Autowired
	private VetRepository vets;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private RestTemplateBuilder builder;

	@BeforeAll
	static void available() {
		assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(PetClinicApplication.class) //
			.profiles("postgres") //
			.properties( //
					"spring.docker.compose.start.arguments=postgres" //
			) //
			.listeners(new PropertiesLogger()) //
			.run(args);
	}

	@Test
	void findAll() throws Exception {
		vets.findAll();
		vets.findAll(); // served from cache
	}

	@Test
	void ownerDetails() {
		RestTemplate template = builder.rootUri("http://localhost:" + port).build();
		ResponseEntity<String> result = template.exchange(RequestEntity.get("/owners/1").build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void postgresSchemaAndDataCanInitializeTwice() {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
				new ClassPathResource("db/postgres/schema.sql"), new ClassPathResource("db/postgres/data.sql"));

		populator.execute(dataSource);
		populator.execute(dataSource);

		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM owners", Integer.class)).isEqualTo(10);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pets", Integer.class)).isEqualTo(13);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM visits", Integer.class)).isEqualTo(4);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vets", Integer.class)).isEqualTo(6);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vet_specialties", Integer.class)).isEqualTo(5);
		assertThat(jdbc.queryForObject("""
				SELECT o.first_name || ' ' || o.last_name
				FROM owners o
				JOIN pets p ON p.owner_id = o.id
				WHERE p.id = 7
				""", String.class)).isEqualTo("Jean Coleman");
		assertThat(jdbc.queryForObject("""
				SELECT t.name
				FROM types t
				JOIN pets p ON p.type_id = t.id
				WHERE p.id = 8
				""", String.class)).isEqualTo("cat");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM visits WHERE pet_id = 8", Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForList("""
				SELECT s.name
				FROM specialties s
				JOIN vet_specialties vs ON vs.specialty_id = s.id
				WHERE vs.vet_id = 3
				ORDER BY s.name
				""", String.class)).containsExactly("dentistry", "surgery");
		assertThat(jdbc.queryForObject("SELECT nextval(pg_get_serial_sequence('owners', 'id'))", Integer.class))
			.isGreaterThan(10);
		assertThat(jdbc.queryForObject("SELECT nextval(pg_get_serial_sequence('visits', 'id'))", Integer.class))
			.isGreaterThan(4);
	}

	static class PropertiesLogger implements ApplicationListener<ApplicationPreparedEvent> {

		private static final Log log = LogFactory.getLog(PropertiesLogger.class);

		private ConfigurableEnvironment environment;

		private boolean isFirstRun = true;

		@Override
		public void onApplicationEvent(ApplicationPreparedEvent event) {
			if (isFirstRun) {
				environment = event.getApplicationContext().getEnvironment();
				printProperties();
			}
			isFirstRun = false;
		}

		public void printProperties() {
			for (EnumerablePropertySource<?> source : findPropertiesPropertySources()) {
				log.info("PropertySource: " + source.getName());
				String[] names = source.getPropertyNames();
				Arrays.sort(names);
				for (String name : names) {
					String resolved = environment.getProperty(name);

					assertNotNull(resolved, "resolved environment property: " + name + " is null.");

					Object sourceProperty = source.getProperty(name);

					assertNotNull(sourceProperty, "source property was expecting an object but is null.");

					assertNotNull(sourceProperty.toString(), "source property toString() returned null.");

					String value = sourceProperty.toString();
					if (resolved.equals(value)) {
						log.info(name + "=" + resolved);
					}
					else {
						log.info(name + "=" + value + " OVERRIDDEN to " + resolved);
					}
				}
			}
		}

		private List<EnumerablePropertySource<?>> findPropertiesPropertySources() {
			List<EnumerablePropertySource<?>> sources = new LinkedList<>();
			for (PropertySource<?> source : environment.getPropertySources()) {
				if (source instanceof EnumerablePropertySource enumerable) {
					sources.add(enumerable);
				}
			}
			return sources;
		}

	}

}

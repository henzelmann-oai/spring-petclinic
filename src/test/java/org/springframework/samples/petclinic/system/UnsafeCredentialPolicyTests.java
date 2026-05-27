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

package org.springframework.samples.petclinic.system;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class UnsafeCredentialPolicyTests {

	private static final List<Path> DEPLOYABLE_CONFIG_FILES = List.of(Path.of("docker-compose.yml"),
			Path.of("k8s/db.yml"), Path.of("k8s/petclinic.yml"),
			Path.of("src/main/resources/application-mysql.properties"),
			Path.of("src/main/resources/application-postgres.properties"));

	@Test
	void deployableConfigDoesNotContainUnsafePasswordDefaults() throws IOException {
		List<String> violations = new ArrayList<>();
		for (Path file : DEPLOYABLE_CONFIG_FILES) {
			List<String> lines = Files.readAllLines(file);
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i).strip();
				if (isPasswordConfiguration(line) && hasUnsafePasswordDefault(line)) {
					violations.add(file + ":" + (i + 1) + " contains an unsafe password default");
				}
			}
		}

		assertThat(violations).isEmpty();
	}

	private boolean isPasswordConfiguration(String line) {
		String lowerCaseLine = line.toLowerCase(Locale.ROOT);
		return !line.startsWith("#") && lowerCaseLine.contains("password");
	}

	private boolean hasUnsafePasswordDefault(String line) {
		String value = passwordValue(line);
		if (value.startsWith("${") && value.endsWith("}")) {
			value = placeholderDefault(value);
		}
		String normalized = unquote(value).strip().toLowerCase(Locale.ROOT);
		return normalized.isEmpty() || normalized.equals("pass") || normalized.equals("petclinic");
	}

	private String passwordValue(String line) {
		int equalsIndex = line.indexOf('=');
		int colonIndex = line.indexOf(':');
		if (equalsIndex >= 0 && (colonIndex < 0 || equalsIndex < colonIndex)) {
			return line.substring(equalsIndex + 1);
		}
		if (colonIndex >= 0) {
			return line.substring(colonIndex + 1);
		}
		return "";
	}

	private String placeholderDefault(String value) {
		String placeholder = value.substring(2, value.length() - 1);
		int defaultIndex = placeholder.indexOf(':');
		if (defaultIndex < 0 || placeholder.startsWith("?", defaultIndex + 1)) {
			return "explicit-placeholder";
		}
		return placeholder.substring(defaultIndex + 1);
	}

	private String unquote(String value) {
		String stripped = value.strip();
		if ((stripped.startsWith("\"") && stripped.endsWith("\""))
				|| (stripped.startsWith("'") && stripped.endsWith("'"))) {
			return stripped.substring(1, stripped.length() - 1);
		}
		return stripped;
	}

}

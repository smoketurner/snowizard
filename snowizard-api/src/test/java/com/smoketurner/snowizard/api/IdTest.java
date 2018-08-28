/*
 * Copyright © 2013, General Electric Corporation
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.smoketurner.snowizard.api;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.junit.Test;

public class IdTest {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
  private final Id id = new Id(1234L);

  @Test
  public void serializesToJSON() throws Exception {
    final String actual = MAPPER.writeValueAsString(id);
    final String expected =
        MAPPER.writeValueAsString(MAPPER.readValue(fixture("fixtures/id.json"), Id.class));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void deserializesFromJSON() throws Exception {
    final Id actual = MAPPER.readValue(fixture("fixtures/id.json"), Id.class);
    assertThat(actual).isEqualTo(id);
  }

  @Test
  public void testEquals() {
    final Id id2 = new Id(1234L);
    assertThat(id2).isEqualTo(id);
  }

  @Test
  public void testToString() {
    final String expected = "Id{id=1234, idStr=1234}";
    assertThat(id.toString()).isEqualTo(expected);
  }

  @Test
  public void testHashCode() {
    assertThat(id.hashCode()).isEqualTo(1265);
  }
}

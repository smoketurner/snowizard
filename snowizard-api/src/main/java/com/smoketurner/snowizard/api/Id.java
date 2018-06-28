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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.dropwizard.jackson.JsonSnakeCase;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import org.hibernate.validator.constraints.NotEmpty;

@Immutable
@JsonSnakeCase
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Id {

  private final long id;

  @NotEmpty private final String idStr;

  /**
   * Constructor
   *
   * @param id Generated ID
   * @param idStr Generated ID as a string
   */
  @JsonCreator
  public Id(@JsonProperty("id") final long id, @JsonProperty("id_str") final String idStr) {
    this.id = id;
    this.idStr = idStr;
  }

  /**
   * Constructor
   *
   * @param id Generated ID
   */
  public Id(final long id) {
    this.id = id;
    this.idStr = String.valueOf(id);
  }

  /**
   * Return the ID as a long value
   *
   * @return the ID as a long value
   */
  @JsonProperty
  public long getId() {
    return id;
  }

  /**
   * Return the ID as a string value
   *
   * @return the ID as a string value
   */
  @JsonProperty("id_str")
  public String getIdAsString() {
    return idStr;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }

    final Id other = (Id) obj;
    return Objects.equals(id, other.id) && Objects.equals(idStr, other.idStr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, idStr);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id).add("idStr", idStr).toString();
  }
}

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
package com.smoketurner.snowizard.application.exceptions;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Immutable
public final class SnowizardException extends WebApplicationException {

  private static final long serialVersionUID = 1L;
  private final Response.Status status;

  @Nullable private final String message;

  /**
   * Constructor
   *
   * @param code Status code to return
   * @param message Error message to return
   */
  public SnowizardException(final int code, @Nullable final String message) {
    super(code);
    this.status = Response.Status.fromStatusCode(code);
    this.message = message;
  }

  /**
   * Constructor
   *
   * @param status Status code to return
   * @param message Error message to return
   */
  public SnowizardException(final Response.Status status, @Nullable final String message) {
    super(status);
    this.status = status;
    this.message = message;
  }

  /**
   * Constructor
   *
   * @param status Status code to return
   * @param message Error message to return
   * @param cause Throwable which caused the exception
   */
  public SnowizardException(
      final Response.Status status, @Nullable final String message, final Throwable cause) {
    super(cause, status);
    this.status = status;
    this.message = message;
  }

  public int getCode() {
    return status.getStatusCode();
  }

  public Response.Status getStatus() {
    return status;
  }

  @Nullable
  @Override
  public String getMessage() {
    return message;
  }
}

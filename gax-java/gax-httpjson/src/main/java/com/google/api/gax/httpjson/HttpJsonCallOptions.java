/*
 * Copyright 2017 Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.google.api.gax.httpjson;

import com.google.api.core.BetaApi;
import com.google.auth.Credentials;
import com.google.auto.value.AutoValue;
import com.google.protobuf.TypeRegistry;
import java.time.Duration;
import javax.annotation.Nullable;
import org.threeten.bp.Instant;

/** Options for an http-json call, including deadline and credentials. */
@BetaApi
@AutoValue
public abstract class HttpJsonCallOptions {
  public static final HttpJsonCallOptions DEFAULT = newBuilder().build();

  @Nullable
  public abstract Duration getTimeout();

  @Nullable
  public abstract Instant getDeadline();

  @Nullable
  public abstract Credentials getCredentials();

  @Nullable
  public abstract TypeRegistry getTypeRegistry();

  public abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_HttpJsonCallOptions.Builder();
  }

  public HttpJsonCallOptions merge(HttpJsonCallOptions inputOptions) {
    if (inputOptions == null) {
      return this;
    }

    Builder builder = this.toBuilder();

    Instant newDeadline = inputOptions.getDeadline();
    if (newDeadline != null) {
      builder.setDeadline(newDeadline);
    }

    if (inputOptions.getTimeout() != null) {
      Duration newTimeout = java.time.Duration.ofMillis(inputOptions.getTimeout().toMillis());
      if (newTimeout != null) {
        builder.setTimeout(newTimeout);
      }
    }

    Credentials newCredentials = inputOptions.getCredentials();
    if (newCredentials != null) {
      builder.setCredentials(newCredentials);
    }

    TypeRegistry newTypeRegistry = inputOptions.getTypeRegistry();
    if (newTypeRegistry != null) {
      builder.setTypeRegistry(newTypeRegistry);
    }

    return builder.build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setTimeout(java.time.Duration value);

    public abstract Builder setDeadline(Instant value);

    public abstract Builder setCredentials(Credentials value);

    public abstract Builder setTypeRegistry(TypeRegistry value);

    public abstract HttpJsonCallOptions build();
  }
}

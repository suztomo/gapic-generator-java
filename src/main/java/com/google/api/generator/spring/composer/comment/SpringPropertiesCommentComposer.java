// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.generator.spring.composer.comment;

import com.google.api.generator.engine.ast.CommentStatement;
import com.google.api.generator.engine.ast.JavaDocComment;
import com.google.api.generator.gapic.composer.comment.CommentComposer;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SpringPropertiesCommentComposer {
  private static final String CLASS_HEADER_GENERAL_DESCRIPTION =
      "Provides default property values for %s client bean";
  private static final String CREDENTIALS_DESCRIPTION =
      "OAuth2 credentials to authenticate and authorize calls to Google Cloud Client Libraries.";
  private static final String QUOTA_PROJECT_ID_DESCRIPTION = "Quota project to use for billing.";
  private static final String EXECUTOR_THREAD_COUNT_DESCRIPTION =
      "Number of threads used for executors.";
  private static final String USE_REST_DESCRIPTION =
      "Allow override of default transport channel provider to use REST instead of gRPC.";

  // Retry Setting Property Descriptions (TODO: refactor?)
  // https://github.com/googleapis/gax-java/blob/main/gax/src/main/java/com/google/api/gax/retrying/RetrySettings.java
  private static final String TOTAL_TIMEOUT_DESCRIPTION =
      "TotalTimeout has ultimate control over how long the logic should keep trying the remote call "
          + "until it gives up completely. The higher the total timeout, the more retries can be attempted.";
  private static final String INITIAL_RETRY_DELAY_DESCRIPTION =
      "InitialRetryDelay controls the delay before the first retry. Subsequent retries will use this "
          + "value adjusted according to the RetryDelayMultiplier.";
  private static final String RETRY_DELAY_MULTIPLIER_DESCRIPTION =
      "RetryDelayMultiplier controls the change in retry delay. The retry delay of the previous call "
          + "is multiplied by the RetryDelayMultiplier to calculate the retry delay for the next call.";
  private static final String MAX_RETRY_DELAY_DESCRIPTION =
      "MaxRetryDelay puts a limit on the value of the retry delay, so that the RetryDelayMultiplier "
          + "can't increase the retry delay higher than this amount.";
  private static final String INITIAL_RPC_TIMEOUT_DESCRIPTION =
      "InitialRpcTimeout controls the timeout for the initial RPC. Subsequent calls will use this "
          + "value adjusted according to the RpcTimeoutMultiplier.";
  private static final String RPC_TIMEOUT_MULTIPLIER_DESCRIPTION =
      "RpcTimeoutMultiplier controls the change in RPC timeout. The timeout of the previous call "
          + "multiplied by the RpcTimeoutMultiplier to calculate the timeout for the next call.";
  private static final String MAX_RPC_TIMEOUT_DESCRIPTION =
      "MaxRpcTimeout puts a limit on the value of the RPC timeout, so that the RpcTimeoutMultiplier "
          + "can't increase the RPC timeout higher than this amount.";

  private static final Map<String, String> RETRY_PROPERTIES_DESCRIPTION_MAP =
      ImmutableMap.of(
          "TotalTimeout", TOTAL_TIMEOUT_DESCRIPTION,
          "InitialRetryDelay", INITIAL_RETRY_DELAY_DESCRIPTION,
          "RetryDelayMultiplier", RETRY_DELAY_MULTIPLIER_DESCRIPTION,
          "MaxRetryDelay", MAX_RETRY_DELAY_DESCRIPTION,
          "InitialRpcTimeout", INITIAL_RPC_TIMEOUT_DESCRIPTION,
          "RpcTimeoutMultiplier", RPC_TIMEOUT_MULTIPLIER_DESCRIPTION,
          "MaxRpcTimeout", MAX_RPC_TIMEOUT_DESCRIPTION);

  public static List<CommentStatement> createClassHeaderComments(
      String configuredClassName, String serviceName) {

    JavaDocComment.Builder javaDocCommentBuilder =
        JavaDocComment.builder()
            .addParagraph(String.format(CLASS_HEADER_GENERAL_DESCRIPTION, serviceName));
    return Arrays.asList(
        CommentComposer.AUTO_GENERATED_CLASS_COMMENT,
        CommentStatement.withComment(javaDocCommentBuilder.build()));
  }

  public static CommentStatement createCredentialsPropertyComment() {
    return toSimpleJavaDocComment(CREDENTIALS_DESCRIPTION);
  }

  public static CommentStatement createQuotaProjectIdPropertyComment() {
    return toSimpleJavaDocComment(QUOTA_PROJECT_ID_DESCRIPTION);
  }

  public static CommentStatement createExecutorThreadCountPropertyComment() {
    return toSimpleJavaDocComment(EXECUTOR_THREAD_COUNT_DESCRIPTION);
  }

  public static CommentStatement createUseRestPropertyComment() {
    return toSimpleJavaDocComment(USE_REST_DESCRIPTION);
  }

  public static CommentStatement createRetryPropertyComment(String propertyName) {
    String comment = RETRY_PROPERTIES_DESCRIPTION_MAP.getOrDefault(propertyName, "");
    return CommentStatement.withComment(JavaDocComment.withComment(comment));
  }

  private static CommentStatement toSimpleJavaDocComment(String comment) {
    return CommentStatement.withComment(JavaDocComment.withComment(comment));
  }
}

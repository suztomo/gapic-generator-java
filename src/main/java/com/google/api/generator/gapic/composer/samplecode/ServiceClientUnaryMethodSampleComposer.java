package com.google.api.generator.gapic.composer.samplecode;

import com.google.api.generator.engine.ast.AssignmentExpr;
import com.google.api.generator.engine.ast.CommentStatement;
import com.google.api.generator.engine.ast.Expr;
import com.google.api.generator.engine.ast.ExprStatement;
import com.google.api.generator.engine.ast.ForStatement;
import com.google.api.generator.engine.ast.LineComment;
import com.google.api.generator.engine.ast.MethodInvocationExpr;
import com.google.api.generator.engine.ast.Statement;
import com.google.api.generator.engine.ast.TryCatchStatement;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.Variable;
import com.google.api.generator.engine.ast.VariableExpr;
import com.google.api.generator.gapic.composer.defaultvalue.DefaultValueComposer;
import com.google.api.generator.gapic.model.Field;
import com.google.api.generator.gapic.model.Message;
import com.google.api.generator.gapic.model.Method;
import com.google.api.generator.gapic.model.RegionTag;
import com.google.api.generator.gapic.model.ResourceName;
import com.google.api.generator.gapic.model.Sample;
import com.google.api.generator.gapic.utils.JavaStyle;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceClientUnaryMethodSampleComposer {
  public static Sample composeDefaultSample(
      Method method,
      TypeNode clientType,
      Map<String, ResourceName> resourceNames,
      Map<String, Message> messageTypes) {
    VariableExpr clientVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setName(JavaStyle.toLowerCamelCase(clientType.reference().name()))
                .setType(clientType)
                .build());

    // Create request variable expression and assign with its default value.
    VariableExpr requestVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("request").setType(method.inputType()).build());
    List<VariableExpr> rpcMethodArgVarExprs = Arrays.asList(requestVarExpr);
    Message requestMessage = messageTypes.get(method.inputType().reference().fullName());
    Preconditions.checkNotNull(
        requestMessage,
        String.format(
            "Could not find the message type %s.", method.inputType().reference().fullName()));
    Expr requestBuilderExpr =
        DefaultValueComposer.createSimpleMessageBuilderValue(
            requestMessage, resourceNames, messageTypes);
    AssignmentExpr requestAssignmentExpr =
        AssignmentExpr.builder()
            .setVariableExpr(requestVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(requestBuilderExpr)
            .build();

    List<Expr> bodyExprs = new ArrayList<>();
    bodyExprs.add(requestAssignmentExpr);

    List<Statement> bodyStatements = new ArrayList<>();
    RegionTag regionTag;
    if (method.isPaged()) {
      // e.g. echoClient.pagedExpand(request).iterateAll()
      Sample unaryPagedRpc =
          composePagedSample(method, clientVarExpr, rpcMethodArgVarExprs, bodyExprs, messageTypes);
      bodyStatements.addAll(unaryPagedRpc.body());
      regionTag = unaryPagedRpc.regionTag();
    } else if (method.hasLro()) {
      Sample unaryLroRpc = composeLroSample(method, clientVarExpr, rpcMethodArgVarExprs, bodyExprs);
      bodyStatements.addAll(unaryLroRpc.body());
      regionTag = unaryLroRpc.regionTag();
    } else {
      // e.g. echoClient.echo(request)
      Sample unaryRpc = composeSample(method, clientVarExpr, rpcMethodArgVarExprs, bodyExprs);
      bodyStatements.addAll(unaryRpc.body());
      regionTag = unaryRpc.regionTag();
    }

    List<Statement> body =
        Arrays.asList(
            TryCatchStatement.builder()
                .setTryResourceExpr(
                    SampleComposerUtil.assignClientVariableWithCreateMethodExpr(clientVarExpr))
                .setTryBody(bodyStatements)
                .setIsSampleCode(true)
                .build());
    return Sample.builder().setBody(body).setRegionTag(regionTag).build();
  }

  static Sample composeSample(
      Method method,
      VariableExpr clientVarExpr,
      List<VariableExpr> rpcMethodArgVarExprs,
      List<Expr> bodyExprs) {

    // Invoke current method based on return type.
    // e.g. if return void, echoClient.echo(..); or,
    // e.g. if return other type, EchoResponse response = echoClient.echo(...);
    boolean returnsVoid = SampleComposerUtil.isProtoEmptyType(method.outputType());
    MethodInvocationExpr clientRpcMethodInvocationExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(clientVarExpr)
            .setMethodName(JavaStyle.toLowerCamelCase(method.name()))
            .setArguments(
                rpcMethodArgVarExprs.stream().map(e -> (Expr) e).collect(Collectors.toList()))
            .setReturnType(method.outputType())
            .build();
    String disambiguation =
        rpcMethodArgVarExprs.stream()
            .map(
                e ->
                    e.variable().type().reference() == null
                        ? JavaStyle.toUpperCamelCase(
                            e.variable().type().typeKind().name().toLowerCase())
                        : JavaStyle.toUpperCamelCase(e.variable().type().reference().name()))
            .collect(Collectors.joining());

    if (returnsVoid) {
      bodyExprs.add(clientRpcMethodInvocationExpr);
    } else {
      VariableExpr responseVarExpr =
          VariableExpr.withVariable(
              Variable.builder().setName("response").setType(method.outputType()).build());
      bodyExprs.add(
          AssignmentExpr.builder()
              .setVariableExpr(responseVarExpr.toBuilder().setIsDecl(true).build())
              .setValueExpr(clientRpcMethodInvocationExpr)
              .build());
    }

    // e.g. serviceName = echoClient
    //      rpcName =  echo
    //      disambiguation = echoRequest
    RegionTag regionTag =
        RegionTag.builder()
            .setServiceName(clientVarExpr.variable().identifier().name())
            .setRpcName(method.name())
            .setOverloadDisambiguation(disambiguation)
            .build();
    return Sample.builder()
        .setBody(
            bodyExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()))
        .setRegionTag(regionTag)
        .build();
  }

  static Sample composePagedSample(
      Method method,
      VariableExpr clientVarExpr,
      List<VariableExpr> rpcMethodArgVarExprs,
      List<Expr> bodyExprs,
      Map<String, Message> messageTypes) {

    // Find the repeated field.
    Message methodOutputMessage = messageTypes.get(method.outputType().reference().fullName());
    Preconditions.checkNotNull(
        methodOutputMessage,
        "Output message %s not found, keys: ",
        method.outputType().reference().fullName(),
        messageTypes.keySet().toString());
    Field repeatedPagedResultsField = methodOutputMessage.findAndUnwrapPaginatedRepeatedField();
    Preconditions.checkNotNull(
        repeatedPagedResultsField,
        String.format(
            "No repeated field found on message %s for method %s",
            methodOutputMessage.name(), method.name()));
    TypeNode repeatedResponseType = repeatedPagedResultsField.type();

    // For loop paged response item on iterateAll method.
    // e.g. for (LogEntry element : loggingServiceV2Client.ListLogs(parent).iterateAll()) {
    //          //doThingsWith(element);
    //      }
    MethodInvocationExpr clientMethodIterateAllExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(clientVarExpr)
            .setMethodName(JavaStyle.toLowerCamelCase(method.name()))
            .setArguments(
                rpcMethodArgVarExprs.stream().map(e -> (Expr) e).collect(Collectors.toList()))
            .build();
    String disambiguation =
        rpcMethodArgVarExprs.stream()
            .map(
                arg ->
                    arg.variable().type().reference() == null
                        ? JavaStyle.toUpperCamelCase(
                            arg.variable().type().typeKind().name().toLowerCase())
                        : JavaStyle.toUpperCamelCase(arg.variable().type().reference().name()))
            .collect(Collectors.joining());

    clientMethodIterateAllExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(clientMethodIterateAllExpr)
            .setMethodName("iterateAll")
            .setReturnType(repeatedResponseType)
            .build();
    disambiguation =
        disambiguation.concat(
            JavaStyle.toUpperCamelCase(clientMethodIterateAllExpr.methodIdentifier().name()));
    ForStatement loopIteratorStatement =
        ForStatement.builder()
            .setLocalVariableExpr(
                VariableExpr.builder()
                    .setVariable(
                        Variable.builder().setName("element").setType(repeatedResponseType).build())
                    .setIsDecl(true)
                    .build())
            .setCollectionExpr(clientMethodIterateAllExpr)
            .setBody(
                Arrays.asList(
                    CommentStatement.withComment(
                        LineComment.withComment("doThingsWith(element);"))))
            .build();

    List<Statement> bodyStatements =
        bodyExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList());
    bodyExprs.clear();
    bodyStatements.add(loopIteratorStatement);

    // e.g. serviceName = echoClient
    //      rpcName =  listContent
    //      disambiguation = iterateAll
    RegionTag regionTag =
        RegionTag.builder()
            .setServiceName(clientVarExpr.variable().identifier().name())
            .setRpcName(method.name())
            .setOverloadDisambiguation(disambiguation)
            .build();
    return Sample.builder().setBody(bodyStatements).setRegionTag(regionTag).build();
  }

  static Sample composeLroSample(
      Method method,
      VariableExpr clientVarExpr,
      List<VariableExpr> rpcMethodArgVarExprs,
      List<Expr> bodyExprs) {
    // Assign response variable with invoking client's LRO method.
    // e.g. if return void, echoClient.waitAsync(ttl).get(); or,
    // e.g. if return other type, WaitResponse response = echoClient.waitAsync(ttl).get();
    MethodInvocationExpr invokeLroGetMethodExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(clientVarExpr)
            .setMethodName(String.format("%sAsync", JavaStyle.toLowerCamelCase(method.name())))
            .setArguments(
                rpcMethodArgVarExprs.stream().map(e -> (Expr) e).collect(Collectors.toList()))
            .build();
    String disambiguation =
        "Async"
            + rpcMethodArgVarExprs.stream()
                .map(
                    e ->
                        e.variable().type().reference() == null
                            ? JavaStyle.toUpperCamelCase(
                                e.variable().type().typeKind().name().toLowerCase())
                            : JavaStyle.toUpperCamelCase(e.variable().type().reference().name()))
                .collect(Collectors.joining());
    invokeLroGetMethodExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(invokeLroGetMethodExpr)
            .setMethodName("get")
            .setReturnType(method.lro().responseType())
            .build();
    disambiguation =
        disambiguation.concat(
            JavaStyle.toUpperCamelCase(invokeLroGetMethodExpr.methodIdentifier().name()));
    boolean returnsVoid = SampleComposerUtil.isProtoEmptyType(method.lro().responseType());
    if (returnsVoid) {
      bodyExprs.add(invokeLroGetMethodExpr);
    } else {
      VariableExpr responseVarExpr =
          VariableExpr.builder()
              .setVariable(
                  Variable.builder()
                      .setName("response")
                      .setType(method.lro().responseType())
                      .build())
              .setIsDecl(true)
              .build();
      bodyExprs.add(
          AssignmentExpr.builder()
              .setVariableExpr(responseVarExpr)
              .setValueExpr(invokeLroGetMethodExpr)
              .build());
    }

    // e.g. serviceName = echoClient
    //      rpcName =  wait
    //      disambiguation = durationGet
    RegionTag regionTag =
        RegionTag.builder()
            .setServiceName(clientVarExpr.variable().identifier().name())
            .setRpcName(method.name())
            .setOverloadDisambiguation(disambiguation)
            .build();
    return Sample.builder()
        .setBody(
            bodyExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()))
        .setRegionTag(regionTag)
        .build();
  }
}

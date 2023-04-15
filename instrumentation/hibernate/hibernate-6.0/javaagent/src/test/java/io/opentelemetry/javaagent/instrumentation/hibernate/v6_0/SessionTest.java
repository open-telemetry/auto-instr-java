/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.hibernate.v6_0;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Named.named;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.assertj.SpanDataAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.query.SelectionQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("deprecation") // testing instrumentation of deprecated class
public class SessionTest extends AbstractHibernateTest {
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("provideHibernateActionParameters")
  void testHibernateAction(Parameter parameter) {
    testing.runWithSpan(
        "parent",
        () -> {
          Session session = sessionFactory.openSession();
          session.beginTransaction();
          try {
            parameter.sessionMethodTest.accept(session, prepopulated.get(0));
          } catch (RuntimeException e) {
            // We expected this, we should see the error field set on the span.
          }
          session.getTransaction().commit();
          session.close();
        });
    assertTraces(parameter);
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("provideHibernateActionWithStatelessSessionParameters")
  void testHibernateActionStatelessSession(Parameter parameter) {
    testing.runWithSpan(
        "parent",
        () -> {
          StatelessSession session = sessionFactory.openStatelessSession();
          session.beginTransaction();
          try {
            parameter.statelessSessionMethodTest.accept(session, prepopulated.get(0));

          } catch (RuntimeException e) {
            // We expected this, we should see the error field set on the span.
          }
          session.getTransaction().commit();
          session.close();
        });
    assertTraces(parameter);
  }

  private static void assertTraces(Parameter parameter) {
    AtomicReference<String> sessionId = new AtomicReference<>();
    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(4)
                .hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        assertSessionSpan(
                            span,
                            trace.getSpan(0),
                            sessionId,
                            "Session." + parameter.methodName + " " + parameter.resource),
                    span -> assertClientSpan(span, trace.getSpan(1)),
                    span ->
                        assertSpanWithSessionId(
                            span, trace.getSpan(0), "Transaction.commit", sessionId)));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("provideHibernateReplicateParameters")
  void testHibernateReplicate(Parameter parameter) {
    AtomicReference<String> sessionId = new AtomicReference<>();
    testing.runWithSpan(
        "parent",
        () -> {
          Session session = sessionFactory.openSession();
          session.beginTransaction();
          try {
            parameter.sessionMethodTest.accept(session, prepopulated.get(0));
          } catch (RuntimeException e) {
            // We expected this, we should see the error field set on the span.
          }
          session.getTransaction().commit();
          session.close();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(5)
                .hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        assertSessionSpan(
                            span,
                            trace.getSpan(0),
                            sessionId,
                            "Session." + parameter.methodName + " " + parameter.resource),
                    span -> assertClientSpan(span, trace.getSpan(1), "SELECT"),
                    span ->
                        assertSpanWithSessionId(
                            span, trace.getSpan(0), "Transaction.commit", sessionId),
                    span -> assertClientSpan(span, trace.getSpan(3))));
  }

  @Test
  void testHibernateFailedReplicate() {
    AtomicReference<String> sessionId = new AtomicReference<>();
    testing.runWithSpan(
        "parent",
        () -> {
          Session session = sessionFactory.openSession();
          session.beginTransaction();
          try {
            session.replicate(
                Long.valueOf(123) /* Not a valid entity */, ReplicationMode.OVERWRITE);
          } catch (RuntimeException e) {
            // We expected this, we should see the error field set on the span.
          }
          session.getTransaction().commit();
          session.close();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(3)
                .hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        span.hasName("Session.replicate java.lang.Long")
                            .hasKind(SpanKind.INTERNAL)
                            .hasParent(trace.getSpan(0))
                            .hasStatus(StatusData.error())
                            .hasException(
                                new UnknownEntityTypeException(
                                    "Unable to locate persister: java.lang.Long"))
                            .hasAttributesSatisfying(
                                attributes -> {
                                  Assertions.assertThat(
                                          attributes.get(
                                              AttributeKey.stringKey("hibernate.session_id")))
                                      .isInstanceOf(String.class);
                                  sessionId.set(
                                      attributes.get(
                                          AttributeKey.stringKey("hibernate.session_id")));
                                }),
                    span ->
                        assertSpanWithSessionId(
                            span, trace.getSpan(0), "Transaction.commit", sessionId)));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("provideHibernateCommitActionParameters")
  void testHibernateCommitAction(Parameter parameter) {
    AtomicReference<String> sessionId = new AtomicReference<>();
    testing.runWithSpan(
        "parent",
        () -> {
          Session session = sessionFactory.openSession();
          session.beginTransaction();
          try {
            parameter.sessionMethodTest.accept(session, prepopulated.get(0));
          } catch (RuntimeException e) {
            // We expected this, we should see the error field set on the span.
          }
          session.getTransaction().commit();
          session.close();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(4)
                .hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        assertSessionSpan(
                            span,
                            trace.getSpan(0),
                            sessionId,
                            "Session." + parameter.methodName + " " + parameter.resource),
                    span ->
                        assertSpanWithSessionId(
                            span, trace.getSpan(0), "Transaction.commit", sessionId),
                    span -> assertClientSpan(span, trace.getSpan(2))));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("provideAttachesStateToQueryParameters")
  void testAttachesStateToQuery(Parameter parameter) {
    AtomicReference<String> sessionId = new AtomicReference<>();
    testing.runWithSpan(
        "parent",
        () -> {
          Session session = sessionFactory.openSession();
          session.beginTransaction();
          SelectionQuery<?> query = parameter.queryBuildMethod.apply(session);
          query.list();
          session.getTransaction().commit();
          session.close();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(4)
                .hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        assertSessionSpan(span, trace.getSpan(0), sessionId, parameter.resource),
                    span ->
                        span.hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(1))
                            .hasAttributesSatisfyingExactly(
                                equalTo(SemanticAttributes.DB_SYSTEM, "h2"),
                                equalTo(SemanticAttributes.DB_NAME, "db1"),
                                equalTo(SemanticAttributes.DB_USER, "sa"),
                                equalTo(SemanticAttributes.DB_CONNECTION_STRING, "h2:mem:"),
                                satisfies(
                                    SemanticAttributes.DB_STATEMENT,
                                    val -> val.isInstanceOf(String.class)),
                                equalTo(SemanticAttributes.DB_OPERATION, "SELECT"),
                                equalTo(SemanticAttributes.DB_SQL_TABLE, "Value")),
                    span ->
                        assertSpanWithSessionId(
                            span, trace.getSpan(0), "Transaction.commit", sessionId)));
  }

  @Test
  void testHibernateOverlappingSessions() {
    AtomicReference<String> sessionId1 = new AtomicReference<>();
    AtomicReference<String> sessionId2 = new AtomicReference<>();
    AtomicReference<String> sessionId3 = new AtomicReference<>();

    testing.runWithSpan(
        "overlapping Sessions",
        () -> {
          Session session1 = sessionFactory.openSession();
          session1.beginTransaction();
          StatelessSession session2 = sessionFactory.openStatelessSession();
          Session session3 = sessionFactory.openSession();

          Value value1 = new Value("Value 1");
          session1.save(value1);
          session2.insert(new Value("Value 2"));
          session3.save(new Value("Value 3"));
          session1.delete(value1);

          session2.close();
          session1.getTransaction().commit();
          session1.close();
          session3.close();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(9)
                .hasSpansSatisfyingExactly(
                    span -> span.hasName("overlapping Sessions"),
                    span ->
                        assertSessionSpan(
                            span,
                            trace.getSpan(0),
                            sessionId1,
                            "Session.save io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value"),
                    span ->
                        assertSessionSpan(
                            span,
                            trace.getSpan(0),
                            sessionId2,
                            "Session.insert io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value"),
                    span -> assertClientSpan(span, trace.getSpan(2), "INSERT"),
                    span ->
                        assertSessionSpan(
                            span,
                            trace.getSpan(0),
                            sessionId3,
                            "Session.save io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value"),
                    span ->
                        assertSpanWithSessionId(
                            span,
                            trace.getSpan(0),
                            "Session.delete io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                            sessionId1),
                    span ->
                        assertSpanWithSessionId(
                            span, trace.getSpan(0), "Transaction.commit", sessionId1),
                    span -> assertClientSpan(span, trace.getSpan(6), "INSERT"),
                    span -> assertClientSpan(span, trace.getSpan(6), "DELETE")));

    assertNotEquals(sessionId1.get(), sessionId2.get());
    assertNotEquals(sessionId2.get(), sessionId3.get());
    assertNotEquals(sessionId1.get(), sessionId3.get());
  }

  private static Stream<Arguments> provideHibernateActionParameters() {
    List<BiConsumer<Session, Value>> sessionMethodTests =
        Arrays.asList(
            (session, val) -> session.lock(val, LockMode.READ),
            (session, val) -> session.lock("Value", val, LockMode.READ),
            (session, val) -> session.lock(null, val, LockMode.READ),
            (session, val) -> session.buildLockRequest(LockOptions.READ).lock(val),
            (session, val) -> session.refresh(val),
            (session, val) -> session.refresh("Value", val),
            (session, val) ->
                session.get(
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value", val.getId()),
            (session, val) -> session.get(Value.class, val.getId()));

    return Stream.of(
        Arguments.of(
            named(
                "lock",
                new Parameter(
                    "lock",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(0),
                    null))),
        Arguments.of(
            named(
                "lock with entity name",
                new Parameter("lock", "Value", null, sessionMethodTests.get(1), null))),
        Arguments.of(
            named(
                "lock with null name",
                new Parameter(
                    "lock",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(2),
                    null))),
        Arguments.of(
            named(
                "buildLockRequest",
                new Parameter(
                    "lock",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(3),
                    null))),
        Arguments.of(
            named(
                "refresh",
                new Parameter(
                    "refresh",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(4),
                    null))),
        Arguments.of(
            named(
                "refresh with entity name",
                new Parameter("refresh", "Value", null, sessionMethodTests.get(5), null))),
        Arguments.of(
            named(
                "get with entity name",
                new Parameter(
                    "get",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(6),
                    null))),
        Arguments.of(
            named(
                "get with entity class",
                new Parameter(
                    "get",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(7),
                    null))));
  }

  private static Stream<Arguments> provideHibernateActionWithStatelessSessionParameters() {

    List<BiConsumer<StatelessSession, Value>> statelessSessionMethodTests =
        Arrays.asList(
            (statelessSession, val) -> statelessSession.refresh(val),
            (statelessSession, val) ->
                statelessSession.refresh(
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value", val),
            (statelessSession, val) ->
                statelessSession.get(
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value", val.getId()),
            (statelessSession, val) -> statelessSession.get(Value.class, val.getId()),
            (statelessSession, val) -> statelessSession.insert(new Value("insert me")),
            (statelessSession, val) ->
                statelessSession.insert(
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    new Value("insert me")),
            (statelessSession, val) -> statelessSession.insert(null, new Value("insert me")),
            (statelessSession, val) -> {
              val.setName("New name");
              statelessSession.update(val);
            },
            (statelessSession, val) -> {
              val.setName("New name");
              statelessSession.update(
                  "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value", val);
            },
            (statelessSession, val) -> {
              statelessSession.delete(val);
              prepopulated.remove(val);
            },
            (statelessSession, val) -> {
              statelessSession.delete(
                  "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value", val);
              prepopulated.remove(val);
            });

    return Stream.of(
        Arguments.of(
            named(
                "refresh",
                new Parameter(
                    "refresh",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    null,
                    statelessSessionMethodTests.get(0)))),
        Arguments.of(
            named(
                "refresh with entity name",
                new Parameter(
                    "refresh",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    null,
                    statelessSessionMethodTests.get(1)))),
        Arguments.of(
            named(
                "get with entity name",
                new Parameter(
                    "get",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    null,
                    statelessSessionMethodTests.get(2)))),
        Arguments.of(
            named(
                "get with entity class",
                new Parameter(
                    "get",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    null,
                    statelessSessionMethodTests.get(3)))),
        Arguments.of(
            named(
                "insert",
                new Parameter(
                    "insert",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    null,
                    statelessSessionMethodTests.get(4)))),
        Arguments.of(
            named(
                "insert with entity name",
                new Parameter(
                    "insert",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    null,
                    statelessSessionMethodTests.get(5)))),
        Arguments.of(
            named(
                "insert with null entity name",
                new Parameter(
                    "insert",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    null,
                    statelessSessionMethodTests.get(6)))),
        Arguments.of(
            named(
                "update (StatelessSession)",
                new Parameter(
                    "update",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    null,
                    statelessSessionMethodTests.get(7)))),
        Arguments.of(
            named(
                "update with entity name (StatelessSession)",
                new Parameter(
                    "update",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    null,
                    statelessSessionMethodTests.get(8)))),
        Arguments.of(
            named(
                "delete (Session)",
                new Parameter(
                    "delete",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    null,
                    statelessSessionMethodTests.get(9)))),
        Arguments.of(
            named(
                "delete with entity name (Session)",
                new Parameter(
                    "delete",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    null,
                    statelessSessionMethodTests.get(10)))));
  }

  private static Stream<Arguments> provideHibernateReplicateParameters() {
    List<BiConsumer<Session, Value>> sessionMethodTests =
        Arrays.asList(
            (session, val) -> {
              Value replicated = new Value(val.getName() + " replicated");
              replicated.setId(val.getId());
              session.replicate(replicated, ReplicationMode.OVERWRITE);
            },
            (session, val) -> {
              Value replicated = new Value(val.getName() + " replicated");
              replicated.setId(val.getId());
              session.replicate("Value", replicated, ReplicationMode.OVERWRITE);
            });

    return Stream.of(
        Arguments.of(
            named(
                "replicate",
                new Parameter(
                    "replicate",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(0),
                    null))),
        Arguments.of(
            named(
                "replicate by entityName",
                new Parameter("replicate", "Value", null, sessionMethodTests.get(1), null))));
  }

  private static Stream<Arguments> provideHibernateCommitActionParameters() {
    List<BiConsumer<Session, Value>> sessionMethodTests =
        Arrays.asList(
            (session, val) -> session.save(new Value("Another value")),
            (session, val) -> session.save("Value", new Value("Another value")),
            (session, val) -> session.saveOrUpdate(new Value("Value")),
            (session, val) -> session.saveOrUpdate("Value", new Value("Value")),
            (session, val) -> {
              val.setName("New name");
              session.saveOrUpdate("Value", val);
            },
            (session, val) -> session.merge(new Value("merge me in")),
            (session, val) -> session.merge("Value", new Value("merge me in")),
            (session, val) -> session.persist(new Value("merge me in")),
            (session, val) -> session.persist("Value", new Value("merge me in")),
            (session, val) -> session.persist(null, new Value("merge me in")),
            (session, val) -> {
              val.setName("New name");
              session.update(val);
            },
            (session, val) -> {
              val.setName("New name");
              session.update("Value", val);
            },
            (session, val) -> {
              session.delete(val);
              prepopulated.remove(val);
            },
            (session, val) -> {
              session.delete("Value", val);
              prepopulated.remove(val);
            });

    return Stream.of(
        Arguments.of(
            named(
                "save",
                new Parameter(
                    "save",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(0),
                    null))),
        Arguments.of(
            named(
                "save with entity name",
                new Parameter("save", "Value", null, sessionMethodTests.get(1), null))),
        Arguments.of(
            named(
                "saveOrUpdate save",
                new Parameter(
                    "saveOrUpdate",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(2),
                    null))),
        Arguments.of(
            named(
                "saveOrUpdate save with entity name",
                new Parameter("saveOrUpdate", "Value", null, sessionMethodTests.get(3), null))),
        Arguments.of(
            named(
                "saveOrUpdate update with entity name",
                new Parameter("saveOrUpdate", "Value", null, sessionMethodTests.get(4), null))),
        Arguments.of(
            named(
                "merge",
                new Parameter(
                    "merge",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(5),
                    null))),
        Arguments.of(
            named(
                "merge with entity name",
                new Parameter("merge", "Value", null, sessionMethodTests.get(6), null))),
        Arguments.of(
            named(
                "persist",
                new Parameter(
                    "persist",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(7),
                    null))),
        Arguments.of(
            named(
                "persist with entity name",
                new Parameter("persist", "Value", null, sessionMethodTests.get(8), null))),
        Arguments.of(
            named(
                "persist with null entity name",
                new Parameter(
                    "persist",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(9),
                    null))),
        Arguments.of(
            named(
                "update (Session)",
                new Parameter(
                    "update",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(10),
                    null))),
        Arguments.of(
            named(
                "update by entityName (Session)",
                new Parameter("update", "Value", null, sessionMethodTests.get(11), null))),
        Arguments.of(
            named(
                "delete (Session)",
                new Parameter(
                    "delete",
                    "io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    null,
                    sessionMethodTests.get(12),
                    null))),
        Arguments.of(
            named(
                "delete by entityName (Session)",
                new Parameter("delete", "Value", null, sessionMethodTests.get(13), null))));
  }

  private static Stream<Arguments> provideAttachesStateToQueryParameters() {
    List<Function<Session, SelectionQuery<?>>> queryBuildMethods =
        Arrays.asList(
            session -> session.createQuery("from Value"),
            session -> session.getNamedQuery("TestNamedQuery"),
            session -> session.createNativeQuery("SELECT * FROM Value"),
            session -> session.createSelectionQuery("from Value"));

    return Stream.of(
        Arguments.of(
            named(
                "createQuery",
                new Parameter(
                    "createQuery",
                    "SELECT io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    queryBuildMethods.get(0),
                    null,
                    null))),
        Arguments.of(
            named(
                "getNamedQuery",
                new Parameter(
                    "getNamedQuery",
                    "SELECT io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    queryBuildMethods.get(1),
                    null,
                    null))),
        Arguments.of(
            named(
                "createNativeQuery",
                new Parameter(
                    "createNativeQuery", "SELECT Value", queryBuildMethods.get(2), null, null))),
        Arguments.of(
            named(
                "createSelectionQuery",
                new Parameter(
                    "createSelectionQuery",
                    "SELECT io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Value",
                    queryBuildMethods.get(3),
                    null,
                    null))));
  }

  private static class Parameter {
    Parameter(
        String methodName,
        String resource,
        Function<Session, SelectionQuery<?>> queryBuildMethod,
        BiConsumer<Session, Value> sessionMethodTest,
        BiConsumer<StatelessSession, Value> statelessSessionMethodTest) {
      this.methodName = methodName;
      this.resource = resource;
      this.sessionMethodTest = sessionMethodTest;
      this.queryBuildMethod = queryBuildMethod;
      this.statelessSessionMethodTest = statelessSessionMethodTest;
    }

    public final String methodName;
    public final String resource;
    public final Function<Session, SelectionQuery<?>> queryBuildMethod;
    public final BiConsumer<Session, Value> sessionMethodTest;
    public final BiConsumer<StatelessSession, Value> statelessSessionMethodTest;
  }

  private static SpanDataAssert assertSessionSpan(
      SpanDataAssert span, SpanData parent, AtomicReference<String> sessionId, String spanName) {
    return span.hasName(spanName)
        .hasKind(SpanKind.INTERNAL)
        .hasParent(parent)
        .hasAttributesSatisfying(
            attributes -> {
              Assertions.assertThat(attributes.get(AttributeKey.stringKey("hibernate.session_id")))
                  .isInstanceOf(String.class);
              sessionId.set(attributes.get(AttributeKey.stringKey("hibernate.session_id")));
            });
  }

  private static SpanDataAssert assertSpanWithSessionId(
      SpanDataAssert span, SpanData parent, String spanName, AtomicReference<String> sessionId) {
    return span.hasName(spanName)
        .hasKind(SpanKind.INTERNAL)
        .hasParent(parent)
        .hasAttributesSatisfying(
            attributes -> {
              assertThat(attributes)
                  .containsEntry(AttributeKey.stringKey("hibernate.session_id"), sessionId.get());
            });
  }

  private static SpanDataAssert assertClientSpan(SpanDataAssert span, SpanData parent) {
    return span.hasKind(SpanKind.CLIENT)
        .hasParent(parent)
        .hasAttributesSatisfyingExactly(
            equalTo(SemanticAttributes.DB_SYSTEM, "h2"),
            equalTo(SemanticAttributes.DB_NAME, "db1"),
            equalTo(SemanticAttributes.DB_USER, "sa"),
            equalTo(SemanticAttributes.DB_CONNECTION_STRING, "h2:mem:"),
            satisfies(SemanticAttributes.DB_STATEMENT, val -> val.isInstanceOf(String.class)),
            satisfies(SemanticAttributes.DB_OPERATION, val -> val.isInstanceOf(String.class)),
            equalTo(SemanticAttributes.DB_SQL_TABLE, "Value"));
  }

  private static SpanDataAssert assertClientSpan(
      SpanDataAssert span, SpanData parent, String verb) {
    return span.hasName(verb.concat(" db1.Value"))
        .hasKind(SpanKind.CLIENT)
        .hasParent(parent)
        .hasAttributesSatisfyingExactly(
            equalTo(SemanticAttributes.DB_SYSTEM, "h2"),
            equalTo(SemanticAttributes.DB_NAME, "db1"),
            equalTo(SemanticAttributes.DB_USER, "sa"),
            equalTo(SemanticAttributes.DB_CONNECTION_STRING, "h2:mem:"),
            satisfies(
                SemanticAttributes.DB_STATEMENT,
                stringAssert -> stringAssert.startsWith(verb.toLowerCase())),
            equalTo(SemanticAttributes.DB_OPERATION, verb),
            equalTo(SemanticAttributes.DB_SQL_TABLE, "Value"));
  }
}

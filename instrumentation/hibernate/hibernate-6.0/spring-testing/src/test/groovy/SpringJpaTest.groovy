/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes
import org.hibernate.Version
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Shared
import spring.jpa.Customer
import spring.jpa.CustomerRepository
import spring.jpa.PersistenceConfig

import static io.opentelemetry.api.trace.SpanKind.CLIENT
import static io.opentelemetry.api.trace.SpanKind.INTERNAL
import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableDatabaseSemconv
import static io.opentelemetry.instrumentation.testing.junit.db.SemconvStabilityUtil.maybeStable

class SpringJpaTest extends AgentInstrumentationSpecification {

  @Shared
  def context = new AnnotationConfigApplicationContext(PersistenceConfig)

  @Shared
  def repo = context.getBean(CustomerRepository)

  @SuppressWarnings("deprecation") // TODO DbIncubatingAttributes.DB_CONNECTION_STRING deprecation
  def "test CRUD"() {
    setup:
    def isHibernate4 = Version.getVersionString().startsWith("4.")
    def customer = new Customer("Bob", "Anonymous")

    expect:
    customer.id == null
    !runWithSpan("parent") {
      repo.findAll().iterator().hasNext()
    }

    def sessionId
    assertTraces(1) {
      trace(0, 4) {
        span(0) {
          name "parent"
          kind INTERNAL
          hasNoParent()
          attributes {
          }
        }
        span(1) {
          name "SELECT spring.jpa.Customer"
          kind INTERNAL
          childOf span(0)
          attributes {
            "hibernate.session_id" {
              sessionId = it
              it instanceof String
            }
          }
        }
        span(2) {
          name "SELECT test.Customer"
          kind CLIENT
          childOf span(1)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "hsqldb"
            "${maybeStable(DbIncubatingAttributes.DB_NAME)}" "test"
            "$DbIncubatingAttributes.DB_USER" emitStableDatabaseSemconv() ? null : "sa"
            "$DbIncubatingAttributes.DB_CONNECTION_STRING" emitStableDatabaseSemconv() ? null : "hsqldb:mem:"
            "${maybeStable(DbIncubatingAttributes.DB_STATEMENT)}" ~/select ([^.]+)\.id([^,]*),([^.]+)\.firstName([^,]*),([^.]+)\.lastName(.*)from Customer(.*)/
            "${maybeStable(DbIncubatingAttributes.DB_OPERATION)}" "SELECT"
            "${maybeStable(DbIncubatingAttributes.DB_SQL_TABLE)}" "Customer"
          }
        }
        span(3) {
          name "Transaction.commit"
          kind INTERNAL
          childOf span(0)
          attributes {
            "hibernate.session_id" sessionId
          }
        }
      }
    }
    clearExportedData()

    when:
    runWithSpan("parent") {
      repo.save(customer)
    }
    def savedId = customer.id

    then:
    customer.id != null
    def sessionId2
    assertTraces(1) {
      trace(0, 4 + (isHibernate4 ? 0 : 1)) {
        span(0) {
          name "parent"
          kind INTERNAL
          hasNoParent()
          attributes {
          }
        }
        span(1) {
          name "Session.persist spring.jpa.Customer"
          kind INTERNAL
          childOf span(0)
          attributes {
            "hibernate.session_id" {
              sessionId2 = it
              it instanceof String
            }
          }
        }
        if (!isHibernate4) {
          span(2) {
            name "CALL test"
            kind CLIENT
            childOf span(1)
            attributes {
              "$DbIncubatingAttributes.DB_SYSTEM" "hsqldb"
              "${maybeStable(DbIncubatingAttributes.DB_NAME)}" "test"
              "$DbIncubatingAttributes.DB_USER" emitStableDatabaseSemconv() ? null : "sa"
              "${maybeStable(DbIncubatingAttributes.DB_STATEMENT)}" "call next value for Customer_SEQ"
              "$DbIncubatingAttributes.DB_CONNECTION_STRING" emitStableDatabaseSemconv() ? null : "hsqldb:mem:"
              "${maybeStable(DbIncubatingAttributes.DB_OPERATION)}" "CALL"
            }
          }
          span(3) {
            name "Transaction.commit"
            kind INTERNAL
            childOf span(0)
            attributes {
              "hibernate.session_id" sessionId2
            }
          }
          span(4) {
            name "INSERT test.Customer"
            kind CLIENT
            childOf span(3)
            attributes {
              "$DbIncubatingAttributes.DB_SYSTEM" "hsqldb"
              "${maybeStable(DbIncubatingAttributes.DB_NAME)}" "test"
              "$DbIncubatingAttributes.DB_USER" emitStableDatabaseSemconv() ? null : "sa"
              "$DbIncubatingAttributes.DB_CONNECTION_STRING" emitStableDatabaseSemconv() ? null : "hsqldb:mem:"
              "${maybeStable(DbIncubatingAttributes.DB_STATEMENT)}" ~/insert into Customer \(.*\) values \(.*\)/
              "${maybeStable(DbIncubatingAttributes.DB_OPERATION)}" "INSERT"
              "${maybeStable(DbIncubatingAttributes.DB_SQL_TABLE)}" "Customer"
            }
          }
        } else {
          span(2) {
            name "INSERT test.Customer"
            kind CLIENT
            childOf span(1)
            attributes {
              "$DbIncubatingAttributes.DB_SYSTEM" "hsqldb"
              "${maybeStable(DbIncubatingAttributes.DB_NAME)}" "test"
              "$DbIncubatingAttributes.DB_USER" emitStableDatabaseSemconv() ? null : "sa"
              "$DbIncubatingAttributes.DB_CONNECTION_STRING" emitStableDatabaseSemconv() ? null : "hsqldb:mem:"
              "${maybeStable(DbIncubatingAttributes.DB_STATEMENT)}" ~/insert into Customer \(.*\) values \(.*\)/
              "${maybeStable(DbIncubatingAttributes.DB_OPERATION)}" "INSERT"
              "${maybeStable(DbIncubatingAttributes.DB_SQL_TABLE)}" "Customer"
            }
          }
          span(3) {
            name "Transaction.commit"
            kind INTERNAL
            childOf span(0)
            attributes {
              "hibernate.session_id" sessionId2
            }
          }
        }
      }
    }
    clearExportedData()

    when:
    customer.firstName = "Bill"
    runWithSpan("parent") {
      repo.save(customer)
    }

    then:
    customer.id == savedId
    def sessionId3
    assertTraces(1) {
      trace(0, 5) {
        span(0) {
          name "parent"
          kind INTERNAL
          hasNoParent()
          attributes {
          }
        }
        span(1) {
          name "Session.merge spring.jpa.Customer"
          kind INTERNAL
          childOf span(0)
          attributes {
            "hibernate.session_id" {
              sessionId3 = it
              it instanceof String
            }
          }
        }
        span(2) {
          name "SELECT test.Customer"
          kind CLIENT
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "hsqldb"
            "${maybeStable(DbIncubatingAttributes.DB_NAME)}" "test"
            "$DbIncubatingAttributes.DB_USER" emitStableDatabaseSemconv() ? null : "sa"
            "$DbIncubatingAttributes.DB_CONNECTION_STRING" emitStableDatabaseSemconv() ? null : "hsqldb:mem:"
            "${maybeStable(DbIncubatingAttributes.DB_STATEMENT)}" ~/select ([^.]+)\.id([^,]*),([^.]+)\.firstName([^,]*),([^.]+)\.lastName (.*)from Customer (.*)where ([^.]+)\.id( ?)=( ?)\?/
            "${maybeStable(DbIncubatingAttributes.DB_OPERATION)}" "SELECT"
            "${maybeStable(DbIncubatingAttributes.DB_SQL_TABLE)}" "Customer"
          }
        }
        span(3) {
          name "Transaction.commit"
          kind INTERNAL
          childOf span(0)
          attributes {
            "hibernate.session_id" sessionId3
          }
        }
        span(4) {
          name "UPDATE test.Customer"
          kind CLIENT
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "hsqldb"
            "${maybeStable(DbIncubatingAttributes.DB_NAME)}" "test"
            "$DbIncubatingAttributes.DB_USER" emitStableDatabaseSemconv() ? null : "sa"
            "$DbIncubatingAttributes.DB_CONNECTION_STRING" emitStableDatabaseSemconv() ? null : "hsqldb:mem:"
            "${maybeStable(DbIncubatingAttributes.DB_STATEMENT)}" ~/update Customer set firstName=\?,(.*)lastName=\? where id=\?/
            "${maybeStable(DbIncubatingAttributes.DB_OPERATION)}" "UPDATE"
            "${maybeStable(DbIncubatingAttributes.DB_SQL_TABLE)}" "Customer"
          }
        }
      }
    }
    clearExportedData()

    when:
    customer = runWithSpan("parent") {
      repo.findByLastName("Anonymous")[0]
    }

    then:
    customer.id == savedId
    customer.firstName == "Bill"
    assertTraces(1) {
      trace(0, 3) {
        span(0) {
          name "parent"
          kind INTERNAL
          hasNoParent()
          attributes {
          }
        }
        span(1) {
          name { it == "SELECT spring.jpa.Customer" || it == "Hibernate Query" }
          kind INTERNAL
          childOf span(0)
          attributes {
            "hibernate.session_id" String
          }
        }
        span(2) {
          name "SELECT test.Customer"
          kind CLIENT
          childOf span(1)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "hsqldb"
            "${maybeStable(DbIncubatingAttributes.DB_NAME)}" "test"
            "$DbIncubatingAttributes.DB_USER" emitStableDatabaseSemconv() ? null : "sa"
            "$DbIncubatingAttributes.DB_CONNECTION_STRING" emitStableDatabaseSemconv() ? null : "hsqldb:mem:"
            "${maybeStable(DbIncubatingAttributes.DB_STATEMENT)}" ~/select ([^.]+)\.id([^,]*),([^.]+)\.firstName([^,]*),([^.]+)\.lastName (.*)from Customer (.*)(where ([^.]+)\.lastName( ?)=( ?)\?|)/
            "${maybeStable(DbIncubatingAttributes.DB_OPERATION)}" "SELECT"
            "${maybeStable(DbIncubatingAttributes.DB_SQL_TABLE)}" "Customer"
          }
        }
      }
    }
    clearExportedData()

    when:
    runWithSpan("parent") {
      repo.delete(customer)
    }

    then:
    assertTraces(1) {
      trace(0, 6 + (isHibernate4 ? 0 : 1)) {
        span(0) {
          name "parent"
          kind INTERNAL
          hasNoParent()
          attributes {
          }
        }
        def offset = 0
        if (!isHibernate4) {
          offset = 2
          span(1) {
            name ~/Session.(get|find) spring.jpa.Customer/
            kind INTERNAL
            childOf span(0)
            attributes {
              "hibernate.session_id" String
            }
          }
          span(2) {
            name "SELECT test.Customer"
            kind CLIENT
            childOf span(1)
            attributes {
              "$DbIncubatingAttributes.DB_SYSTEM" "hsqldb"
              "${maybeStable(DbIncubatingAttributes.DB_NAME)}" "test"
              "$DbIncubatingAttributes.DB_USER" emitStableDatabaseSemconv() ? null : "sa"
              "$DbIncubatingAttributes.DB_CONNECTION_STRING" emitStableDatabaseSemconv() ? null : "hsqldb:mem:"
              "${maybeStable(DbIncubatingAttributes.DB_STATEMENT)}" ~/select ([^.]+)\.id([^,]*),([^.]+)\.firstName([^,]*),([^.]+)\.lastName (.*)from Customer (.*)where ([^.]+)\.id( ?)=( ?)\?/
              "${maybeStable(DbIncubatingAttributes.DB_OPERATION)}" "SELECT"
              "${maybeStable(DbIncubatingAttributes.DB_SQL_TABLE)}" "Customer"
            }
          }
        }
        span(1 + offset) {
          name "Session.merge spring.jpa.Customer"
          kind INTERNAL
          childOf span(0)
          attributes {
            "hibernate.session_id" String
          }
        }
        if (isHibernate4) {
          offset = 1
          span(2) {
            name "SELECT test.Customer"
            kind CLIENT
            childOf span(1)
            attributes {
              "$DbIncubatingAttributes.DB_SYSTEM" "hsqldb"
              "${maybeStable(DbIncubatingAttributes.DB_NAME)}" "test"
              "$DbIncubatingAttributes.DB_USER" emitStableDatabaseSemconv() ? null : "sa"
              "$DbIncubatingAttributes.DB_CONNECTION_STRING" emitStableDatabaseSemconv() ? null : "hsqldb:mem:"
              "${maybeStable(DbIncubatingAttributes.DB_STATEMENT)}" ~/select ([^.]+)\.id([^,]*),([^.]+)\.firstName([^,]*),([^.]+)\.lastName (.*)from Customer (.*)where ([^.]+)\.id( ?)=( ?)\?/
              "${maybeStable(DbIncubatingAttributes.DB_OPERATION)}" "SELECT"
              "${maybeStable(DbIncubatingAttributes.DB_SQL_TABLE)}" "Customer"
            }
          }
        }
        span(2 + offset) {
          name "Session.delete spring.jpa.Customer"
          kind INTERNAL
          childOf span(0)
          attributes {
            "hibernate.session_id" String
          }
        }
        span(3 + offset) {
          name "Transaction.commit"
          kind INTERNAL
          childOf span(0)
          attributes {
            "hibernate.session_id" String
          }
        }
        span(4 + offset) {
          name "DELETE test.Customer"
          kind CLIENT
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "hsqldb"
            "${maybeStable(DbIncubatingAttributes.DB_NAME)}" "test"
            "$DbIncubatingAttributes.DB_USER" emitStableDatabaseSemconv() ? null : "sa"
            "$DbIncubatingAttributes.DB_CONNECTION_STRING" emitStableDatabaseSemconv() ? null : "hsqldb:mem:"
            "${maybeStable(DbIncubatingAttributes.DB_STATEMENT)}" "delete from Customer where id=?"
            "${maybeStable(DbIncubatingAttributes.DB_OPERATION)}" "DELETE"
            "${maybeStable(DbIncubatingAttributes.DB_SQL_TABLE)}" "Customer"
          }
        }
      }
    }
  }
}

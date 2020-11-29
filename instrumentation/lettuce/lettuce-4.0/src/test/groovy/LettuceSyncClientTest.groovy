/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static io.opentelemetry.api.trace.Span.Kind.CLIENT

import com.lambdaworks.redis.ClientOptions
import com.lambdaworks.redis.RedisClient
import com.lambdaworks.redis.RedisConnectionException
import com.lambdaworks.redis.api.StatefulConnection
import com.lambdaworks.redis.api.sync.RedisCommands
import io.opentelemetry.api.trace.attributes.SemanticAttributes
import io.opentelemetry.instrumentation.test.AgentTestRunner
import io.opentelemetry.instrumentation.test.utils.PortUtils
import redis.embedded.RedisServer
import spock.lang.Shared

class LettuceSyncClientTest extends AgentTestRunner {
  public static final String HOST = "localhost"
  public static final int DB_INDEX = 0
  // Disable autoreconnect so we do not get stray traces popping up on server shutdown
  public static final ClientOptions CLIENT_OPTIONS = new ClientOptions.Builder().autoReconnect(false).build()

  @Shared
  int port
  @Shared
  int incorrectPort
  @Shared
  String dbAddr
  @Shared
  String dbAddrNonExistent
  @Shared
  String dbUriNonExistent
  @Shared
  String embeddedDbUri

  @Shared
  RedisServer redisServer

  @Shared
  Map<String, String> testHashMap = [
    firstname: "John",
    lastname : "Doe",
    age      : "53"
  ]

  RedisClient redisClient
  StatefulConnection connection
  RedisCommands<String, ?> syncCommands

  def setupSpec() {
    port = PortUtils.randomOpenPort()
    incorrectPort = PortUtils.randomOpenPort()
    dbAddr = HOST + ":" + port + "/" + DB_INDEX
    dbAddrNonExistent = HOST + ":" + incorrectPort + "/" + DB_INDEX
    dbUriNonExistent = "redis://" + dbAddrNonExistent
    embeddedDbUri = "redis://" + dbAddr

    redisServer = RedisServer.builder()
    // bind to localhost to avoid firewall popup
      .setting("bind " + HOST)
    // set max memory to avoid problems in CI
      .setting("maxmemory 128M")
      .port(port).build()
  }

  def setup() {
    redisClient = RedisClient.create(embeddedDbUri)

    redisServer.start()
    connection = redisClient.connect()
    syncCommands = connection.sync()

    syncCommands.set("TESTKEY", "TESTVAL")
    syncCommands.hmset("TESTHM", testHashMap)

    // 2 sets + 1 connect trace
    TEST_WRITER.waitForTraces(3)
    TEST_WRITER.clear()
  }

  def cleanup() {
    connection.close()
    redisServer.stop()
  }

  def "connect"() {
    setup:
    RedisClient testConnectionClient = RedisClient.create(embeddedDbUri)
    testConnectionClient.setOptions(CLIENT_OPTIONS)

    when:
    StatefulConnection connection = testConnectionClient.connect()

    then:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "CONNECT"
          kind CLIENT
          errored false
          attributes {
            "${SemanticAttributes.NET_PEER_NAME.key()}" HOST
            "${SemanticAttributes.NET_PEER_IP.key()}" "127.0.0.1"
            "${SemanticAttributes.NET_PEER_PORT.key()}" port
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "CONNECT"
          }
        }
      }
    }

    cleanup:
    connection.close()
  }

  def "connect exception"() {
    setup:
    RedisClient testConnectionClient = RedisClient.create(dbUriNonExistent)
    testConnectionClient.setOptions(CLIENT_OPTIONS)

    when:
    testConnectionClient.connect()

    then:
    thrown RedisConnectionException
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "CONNECT"
          kind CLIENT
          errored true
          errorEvent RedisConnectionException, String
          attributes {
            "${SemanticAttributes.NET_PEER_NAME.key()}" HOST
            "${SemanticAttributes.NET_PEER_IP.key()}" "127.0.0.1"
            "${SemanticAttributes.NET_PEER_PORT.key()}" incorrectPort
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "CONNECT"
          }
        }
      }
    }
  }

  def "set command"() {
    setup:
    String res = syncCommands.set("TESTSETKEY", "TESTSETVAL")

    expect:
    res == "OK"
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "SET"
          kind CLIENT
          errored false
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "SET"
          }
        }
      }
    }
  }

  def "get command"() {
    setup:
    String res = syncCommands.get("TESTKEY")

    expect:
    res == "TESTVAL"
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "GET"
          kind CLIENT
          errored false
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "GET"
          }
        }
      }
    }
  }

  def "get non existent key command"() {
    setup:
    String res = syncCommands.get("NON_EXISTENT_KEY")

    expect:
    res == null
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "GET"
          kind CLIENT
          errored false
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "GET"
          }
        }
      }
    }
  }

  def "command with no arguments"() {
    setup:
    def keyRetrieved = syncCommands.randomkey()

    expect:
    keyRetrieved != null
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "RANDOMKEY"
          kind CLIENT
          errored false
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "RANDOMKEY"
          }
        }
      }
    }
  }

  def "list command"() {
    setup:
    long res = syncCommands.lpush("TESTLIST", "TESTLIST ELEMENT")

    expect:
    res == 1
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "LPUSH"
          kind CLIENT
          errored false
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "LPUSH"
          }
        }
      }
    }
  }

  def "hash set command"() {
    setup:
    def res = syncCommands.hmset("user", testHashMap)

    expect:
    res == "OK"
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "HMSET"
          kind CLIENT
          errored false
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "HMSET"
          }
        }
      }
    }
  }

  def "hash getall command"() {
    setup:
    Map<String, String> res = syncCommands.hgetall("TESTHM")

    expect:
    res == testHashMap
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "HGETALL"
          kind CLIENT
          errored false
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "HGETALL"
          }
        }
      }
    }
  }

  def "debug segfault command (returns void) with no argument should produce span"() {
    setup:
    syncCommands.debugSegfault()

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "DEBUG"
          kind CLIENT
          errored false
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "DEBUG"
          }
        }
      }
    }
  }

  def "shutdown command (returns void) should produce a span"() {
    setup:
    syncCommands.shutdown(false)

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "SHUTDOWN"
          kind CLIENT
          errored false
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key()}" "redis"
            "${SemanticAttributes.DB_STATEMENT.key()}" "SHUTDOWN"
          }
        }
      }
    }
  }
}

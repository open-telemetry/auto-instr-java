import io.opentelemetry.auto.instrumentation.api.MoreTags
import io.opentelemetry.auto.instrumentation.api.Tags
import io.opentelemetry.auto.test.AgentTestRunner

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

import static io.opentelemetry.auto.test.utils.TraceUtils.basicSpan
import static io.opentelemetry.auto.test.utils.TraceUtils.runUnderTrace

class FilterTest extends AgentTestRunner {
  static {
    System.setProperty("opentelemetry.auto.integration.servlet-filter.enabled", "true")
  }

  def "test doFilter no-parent"() {
    when:
    filter.doFilter(null, null, null)

    then:
    assertTraces(0) {}

    where:
    filter = new TestFilter()
  }

  def "test doFilter with parent"() {
    when:
    runUnderTrace("parent") {
      filter.doFilter(null, null, null)
    }

    then:
    assertTraces(1) {
      trace(0, 2) {
        basicSpan(it, 0, "parent")
        span(1) {
          operationName "servlet.filter"
          childOf span(0)
          tags {
            "$MoreTags.RESOURCE_NAME" "${filter.class.simpleName}.doFilter"
            "$Tags.COMPONENT" "java-web-servlet-filter"
          }
        }
      }
    }

    where:
    filter << [new TestFilter(), new TestFilter() {}]
  }

  def "test doFilter exception"() {
    setup:
    def ex = new Exception("some error")
    def filter = new TestFilter() {
      @Override
      void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        throw ex
      }
    }

    when:
    runUnderTrace("parent") {
      filter.doFilter(null, null, null)
    }

    then:
    def th = thrown(Exception)
    th == ex

    assertTraces(1) {
      trace(0, 2) {
        basicSpan(it, 0, "parent", null, null, ex)
        span(1) {
          operationName "servlet.filter"
          childOf span(0)
          errored true
          tags {
            "$MoreTags.RESOURCE_NAME" "${filter.class.simpleName}.doFilter"
            "$Tags.COMPONENT" "java-web-servlet-filter"
            errorTags(ex.class, ex.message)
          }
        }
      }
    }
  }

  static class TestFilter implements Filter {

    @Override
    void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    }

    @Override
    void destroy() {
    }
  }
}

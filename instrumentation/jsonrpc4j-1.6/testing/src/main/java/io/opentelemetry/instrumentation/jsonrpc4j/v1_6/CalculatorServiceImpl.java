package io.opentelemetry.instrumentation.jsonrpc4j.v1_6;

public class CalculatorServiceImpl implements  CalculatorService {
  @Override
  public int add(int a, int b) {
    return a + b;
  }

  @Override
  public int subtract(int a, int b) {
    return a - b;
  }
}

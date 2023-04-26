package io.opentelemetry.javaagent.instrumentation.thrift;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransport;

public class ClientProtocolFactoryWrapper implements  TProtocolFactory{
  public TProtocolFactory innerProtocolFactoryWrapper;
  @Override
  public TProtocol getProtocol(TTransport tTransport) {
    TProtocol protocol = innerProtocolFactoryWrapper.getProtocol(tTransport);
    if(protocol instanceof ClientOutProtocolWrapper){
      return protocol;
    }
    return new ClientOutProtocolWrapper(innerProtocolFactoryWrapper.getProtocol(tTransport));
  }
  public  ClientProtocolFactoryWrapper(TProtocolFactory inner){
    innerProtocolFactoryWrapper = inner;
  }
}

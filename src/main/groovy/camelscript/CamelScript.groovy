package camelscript

import org.apache.camel.CamelContext
import org.apache.camel.ConsumerTemplate
import org.apache.camel.Exchange
import org.apache.camel.ProducerTemplate
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.DefaultExchange
import org.apache.camel.impl.JndiRegistry
import org.apache.camel.impl.PropertyPlaceholderDelegateRegistry
import org.apache.camel.impl.SimpleRegistry
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

import java.util.concurrent.Future

/**
 * @author Tommy Barker
 */
class CamelScript {

    final CamelContext camelContext
    final ProducerTemplate producerTemplate
    final ConsumerTemplate consumerTemplate

    public CamelScript() {
        camelContext = new DefaultCamelContext()
        def registry = new PropertyPlaceholderDelegateRegistry(camelContext, new SimpleRegistry())
        camelContext.registry = registry
        producerTemplate = camelContext.createProducerTemplate()
        consumerTemplate = camelContext.createConsumerTemplate()
    }

    CamelScript bind(object) {
        checkNull(object)

        def name = object.class.getSimpleName()
        def m = name =~ /([A-Z]).*/
        m.lookingAt()
        def lowerCaseLetter = m.group(1).toLowerCase()
        def bindingName = lowerCaseLetter + name.substring(1)

        bind(bindingName, object)
        return this
    }

    private void checkNull(object) {
        if (object == null) {
            throw new IllegalArgumentException("the name or value of the binding cannot be null")
        }
    }

    CamelScript bind(String name, object) {
        checkNull(name)
        checkNull(object)
        //the first registry is a PropertyPlaceholderDelegateRegistry that delegates to a JndiRegsitry
        Map registry = camelContext.registry.registry
        registry[name] = object
        return this
    }

    CamelScript consume(String endpoint, Closure closure) {
        def consumer = { ConsumerTemplate consumerTemplate ->
            return consumerTemplate.receive(endpoint)
        }
        consumeHelper(consumer, closure)
        return this
    }

    CamelScript consumeNoWait(String endpoint, Closure closure) {
        def consumer = { ConsumerTemplate consumerTemplate ->
            return consumerTemplate.receiveNoWait(endpoint)
        }
        consumeHelper(consumer, closure)
        return this
    }

    void consumeWait(String endpoint, long wait, Closure closure) {
        def consumer = { ConsumerTemplate consumerTemplate ->
            return consumerTemplate.receive(endpoint, wait)
        }
        consumeHelper(consumer, closure)
    }

    void consumeTillDone(String endpoint, long wait = 5000L, Closure closure) {
        boolean hasValue = true
        while (hasValue) {
            def consumer = { ConsumerTemplate consumerTemplate ->
                Exchange exchange = consumerTemplate.receive(endpoint, wait)
                hasValue = exchange != null
                return exchange
            }
            consumeHelper(consumer, false, closure)
        }
    }

    private void consumeHelper(Closure consume, boolean processNull = true, Closure processBody) {
        Exchange body = null
        try {
            body = consume.call(consumerTemplate)
            def parameter = processBody.parameterTypes[0]

            if (parameter == Exchange && body != null) {
                processBody.call(body)
            } else {
                def messageBody = null
                if (body) {
                    messageBody = body.in.getBody(parameter)
                    if (messageBody == null) {
                        messageBody = body.in.body.asType(parameter)
                    }
                }
                if (messageBody != null) {
                    processBody.call(messageBody)
                } else if (processNull) {
                    processBody.call(null)
                }
            }
        } catch (Exception e) {
            if (body) {
                body.exception = e
            }
            throw e
        } finally {
            if (body) {
                consumerTemplate.doneUoW(body)
            }
        }
    }

    public <T> T convertTo(Class<T> convertion, valueToConvert) {
        def converted = camelContext.typeConverter.convertTo(convertion, valueToConvert)
        if (converted == null) {
            try {
                converted = valueToConvert.asType(convertion)
            } catch (GroovyCastException ex) {
                //do nothing, no convertion available
            }
        }
        return converted
    }

    Exchange send(String endpoint, body) {
        send(endpoint, body, [:])
    }

    Exchange send(String endpoint, body, Map headers) {
        DefaultExchange exchange = createExchange(camelContext, body, headers)

        def wrappedResponseExchange = new WrappedResponseExchange()
        def response = producerTemplate.send(endpoint, exchange)
        wrappedResponseExchange.exchange = response
        return wrappedResponseExchange
    }

    Future<Exchange> asyncSend(String endpoint, body) {
        asyncSend(endpoint, body, [:])
    }

    Future<Exchange> asyncSend(String endpoint, body, Map headers) {
        def exchange = createExchange(camelContext, body, headers)
        Future<Exchange> future = producerTemplate.asyncSend(endpoint, exchange)
        return new FutureWrapper(future: future)
    }

    private DefaultExchange createExchange(CamelContext camelContext, body, headers) {
        def exchange = new DefaultExchange(camelContext)
        exchange.in.body = body
        exchange.in.headers = headers
        exchange
    }


}

class WrappedResponseExchange implements Exchange {
    @Delegate
    Exchange exchange

    def asType(Class type) {
        def response = exchange.out.getBody(type)
        if (response) return response

        def body = this.out.body
        if (body) {
            throw new GroovyCastException(body, type)
        } else {
            throw new NullPointerException("Exception body is null, can't convert to $type")
        }
    }
}

class FutureWrapper implements Future<Exchange> {
    @Delegate
    Future<Exchange> future

    Exchange get() {
        Exchange exchange = future.get()
        new WrappedResponseExchange(exchange: exchange)
    }
}
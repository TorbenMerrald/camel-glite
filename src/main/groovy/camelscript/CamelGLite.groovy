package camelscript

import groovy.util.logging.Slf4j
import org.apache.camel.*
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.DefaultExchange
import org.apache.camel.impl.SimpleRegistry
import org.apache.camel.spi.Registry
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

import java.util.concurrent.Future

/**
 * @author Tommy Barker
 */
@Slf4j
class CamelGLite implements Closeable {

    final CamelContext camelContext
    final ProducerTemplate producerTemplate
    final ConsumerTemplate consumerTemplate

    public CamelGLite() {
        this(new SimpleRegistry())
    }

    public CamelGLite(CamelContext camelContext) {
        this.camelContext = camelContext
        producerTemplate = camelContext.createProducerTemplate()
        consumerTemplate = camelContext.createConsumerTemplate()
        camelContext.addService(consumerTemplate)
        camelContext.addService(producerTemplate)
        camelContext.start()
        addShutdownHook {
            close()
        }
    }

    public CamelGLite(Registry registry) {
        this(new DefaultCamelContext(registry))
    }

    public CamelGLite(Binding binding) {
        this(new BindingBackedRegistry(binding))
    }

    CamelGLite bind(object) {
        checkNull(object)

        def name = object.class.getSimpleName()
        def m = name =~ /([A-Z]).*/
        m.lookingAt()
        def lowerCaseLetter = m.group(1).toLowerCase()
        def bindingName = lowerCaseLetter + name.substring(1)

        bind(bindingName, object)
        return this
    }

    private static void checkNull(object) {
        if (object == null) {
            throw new IllegalArgumentException("the name or value of the binding cannot be null")
        }
    }

    CamelGLite bind(String name, object) {
        checkNull(name)
        checkNull(object)
        //the first registry is a PropertyPlaceholderDelegateRegistry that delegates to a map based registry
        def delegate = camelContext.registry.delegate
        def notMap = !(delegate instanceof Map)
        if (notMap) {
            throw new UnsupportedOperationException("registry must implement [Map] to use [bind] methods")
        }
        Map registry = delegate
        registry[name] = object
        return this
    }

    CamelGLite consume(String endpoint, Closure closure) {
        def consumer = { ConsumerTemplate consumerTemplate ->
            return consumerTemplate.receive(endpoint)
        }
        consumeHelper(consumer, closure)
        return this
    }

    void consumeForever(String endpoint, Closure closure) {
        consumeForever(endpoint, 5000L, closure)
    }

    void consumeForever(String endpoint, long wait, Closure closure) {
        while (!Thread.currentThread().isInterrupted()) {
            consumeWait(endpoint, wait, closure)
        }
        log.info "[consumeForever] was interrupted"
    }

    CamelGLite addRoutes(RouteBuilder routeBuilder) {
        camelContext.addRoutes(routeBuilder)

        return this
    }

    CamelGLite addRoutes(Closure closure) {
        def routeBuilder = new CamelGLiteRouteBuilder(closure: closure)
        addRoutes(routeBuilder)

        return this
    }

    GLiteExchange createExchange() {
        def exchange = new DefaultExchange(camelContext)
        new GLiteExchange(camelGLite: this, exchange: exchange)
    }

    CamelGLite consumeNoWait(String endpoint, Closure closure) {
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

    private void consumeHelper(Closure<Exchange> consume, boolean processNull = true, Closure processBody) {
        Exchange body = null
        try {
            body = consume.call(consumerTemplate)
            def parameter = processBody.parameterTypes[0]

            if (parameter == Exchange && body != null) {
                processBody.call(body)
            }
            else {
                def messageBody = null
                if (body) {
                    messageBody = body.in.getBody(parameter)
                    if (messageBody == null) {
                        messageBody = body.in.body.asType(parameter)
                    }
                }
                if (messageBody != null) {
                    processBody.call(messageBody)
                }
                else if (processNull) {
                    processBody.call(null)
                }
            }
        }
        catch (Exception e) {
            if (body) {
                body.exception = e
            }
            throw e
        }
        finally {
            if (body) {
                consumerTemplate.doneUoW(body)
            }
        }
    }

    @SuppressWarnings("GroovyUnusedCatchParameter")
    public <T> T convertTo(Class<T> convertion, valueToConvert) {
        def converted = camelContext.typeConverter.convertTo(convertion, valueToConvert)
        if (converted == null) {
            try {
                converted = valueToConvert.asType(convertion)
            }
            catch (GroovyCastException ex) {
                //do nothing, no convertion available
            }
        }
        return converted
    }

    GLiteExchange send(String endpoint) {
        def exchange = createExchange()
        send(endpoint, exchange)
    }

    GLiteExchange send(String endpoint, body) throws GLiteException {
        send(endpoint, body, [:])
    }

    GLiteExchange send(String endpoint, body, Map headers) throws GLiteException {
        Exchange exchange
        if (body instanceof Exchange) {
            exchange = body.copy()
            if (exchange.out.body || exchange.out.headers) {
                exchange.in = exchange.out
                exchange.out = null
            }
        }
        else {
            exchange = createExchangeHelper(camelContext, body, headers)
        }

        def wrappedResponseExchange = new GLiteExchange(camelGLite: this)
        def response = producerTemplate.send(endpoint, exchange)
        wrappedResponseExchange.exchange = response

        if (wrappedResponseExchange.getException()) {
            throw new GLiteException(wrappedResponseExchange, wrappedResponseExchange.getException())
        }

        return wrappedResponseExchange
    }

    Future<GLiteExchange> asyncSend(String endpoint, body) {
        asyncSend(endpoint, body, [:])
    }

    Future<GLiteExchange> asyncSend(String endpoint, body, Map headers) {
        def exchange = createExchangeHelper(camelContext, body, headers)
        Future<Exchange> future = producerTemplate.asyncSend(endpoint, exchange)
        return new FutureWrapper(future: future, camelGLite: this)
    }

    private static DefaultExchange createExchangeHelper(CamelContext camelContext, body, headers) {
        def exchange = new DefaultExchange(camelContext)
        exchange.in.body = body
        exchange.in.headers = headers
        exchange
    }

    @Override
    void close() throws IOException {
        try {
            if (camelContext.status != ServiceStatus.Stopped) {
                camelContext.stop()
            }
        }
        catch (Throwable ignore) {
            log.warn "Could not shutdown camel context properly"
        }
    }
}

class GLiteExchange implements Exchange {
    @Delegate
    Exchange exchange
    CamelGLite camelGLite

    def asType(Class type) {
        def response = exchange.out.getBody(type)
        if (response) return response

        def body = this.out.body
        if (body) {
            throw new GroovyCastException(body, type)
        }
        else {
            throw new NullPointerException("Exception body is null, can't convert to $type")
        }
    }

    GLiteExchange send(String uri) {
        camelGLite.send(uri, this)
    }

    Future<GLiteExchange> asyncSend(String uri) {
        camelGLite.asyncSend(uri, this)
    }

    GLiteExchange process(Closure closure) {
        closure.call(this)
        return this
    }
}

class FutureWrapper implements Future<GLiteExchange> {
    @Delegate
    Future<Exchange> future
    CamelGLite camelGLite

    GLiteExchange get() {
        Exchange exchange = future.get()
        def wrappedResponseExchange = new GLiteExchange(camelGLite: camelGLite, exchange: exchange)
        if (wrappedResponseExchange.getException()) {
            throw new GLiteException(wrappedResponseExchange, wrappedResponseExchange.getException())
        }

        return wrappedResponseExchange
    }
}

class GLiteException extends Exception {
    Exchange exchange

    GLiteException(Exchange exchange, Throwable cause) {
        super(cause)
        this.exchange = exchange
    }
}
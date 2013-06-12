package camelscript

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.file.GenericFile
import org.apache.camel.component.file.GenericFileFilter
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultExchange
import org.apache.camel.impl.JndiRegistry
import org.apache.camel.spi.Registry
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * @author Tommy Barker
 *
 */
class CamelGLiteSpec extends Specification {

    private static final String CAMEL_GLITE_SPEC = "camelGLiteSpec"
    @Rule
    TemporaryFolder tmpDirectory = new TemporaryFolder()
    def camelGLite = new CamelGLite()

    def setup() {
        createFiles()
    }

    def "test null binding failure"() {
        when: camelGLite.bind(null)
        then: thrown IllegalArgumentException
    }

    def "test null value binding faillure"() {
        when: camelGLite.bind("foo", null)
        then: thrown IllegalArgumentException
    }

    def "test binding names"() {
        when: "no name is given for the binding"
        camelGLite.bind(this)

        then: "the uncapitalized name of the class is used"
        Registry registry = camelGLite.camelContext.registry
        this == registry.lookupByName(CAMEL_GLITE_SPEC)
    }

    @SuppressWarnings("GroovyVariableNotAssigned")
    def "basic routing test"() {
        when:
        def fromCalled = false
        def body
        camelGLite.with {
            asyncSend("seda:test", "testBody")
            consume("seda:test") {
                body = it
                fromCalled = true
            }
        }

        then:
        fromCalled
        "testBody" == body
    }

    def "test more complex routing"() {
        def context = camelGLite.camelContext
        def fileFilter = [
                accept: { GenericFile file ->
                    return file.fileName.startsWith("file1") ||
                            file.fileName.startsWith("file2") ||
                            file.fileName.startsWith("file3") ||
                            file.fileName.startsWith("file4")

                }
        ] as GenericFileFilter

        camelGLite.bind("fileFilter", fileFilter)

        createFiles()
        MockEndpoint mock = context.getEndpoint("mock:endFull", MockEndpoint)
        mock.expectedMessageCount(4)
        Set fileNames = []

        when:
        //let's do 5 messages.  The fifth should be ignored because of the file filter
        (1..5).each {
            camelGLite.with {
                consumeWait("file://${tmpDirectory.root.path}?noop=true&initialDelay=0&filter=#fileFilter" as String, 1000L) { GenericFile file ->
                    if (file != null) {
                        fileNames << file.fileName
                        send("mock:endFull", it)
                    }
                }
            }
        }

        then:
        4 == fileNames.size()
        mock.assertIsSatisfied()
    }

    @SuppressWarnings("GroovyUnusedCatchParameter")
    def "test routing failures"() {
        createFiles()
        boolean exceptionOccurred = false
        when:
        camelGLite.with() {
            try {
                consume("file://${tmpDirectory.root.path}?initialDelay=0&moveFailed=.error" as String) { GenericFile file ->
                    throw new RuntimeException("maent to fail for testing")
                }
            }
            catch (RuntimeException e) {
                exceptionOccurred = true
            }
        }

        then:
        exceptionOccurred
        new File("${tmpDirectory.root.path}/.error").listFiles().length > 0
    }

    def "check responses from a route"() {
        when:
        def camelContext = camelGLite.camelContext
        camelContext.addRoutes(
                new RouteBuilder() {
                    @Override
                    void configure() throws Exception {
                        from("direct:start").process(
                                new Processor() {
                                    @Override
                                    void process(Exchange exchange) throws Exception {
                                        exchange.out.body = 5
                                    }
                                }
                        )
                    }
                }
        )
        camelContext.start()
        int response = camelGLite.send("direct:start", "hello") as int

        then:
        5 == response
    }

    def "check error handling from response"() {
        setup:
        setupErrorRoute()

        when:
        camelGLite.send("direct:start", "foo")

        then:
        thrown ResponseException
    }

    def "check error handling from async response"() {
        setup:
        setupErrorRoute()

        when:
        def future = camelGLite.asyncSend("direct:start", "bar")
        future.get()

        then:
        thrown ResponseException
    }

    def "hello world example with timer"() {
        when: "when consuming from a timer"
        def consumeOccurred = false
        camelGLite.consume("timer:jdkTimer?period=100") {
            consumeOccurred = true
        }

        then: "the callback should be called"
        consumeOccurred
    }

    def "CamelGLite supports binding backed registries"() {
        given: "a binding with the variable foo"
        def binding = new Binding()
        binding.setVariable("foo", "fam")

        when: "when I construct an instance of CamelGLite with the Binding"
        def glite = new CamelGLite(binding)

        then: "then I should be able to retrieve the value of foo from the registry"
        def registry = glite.camelContext.registry
        "fam" == registry.lookupByName("foo")

        when: "when I add variable bar after construction"
        binding.setVariable("bar", "baz")

        then: "I should be able to grab its value from the registry as well"
        "baz" == registry.lookupByName("bar")

        when: "bind method is called"
        glite.bind(this)

        then: "I should be able to retrieve value from registry and binding"
        this == registry.lookupByName(CAMEL_GLITE_SPEC)
        binding.hasVariable(CAMEL_GLITE_SPEC)
    }

    def "bind only works if registry is of type Map"() {
        given: "glite with a context that contains a non Map registry"
        def glite = new CamelGLite(new JndiRegistry())

        when: "bind is called properly"
        glite.bind(this)

        then: "exception occurs indicating the call is not supported"
        thrown UnsupportedOperationException
    }

    def "can consume forever until thread is interupted"() {
        when: "consuming forever from a time route with 100ms period, but interrupting thread after 500ms"
        int count = 0
        def thread = new Thread()
        thread.start {
            camelGLite.with {
                consumeForever("timer:testTimer?period=100&delay=0") {
                    count++
                }
            }
        }
        Thread.sleep(500)
        thread.interrupt()
        thread.join(200) //give it a chance to finish

        then: "at least 2 messages should have been processed"
        !thread.alive
        count >= 2
    }

    def "exchange pipeline spec"() {
        given: "an exchange with an out message and simple route"
        def context = camelGLite.camelContext
        def exchange = new DefaultExchange(context)
        def message = "hello from out"
        exchange.out.body = message
        boolean inIsOut = false
        context.addRoutes(
                new RouteBuilder() {
                    @Override
                    void configure() throws Exception {
                        from("direct:start").process(new Processor() {
                            @Override
                            void process(Exchange exchangeToTest) throws Exception {
                                inIsOut = exchangeToTest.in.body == message
                            }
                        })
                    }
                }
        )

        when: "sending the exchange to the simple route"
        camelGLite.send("direct:start", exchange)

        then: "the output becomes the input message"
        inIsOut
    }

    def "addRoute spec"() {
        when: "adding a route"
        boolean ran = false
        camelGLite.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("direct:start").process(new Processor() {
                    @Override
                    void process(Exchange exchange) throws Exception {
                        ran = true
                    }
                })
            }
        })

        then: "it is accessible and usable"
        camelGLite.send("direct:start", "hello")
        ran
    }

    def "createExchange spec"() {
        when: "create exchange is called"
        DefaultExchange exchange = camelGLite.createExchange() as DefaultExchange

        then: "an exchange is created using the underlying camelContext"
        exchange.context == camelGLite.camelContext
    }

    def getErrorDirectory() {
        new File("${tmpDirectory.root.path}/.error")
    }

    def createFiles() {
        tmpDirectory.newFile("unused")
        tmpDirectory.newFile("file1")
        tmpDirectory.newFile("file2")
        tmpDirectory.newFile("file3")
        tmpDirectory.newFile("file4")
        tmpDirectory.newFile("file5")
        tmpDirectory.newFile("file6")
    }

    def setupErrorRoute() {
        def camelContext = camelGLite.camelContext
        camelContext.addRoutes(
                new RouteBuilder() {
                    @Override
                    void configure() throws Exception {
                        from("direct:start").process(
                                {
                                    throw new RuntimeException("meant to do that")
                                } as Processor
                        )
                    }
                }
        )
        camelContext.start()
    }
}





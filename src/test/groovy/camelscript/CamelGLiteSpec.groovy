package camelscript

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.file.GenericFile
import org.apache.camel.component.file.GenericFileFilter
import org.apache.camel.component.mock.MockEndpoint
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * @author Tommy Barker
 *
 */
class CamelGLiteSpec extends Specification {

    @Rule
    TemporaryFolder tmpDirectory = new TemporaryFolder()
    def camelScript = new CamelGLite()

    def setup() {
        createFiles()
    }

    def "test null binding failure"() {
        when: camelScript.bind(null)
        then: thrown IllegalArgumentException
    }

    def "test null value binding faillure"() {
        when: camelScript.bind("foo", null)
        then: thrown IllegalArgumentException
    }

    def "test binding names"() {
        when: "no name is given for the binding"
        camelScript.bind(this)

        then: "the uncapitalized name of the class is used"
        def registry = camelScript.camelContext.registry
        this == registry.lookup("camelGLiteSpec")
    }

    @SuppressWarnings("GroovyVariableNotAssigned")
    def "basic routing test"() {
        when:
        def fromCalled = false
        def body
        camelScript.with {
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
        def context = camelScript.camelContext
        def fileFilter = [
                accept: { GenericFile file ->
                    return file.fileName.startsWith("file1") ||
                            file.fileName.startsWith("file2") ||
                            file.fileName.startsWith("file3") ||
                            file.fileName.startsWith("file4")

                }
        ] as GenericFileFilter

        camelScript.bind("fileFilter", fileFilter)

        createFiles()
        MockEndpoint mock = context.getEndpoint("mock:endFull", MockEndpoint)
        mock.expectedMessageCount(4)
        Set fileNames = []

        when:
        //let's do 5 messages.  The fifth should be ignored because of the file filter
        (1..5).each {
            camelScript.with {
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
        camelScript.with() {
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
        def camelContext = camelScript.camelContext
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
        int response = camelScript.send("direct:start", "hello") as int

        then:
        5 == response
    }

    def "check error handling from response"() {
        setup:
        setupErrorRoute()

        when:
        camelScript.send("direct:start", "foo")

        then:
        thrown ResponseException
    }

    def "check error handling from async response"() {
        setup:
        setupErrorRoute()

        when:
        def future = camelScript.asyncSend("direct:start", "bar")
        future.get()

        then:
        thrown ResponseException
    }

    def "hello world example with timer"() {
        when: "when consuming from a timer"
        def consumeOccurred = false
        camelScript.consume("timer:jdkTimer?period=100") {
            consumeOccurred = true
        }

        then: "the callback should be called"
        consumeOccurred
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
        def camelContext = camelScript.camelContext
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





package camelscript

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.file.GenericFile
import org.apache.camel.component.file.GenericFileFilter
import org.apache.camel.component.mock.MockEndpoint
import org.junit.Test

/**
 * @author Tommy Barker
 *
 */
class CamelScriptTest {

    def camelScript = new CamelScript()

    @Test
    void "if an object is not provided for binding, IllegalArgumentException is thrown"() {
        checkForIllegalArgumentException { camelScript.bind(null) }
        checkForIllegalArgumentException { camelScript.bind("foo", null) }
    }

    @Test
    void "if no name is given for a binding, the uncapitalized simple class name is used"() {
        def camelScriptTest = "camelScriptTest"
        camelScript.bind(this)
        def registry = camelScript.camelContext.registry
        assert this == registry.lookup(camelScriptTest)
    }

    @Test
    void "do basic from and to test"() {

        def fromCalled = false

        camelScript.with {
            asyncSend("seda:test", "testBody")
            consume("seda:test") {
                assert "testBody" == it
                fromCalled = true
            }
        }
        assert fromCalled
    }

    @Test
    void "test full blown routing examples"() {
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

        deleteTempDirectoryAndFiles()
        createTempDirectoryAndFiles()
        MockEndpoint mock = context.getEndpoint("mock:endFull")
        mock.expectedMessageCount(4)
        Set fileNames = []
        //let's do 5 messages.  The fifth should be ignored because of the file filter
        (1..5).each {
            camelScript.with {
                consumeWait("file://${tmpDirectory.path}?noop=true&initialDelay=0&filter=#fileFilter" as String, 1000L) { GenericFile file ->
                    if (file != null) {
                        fileNames << file.fileName
                        send("mock:endFull", it)
                    }
                }
            }
        }
        assert 4 == fileNames.size()
        mock.assertIsSatisfied()
        deleteTempDirectoryAndFiles()
    }

    @Test
    void "if there is a failure proper actions should take place, such as moving files on error"() {
        createTempDirectoryAndFiles()
        camelScript.with() {
            try {
                consume("file://${tmpDirectory.path}?initialDelay=0&moveFailed=.error" as String) { GenericFile file ->
                    throw new RuntimeException("maent to fail for testing")
                }
                assert false : "exception should have occurred"
            } catch (RuntimeException e) {
                assert new File("${tmpDirectory.path}/.error").listFiles()
            }
        }

        deleteTempDirectoryAndFiles()
    }

    @Test
    void "check responses from a route"() {
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
        assert 5 == response
    }

    void checkForIllegalArgumentException(Closure closure) {
        try {
            closure.call()
            assert false: "exception should have occurred"
        } catch (IllegalArgumentException) {
        }
    }

    def getTmpDirectory() {
        def home = System.getProperty("user.home")
        new File("${home}/.camelscripttmp")
    }

    def getErrorDirectory() {
        new File("${tmpDirectory.path}/.error")
    }

    def deleteErrorFiles() {
        if (errorDirectory.exists()) {
            errorDirectory.eachFile {
                it.delete()
            }
            errorDirectory.delete()
        }
    }

    def deleteTempDirectoryAndFiles() {
        deleteFiles()
        deleteErrorFiles()
        tmpDirectory.delete()
    }

    def deleteFiles() {
        tmpDirectory.listFiles().each {
            it.delete()
        }
    }

    def createTempDirectoryAndFiles() {
        def tempDirectory = getTmpDirectory()
        tempDirectory.mkdir()
        deleteFiles()

        File.createTempFile("unused", "metridocTest", tempDirectory)
        File.createTempFile("file1", "metridocTest", tempDirectory)
        File.createTempFile("file2", "metridocTest", tempDirectory)
        File.createTempFile("file3", "metridocTest", tempDirectory)
        File.createTempFile("file4", "metridocTest", tempDirectory)
        File.createTempFile("file5", "metridocTest", tempDirectory)
        File.createTempFile("file6", "metridocTest", tempDirectory)
    }
}





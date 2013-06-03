/**
 * a redo of this <a href:"http://saltnlight5.blogspot.com/2012/08/getting-started-with-apache-camel-using.html">blog</a>
 * using CamelGLite
 */
@Grab("com.github.camel-glite:camel-glite:0.2.3") @Grab('org.slf4j:slf4j-simple:1.6.6') @GrabResolver(name = 'camel-glite', root = 'http://jcenter.bintray.com/')
import camelscript.CamelGLite
import org.apache.camel.Exchange

def gLite = new CamelGLite()

gLite.with {
    consumeTillDone("timer://jdkTimer?period=1000", 2000) { Exchange exchange ->
        send("log://camelLogger?level=INFO", "hello from camel")
    }
}

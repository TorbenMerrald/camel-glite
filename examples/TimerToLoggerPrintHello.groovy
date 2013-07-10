/**
 * a redo of this <a href:"http://saltnlight5.blogspot.com/2012/08/getting-started-with-apache-camel-using.html">blog</a>
 * using CamelGLite
 */
@Grab("com.github.camel-glite:camel-glite:0.5.RC1")
@Grab("org.apache.camel:camel-groovy:2.11.0")
@Grab("org.apache.camel:camel-core:2.11.0")
@Grab('org.slf4j:slf4j-simple:1.6.6')
@GrabResolver(name = 'camel-glite', root = 'http://jcenter.bintray.com/')
import camelscript.CamelGLite

new CamelGLite().with {
    consumeForever("timer://jdkTimer?period=1000") {
        send("log://camelLogger?level=INFO").process {
            println "hello world"
        }
    }
}

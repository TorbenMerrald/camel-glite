@Grab("com.github.camel-glite:camel-glite:0.4.1")
@Grab('org.slf4j:slf4j-simple:1.6.6')
@Grab('org.apache.activemq:activemq-camel:5.6.0')
@Grab('org.apache.activemq:activemq-core:5.6.0')
//this needs to be explicitly set since activemq-camel will use a different version
@Grab('org.apache.camel:camel-jms:2.11.0')
@GrabResolver(name = 'camel-glite', root = 'http://jcenter.bintray.com/')
import camelscript.CamelGLite
import org.apache.activemq.camel.component.ActiveMQComponent

def glite = new CamelGLite()
glite.camelContext.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://localhost?broker.persistent=false"))
println "starting jms consumption"
def camelThread = Thread.start {
    glite.consumeTillDone("activemq:queue:foo", 3000) { String greeting ->
        println "Hi number [$greeting]"
    }
}

println "sending data to foo"
(1..10).each{ glite.send("activemq:queue:foo", it) }
camelThread.join()
package camelscript

import spock.lang.Specification

/**
 * Created with IntelliJ IDEA on 6/4/13
 * @author Tommy Barker
 */
class BindingBackedRegistrySpec extends Specification {

    def "lookup should work against the binding"() {
        given: "a binding with 2 integer variables"
        def binding = new Binding()
        binding.fooInt = 1
        binding.barInt = 2

        and: "2 string variables"
        binding.fooString = "foo"
        binding.barString = "bar"

        and: "a registry with the binding backing it"
        def registry = new BindingBackedRegistry(binding)

        when: "looking up Integers"
        def result = registry.lookupByType(Integer)

        then: "2 items are returned with appropriate values"
        1 == result.fooInt
        2 == result.barInt

        when: "lookup up Strings"
        result = registry.lookupByType(String)

        then: "2 items are returned with appropriate values"
        "foo" == result.fooString
        "bar" == result.barString

    }
}
